package com.jiushuo.uavRct.entity.mqtt;

public class AircraftInfoResponse extends DefaultResponse {
    private String name;
    private String firmwarePackageVersion;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFirmwarePackageVersion() {
        return firmwarePackageVersion;
    }

    public void setFirmwarePackageVersion(String firmwarePackageVersion) {
        this.firmwarePackageVersion = firmwarePackageVersion;
    }
}
