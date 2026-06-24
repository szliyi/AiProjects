package com.aiamp.navigation.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 常去地点/通勤设置存储工具类
 * 存储通勤方式、上下班时间、推测推荐开关、云端保存开关等
 */
public class CommuteStore {

    private static final String PREF_NAME = "commute_settings";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ==================== 通勤方式 ====================
    // 0=默认, 1=驾车, 2=公交, 3=打车
    private static final String KEY_COMMUTE_MODE = "commute_mode";

    public static int getCommuteMode(Context context) {
        return getPrefs(context).getInt(KEY_COMMUTE_MODE, 0);
    }

    public static void setCommuteMode(Context context, int mode) {
        getPrefs(context).edit().putInt(KEY_COMMUTE_MODE, mode).apply();
    }

    public static String getCommuteModeText(Context context) {
        switch (getCommuteMode(context)) {
            case 1: return "驾车";
            case 2: return "公交";
            case 3: return "打车";
            default: return "默认";
        }
    }

    // ==================== 上班时间 ====================
    private static final String KEY_WORK_TIME_HOUR = "work_time_hour";
    private static final String KEY_WORK_TIME_MINUTE = "work_time_minute";

    public static int getWorkTimeHour(Context context) {
        return getPrefs(context).getInt(KEY_WORK_TIME_HOUR, 9);
    }

    public static void setWorkTimeHour(Context context, int hour) {
        getPrefs(context).edit().putInt(KEY_WORK_TIME_HOUR, hour).apply();
    }

    public static int getWorkTimeMinute(Context context) {
        return getPrefs(context).getInt(KEY_WORK_TIME_MINUTE, 0);
    }

    public static void setWorkTimeMinute(Context context, int minute) {
        getPrefs(context).edit().putInt(KEY_WORK_TIME_MINUTE, minute).apply();
    }

    public static String getWorkTimeText(Context context) {
        return String.format("%02d:%02d", getWorkTimeHour(context), getWorkTimeMinute(context));
    }

    // ==================== 回家时间 ====================
    private static final String KEY_HOME_TIME_HOUR = "home_time_hour";
    private static final String KEY_HOME_TIME_MINUTE = "home_time_minute";

    public static int getHomeTimeHour(Context context) {
        return getPrefs(context).getInt(KEY_HOME_TIME_HOUR, 18);
    }

    public static void setHomeTimeHour(Context context, int hour) {
        getPrefs(context).edit().putInt(KEY_HOME_TIME_HOUR, hour).apply();
    }

    public static int getHomeTimeMinute(Context context) {
        return getPrefs(context).getInt(KEY_HOME_TIME_MINUTE, 0);
    }

    public static void setHomeTimeMinute(Context context, int minute) {
        getPrefs(context).edit().putInt(KEY_HOME_TIME_MINUTE, minute).apply();
    }

    public static String getHomeTimeText(Context context) {
        return String.format("%02d:%02d", getHomeTimeHour(context), getHomeTimeMinute(context));
    }

    // ==================== 推测常去地点及推荐路线 ====================
    private static final String KEY_GUESS_RECOMMEND = "guess_recommend";

    public static boolean isGuessRecommendEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_GUESS_RECOMMEND, true);
    }

    public static void setGuessRecommendEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_GUESS_RECOMMEND, enabled).apply();
    }

    // ==================== 保存常去地点至云端 ====================
    private static final String KEY_CLOUD_SAVE = "cloud_save";

    public static boolean isCloudSaveEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_CLOUD_SAVE, false);
    }

    public static void setCloudSaveEnabled(Context context, boolean enabled) {
        getPrefs(context).edit().putBoolean(KEY_CLOUD_SAVE, enabled).apply();
    }
}
