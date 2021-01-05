package com.jiushuo.uavRct.entity.mqtt;

public class DefaultResponse {
    private Integer code = RequestUtil.CODE_SUCCESS;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }
}
