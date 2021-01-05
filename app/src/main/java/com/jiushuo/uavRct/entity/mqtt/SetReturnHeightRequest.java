package com.jiushuo.uavRct.entity.mqtt;

public class SetReturnHeightRequest extends DefaultRequest {
    private Integer height;

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }
}
