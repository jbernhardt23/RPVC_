package com.example.josebernhardt.rpvc_;

import android.app.FragmentManager;
import android.app.Notification;
import android.graphics.Color;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


/**
 * @author Jose Bernhardt
 */

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static List<Car> CarList = new ArrayList<>();
    private double lat, lon, carSpeed;
    private String carId;
    private boolean flagCommand = false;
    private boolean flagMap = false;
    private boolean flagBT = false;
    private boolean flagConnected = false;
    private boolean flag2 = false;
    public static boolean writeFlag = false;
    private NavigationView navigationView;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice;
    private Intent enableBtIntent;
    Thread mConnectThread;
    Thread mConnectedThread;
    Thread mSendData;
    private ProgressDialog dialog, dialog1;
    private double frontSensorNum = 0;
    private double backSensorNum = 0;
    private double distance = 400;
    private Double frontDataPercentage = 0.0;
    private Double backDataPercentage = 0.0;
    private int dataToDisplay = 0;
    private static final String CARD_ID = "Xbee3";
    private Snackbar snackbar2, snackbarOffline;
    private TwoProgressDialog twoProgressDialog;
    private String frontSensor ="0";
    private String backSensor ="0";

    //Two instances of the fragments objects we are using
    private GmapFragment map = new GmapFragment();
    private CommandCenter command = new CommandCenter();

    private Handler pdCanceller = new Handler();
    private Handler deleteOldCarsHandler = new Handler();
    private Handler refreshListHandler = new Handler();
    private Car myCar = GmapFragment.myCar;
    private TextView statusText;
    private NotificationManager notificationManager;
    private NotificationManager notificationManager2;
    private NotificationCompat.Builder mBuilder;
    private NotificationCompat.Builder leftCarBuilder;
    private AlertDialog panicAlertDialog;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //BT filters to manage connection status
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(mReceiver, filter);


        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //Mark first item of the drawer selected
        navigationView.getMenu().getItem(0).setChecked(true);

        //Start Maps Fragment
        android.app.FragmentManager fm = getFragmentManager();
        fm.beginTransaction().replace(R.id.content_frame, new GmapFragment()).commit();
        flagMap = true;

        //BT object
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported",
                    Toast.LENGTH_LONG).show();
        }

        //Checking if BT is on
        if (!mBluetoothAdapter.isEnabled()) {
            enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        //Online/offline message
        snackbarOffline = Snackbar.make(navigationView, "You're Offline!", Snackbar.LENGTH_INDEFINITE);
        View  snackbarView = snackbarOffline.getView();
        snackbarView.setBackgroundColor(Color.parseColor("#b71c1c"));
        snackbarOffline.show();

        //Setting notification objects for alerts of proximity
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(MainActivity.this);
        mBuilder.setSmallIcon(R.drawable.car_icon);
        mBuilder.setSound(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.censor));

        //Object for car left notification manger
        notificationManager2 = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        leftCarBuilder = new NotificationCompat.Builder(MainActivity.this);
        leftCarBuilder.setSmallIcon(R.drawable.car_icon);
        leftCarBuilder.setContentTitle("Car out of range");
        leftCarBuilder.setPriority(Notification.PRIORITY_MAX);

        //Setting panic dialog to advise user of what will happen
        panicAlertDialog = new AlertDialog.Builder(MainActivity.this).create();
        panicAlertDialog.setTitle("Sending panic alert!");
        panicAlertDialog.setMessage("Marking ON will alert all Cars of you current" +
                " situation");

        //Starting checks cars runnable to see who is active
        deleteOldCarsHandler.postDelayed(checkCarsRunnable, 5000);
        refreshListHandler.postDelayed(refreshListRunnable, 1000);

        //Progress dialogs
        twoProgressDialog = new TwoProgressDialog(this);
        twoProgressDialog.setMessage("Sensors Monitor");
        twoProgressDialog.setCancelable(true);


    }


    @Override
    protected void onResume() {
        super.onResume();

    }


    /**
     * Handling toggle events that will send boradcast whenever a Car is on panic
     * @param view
     */
    public void onToggleClicked(View view){

            if(((ToggleButton) view).isChecked()) {

                panicAlertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                myCar.setCarCrashed("Down");
                            }
                        });
                panicAlertDialog.show();

            } else {
                myCar.setCarCrashed("Active");
            }


    }


    /**
     * Handles the touch event when floating notification gets clicked
     * @param v Current view of the App
     */
    public void floatingAction(View v){

        twoProgressDialog.show();

        /**
         * Thread to update real time sensor monitor
         */
        Thread updateProgressThread = new Thread() {
            @Override
            public void run() {
                try {

                    while(twoProgressDialog.isShowing()){
                        twoProgressDialog.setProgress(frontDataPercentage.intValue());
                        twoProgressDialog.setSecondaryProgress(backDataPercentage.intValue());
                        sleep(100);
                    }

                    frontDataPercentage = 0.0;
                    backDataPercentage = 0.0;

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        updateProgressThread.start();



    }



    /***
     * Sending Local Broadcast on app to send updates of cars
     * information to the Car List. This is sent every second.
     */
    private Runnable refreshListRunnable = new Runnable() {
        @Override
        public void run() {

            Intent intent = new Intent("refresh_data");
            LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(intent);
            refreshListHandler.postDelayed(this, 1000);
        }
    };



    /***
     * Handler to check if cars are still active on the network
     * Dead Interval time 5 Seconds
     * Cars have to keep sending position within this interval
     */

    private Runnable checkCarsRunnable = new Runnable() {
        @Override
        public void run() {

            if (!CarList.isEmpty()) {
                for (int i = 0; i < CarList.size(); i++) {
                    if (!CarList.get(i).isTimer()) {
                        leftCarBuilder.setContentText(CarList.get(i).getCarId()
                                + " has left network");
                        notificationManager2.notify(0, leftCarBuilder.build());
                        CarList.remove(i);
                    }
                }
                for (int i = 0; i < CarList.size(); i++) {
                    CarList.get(i).setInicialTimer(false);
                }
            }
            deleteOldCarsHandler.postDelayed(this, 5000);
        }
    };


    /***
     * Main handler that recieves all information from all
     * cars joining the network. Responsable of updating
     * cars position and sensors real time. Also calculates
     * distance from incoming cars from our current position and
     * provides heads up alerts.
     */
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            byte[] writeBuf = (byte[]) msg.obj;
            int begin = (int) msg.arg1;
            int end = (int) msg.arg2;
            statusText = (TextView) findViewById(R.id.alertWhereText);

            String carID;

            switch (msg.what) {
                case 1:
                    lat = 0;
                    lon = 0;
                    //Converting buffer to string
                    String writeMessage = new String(writeBuf);
                    writeMessage = writeMessage.substring(begin, end);

                    try {
                        myCar = GmapFragment.myCar;
                        if(writeMessage.contains("/")) {
                            writeMessage = writeMessage.replace("/", ",");
                            String[] CarsData = writeMessage.split(",");

                            //Assigning temp info of incoming car
                            carID = CarsData[0];
                            String tempLat = CarsData[1];
                            String tempLon = CarsData[2];
                            String carPanic = CarsData[3];
                            String carBearing = CarsData[4];

                            try {

                                //Assigning temp sensors data
                                String[] sensorData = CarsData[4].split("%");
                                frontSensor = sensorData[0];
                                backSensor = sensorData[1];
                                frontSensorNum = Double.parseDouble(frontSensor);
                                backSensorNum = Double.parseDouble(backSensor);
                                if(!(frontSensorNum == 0.0))
                                    frontDataPercentage = ((distance - frontSensorNum )/ distance) * 100;
                                if(!(backSensorNum == 0.0))
                                backDataPercentage = ((distance - backSensorNum) / distance) * 100;


                            } catch (Exception e) {
                                System.out.println("Bad sensor data");
                            }

                            lat = Double.parseDouble(tempLat);
                            lon = Double.parseDouble(tempLon);

                            if (!CarList.isEmpty() ) {
                                for (int i = 0; i < CarList.size(); i++) {
                                    if (CarList.get(i).getCarId().equals(carID)) {
                                        CarList.get(i).setLon(lon);
                                        CarList.get(i).setLat(lat);
                                        CarList.get(i).setCarCrashed(carPanic);
                                        CarList.get(i).setInicialTimer(true);
                                        CarList.get(i).setBearing(Float.parseFloat(carBearing));

                                        //Compare if current car is close to my Car
                                        Location nextCarPosition = new Location("Point A");
                                        nextCarPosition.setLatitude(lat);
                                        nextCarPosition.setLongitude(lon);

                                        Location myCarsPosition = new Location("Point B");
                                        myCarsPosition.setLatitude(myCar.getLat());
                                        myCarsPosition.setLongitude(myCar.getLon());

                                        float distanceBetween = myCarsPosition.distanceTo(nextCarPosition);
                                        float backAlert = 0.0f;
                                        float rightAlert = 0.0f;
                                        float leftAlert = 0.0f;

                                        if(CarList.get(i).getBearing() >= 0 && CarList.get(i).getBearing() <= 90){

                                            backAlert = myCar.getBearing() + 180;
                                            rightAlert = myCar.getBearing() + 270;
                                            leftAlert = myCar.getBearing() + 90;

                                        }else if(CarList.get(i).getBearing() >= 90 && CarList.get(i).getBearing() <= 180){
                                            backAlert = myCar.getBearing() + 180;
                                            rightAlert = myCar.getBearing() - 90;
                                            leftAlert = myCar.getBearing() + 90;

                                        }else if(CarList.get(i).getBearing() >= 180 && CarList.get(i).getBearing() <= 270){

                                            backAlert = myCar.getBearing() - 180;
                                            rightAlert = myCar.getBearing() - 90;
                                            leftAlert = myCar.getBearing() + 90;

                                        }else if(CarList.get(i).getBearing() >= 270 && CarList.get(i).getBearing() <= 360){
                                            backAlert = myCar.getBearing() - 180;
                                            rightAlert = myCar.getBearing() - 270;
                                            leftAlert = myCar.getBearing() - 90;

                                        }



                                        if ((distanceBetween <20)) {
                                            mBuilder.setContentTitle("Car! " + carID + " getting close");
                                            mBuilder.setPriority(Notification.PRIORITY_MAX);
                                            mBuilder.setContentText("Distance: " +
                                                    String.valueOf(String.format("%.2f",CarList.get(i).getDistanceBetween())) + " m");
                                            mBuilder.setSound(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.censor));
                                            notificationManager.notify(0, mBuilder.build());

                                        }else if(distanceBetween >= 20 && distanceBetween <=30){

                                            if(Float.parseFloat(carBearing) >= (myCar.getBearing()  -15)
                                                    && Float.parseFloat(carBearing)<= (myCar.getBearing() + 15)){
                                                statusText.setText("Vengo de frente tuyo");
                                                mBuilder.setContentTitle(CarList.get(i).getCarId()
                                                                + " coming by the front");
                                                mBuilder.setPriority(Notification.PRIORITY_MAX);
                                                mBuilder.setContentText("Distance: " +
                                                        String.valueOf(String.format("%.2f",CarList.get(i).getDistanceBetween())) + " m");
                                                mBuilder.setSound(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.carfront));
                                                notificationManager.notify(0, mBuilder.build());
                                            }else if(Float.parseFloat(carBearing) >= (backAlert - 15)
                                                    && Float.parseFloat(carBearing)<= (backAlert + 15)) {
                                                statusText.setText("Vengo por atra de ti");
                                                mBuilder.setContentTitle(CarList.get(i).getCarId()
                                                        + " coming from behind");
                                                mBuilder.setPriority(Notification.PRIORITY_MAX);
                                                mBuilder.setContentText("Distance: " +
                                                        String.valueOf(String.format("%.2f",CarList.get(i).getDistanceBetween())) + " m");
                                                mBuilder.setSound(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.carbehind));
                                                notificationManager.notify(0, mBuilder.build());

                                            }else if(Float.parseFloat(carBearing) >= (leftAlert  - 15)
                                                    && Float.parseFloat(carBearing)<= (leftAlert + 15)) {
                                                statusText.setText("Vengo por la izquiera");
                                                mBuilder.setContentTitle(CarList.get(i).getCarId()
                                                        + " coming from your left");
                                                mBuilder.setPriority(Notification.PRIORITY_MAX);
                                                mBuilder.setContentText("Distance: " +
                                                        String.valueOf(String.format("%.2f",CarList.get(i).getDistanceBetween())) + " m");
                                                mBuilder.setSound(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.carleft));
                                                notificationManager.notify(0, mBuilder.build());

                                            }else if(Float.parseFloat(carBearing) >= (rightAlert  - 15)
                                                    && Float.parseFloat(carBearing)<= (rightAlert + 15)){
                                                statusText.setText("Vengo por la derecha");
                                                mBuilder.setContentTitle(CarList.get(i).getCarId()
                                                        + " coming from your left");
                                                mBuilder.setPriority(Notification.PRIORITY_MAX);
                                                mBuilder.setContentText("Distance: " +
                                                        String.valueOf(String.format("%.2f",CarList.get(i).getDistanceBetween())) + " m");
                                                mBuilder.setSound(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.carright));
                                                notificationManager.notify(0, mBuilder.build());

                                            } else{
                                                statusText.setText("No soy amenaza");
                                            }

                                            CarList.get(i).setDistanceBetween(distanceBetween);
                                            distanceBetween = 0;


                                        }else{
                                            CarList.get(i).setDistanceBetween(distanceBetween);
                                            distanceBetween = 0;

                                        }

                                        break;
                                    } else if (CarList.size() - 1 == i && carId != CARD_ID && carID.contains("Xbee")) {
                                        Car newCar = new Car(lat, lon, carID, false, "Active");
                                        CarList.add(newCar);
                                        
                                    }
                                }
                            } else if (carID.contains("Xbee")) {
                                //Here we add a new car to the newtork
                                Car newCar = new Car(lat, lon, carID, false, "Active");
                                CarList.add(newCar);
                                mBuilder.setContentTitle("New car on range");
                                mBuilder.setPriority(Notification.PRIORITY_MAX);
                                mBuilder.setContentText("Car: " + carID
                                        + " has joined!");
                                mBuilder.setSound(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.censor));
                                notificationManager.notify(0, mBuilder.build());


                            }
                        }else if(writeMessage.contains("!")){
                            try{
                                //This is the scenario when we are not connected to the network
                                // and we need to get sensors information.
                                String[] sensorData = writeMessage.split("!");
                                frontSensor = sensorData[0];
                                backSensor = sensorData[1];
                                frontSensorNum = Double.parseDouble(frontSensor);
                                backSensorNum = Double.parseDouble(backSensor);
                                if(!(frontSensorNum == 0.0)) {
                                    frontDataPercentage = ((distance - frontSensorNum) / distance) * 100;
                                }
                                if(!(backSensorNum == 0.0)) {
                                    backDataPercentage = ((distance - backSensorNum) / distance) * 100;
                                }

                            }catch(Exception e){

                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;

                case 2:
                    Snackbar snackbar = Snackbar.make(navigationView, "Bluetooth Ready, Device Paired: " +
                                    mDevice.getName(),
                            Snackbar.LENGTH_INDEFINITE);
                    snackbar.show();
                    break;
                case 3:
                    snackbar2 = Snackbar.make(navigationView, "You're Online!", Snackbar.LENGTH_INDEFINITE);
                    snackbar2.setActionTextColor(Color.GREEN);
                    View  snackbarView = snackbar2.getView();
                    snackbarView.setBackgroundColor(Color.parseColor("#1B5E20"));
                    snackbar2.show();
                    break;
            }
        }
    };



    //Timeout if connection with Bluetooth not established
    Runnable progressRunnable = new Runnable() {

        @Override
        public void run() {
            dialog1.cancel();
            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle("Connection failed");
            alertDialog.setMessage("Unable to connect to module, please try again.");
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        }
    };


    //The BroadcastReceiver that listens for bluetooth broadcasts when disconnected
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //  BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                if (!flagBT) {
                    enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, 1);
                } else {
                    Snackbar snackbar = Snackbar.make(navigationView, "Bluetooth connection lost! Re-trying... ",
                            Snackbar.LENGTH_INDEFINITE);
                    snackbar.show();
                    flagBT = false;
                    mConnectedThread.interrupt();
                    mSendData.interrupt();
                    snackbar2.dismiss();
                    snackbarOffline.show();


                }
            }
        }
    };

    //Thread for the recieving cars position
    private class ConnectedThread extends Thread {

        private BluetoothSocket mmSocket;
        private InputStream mmInStream;
        private OutputStream mmOutStream;
        private boolean flag = false;

        public ConnectedThread(BluetoothSocket socket) {

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;

        }

        public void run() {

            byte[] buffer = new byte[1024];
            int begin = 0;
            int beginSensor = 0;
            int bytes = 0;
            //Dismiss dialog called on the drawer
            dialog.dismiss();
            snackbarOffline.dismiss();
            mHandler.obtainMessage(3).sendToTarget();


            while (true && !isInterrupted()) {

                try {

                    bytes += mmInStream.read(buffer, bytes, buffer.length - bytes);
                    for (int i = begin; i < bytes; i++) {
                        if (buffer[i] == "#".getBytes()[0]) {
                           mHandler.obtainMessage(1, begin, i, buffer).sendToTarget();
                            begin = i + 1;
                            if (i == bytes - 1) {
                                bytes = 0;
                                begin = 0;
                            }
                        }
                    }

                } catch (IOException e) {
                    break;
                }
            }
        }

    }

    //Thread sending Cars info
    private class SendData extends Thread {

        private BluetoothSocket mmSocket;
        private OutputStream mmOutStream;

        public SendData(BluetoothSocket socket) {

            mmSocket = socket;
            OutputStream tmpOut = null;
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }
            mmOutStream = tmpOut;

        }

        public void run() {
            Car myCar;


            while (true && !isInterrupted()) {

                myCar = GmapFragment.myCar;
                if (myCar.getLat() != 0 && myCar.getLon() != 0 && myCar != null) {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    try {
                        //Preparing buffer to be sent
                        outputStream.write("/".getBytes());
                        outputStream.write(myCar.getCarId().getBytes());
                        outputStream.write(",".getBytes());
                        outputStream.write(myCar.toString().getBytes());
                        outputStream.write(",".getBytes());
                        outputStream.write(myCar.getCarCrashed().getBytes());
                        outputStream.write(",".getBytes());
                        outputStream.write(String.valueOf(myCar.getBearing()).getBytes());

                        byte dataSend[] = outputStream.toByteArray();
                        write(dataSend);

                    } catch (IOException e) {
                        e.getMessage();
                    }

                }
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes) {
            try {

                mmOutStream.write(bytes);

            } catch (IOException e) {
            }
        }

    }

    //Thread for the Blueetooth
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");


        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
            }
            mmSocket = tmp;


        }

        public void run() {

            mBluetoothAdapter.cancelDiscovery();
            try {
                mmSocket.connect();
                flagBT = true;
                mConnectedThread = new ConnectedThread(mmSocket);
                mSendData = new SendData(mmSocket);


                dialog1.dismiss();
                mHandler.obtainMessage(2).sendToTarget();


            } catch (IOException connectException) {
                try {
                    pdCanceller.postDelayed(progressRunnable, 5000);

                    mmSocket.close();
                } catch (IOException closeException) {
                }
                return;
            }
        }

    }



    @Override
    public void onBackPressed() {

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {

            //Checking if we have Map on the backStack
            if (getFragmentManager().findFragmentByTag("MapFragment") != null) {
                //Marking items from the Navigation as selected
                navigationView.getMenu().getItem(1).setChecked(false);
                navigationView.getMenu().getItem(0).setChecked(true);
                //Getting de last fragment
                getFragmentManager().popBackStack();
                flagCommand = false;
            } else {
                super.onBackPressed();
            }
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        android.app.FragmentManager fm = getFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();



        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera && flagMap == false) {
            flagMap = true;
            flagCommand = false;
            //Verifying if we have the Map on the BackStack to call it
            if (getFragmentManager().findFragmentByTag("MapFragment") != null) {
                getFragmentManager().popBackStack();

            }


        } else if (id == R.id.nav_gallery && flagCommand == false) {

            flagMap = false;
            flagCommand = true;
            //Verifying if we have the Command Center fragment on the Stack to call it
            if (getFragmentManager().findFragmentByTag("CommandFragment") != null) {
                getFragmentManager().popBackStackImmediate("CommandFragment", FragmentManager
                        .POP_BACK_STACK_INCLUSIVE);
            }
            //Calling Command Center when Drawer touched and saving Map Fragment on the stack
            //transaction.setCustomAnimations(android.R.animator.fade_in, android.R.animator.fade_out);
            transaction.replace(R.id.content_frame, command);
            // transaction.add(command, "CommandFragment");
            transaction.addToBackStack("CommandFragment");
            transaction.add(map, "MapFragment");
            transaction.addToBackStack("MapFragment");
            transaction.commit();

        } else if (id == R.id.nav_share) {
            //List of devices
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    if (device.getName().equals("HC-06")) {
                        dialog1 = ProgressDialog.show(MainActivity.this, "", "Establishing connection with " +
                                "Bluetooth...", true, false);

                        mDevice = device;
                        mConnectThread = new ConnectThread(mDevice);
                        mConnectThread.start();


                    }
                }

            } else {
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("Bluetooth");
                alertDialog.setMessage("The module has not being paired with your phone or BT is OFF");
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();

            }

        } else if (id == R.id.nav_send) {
            flag2 = true;
            if (flagBT) {
                dialog = ProgressDialog.show(MainActivity.this, "", "Initiating ...", true, false);
                mConnectedThread.start();
                mSendData.start();
                flag2 = true;

            } else {

                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("Bluetooth not initiated!");
                alertDialog.setMessage("Please make sure bluetooth connection is ready.");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


}