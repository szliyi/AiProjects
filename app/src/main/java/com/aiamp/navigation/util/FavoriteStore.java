package com.aiamp.navigation.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.aiamp.navigation.model.SearchResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 收藏地点管理工具类
 * 使用 SharedPreferences 存储，支持最多100个收藏地点
 */
public class FavoriteStore {

    private static final String PREFS_NAME = "favorite_places";
    private static final String KEY_FAVORITES = "favorites";
    private static final int MAX_FAVORITES = 100;

    /**
     * 加载所有收藏地点
     */
    public static List<SearchResult> loadAll(Context context) {
        List<SearchResult> list = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_FAVORITES, "[]");
        android.util.Log.d("FavoriteStore", "loadAll json=" + json);
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                SearchResult sr = new SearchResult();
                sr.setPoiId(obj.optString("poiId", ""));
                sr.setName(obj.getString("name"));
                sr.setAddress(obj.getString("address"));
                sr.setLatitude(obj.getDouble("lat"));
                sr.setLongitude(obj.getDouble("lng"));
                sr.setType(obj.optString("type", ""));
                sr.setTag(obj.optString("tag", ""));
                android.util.Log.d("FavoriteStore", "loadAll[" + i + "] tag=" + sr.getTag());
                list.add(sr);
            }
        } catch (JSONException e) {
            // ignore
        }
        return list;
    }

    /**
     * 添加收藏地点（去重：相同name+lat+lng视为同一地点）
     * @return true=添加成功, false=已达上限或已存在
     */
    public static boolean add(Context context, SearchResult place) {
        List<SearchResult> list = loadAll(context);

        // 去重检查
        for (SearchResult existing : list) {
            if (isSamePlace(existing, place)) {
                return false; // 已存在
            }
        }

        // 上限检查
        if (list.size() >= MAX_FAVORITES) {
            return false;
        }

        list.add(place);
        saveList(context, list);
        return true;
    }

    /**
     * 删除指定收藏地点
     * @param index 列表索引
     */
    public static void remove(Context context, int index) {
        List<SearchResult> list = loadAll(context);
        if (index >= 0 && index < list.size()) {
            list.remove(index);
            saveList(context, list);
        }
    }

    /**
     * 删除多个收藏地点
     */
    public static void removeAll(Context context, List<Integer> indices) {
        List<SearchResult> list = loadAll(context);
        // 从大到小排序索引，避免删除时索引偏移
        List<Integer> sorted = new ArrayList<>(indices);
        java.util.Collections.sort(sorted, (a, b) -> b - a);
        for (int idx : sorted) {
            if (idx >= 0 && idx < list.size()) {
                list.remove(idx);
            }
        }
        saveList(context, list);
    }

    /**
     * 获取收藏数量
     */
    public static int getCount(Context context) {
        return loadAll(context).size();
    }

    /**
     * 判断地点是否已收藏
     */
    public static boolean isFavorited(Context context, SearchResult place) {
        List<SearchResult> list = loadAll(context);
        for (SearchResult existing : list) {
            if (isSamePlace(existing, place)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 取消收藏（按name+lat+lng匹配）
     */
    public static boolean removeByPlace(Context context, SearchResult place) {
        List<SearchResult> list = loadAll(context);
        for (int i = 0; i < list.size(); i++) {
            if (isSamePlace(list.get(i), place)) {
                list.remove(i);
                saveList(context, list);
                return true;
            }
        }
        return false;
    }

    /**
     * 更新指定收藏地点的标记
     * @param index 列表索引
     * @param tag 标记内容
     */
    public static void updateTag(Context context, int index, String tag) {
        List<SearchResult> list = loadAll(context);
        android.util.Log.d("FavoriteStore", "updateTag index=" + index + " listSize=" + list.size() + " tag=" + tag);
        if (index >= 0 && index < list.size()) {
            list.get(index).setTag(tag);
            saveList(context, list);
            android.util.Log.d("FavoriteStore", "updateTag saved, verifying: " + list.get(index).getTag());
        } else {
            android.util.Log.e("FavoriteStore", "updateTag index out of bounds! index=" + index + " size=" + list.size());
        }
    }

    /**
     * 判断两个地点是否相同
     */
    private static boolean isSamePlace(SearchResult a, SearchResult b) {
        if (a == null || b == null) return false;
        // 通过经纬度 + 名称判断
        return Math.abs(a.getLatitude() - b.getLatitude()) < 0.000001
                && Math.abs(a.getLongitude() - b.getLongitude()) < 0.000001
                && a.getName() != null && a.getName().equals(b.getName());
    }

    /**
     * 持久化保存列表
     */
    private static void saveList(Context context, List<SearchResult> list) {
        JSONArray arr = new JSONArray();
        for (SearchResult sr : list) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("poiId", sr.getPoiId() != null ? sr.getPoiId() : "");
                obj.put("name", sr.getName() != null ? sr.getName() : "");
                obj.put("address", sr.getAddress() != null ? sr.getAddress() : "");
                obj.put("lat", sr.getLatitude());
                obj.put("lng", sr.getLongitude());
                obj.put("type", sr.getType() != null ? sr.getType() : "");
                obj.put("tag", sr.getTag() != null ? sr.getTag() : "");
                arr.put(obj);
            } catch (JSONException e) {
                // ignore
            }
        }
        String jsonStr = arr.toString();
        android.util.Log.d("FavoriteStore", "saveList json=" + jsonStr);
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_FAVORITES, jsonStr).apply();
    }
}
