package com.jiushuo.uavRct.entity.mqtt;

public class WaypointAction {
    public enum WaypointActionType {
        STAY,
        START_TAKE_PHOTO,
        START_RECORD,
        STOP_RECORD,
        ROTATE_AIRCRAFT,
        GIMBAL_PITCH,
        CAMERA_ZOOM,
        CAMERA_FOCUS,
        FINE_TUNE_GIMBAL_PITCH,
        RESET_GIMBAL_YAW,
    }

    private WaypointActionType actionType;
    private int actionParam = 2;

    public WaypointActionType getActionType() {
        return actionType;
    }

    public void setActionType(WaypointActionType actionType) {
        this.actionType = actionType;
    }

    public int getActionParam() {
        return actionParam;
    }

    public void setActionParam(int actionParam) {
        this.actionParam = actionParam;
    }
}