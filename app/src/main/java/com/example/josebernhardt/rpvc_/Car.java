package com.example.josebernhardt.rpvc_;

/**
 * Class that represents a Car object
 * Created by jose on 30/04/16.
 */
public class Car {

    private double lat, lon, currentSpeed;
    private boolean timer = false;
    private String  carCrashed = "Active";
    private String carId;


    private float distanceBetween;

    public Car(double lat, double lon, String carId, boolean timer, String carCrashed) {
        this.lat = lat;
        this.lon = lon;
        this.carId = carId;
        this.timer = timer;
        this.carCrashed = carCrashed;
    }

    public Car(){

    }

    public float getDistanceBetween() {
        return distanceBetween;
    }

    public void setDistanceBetween(float distanceBetween) {
        this.distanceBetween = distanceBetween;
    }
    public boolean isTimer() {
        return timer;
    }

    public void setInicialTimer(boolean timer) {
        this.timer = timer;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public double getCurrentSpeed() {
        return currentSpeed;
    }

    public void setCurrentSpeed(double currentSpeed) {
        this.currentSpeed = currentSpeed;
    }

    public String getCarCrashed() {
        return carCrashed;
    }

    public void setCarCrashed(String carCrashed) {
        this.carCrashed = carCrashed;
    }

    public String getCarId() {
        return carId;
    }

    public void setCarId(String carId) {
        this.carId = carId;
    }

    @Override
    public String toString() {
        return  lat +
                ","+ lon;
    }

}
