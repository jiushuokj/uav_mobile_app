package com.jiushuo.uavRct.entity.mqtt;

public class RequestUtil {
    public static final String AIRCRAFT = "Aircraft";
    public static final String FLIGHT_CONTROLLER = "FlightController";
    public static final String CAMERA = "Camera";

    public static final String LOCK = "Lock";
    public static final String UNLOCK = "Unlock";
    public static final String SET_RETURN_HEIGHT = "SetReturnHeight";
    public static final String TAKE_OFF = "TakeOff";
    public static final String LAND = "Land";
    public static final String CANCEL_LANDING = "CancelLanding";
    public static final String CONFIRM_LANDING = "ConfirmLanding";
    public static final String HOVER = "Hover";
    public static final String FLY_TO = "FlyTo";
    public static final String RETURN_HOME = "ReturnHome";

    public static final String GIMBAL_CONTROL = "GimbalControl";

    public static final String CAMERA_SETTING = "CameraSetting";
    public static final String CAMERA_MODE = "CameraMode";
    public static final String TAKE_PHOTO = "TakePhoto";
    public static final String START_RECORD_VIDEO = "StartRecordVideo";
    public static final String STOP_RECORD_VIDEO = "StopRecordVideo";
    public static final String ZOOM_IN = "ZoomIn";
    public static final String ZOOM_OUT = "ZoomOut";
    public static final String ZOOM_RESET = "ZoomReset";

    public static final String WAYPOINT_LOAD = "WaypointLoad";
    public static final String WAYPOINT_UPLOAD = "WaypointUpload";
    public static final String WAYPOINT_START = "WaypointStart";
    public static final String WAYPOINT_PAUSE = "WaypointPause";
    public static final String WAYPOINT_RESUME = "WaypointResume";
    public static final String WAYPOINT_STOP = "WaypointStop";

    public static final String HOTPOINT_START = "HotpointStart";
    public static final String HOTPOINT_PAUSE = "HotpointPause";
    public static final String HOTPOINT_RESUME = "HotpointResume";
    public static final String HOTPOINT_STOP = "HotpointStop";



    public static final String RES_TOPIC_NAME = "resTopicName";

    public static final int CODE_SUCCESS = 0;
    public static final int CODE_FAIL = 111111;
    public static final int CODE_FLY_TO_STOPPED = 111119;
    public static final int CODE_FLY_TO_FINISHED = 111118;
    public static final int CODE_FLY_TO_RUNNING = 111117;
    public static final int CODE_FLY_TO_START_ERROR = 111116;
    public static final int CODE_FLY_TO_STOP_ERROR = 111115;
    public static final int CODE_FLY_TO_PROCESSED = 111114;
    public static final int CODE_FLY_TO_STARTED = 111110;
}
