package com.jiushuo.uavRct.entity.sqlite;

import org.litepal.crud.LitePalSupport;

public class Action extends LitePalSupport {
    private String actionId;
    private String waypointId;
    private int actionType;
    private int actionParam = 2;

    public String getActionId() {
        return actionId;
    }

    public void setActionId(String actionId) {
        this.actionId = actionId;
    }

    public String getWaypointId() {
        return waypointId;
    }

    public void setWaypointId(String waypointId) {
        this.waypointId = waypointId;
    }

    public int getActionType() {
        return actionType;
    }

    public void setActionType(int actionType) {
        this.actionType = actionType;
    }

    public int getActionParam() {
        return actionParam;
    }

    public void setActionParam(int actionParam) {
        this.actionParam = actionParam;
    }
}
