package com.jiushuo.uavRct.entity.mqtt;

public class HotpointMissionRequest extends DefaultRequest {
    private double latitude;
    private double longitude;
    private HotpointStartPoint startPoint;
    private HotpointHeading heading;
    private double altitude;//[5,500]
    private double radius;//[5,500]
    private float angularVelocity;//角速度
    private boolean clockwise;//如果为true则顺时针转

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

    public HotpointStartPoint getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(HotpointStartPoint startPoint) {
        this.startPoint = startPoint;
    }

    public HotpointHeading getHeading() {
        return heading;
    }

    public void setHeading(HotpointHeading heading) {
        this.heading = heading;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public float getAngularVelocity() {
        return angularVelocity;
    }

    public void setAngularVelocity(float angularVelocity) {
        this.angularVelocity = angularVelocity;
    }

    public boolean isClockwise() {
        return clockwise;
    }

    public void setClockwise(boolean clockwise) {
        this.clockwise = clockwise;
    }

    public enum HotpointStartPoint {
        NORTH,    //Start from the North.
        SOUTH,    //Start from the South.
        WEST,    //Start from the West.
        EAST,    //Start from the East.
        NEAREST,    //Start the circle surrounding the hotpoint at the nearest point on the circle to the aircraft's current location.
    }

    public enum HotpointHeading {
        ALONG_CIRCLE_LOOKING_FORWARDS,    //Heading is in the forward direction of travel along the circular path.
        ALONG_CIRCLE_LOOKING_BACKWARDS, //Heading is in the backward direction of travel along the circular path.
        TOWARDS_HOT_POINT,    //Heading is toward the hotpoint.
        AWAY_FROM_HOT_POINT,    //Heading of the aircraft is looking away from the hotpoint. It is in the direction of the vector defined from the hotpoint to the aircraft.
        CONTROLLED_BY_REMOTE_CONT, //ROLLER	Heading is controlled by the remote controller.
        USING_INITIAL_HEADING,  //The heading remains the same as the heading of the aircraft when the mission started.
    }
}
