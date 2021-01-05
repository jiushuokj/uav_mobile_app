package com.jiushuo.uavRct.entity.mqtt;

public class CameraModeResponse extends DefaultResponse {
    public final static int PHOTO_MODE =0;
    public final static int VIDEO_MODE =1;
    private int cameraMode;

    public int getCameraMode() {
        return cameraMode;
    }

    public void setCameraMode(int cameraMode) {
        this.cameraMode = cameraMode;
    }
}
