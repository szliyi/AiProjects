package com.aiamp.navigation.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 设置项持久化工具类
 * 使用 SharedPreferences 存储各项设置
 */
public class SettingsStore {

    private static final String PREFS_NAME = "app_settings";

    // 设置项Key
    private static final String KEY_MAP_MODE = "map_mode";               // 0=标准, 1=卫星
    private static final String KEY_DAY_NIGHT_MODE = "day_night_mode";   // 0=自动, 1=白天, 2=夜间
    private static final String KEY_VOICE_BROADCAST = "voice_broadcast"; // 0=关闭, 1=开启, 2=仅导航
    private static final String KEY_ROUTE_PREFERENCE = "route_pref";     // 0=智能推荐, 1=躲避拥堵, 2=不走高速, 3=少收费, 4=高速优先
    private static final String KEY_AVOID_CONGESTION = "avoid_congestion"; // 0=关闭, 1=开启
    private static final String KEY_AVOID_RESTRICTION = "avoid_restriction"; // 0=关闭, 1=开启
    private static final String KEY_TRAFFIC = "traffic";                 // 0=关闭, 1=开启

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ==================== 地图模式 ====================

    /** 0=标准地图, 1=卫星地图 */
    public static int getMapMode(Context context) {
        return getPrefs(context).getInt(KEY_MAP_MODE, 0);
    }

    public static void setMapMode(Context context, int mode) {
        getPrefs(context).edit().putInt(KEY_MAP_MODE, mode).apply();
    }

    public static String getMapModeText(Context context) {
        int mode = getMapMode(context);
        return mode == 1 ? "卫星地图" : "标准地图";
    }

    // ==================== 日夜模式 ====================

    /** 0=自动切换, 1=白天, 2=夜间 */
    public static int getDayNightMode(Context context) {
        return getPrefs(context).getInt(KEY_DAY_NIGHT_MODE, 0);
    }

    public static void setDayNightMode(Context context, int mode) {
        getPrefs(context).edit().putInt(KEY_DAY_NIGHT_MODE, mode).apply();
    }

    public static String getDayNightModeText(Context context) {
        int mode = getDayNightMode(context);
        switch (mode) {
            case 1: return "白天";
            case 2: return "夜间";
            default: return "自动切换";
        }
    }

    // ==================== 语音播报 ====================

    /** 0=关闭, 1=开启, 2=仅导航播报 */
    public static int getVoiceBroadcast(Context context) {
        return getPrefs(context).getInt(KEY_VOICE_BROADCAST, 1);
    }

    public static void setVoiceBroadcast(Context context, int mode) {
        getPrefs(context).edit().putInt(KEY_VOICE_BROADCAST, mode).apply();
    }

    public static String getVoiceBroadcastText(Context context) {
        int mode = getVoiceBroadcast(context);
        switch (mode) {
            case 0: return "关闭";
            case 2: return "仅导航";
            default: return "开启";
        }
    }

    // ==================== 路线偏好 ====================

    /** 0=智能推荐, 1=躲避拥堵, 2=不走高速, 3=少收费, 4=高速优先 */
    public static int getRoutePreference(Context context) {
        return getPrefs(context).getInt(KEY_ROUTE_PREFERENCE, 0);
    }

    public static void setRoutePreference(Context context, int pref) {
        getPrefs(context).edit().putInt(KEY_ROUTE_PREFERENCE, pref).apply();
    }

    public static String getRoutePreferenceText(Context context) {
        int pref = getRoutePreference(context);
        switch (pref) {
            case 1: return "躲避拥堵";
            case 2: return "不走高速";
            case 3: return "少收费";
            case 4: return "高速优先";
            default: return "智能推荐";
        }
    }

    // ==================== 避开拥堵 ====================

    public static boolean isAvoidCongestion(Context context) {
        return getPrefs(context).getInt(KEY_AVOID_CONGESTION, 1) == 1;
    }

    public static void setAvoidCongestion(Context context, boolean avoid) {
        getPrefs(context).edit().putInt(KEY_AVOID_CONGESTION, avoid ? 1 : 0).apply();
    }

    public static String getAvoidCongestionText(Context context) {
        return isAvoidCongestion(context) ? "开启" : "关闭";
    }

    // ==================== 避开限行 ====================

    public static boolean isAvoidRestriction(Context context) {
        return getPrefs(context).getInt(KEY_AVOID_RESTRICTION, 0) == 1;
    }

    public static void setAvoidRestriction(Context context, boolean avoid) {
        getPrefs(context).edit().putInt(KEY_AVOID_RESTRICTION, avoid ? 1 : 0).apply();
    }

    public static String getAvoidRestrictionText(Context context) {
        return isAvoidRestriction(context) ? "开启" : "关闭";
    }

    // ==================== 实时路况 ====================

    public static boolean isTrafficEnabled(Context context) {
        return getPrefs(context).getInt(KEY_TRAFFIC, 1) == 1;
    }

    public static void setTrafficEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putInt(KEY_TRAFFIC, enabled ? 1 : 0).apply();
    }

    public static String getTrafficText(Context context) {
        return isTrafficEnabled(context) ? "开启" : "关闭";
    }
}
