package com.example.josebernhardt.rpvc_;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;



public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static List<Car> CarList = new ArrayList<>();
    public double latitude, longitude;
    double lat, lon, carSpeed;
    int carId;
    private boolean flagCommand = false;
    private boolean flagMap = false;
    private boolean flagBT = false;
    private boolean flagConnected = false;
    private boolean flag2 = false;
    public static boolean writeFlag = false;
    NavigationView navigationView;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice mDevice;
    Thread mConnectThread;
    Thread mConnectedThread;
    ProgressDialog dialog;
    ProgressDialog dialog1;
    Button testBtn;



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


        //Test button
        testBtn = (Button) findViewById(R.id.test);

        //BT object
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported",
             Toast.LENGTH_LONG).show();
        }

        //Checking if BT is on
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
        //Sending test data
        addCar(16, -45.58, 45, 1);
        addCar(16.58, -45.58, 45, 2);
        addCar(16.58, -45.58, 45, 3);
        addCar(16, -45.58, 45, 4);
        addCar(16.58, -45.58, 45, 5);
        addCar(16.58, -45.58, 45, 6);
        addCar(16, -45.58, 45, 7);
        addCar(16.58, -45.58, 45, 8);
        addCar(latitude, longitude, 45, 9);


    }
    public void showText(String text){
        Toast.makeText(this, text,
                Toast.LENGTH_LONG).show();
    }

    //Handler to get DATA and use it on the UI
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            byte[] writeBuf = (byte[]) msg.obj;
            int begin = (int)msg.arg1;
            int end = (int)msg.arg2;

            switch(msg.what) {
                case 1:
                    String writeMessage = new String(writeBuf);
                    writeMessage = writeMessage.substring(begin, end);
                    showText(writeMessage);
                    break;
            }
        }
    };

    //Displaying information of thread status
    Handler displayHandler = new Handler()
    {
        public void handleMessage(Message message)
        {
            switch (message.what) {
                case 1:
                Snackbar snackbar = Snackbar.make(navigationView, "Bluetooth Ready, Device Paired: " +
                        mDevice.getName(),
                        Snackbar.LENGTH_LONG);
                snackbar.show();
                    break;
                case 2:
                    Snackbar snackbar2 = Snackbar.make(navigationView, "Connection successful!", Snackbar.LENGTH_LONG);
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

    Handler pdCanceller = new Handler();

    //The BroadcastReceiver that listens for bluetooth broadcasts
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
          //  BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                 if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                Snackbar snackbar = Snackbar.make(navigationView, "Bluetooth connection lost! ",
                        Snackbar.LENGTH_LONG);
                snackbar.show();
                flagBT = false;
                mConnectedThread.interrupt();
            }
        }
    };

    //Thread for the recieving and sending part
    private class ConnectedThread extends Thread {

        private  BluetoothSocket mmSocket;
        private  InputStream mmInStream ;
        private  OutputStream mmOutStream;
        String data = "*";
        String endData = "n";

        public ConnectedThread(BluetoothSocket socket) {

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;

        }

        public void run() {

                byte[] buffer = new byte[1024];
                int begin = 0;
                int bytes = 0;
                //Dismiss dialog called on the drawer
                dialog.dismiss();
                displayHandler.obtainMessage(2).sendToTarget();

                testBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        write(data.getBytes());
                        write(CarList.get(0).toString().getBytes());
                        CarList.remove(CarList.size() - 1);

                    }
                });


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
                                    write(endData.getBytes());
                                }
                            }
                        }
                    } catch (IOException e) {
                        break;
                    }
                }
        }
        public void write(byte[] bytes) {
            try {

                mmOutStream.write(bytes);

            } catch (IOException e) { }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
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
            } catch (IOException e) { }
            mmSocket = tmp;


        }
        public void run() {

            mBluetoothAdapter.cancelDiscovery();
                try {
                    mmSocket.connect();
                    flagBT = true;
                    mConnectedThread = new ConnectedThread(mmSocket);
                    dialog1.dismiss();
                    displayHandler.obtainMessage(1).sendToTarget();

                } catch (IOException connectException) {
                    try {
                        pdCanceller.postDelayed(progressRunnable, 5000);
                        mmSocket.close();
                    } catch (IOException closeException) {}
                    return;
                }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }


    }

    //AddCar method
    public void addCar(double lat, double lon, double carSpeed, int carId ){


        Car car = new Car(lat,lon,carSpeed,carId);
        CarList.add(car);


    }


    @Override
    public void onBackPressed() {

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {

            //Checking if we have Map on the backStack
            if (getFragmentManager().findFragmentByTag("MapFragment") != null){
                //Marking items from the Navigation as selected
                navigationView.getMenu().getItem(1).setChecked(false);
                navigationView.getMenu().getItem(0).setChecked(true);
                //Getting de last fragment
                getFragmentManager().popBackStack();
                flagCommand = false;
            }else {
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
            FragmentTransaction transaction = fm.beginTransaction();
            transaction.replace(R.id.content_frame, new CommandCenter());
            transaction.add(new CommandCenter(), "CommandFragment");
            transaction.addToBackStack("CommandFragment");
            transaction.add(new GmapFragment(),"MapFragment");
            transaction.addToBackStack("MapFragment");
            transaction.commit();

        } else if (id == R.id.nav_share ) {
            //List of devices
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    if(device.getName().equals("HC-06")) {
                        dialog1 = ProgressDialog.show(MainActivity.this, "", "Establishing connection with " +
                                "Bluetooth...",true,false);

                        mDevice = device;
                        mConnectThread = new ConnectThread(mDevice);
                        mConnectThread.start();

                    }
                }

            }else{
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle("Bluetooth Module");
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
            if(flagBT) {
                dialog = ProgressDialog.show(MainActivity.this, "", "Initiating ...",true,false);
                mConnectedThread.start();
                flag2 = true;

            }else{

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