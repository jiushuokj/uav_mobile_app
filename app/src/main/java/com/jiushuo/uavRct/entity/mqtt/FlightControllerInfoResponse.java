package com.jiushuo.uavRct.entity.mqtt;

public class FlightControllerInfoResponse extends DefaultResponse {
    private boolean homeLocationSet;
    private double homeLocationLatitude;
    private double homeLocationLongitude;
    private double maxFlightHeight;
    private double maxFlightRadius;

    public boolean isHomeLocationSet() {
        return homeLocationSet;
    }

    public void setHomeLocationSet(boolean homeLocationSet) {
        this.homeLocationSet = homeLocationSet;
    }

    public double getHomeLocationLatitude() {
        return homeLocationLatitude;
    }

    public void setHomeLocationLatitude(double homeLocationLatitude) {
        this.homeLocationLatitude = homeLocationLatitude;
    }

    public double getHomeLocationLongitude() {
        return homeLocationLongitude;
    }

    public void setHomeLocationLongitude(double homeLocationLongitude) {
        this.homeLocationLongitude = homeLocationLongitude;
    }

    public double getMaxFlightHeight() {
        return maxFlightHeight;
    }

    public void setMaxFlightHeight(double maxFlightHeight) {
        this.maxFlightHeight = maxFlightHeight;
    }

    public double getMaxFlightRadius() {
        return maxFlightRadius;
    }

    public void setMaxFlightRadius(double maxFlightRadius) {
        this.maxFlightRadius = maxFlightRadius;
    }
}
