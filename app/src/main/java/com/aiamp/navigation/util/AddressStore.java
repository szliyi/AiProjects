package com.aiamp.navigation.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 常用地址存储工具类 - 用于存储和读取家/公司地址
 */
public class AddressStore {

    private static final String PREF_NAME = "common_addresses";
    private static final String KEY_HOME_NAME = "home_name";
    private static final String KEY_HOME_ADDRESS = "home_address";
    private static final String KEY_HOME_LAT = "home_lat";
    private static final String KEY_HOME_LNG = "home_lng";
    private static final String KEY_COMPANY_NAME = "company_name";
    private static final String KEY_COMPANY_ADDRESS = "company_address";
    private static final String KEY_COMPANY_LAT = "company_lat";
    private static final String KEY_COMPANY_LNG = "company_lng";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 地址信息模型
     */
    public static class AddressInfo {
        public String name;
        public String address;
        public double latitude;
        public double longitude;

        public AddressInfo(String name, String address, double lat, double lng) {
            this.name = name;
            this.address = address;
            this.latitude = lat;
            this.longitude = lng;
        }

        public boolean isValid() {
            return name != null && !name.isEmpty() && latitude != 0 && longitude != 0;
        }
    }

    // ========== 家庭地址 ==========

    public static void saveHomeAddress(Context context, String name, String address, double lat, double lng) {
        getPrefs(context).edit()
                .putString(KEY_HOME_NAME, name)
                .putString(KEY_HOME_ADDRESS, address)
                .putFloat(KEY_HOME_LAT, (float) lat)
                .putFloat(KEY_HOME_LNG, (float) lng)
                .apply();
    }

    public static AddressInfo getHomeAddress(Context context) {
        SharedPreferences prefs = getPrefs(context);
        String name = prefs.getString(KEY_HOME_NAME, null);
        String address = prefs.getString(KEY_HOME_ADDRESS, null);
        float lat = prefs.getFloat(KEY_HOME_LAT, 0);
        float lng = prefs.getFloat(KEY_HOME_LNG, 0);
        if (name == null) return null;
        return new AddressInfo(name, address, lat, lng);
    }

    public static boolean hasHomeAddress(Context context) {
        return getHomeAddress(context) != null;
    }

    // ========== 公司地址 ==========

    public static void saveCompanyAddress(Context context, String name, String address, double lat, double lng) {
        getPrefs(context).edit()
                .putString(KEY_COMPANY_NAME, name)
                .putString(KEY_COMPANY_ADDRESS, address)
                .putFloat(KEY_COMPANY_LAT, (float) lat)
                .putFloat(KEY_COMPANY_LNG, (float) lng)
                .apply();
    }

    public static AddressInfo getCompanyAddress(Context context) {
        SharedPreferences prefs = getPrefs(context);
        String name = prefs.getString(KEY_COMPANY_NAME, null);
        String address = prefs.getString(KEY_COMPANY_ADDRESS, null);
        float lat = prefs.getFloat(KEY_COMPANY_LAT, 0);
        float lng = prefs.getFloat(KEY_COMPANY_LNG, 0);
        if (name == null) return null;
        return new AddressInfo(name, address, lat, lng);
    }

    public static boolean hasCompanyAddress(Context context) {
        return getCompanyAddress(context) != null;
    }

    // ========== 清空 ==========

    public static void clearHomeAddress(Context context) {
        getPrefs(context).edit()
                .remove(KEY_HOME_NAME)
                .remove(KEY_HOME_ADDRESS)
                .remove(KEY_HOME_LAT)
                .remove(KEY_HOME_LNG)
                .apply();
    }

    public static void clearCompanyAddress(Context context) {
        getPrefs(context).edit()
                .remove(KEY_COMPANY_NAME)
                .remove(KEY_COMPANY_ADDRESS)
                .remove(KEY_COMPANY_LAT)
                .remove(KEY_COMPANY_LNG)
                .apply();
    }
}
