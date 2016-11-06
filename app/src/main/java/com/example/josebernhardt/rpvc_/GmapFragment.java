package com.example.josebernhardt.rpvc_;

import android.Manifest;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.TextView;


import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


import java.util.ArrayList;
import java.util.List;




/**
 * Created by jose on 30/04/16.
 */
public class GmapFragment extends Fragment implements OnMapReadyCallback, SensorEventListener {

    private  List<Car> CarList;
    public static Car myCar = new Car();
    private Marker marker, mMarker;
    MapFragment fragment;
    private GoogleMap gMap;
    private ProgressDialog dialog;
    View v;
    Thread putCar;
    private List<Marker> markersList = new ArrayList<>();
    private LocationManager locationManager;
    private LocationListener gpsListener, networkListener;
    private double latitude;
    private double longitude;
    private boolean gpsProviderReady = false;
    private boolean flag = false;
    private static final String CARD_ID = "Xbee1";
    private AlertDialog.Builder builder;
    private AlertDialog alertDialog;
    private ProgressDialog dialogMapLoading;
    private Bitmap icon;
    private SensorManager mSensorManager;
    private float degree = 0f;

    // record the compass picture angle turned
    private float currentDegree = 0f;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

      //  dialogMapLoading  = ProgressDialog.show(getActivity(), "", "Getting your position...", true, false);
        icon = BitmapFactory.decodeResource(getActivity().getApplicationContext().getResources(),
                R.drawable.mapicon);

