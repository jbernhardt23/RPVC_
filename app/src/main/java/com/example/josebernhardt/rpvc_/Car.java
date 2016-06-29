package com.example.josebernhardt.rpvc_;

/**
 * Created by jose on 30/04/16.
 */
public class Car {

    private double lat, lon, currentSpeed;
    private boolean inicialStatus, frontSensor, backSensor, carCrashed;
    private int carId;

    public Car(double lat, double lon, double currentSpeed, int carId) {
        this.lat = lat;
        this.lon = lon;
        this.currentSpeed = currentSpeed;
        this.carId = carId;
    }

    public boolean isInicialStatus() {
        return inicialStatus;
    }

    public void setInicialStatus(boolean inicialStatus) {
        this.inicialStatus = inicialStatus;
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

    public boolean isFrontSensor() {
        return frontSensor;
    }

    public void setFrontSensor(boolean frontSensor) {
        this.frontSensor = frontSensor;
    }

    public boolean isBackSensor() {
        return backSensor;
    }

    public void setBackSensor(boolean backSensor) {
        this.backSensor = backSensor;
    }

    public boolean isCarCrashed() {
        return carCrashed;
    }

    public void setCarCrashed(boolean carCrashed) {
        this.carCrashed = carCrashed;
    }

    public int getCarId() {
        return carId;
    }

    public void setCarId(int carId) {
        this.carId = carId;
    }

    @Override
    public String toString() {
        return "Car{" +
                "lat=" + lat +
                ", lon=" + lon +
                '}';
    }

}
