package com.jiushuo.uavRct.entity.sqlite;

import org.litepal.crud.LitePalSupport;

public class WayPoint extends LitePalSupport {
    private String waypointId;
    private String missionId;
    private double latitude;
    private double longitude;
    private boolean usingMissionAltitude;
    private float altitude;
    private int heading;
    private int turnMode;
    private float gimbalPitch;

    public String getWaypointId() {
        return waypointId;
    }

    public void setWaypointId(String waypointId) {
        this.waypointId = waypointId;
    }

    public String getMissionId() {
        return missionId;
    }

    public void setMissionId(String missionId) {
        this.missionId = missionId;
    }

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

    public int getTurnMode() {
        return turnMode;
    }

    public void setTurnMode(int turnMode) {
        this.turnMode = turnMode;
    }

    public float getGimbalPitch() {
        return gimbalPitch;
    }

    public void setGimbalPitch(float gimbalPitch) {
        this.gimbalPitch = gimbalPitch;
    }
}
