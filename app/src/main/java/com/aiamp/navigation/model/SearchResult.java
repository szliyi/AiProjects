package com.aiamp.navigation.model;

/**
 * POI搜索结果数据类
 */
public class SearchResult {
    private String poiId;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private String type;          // POI类型：餐饮、购物、加油站等
    private String tag;           // 用户自定义标记：如美食、历史、常去等
    private float distance;       // 距离当前定位的距离（米）

    public SearchResult() {}

    public SearchResult(String name, String address, double latitude, double longitude) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getPoiId() { return poiId; }
    public void setPoiId(String poiId) { this.poiId = poiId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public float getDistance() { return distance; }
    public void setDistance(float distance) { this.distance = distance; }

    public String getDistanceText() {
        if (distance >= 1000) {
            return String.format("%.1fkm", distance / 1000.0f);
        }
        return (int) distance + "m";
    }
}
