package com.example.josebernhardt.rpvc_;

import android.Manifest;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

//import com.google.android.gms.location.LocationListener;


/**
 * Created by jose on 30/04/16.
 */
public class GmapFragment extends Fragment implements OnMapReadyCallback {

    List<Car> CarList = MainActivity.CarList;
    MainActivity Car = new MainActivity();
    Marker marker;
    MapFragment fragment;
    private GoogleMap gMap;
    ProgressDialog dialog;
    View v;
    List<Marker> markersList = new ArrayList<>();
    private LocationManager locationManager;
    private LocationListener locationListener;
    private double latitude;
    private double longitude;
    private boolean flag = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Toast.makeText(getActivity(), "ME GUA TOLMYYYYY",
        //  Toast.LENGTH_LONG).show();


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

        //Setting Map
        fragment = (MapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        fragment.getMapAsync(this);

      /*  try {
            //Getting map in case is Null
            if (gMap == null) {
                gMap = ((MapFragment) getFragmentManager().
                        findFragmentById(R.id.map)).getMap();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        //  dialog = ProgressDialog.show(getActivity(), "", "Loading Map", true, false);
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        // dialog.dismiss();

        gMap = googleMap;
        permissions();

        //locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(18.4363957, -69.9509048)));
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(10));
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.getUiSettings().setZoomGesturesEnabled(true);
        googleMap.getUiSettings().setZoomControlsEnabled(true);


        locationManager = (LocationManager) getActivity().getSystemService(Context
                .LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                latitude = location.getLatitude();
                longitude = location.getLongitude();
                LatLng pos = new LatLng(latitude, longitude);
                markersList.get(0).setPosition(pos);
                markersList.get(0).setSnippet("Car Speed: " + (location.getSpeed() * 3600 / 1000 + "km/h"));
                markersList.get(0).hideInfoWindow();
                markersList.get(0).showInfoWindow();
                CarList.get(0).setCurrentSpeed(location.getSpeed());

                googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(latitude, longitude)));

            }

            @Override
            public void onProviderDisabled(String s) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }
        };

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);




        //Adding markers for every Car on the Map

        for (int i =0; i < CarList.size(); i++ ){

            LatLng pos = new LatLng(latitude, longitude);
            marker = googleMap.addMarker(new MarkerOptions()
                    .title("Car " + i)
                    .snippet("Car Speed: " + CarList.get(i).getCurrentSpeed() + "KPH")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .position(pos));

            marker.showInfoWindow();
            markersList.add(marker);

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
                gMap.setMyLocationEnabled(true);
                locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

            }
        } else {

            gMap.setMyLocationEnabled(true);

        }


    }
    @Override
    public void onResume() {
        super.onResume();

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        switch (requestCode){

            case 10:

                if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    gMap.setMyLocationEnabled(true);
                locationManager = (LocationManager) getActivity()
                        .getSystemService(Context.LOCATION_SERVICE);
                locationManager.requestLocationUpdates("gps", 5000, 0, locationListener);
                flag = true;

                return;
        }

    }




}



