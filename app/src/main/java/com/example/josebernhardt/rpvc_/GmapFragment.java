package com.example.josebernhardt.rpvc_;

import android.Manifest;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import android.widget.TextView;


import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


import java.util.ArrayList;
import java.util.List;




/**
 * Created by jose on 30/04/16.
 */
public class GmapFragment extends Fragment implements OnMapReadyCallback{

    private  List<Car> CarList;
    public static Car myCar = new Car();
    public static float myAcurracy;
    public  static String myProvider = "";
    private Marker marker, mMarker;
    MapFragment fragment;
    private GoogleMap gMap;
    private ProgressDialog dialog;
    View v;
    Thread putCar;
    private List<Marker> markersList = new ArrayList<>();
    private List<Circle> circleList = new ArrayList<>();
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
    TextView tvHeading;
    private static float GPSbearing;
    private Location prevGPSLoc;
    private Location newGPSLoc;
    private static float networkBearing;
    private Location prevNetworkLoc;
    private Location newNetworkLoc;
    private CameraPosition oldPos, pos;
    private int strokeColor = 0xffff0000;
    //opaque red fill
    private int shadeColor = 0x44ff0000;

    private int mStrokeColor = 0x00008000;
    private int mShadeColor = 0x566D7E00;

    private float circleRadius = 0.2f;
    private Circle circle, mCircle;




    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

      //  dialogMapLoading  = ProgressDialog.show(getActivity(), "", "Getting your position...", true, false);

    }



    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

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
        tvHeading = (TextView) getView().findViewById(R.id.sensorText);

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
        //Initial Set up
        LatLng pos = new LatLng(0, 0);

        marker = googleMap.addMarker(new MarkerOptions()
                .title("Car:" + " " + CARD_ID)
                .position(pos)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.redmapicon)));

        circle = googleMap.addCircle(new CircleOptions()
                .center(pos)
                .radius(0)
                .fillColor(shadeColor)
                .strokeColor(strokeColor).strokeWidth(2));

        googleMap.getUiSettings().setRotateGesturesEnabled(false);
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(19));
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.getUiSettings().setZoomGesturesEnabled(true);
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        //Location Listener for both providers to take the best one
        networkListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                if(!gpsProviderReady) {

                    //Updating position to get Bearing
                    newNetworkLoc= location;
                    if(prevNetworkLoc == null)
                        prevNetworkLoc = location;

                    networkBearing = prevNetworkLoc.bearingTo(newNetworkLoc);
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    myAcurracy = location.getAccuracy();
                    myProvider = location.getProvider();
                    myCar.setCarId(CARD_ID);
                    myCar.setLat(latitude);
                    myCar.setLon(longitude);
                    myCar.setCurrentSpeed(location.getSpeed());
                    myCar.setAccurracy(location.getAccuracy());


                    LatLng pos = new LatLng(latitude, longitude);
                    marker.setPosition(pos);
                    marker.setSnippet("Car Speed: " + String.format("%.2f",location.getSpeed() * 3600 / 1000) + "km/h");

                    circle.setCenter(pos);
                    circle.setRadius(location.getAccuracy());

                    if (!(networkBearing == 0.0)) {
                        if(networkBearing < 0.0){
                            networkBearing = networkBearing + 360;
                            marker.setAnchor(0.5f, 0.5f);
                            marker.setRotation(networkBearing);
                            myCar.setBearing(networkBearing);
                            tvHeading.setText(String.valueOf(myCar.getBearing()));
                        }else{
                            marker.setAnchor(0.5f, 0.5f);
                            marker.setRotation(networkBearing);
                            myCar.setBearing(networkBearing);
                            tvHeading.setText(String.valueOf(myCar.getBearing()));
                        }
                    }


                    marker.setAnchor(0.5f,0.5f);
                    googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(latitude, longitude)));
                  //  googleMap.animateCamera(CameraUpdateFactory.zoomTo(15));

