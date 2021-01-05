package com.jiushuo.uavRct.entity.mqtt;

public class ZoomResponse extends DefaultResponse{
    private String magnification;

    public String getMagnification() {
        return magnification;
    }

    public void setMagnification(String magnification) {
        this.magnification = magnification;
    }
}
