package com.aiamp.navigation.model;

/**
 * 路线信息数据类
 */
public class RouteInfo {
    private int routeId;
    private String tag;           // 路线标签：最快、最短、推荐等
    private int duration;         // 预计时间（秒）
    private int distance;         // 距离（米）
    private int trafficLights;    // 红绿灯数量
    private float toll;           // 过路费（元）
    private boolean isSelected;

    public RouteInfo() {}

    public RouteInfo(int routeId, String tag, int duration, int distance, int trafficLights, float toll) {
        this.routeId = routeId;
        this.tag = tag;
        this.duration = duration;
        this.distance = distance;
        this.trafficLights = trafficLights;
        this.toll = toll;
        this.isSelected = false;
    }

    public int getRouteId() { return routeId; }
    public void setRouteId(int routeId) { this.routeId = routeId; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public int getDistance() { return distance; }
    public void setDistance(int distance) { this.distance = distance; }

    public int getTrafficLights() { return trafficLights; }
    public void setTrafficLights(int trafficLights) { this.trafficLights = trafficLights; }

    public float getToll() { return toll; }
    public void setToll(float toll) { this.toll = toll; }

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }

    public String getDurationText() {
        int hours = duration / 3600;
        int minutes = (duration % 3600) / 60;
        if (hours > 0) {
            return hours + "小时" + minutes + "分钟";
        }
        return minutes + "分钟";
    }

    public String getDistanceText() {
        if (distance >= 1000) {
            return String.format("%.1f公里", distance / 1000.0f);
        }
        return distance + "米";
    }
}