//                        CameraPosition currentPlace = new CameraPosition.Builder()
//                                .target(pos)
//                                .bearing(networkBearing).zoom(18f).build();
//                        googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(currentPlace));


                    prevNetworkLoc = newNetworkLoc;
            }

            }

            @Override
            public void onProviderDisabled(String s) {


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

                if(location.getAccuracy() > 0 && location.getAccuracy() < 15) {

                    //Updating position to get Bearing
                    newGPSLoc = location;
                    if(prevGPSLoc == null){
                        prevGPSLoc = location;
                    }
                    GPSbearing = prevGPSLoc.bearingTo(newGPSLoc);
                    gpsProviderReady = true;
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    myAcurracy = location.getAccuracy();
                    myProvider = location.getProvider();
                    System.out.println(myProvider);
                    myCar.setCarId(CARD_ID);
                    myCar.setLat(latitude);
                    myCar.setLon(longitude);
                    myCar.setCurrentSpeed(location.getSpeed());



                    if (!(GPSbearing == 0.0)) {
                        if(GPSbearing < 0.0){
                            GPSbearing = GPSbearing + 360;
                            marker.setAnchor(0.5f, 0.5f);
                            marker.setRotation(GPSbearing);
                            myCar.setBearing(GPSbearing);
                            tvHeading.setText(String.valueOf(myCar.getBearing()));
                        }else{
                            marker.setAnchor(0.5f, 0.5f);
                            marker.setRotation(GPSbearing);
                            myCar.setBearing(GPSbearing);
                            tvHeading.setText(String.valueOf(myCar.getBearing()));
                        }
                    }


                    LatLng pos = new LatLng(latitude, longitude);
                    marker.setPosition(pos);
                    marker.setSnippet("Car Speed: " + String.format("%.2f",location.getSpeed() * 3600 / 1000) + "km/h");

                    circle.setCenter(pos);
                    circle.setRadius(location.getAccuracy());

                    //    marker.hideInfoWindow();
                   // marker.showInfoWindow();
                    googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(latitude, longitude)));
                   // googleMap.animateCamera(CameraUpdateFactory.zoomTo(25));
//                    CameraPosition currentPlace = new CameraPosition.Builder()
//                            .target(pos)
//                            .bearing(GPSbearing).zoom(18f).build();
//                    googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(currentPlace));
                    prevGPSLoc = newGPSLoc;

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


    /***
     * Main handler that takes care of drawing the cars on the Map
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
                                    && CarList.size() == markersList.size()
                                    && CarList.size() == circleList.size()) {

                                LatLng pos = new LatLng(CarList.get(i).getLat(), CarList.get(i).getLon());
                                markersList.get(i).setPosition(pos);
                                markersList.get(i).setAnchor(0.5f, 0.5f);
                                markersList.get(i).setRotation(CarList.get(i).getBearing());

                                circleList.get(i).setRadius(CarList.get(i).getAccurracy());
                                circleList.get(i).setCenter(pos);

                            } else {
                                gMap.clear();
                                markersList.clear();
                                circleList.clear();

                                LatLng pos = new LatLng(latitude, longitude);
                                marker = gMap.addMarker(new MarkerOptions()
                                        .title("Car:" + " " + CARD_ID)
                                        .position(pos)
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.redmapicon)));

                                circle = gMap.addCircle(new CircleOptions()
                                        .center(pos)
                                        .radius(myAcurracy)
                                        .fillColor(shadeColor)
                                        .strokeColor(strokeColor).strokeWidth(2));

                                for (int k = 0; k < CarList.size(); k++) {

                                    LatLng pos2 = new LatLng(CarList.get(k).getLat(), CarList.get(k).getLon());
                                    mMarker = gMap.addMarker(new MarkerOptions()
                                            .title("Car:" + " " + CarList.get(k).getCarId())
                                            .snippet("Car Speed: " + CarList.get(k).getCurrentSpeed() + "KPH")
                                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.mapicon))
                                            .position(pos2));

                                    mMarker.setAnchor(0.5f, 0.5f);
                                    mMarker.setRotation(CarList.get(k).getBearing());
                                  //  mMarker.showInfoWindow();
                                    markersList.add(mMarker);

                                    mCircle = gMap.addCircle(new CircleOptions()
                                            .center(pos2)
                                            .radius(CarList.get(k).getAccurracy())
                                            .fillColor(mShadeColor)
                                            .strokeColor(mStrokeColor).strokeWidth(2));

                                    circleList.add(mCircle);
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
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.mapicon))
                                    .position(pos));

                            mMarker.setAnchor(0.5f, 0.5f);
                            mMarker.setRotation(CarList.get(i).getBearing());
                           // mMarker.showInfoWindow();
                            markersList.add(mMarker);


                            mCircle = gMap.addCircle(new CircleOptions()
                                    .center(pos)
                                    .radius(CarList.get(i).getAccurracy())
                                    .fillColor(mShadeColor)
                                    .strokeColor(mStrokeColor).strokeWidth(2));

                            circleList.add(mCircle);
                        }
                    }

                    break;
                case 2:
                    gMap.clear();
                    markersList.clear();
                    circleList.clear();

                    LatLng pos = new LatLng(latitude, longitude);
                    marker = gMap.addMarker(new MarkerOptions()
                            .title("Car:" + " " + CARD_ID)
                            .position(pos)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.redmapicon)));

                    circle = gMap.addCircle(new CircleOptions()
                            .center(pos)
                            .radius(myAcurracy)
                            .fillColor(shadeColor)
                            .strokeColor(strokeColor).strokeWidth(2));

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