        // initialize your android device sensor capabilities
        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
    }



    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        // for the system's orientation sensor registered listeners
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_UI);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        try {
            v = inflater.inflate(R.layout.map_fragment, container, false);
        } catch (Exception e) {

        }
        return v;

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

            if(gMap == null) {
                //Setting Map
                fragment = (MapFragment) getChildFragmentManager().findFragmentById(R.id.map);
                fragment.getMapAsync(this);
            }

          /*  //Getting map in case is Null
            if (gMap == null) {
                gMap = ((MapFragment) getFragmentManager().
                        findFragmentById(R.id.map)).getMap();
            }*/

    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        //Set up of dialog for user to be aware of GPS settings to be on
        builder = new AlertDialog.Builder(getActivity());
        gMap = googleMap;

        //TODO this is not working properly, app crash requesting permissions first time
        permissions();

        putCar = new putCar();
        putCar.start();

        LatLng pos = new LatLng(0, 0);
        marker = googleMap.addMarker(new MarkerOptions()
                .title("Car:" + " " + CARD_ID)
                .position(pos)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.mapicon))
                .rotation(degree));

        googleMap.animateCamera(CameraUpdateFactory.zoomTo(17));
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.getUiSettings().setZoomGesturesEnabled(true);
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        //Location Listener for both providers to take the best one
        networkListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                if(!gpsProviderReady) {

                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    myCar.setCarId(CARD_ID);
                    myCar.setLat(latitude);
                    myCar.setLon(longitude);
                    myCar.setCurrentSpeed(location.getSpeed());
                    LatLng pos = new LatLng(latitude, longitude);
                    marker.setPosition(pos);
                    marker.setSnippet("Car Speed: " + (location.getSpeed() * 3600 / 1000 + "km/h"));
                    marker.hideInfoWindow();
                    marker.showInfoWindow();
                    marker.setRotation(degree);
                    googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(latitude, longitude)));
                    googleMap.animateCamera(CameraUpdateFactory.zoomTo(17));
            }

            }

            @Override
            public void onProviderDisabled(String s) {


            /*    AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
                alertDialog.setTitle("Network provider lost");
                alertDialog.setMessage("Seems that you have lost connection with your carrier" +
                        " Please check your signal. ");
                alertDialog.setCancelable(false);
                alertDialog.setCanceledOnTouchOutside(false);
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                Intent intent = new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
                                startActivity(intent);
                            }
                        });
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                alertDialog.show();*/


            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }
        };
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, networkListener);

        gpsListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                if(location.getAccuracy() > 0 && location.getAccuracy() < 20) {

                    gpsProviderReady = true;
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    System.out.println(location.getAccuracy() + " " + location.getProvider());
                    myCar.setCarId(CARD_ID);
                    myCar.setLat(latitude);
                    myCar.setLon(longitude);
                    myCar.setCurrentSpeed(location.getSpeed());
                    marker.setRotation(degree);
                    LatLng pos = new LatLng(latitude, longitude);
                    marker.setPosition(pos);
                    marker.setSnippet("Car Speed: " + (location.getSpeed() * 3600 / 1000 + "km/h"));
                    //    marker.hideInfoWindow();
                    marker.showInfoWindow();
                    googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(latitude, longitude)));
                    googleMap.animateCamera(CameraUpdateFactory.zoomTo(17));
                }else{
                    gpsProviderReady =false;
                }

            }

            @Override
            public void onProviderDisabled(String s) {

                AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
                alertDialog.setTitle("GPS Disable!");
                alertDialog.setMessage("We require to have GPS enable at all time" +
                        " to ensure best accuracy possible. ");
                alertDialog.setCancelable(false);
                alertDialog.setCanceledOnTouchOutside(false);
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(intent);
                            }
                        });
                alertDialog.show();

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }
        };
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsListener);


    }

    /**
     * Sensor listener methos that recieves all new sensor update changes
     * @param event
     */
    @Override
    public void onSensorChanged(SensorEvent event) {

        // get the angle around the z-axis rotated
        degree = Math.round(event.values[0]);

        TextView tvHeading = (TextView) getView().findViewById(R.id.sensorText);
        tvHeading.setText("Heading: " + Float.toString(degree) + " degrees");

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not in use

    }

    /***
     * Main handler that takes care of drawing the markers on the Map
     * depending on the current List of Cars
     */
    private  Handler displayHandler = new Handler() {
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    CarList = MainActivity.CarList;
                    //Adding markers for every Car on the Map

                    if (!markersList.isEmpty()) {
                        for (int i = 0; i < CarList.size(); i++) {
                            if (markersList.get(i).getTitle().contains(CarList.get(i).getCarId())
                                    && CarList.size() == markersList.size()) {

                                LatLng pos = new LatLng(CarList.get(i).getLat(), CarList.get(i).getLon());
                                markersList.get(i).setPosition(pos);
                            } else {
                                gMap.clear();
                                markersList.clear();

                                LatLng pos = new LatLng(latitude, longitude);
                                marker = gMap.addMarker(new MarkerOptions()
                                        .title("Car:" + " " + CARD_ID)
                                        .position(pos)
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                                for (int k = 0; k < CarList.size(); k++) {
                                    LatLng pos2 = new LatLng(CarList.get(k).getLat(), CarList.get(k).getLon());
                                    mMarker = gMap.addMarker(new MarkerOptions()
                                            .title("Car:" + " " + CarList.get(k).getCarId())
                                            .snippet("Car Speed: " + CarList.get(k).getCurrentSpeed() + "KPH")
                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                                            .position(pos2));

                                    mMarker.showInfoWindow();
                                    markersList.add(mMarker);
                                }
                                break;
                            }

                        }
                    } else {
                        for (int i = 0; i < CarList.size(); i++) {
                            LatLng pos = new LatLng(CarList.get(i).getLat(), CarList.get(i).getLon());
                            mMarker = gMap.addMarker(new MarkerOptions()
                                    .title("Car:" + " " + CarList.get(i).getCarId())
                                    .snippet("Car Speed: " + CarList.get(i).getCurrentSpeed() + "KPH")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                                    .position(pos));

                            mMarker.showInfoWindow();
                            markersList.add(mMarker);
                        }
                    }

                    break;
                case 2:
                    gMap.clear();
                    markersList.clear();


                    LatLng pos = new LatLng(latitude, longitude);
                    marker = gMap.addMarker(new MarkerOptions()
                            .title("Car:" + " " + CARD_ID)
                            .position(pos)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                    marker.showInfoWindow();

                    break;
            }
        }
    };

    private class putCar extends Thread {


        public void run() {

            CarList = MainActivity.CarList;
            while (!isInterrupted()) {
                if (!CarList.isEmpty() && gMap != null) {
                    displayHandler.obtainMessage(1).sendToTarget();

                }else if(!markersList.isEmpty()){
                    displayHandler.obtainMessage(2).sendToTarget();
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }


    private void permissions() {
        //Handling SDK 23 permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED && ActivityCompat
                    .checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.INTERNET
                }, 10);

            } else {
             //   gMap.setMyLocationEnabled(true);
                locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

            }
        }

    }




    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {

            case 10:

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                 //   gMap.setMyLocationEnabled(true);
                    locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
                    flag = true;
                }
                return;
        }

    }



}



