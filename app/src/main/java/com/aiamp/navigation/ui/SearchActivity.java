package com.aiamp.navigation.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aiamp.navigation.R;
import com.aiamp.navigation.adapter.SearchResultAdapter;
import com.aiamp.navigation.model.SearchResult;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItemV2;
import com.amap.api.services.help.Inputtips;
import com.amap.api.services.help.InputtipsQuery;
import com.amap.api.services.help.Tip;
import com.amap.api.services.poisearch.PoiResultV2;
import com.amap.api.services.poisearch.PoiSearchV2;
import com.google.android.flexbox.FlexboxLayout;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 搜索页面 - 目的地搜索
 */
public class SearchActivity extends AppCompatActivity implements
        PoiSearchV2.OnPoiSearchListener, Inputtips.InputtipsListener {

    private static final String TAG = "SearchActivity";
    private static final String PREFS_NAME = "search_history";
    private static final String KEY_HISTORY = "history_keywords";
    private static final int MAX_HISTORY = 20;

    private EditText etSearch;
    private RecyclerView rvSearchResults;
    private SearchResultAdapter searchResultAdapter;
    private List<SearchResult> searchResults = new ArrayList<>();
    private LinearLayout emptyView;
    private LinearLayout historyLayout;
    private FlexboxLayout flexboxHistory;
    private TextView tvClearHistory;

    private double myLat, myLng;
    private LatLonPoint myLocation;
    private PoiSearchV2 poiSearch;
    private LinkedHashSet<String> historySet = new LinkedHashSet<>();
    private String searchMode; // "pick_start" / "pick_end" / null（普通搜索）
    private boolean pickMode; // 地图选点模式
    private String currentCity = ""; // 当前城市名

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // 获取我的位置
        myLat = getIntent().getDoubleExtra("lat", 39.904989);
        myLng = getIntent().getDoubleExtra("lng", 116.405285);
        myLocation = new LatLonPoint(myLat, myLng);

        // 获取当前城市名
        currentCity = getIntent().getStringExtra("city");
        if (currentCity == null) currentCity = "";

        // 搜索模式：pick_start / pick_end / null
        searchMode = getIntent().getStringExtra("search_mode");
        pickMode = getIntent().getBooleanExtra("pick_mode", false);

        initViews();
    }

    private void initViews() {
        // 返回按钮
        ImageView ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> finish());

        // 搜索框
        etSearch = findViewById(R.id.etSearch);
        // 根据模式调整提示文字
        if ("pick_start".equals(searchMode)) {
            etSearch.setHint("输入起点");
        } else if ("pick_end".equals(searchMode)) {
            etSearch.setHint("输入终点");
        }
        etSearch.requestFocus();
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 输入时自动提示
                if (s.length() > 0) {
                    searchInputTips(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 搜索按钮
        TextView tvSearchBtn = findViewById(R.id.tvSearchBtn);
        tvSearchBtn.setOnClickListener(v -> {
            String keyword = etSearch.getText().toString().trim();
            if (!keyword.isEmpty()) {
                performSearch(keyword);
            }
        });

        // 搜索结果列表
        rvSearchResults = findViewById(R.id.rvSearchResults);
        rvSearchResults.setLayoutManager(new LinearLayoutManager(this));
        searchResultAdapter = new SearchResultAdapter(searchResults, result -> {
            navigateToRoute(result);
        });
        rvSearchResults.setAdapter(searchResultAdapter);

        // 空状态
        emptyView = findViewById(R.id.emptyView);
        historyLayout = findViewById(R.id.historyLayout);
        flexboxHistory = findViewById(R.id.flexboxHistory);

        // 清除历史按钮
        tvClearHistory = findViewById(R.id.tvClearHistory);
        tvClearHistory.setOnClickListener(v -> clearHistory());

        // 加载并显示搜索历史
        loadHistory();
        renderHistory();
    }

    private void searchInputTips(String keyword) {
        InputtipsQuery inputQuery = new InputtipsQuery(keyword, "");
        Inputtips inputTips = new Inputtips(this, inputQuery);
        inputTips.setInputtipsListener(this);
        inputTips.requestInputtipsAsyn();
    }

    private void performSearch(String keyword) {
        // 保存搜索历史
        saveHistory(keyword);

        // 隐藏历史布局，显示搜索结果区域
        historyLayout.setVisibility(View.GONE);
        rvSearchResults.setVisibility(View.VISIBLE);

        try {
            // city参数：传入当前城市名可在指定城市范围内搜索
            // 高德SDK: 空字符串=全国搜索，但全国搜索时仅返回建议城市列表，不会直接返回POI
            // 所以必须传入城市名或城市编码才能直接获取POI结果
            String city = !currentCity.isEmpty() ? currentCity : "";
            PoiSearchV2.Query query = new PoiSearchV2.Query(keyword, "", city);
            query.setPageSize(30);
            query.setPageNum(0);
            if (myLocation != null) {
                query.setLocation(myLocation);
                query.setDistanceSort(true);
            }

            // 每次新建PoiSearchV2实例（避免setQuery在新版SDK中的兼容问题）
            if (poiSearch != null) {
                poiSearch.setOnPoiSearchListener(null);
            }
            poiSearch = new PoiSearchV2(this, query);
            poiSearch.setOnPoiSearchListener(this);
            poiSearch.searchPOIAsyn();

            Log.d(TAG, "执行搜索: keyword=" + keyword + ", city=" + city + ", location=" + myLocation);
        } catch (Exception e) {
            // 兼容：某些SDK版本PoiSearchV2构造函数可能抛运行时异常
            Log.e(TAG, "搜索初始化异常: " + e.getMessage(), e);
            emptyView.setVisibility(View.VISIBLE);
            rvSearchResults.setVisibility(View.GONE);
            String errDetail = e.getMessage() != null ? e.getMessage() : "未知错误";
            Toast.makeText(this, "搜索初始化失败: " + errDetail, Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToRoute(SearchResult destination) {
        if (pickMode) {
            // 地图选点模式：返回选中的地址给调用方
            Intent data = new Intent();
            data.putExtra("name", destination.getName());
            data.putExtra("address", destination.getAddress());
            data.putExtra("lat", destination.getLatitude());
            data.putExtra("lng", destination.getLongitude());
            setResult(RESULT_OK, data);
            finish();
        } else if (searchMode != null) {
            // 选择起点/终点模式：返回结果给调用方
            Intent data = new Intent();
            data.putExtra("picked_name", destination.getName());
            data.putExtra("picked_lat", destination.getLatitude());
            data.putExtra("picked_lng", destination.getLongitude());
            data.putExtra("search_mode", searchMode);
            setResult(RESULT_OK, data);
            finish();
        } else {
            // 普通搜索模式：跳转路线规划
            Intent intent = new Intent(SearchActivity.this, RouteActivity.class);
            intent.putExtra("start_name", "我的位置");
            intent.putExtra("start_lat", myLat);
            intent.putExtra("start_lng", myLng);
            intent.putExtra("end_name", destination.getName());
            intent.putExtra("end_lat", destination.getLatitude());
            intent.putExtra("end_lng", destination.getLongitude());
            startActivity(intent);
        }
    }

    // ==================== PoiSearchV2.OnPoiSearchListener ====================

    @Override
    public void onPoiSearched(PoiResultV2 result, int errorCode) {
        // 详细日志：打印所有可能的信息帮助排查
        int cityCount = 0;
        try {
            // 尝试通过反射获取建议城市数量（兼容不同SDK版本）
            java.lang.reflect.Method m = result != null ? result.getClass().getMethod("getSearchSuggestionCitys") : null;
            if (m != null) {
                Object cities = m.invoke(result);
                if (cities instanceof List) {
                    cityCount = ((List<?>) cities).size();
                }
            }
        } catch (Exception ignore) {}
        Log.d(TAG, "onPoiSearched回调: errorCode=" + errorCode
                + ", result=" + (result != null ? "有结果" : "null")
                + ", pois=" + (result != null && result.getPois() != null ? result.getPois().size() : 0)
                + ", suggestionCities=" + cityCount);

        if (errorCode == AMapException.CODE_AMAP_SUCCESS && result != null
                && result.getPois() != null && !result.getPois().isEmpty()) {
            searchResults.clear();
            for (PoiItemV2 poi : result.getPois()) {
                if (poi.getLatLonPoint() == null) continue;
                SearchResult sr = new SearchResult();
                sr.setName(poi.getTitle());
                sr.setAddress(poi.getProvinceName() + poi.getCityName()
                        + poi.getAdName() + poi.getSnippet());
                sr.setLatitude(poi.getLatLonPoint().getLatitude());
                sr.setLongitude(poi.getLatLonPoint().getLongitude());
                sr.setType(poi.getTypeDes());
                float dist = calculateDistance(myLocation.getLatitude(), myLocation.getLongitude(),
                        poi.getLatLonPoint().getLatitude(), poi.getLatLonPoint().getLongitude());
                sr.setDistance(dist);
                searchResults.add(sr);
            }
            searchResultAdapter.notifyDataSetChanged();

            if (searchResults.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                rvSearchResults.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.GONE);
                rvSearchResults.setVisibility(View.VISIBLE);
            }
        } else {
            // 搜索失败或结果为空
            String errMsg;
            if (errorCode == 1002) {
                errMsg = "API Key 无效，请检查高德Key配置";
            } else if (errorCode == 1003) {
                errMsg = "API Key 过期";
            } else if (errorCode == 1001) {
                errMsg = "签名验证失败，请检查签名文件";
            } else if (errorCode == 1802) {
                errMsg = "网络连接失败，请检查网络";
            } else if (errorCode != AMapException.CODE_AMAP_SUCCESS) {
                errMsg = "搜索失败(code:" + errorCode + ")";
            } else {
                errMsg = "未找到结果";
            }
            Log.e(TAG, errMsg + ", result=" + (result != null ? result.toString() : "null"));
            emptyView.setVisibility(View.VISIBLE);
            rvSearchResults.setVisibility(View.GONE);
            Toast.makeText(this, errMsg, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPoiItemSearched(PoiItemV2 poiItemV2, int i) {}

    @Override
    public void onVisualSearched(com.amap.api.services.poisearch.VisualSearchResult visualSearchResult, int i) {}

    /**
     * 使用Haversine公式计算两点间距离（米）
     */
    private float calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final double EARTH_RADIUS = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (float) (EARTH_RADIUS * c);
    }

    // ==================== Inputtips.InputtipsListener ====================

    @Override
    public void onGetInputtips(List<Tip> tipList, int errorCode) {
        // 输入提示，这里简化处理，不显示下拉提示
        // 实际项目中可以在这里做搜索建议列表
        Log.d(TAG, "获取到输入提示: " + (tipList != null ? tipList.size() : 0) + "条");
    }

    // ==================== 搜索历史管理 ====================

    /**
     * 加载搜索历史
     */
    private void loadHistory() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_HISTORY, "[]");
        historySet.clear();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                historySet.add(arr.getString(i));
            }
        } catch (JSONException e) {
            Log.e(TAG, "加载历史记录失败: " + e.getMessage());
        }
    }

    /**
     * 保存搜索关键词到历史（去重，新关键词放在最前面，最多保留MAX_HISTORY条）
     */
    private void saveHistory(String keyword) {
        if (keyword.isEmpty()) return;
        // 去重：先移除再插入到最前面
        historySet.remove(keyword);
        // 如果超过上限，移除最旧的
        if (historySet.size() >= MAX_HISTORY) {
            String oldest = historySet.iterator().next();
            historySet.remove(oldest);
        }
        historySet.add(keyword);

        // 持久化
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_HISTORY, new JSONArray(new ArrayList<>(historySet)).toString()).apply();

        // 刷新历史UI
        renderHistory();
    }

    /**
     * 渲染搜索历史标签到FlexboxLayout
     */
    private void renderHistory() {
        flexboxHistory.removeAllViews();
        if (historySet.isEmpty()) {
            historyLayout.setVisibility(View.GONE);
            return;
        }
        historyLayout.setVisibility(View.VISIBLE);

        // 反转顺序，最新的在前
        List<String> reversedList = new ArrayList<>(historySet);
        java.util.Collections.reverse(reversedList);

        for (String keyword : reversedList) {
            TextView tag = createHistoryTag(keyword);
            flexboxHistory.addView(tag);
        }

        // 末尾追加"清除历史记录"标签
        TextView clearTag = createClearHistoryTag();
        flexboxHistory.addView(clearTag);
    }

    /**
     * 创建单个历史标签View
     */
    private TextView createHistoryTag(String keyword) {
        TextView tag = new TextView(this);
        tag.setText(keyword);
        tag.setTextColor(0xFF333333);
        tag.setTextSize(13);
        tag.setBackgroundResource(R.drawable.bg_history_tag);
        tag.setPadding(28, 14, 28, 14);
        tag.setSingleLine(true);
        tag.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tag.setMaxWidth(dpToPx(160));

        FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 20, 20);
        tag.setLayoutParams(params);

        tag.setOnClickListener(v -> {
            etSearch.setText(keyword);
            etSearch.setSelection(keyword.length());
            performSearch(keyword);
        });

        return tag;
    }

    /**
     * 创建"清除历史记录"标签
     */
    private TextView createClearHistoryTag() {
        TextView tag = new TextView(this);
        tag.setText("清除历史记录");
        tag.setTextColor(0xFFE74C3C);
        tag.setTextSize(13);
        tag.setBackgroundResource(R.drawable.bg_history_tag);
        tag.setPadding(28, 14, 28, 14);
        tag.setSingleLine(true);

        FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 20, 20);
        tag.setLayoutParams(params);

        tag.setOnClickListener(v -> clearHistory());

        return tag;
    }

    /**
     * 清除所有搜索历史
     */
    private void clearHistory() {
        historySet.clear();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_HISTORY, "[]").apply();
        renderHistory();
        Toast.makeText(this, "历史记录已清除", Toast.LENGTH_SHORT).show();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }
}
