package com.jiushuo.uavRct.entity.mqtt;

import java.util.ArrayList;
import java.util.List;

public class Waypoint {
    private double latitude;
    private double longitude;
    private boolean usingMissionAltitude;
    private float altitude;
    private int heading;

    public enum WaypointTurnMode {
        CLOCKWISE,
        COUNTER_CLOCKWISE
    }

    private WaypointTurnMode turnMode;
    private float gimbalPitch;
    private float shootPhotoTimeInterval;
    private float shootPhotoDistanceInterval;
    private List<WaypointAction> actions = new ArrayList<>();

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean isUsingMissionAltitude() {
        return usingMissionAltitude;
    }

    public void setUsingMissionAltitude(boolean usingMissionAltitude) {
        this.usingMissionAltitude = usingMissionAltitude;
    }

    public float getAltitude() {
        return altitude;
    }

    public void setAltitude(float altitude) {
        this.altitude = altitude;
    }

    public int getHeading() {
        return heading;
    }

    public void setHeading(int heading) {
        this.heading = heading;
    }

    public WaypointTurnMode getTurnMode() {
        return turnMode;
    }

    public void setTurnMode(WaypointTurnMode turnMode) {
        this.turnMode = turnMode;
    }

    public float getGimbalPitch() {
        return gimbalPitch;
    }

    public void setGimbalPitch(float gimbalPitch) {
        this.gimbalPitch = gimbalPitch;
    }

    public float getShootPhotoTimeInterval() {
        return shootPhotoTimeInterval;
    }

    public void setShootPhotoTimeInterval(float shootPhotoTimeInterval) {
        this.shootPhotoTimeInterval = shootPhotoTimeInterval;
    }

    public float getShootPhotoDistanceInterval() {
        return shootPhotoDistanceInterval;
    }

    public void setShootPhotoDistanceInterval(float shootPhotoDistanceInterval) {
        this.shootPhotoDistanceInterval = shootPhotoDistanceInterval;
    }

    public List<WaypointAction> getActions() {
        return actions;
    }

    public void setActions(List<WaypointAction> actions) {
        this.actions = actions;
    }
}