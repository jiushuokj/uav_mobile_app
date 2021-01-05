package com.jiushuo.uavRct.entity.sqlite;

import org.litepal.crud.LitePalSupport;

public class Mission extends LitePalSupport {
    private String missionId;
    private String name;
    private int type;

    public String getMissionId() {
        return missionId;
    }

    public void setMissionId(String missionId) {
        this.missionId = missionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
