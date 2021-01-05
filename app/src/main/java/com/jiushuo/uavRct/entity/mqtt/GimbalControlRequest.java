package com.jiushuo.uavRct.entity.mqtt;

public class GimbalControlRequest extends DefaultRequest {
    private RotationMode mode;
    private float pitch;
    private float roll;
    private float yaw;
    private double time;

    public enum RotationMode {
        RELATIVE_ANGLE,
        ABSOLUTE_ANGLE,
        SPEED
    }

    public RotationMode getMode() {
        return mode;
    }

    public void setMode(RotationMode mode) {
        this.mode = mode;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public float getRoll() {
        return roll;
    }

    public void setRoll(float roll) {
        this.roll = roll;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }
}
