package com.jiushuo.uavRct.entity.mqtt;

import java.util.ArrayList;
import java.util.List;

public class WaypointMissionRequest extends DefaultRequest {
    private String id;
    private String missionName;
    private float autoFlightSpeed;
    private float maxFlightSpeed;
    private float missionAltitude;

    public enum WaypointMissionHeadingMode {
        //沿航线方向
        AUTO,
        //使用最初的方向
        USING_INITIAL_DIRECTION,
        //手动控制
        CONTROL_BY_REMOTE_CONTROLLER,
        //使用航点的方向
        USING_WAYPOINT_HEADING,
        //依照每个航点设置
        TOWARD_POINT_OF_INTEREST
    }
    //飞行器偏航角，为USING_WAYPOINT_HEADING（依照每个航点设置）
    private WaypointMissionHeadingMode headingMode;
    //if the gimbal pitch rotation can be controlled during the waypoint mission. When true, gimbalPitch in Waypoint is used to control gimbal pitch
    private boolean gimbalPitchRotationEnabled;
    public enum WaypointMissionFinishedAction {
        NO_ACTION,
        GO_HOME,
        AUTO_LAND,
        GO_FIRST_WAYPOINT,
        CONTINUE_UNTIL_END
    }

    private WaypointMissionFinishedAction finishedAction;
    private List<Waypoint> waypoints = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMissionName() {
        return missionName;
    }

    public void setMissionName(String missionName) {
        this.missionName = missionName;
    }

    public float getAutoFlightSpeed() {
        return autoFlightSpeed;
    }

    public void setAutoFlightSpeed(float autoFlightSpeed) {
        this.autoFlightSpeed = autoFlightSpeed;
    }

    public float getMaxFlightSpeed() {
        return maxFlightSpeed;
    }

    public void setMaxFlightSpeed(float maxFlightSpeed) {
        this.maxFlightSpeed = maxFlightSpeed;
    }

    public float getMissionAltitude() {
        return missionAltitude;
    }

    public void setMissionAltitude(float missionAltitude) {
        this.missionAltitude = missionAltitude;
    }

    public WaypointMissionHeadingMode getHeadingMode() {
        return headingMode;
    }

    public void setHeadingMode(WaypointMissionHeadingMode headingMode) {
        this.headingMode = headingMode;
    }

    public boolean isGimbalPitchRotationEnabled() {
        return gimbalPitchRotationEnabled;
    }

    public void setGimbalPitchRotationEnabled(boolean gimbalPitchRotationEnabled) {
        this.gimbalPitchRotationEnabled = gimbalPitchRotationEnabled;
    }

    public WaypointMissionFinishedAction getFinishedAction() {
        return finishedAction;
    }

    public void setFinishedAction(WaypointMissionFinishedAction finishedAction) {
        this.finishedAction = finishedAction;
    }

    public List<Waypoint> getWaypoints() {
        return waypoints;
    }

    public void setWaypoints(List<Waypoint> waypoints) {
        this.waypoints = waypoints;
    }
}






