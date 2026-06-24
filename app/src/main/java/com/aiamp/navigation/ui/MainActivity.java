package com.aiamp.navigation.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aiamp.navigation.R;
import com.aiamp.navigation.adapter.NearbyPoiAdapter;
import com.aiamp.navigation.model.SearchResult;
import com.aiamp.navigation.util.AddressStore;
import com.aiamp.navigation.util.FavoriteStore;
import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.core.PoiItemV2;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.poisearch.PoiResultV2;
import com.amap.api.services.poisearch.PoiSearchV2;

import java.util.ArrayList;
import java.util.List;

/**
 * 主页面 - 类似高德地图手机端首页
 * 车机横屏设计：左侧面板 + 右侧地图
 */
public class MainActivity extends AppCompatActivity implements
        AMapLocationListener, PoiSearchV2.OnPoiSearchListener, GeocodeSearch.OnGeocodeSearchListener {

    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    private MapView mapView;
    private AMap aMap;
    private RecyclerView rvNearbyPoi;
    private NearbyPoiAdapter nearbyPoiAdapter;
    private List<SearchResult> nearbyPoiList = new ArrayList<>();

    // 定位相关
    private AMapLocationClient locationClient;
    private AMapLocationClientOption locationOption;
    private LatLonPoint currentLocation;
    private boolean isFirstLocate = true;

    // 搜索相关
    private PoiSearchV2.Query currentQuery;
    private PoiSearchV2 poiSearch;

    // 当前城市名
    private String currentCity = "";

    // 左侧面板隐藏/显示
    private LinearLayout leftPanel;
    private ImageButton btnTogglePanel;
    private boolean isPanelVisible = true;
    private float panelWidth = 0;  // 面板实际宽度，动画时计算

    // 地址设置面板
    private LinearLayout addressSetupPanel;
    private TextView tvAddressSetupTitle;
    private EditText etAddressInput;
    private TextView tvHomeAddressTitle, tvHomeAddressDetail;
    private TextView tvCompanyAddressTitle, tvCompanyAddressDetail;
    private TextView btnSaveAddress;
    private String currentAddressType = "公司"; // "家" 或 "公司"
    private double selectedAddressLat = 0;
    private double selectedAddressLng = 0;

    // 逆地理编码
    private GeocodeSearch geocodeSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initMap(savedInstanceState);
        initViews();
        checkPermissions();
        initLocation();
        initNearbyPoi();
    }

    private void initMap(Bundle savedInstanceState) {
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        aMap = mapView.getMap();
        if (aMap != null) {
            setupMap();
        }
    }

    private void setupMap() {
        // 设置地图类型
        aMap.setMapType(AMap.MAP_TYPE_NORMAL);

        // UI设置
        UiSettings uiSettings = aMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(false);
        uiSettings.setMyLocationButtonEnabled(false);
        uiSettings.setCompassEnabled(true);
        uiSettings.setScaleControlsEnabled(true);
        uiSettings.setAllGesturesEnabled(true);

        // 设置定位样式
        MyLocationStyle locationStyle = new MyLocationStyle();
        locationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);
        locationStyle.interval(2000);
        locationStyle.showMyLocation(true);
        aMap.setMyLocationStyle(locationStyle);

        // 设置地图点击监听
        aMap.setOnMapClickListener(latLng -> {
            // 点击地图收起键盘等操作
        });

        // 设置POI点击监听
        aMap.setOnPOIClickListener(poi -> {
            // 点击POI后弹出选择：导航 或 收藏
            SearchResult result = new SearchResult(poi.getName(), poi.getPoiId(),
                    poi.getCoordinate().latitude, poi.getCoordinate().longitude);
            showPoiActionDialog(result);
        });
    }

    private void initViews() {
        // 搜索栏
        View searchBar = findViewById(R.id.searchBar);
        searchBar.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SearchActivity.class);
            if (currentLocation != null) {
                intent.putExtra("lat", currentLocation.getLatitude());
                intent.putExtra("lng", currentLocation.getLongitude());
            }
            if (!currentCity.isEmpty()) {
                intent.putExtra("city", currentCity);
            }
            startActivity(intent);
        });

        // 语音搜索
        findViewById(R.id.ivVoiceSearch).setOnClickListener(v -> {
            Toast.makeText(this, "语音搜索功能开发中", Toast.LENGTH_SHORT).show();
        });

        // 功能按钮
        findViewById(R.id.btnDrive).setOnClickListener(v -> {
            Toast.makeText(this, "请先搜索目的地", Toast.LENGTH_SHORT).show();
        });

        // 回家/去公司快捷按钮
        findViewById(R.id.btnGoHome).setOnClickListener(v -> {
            quickNavigate("家");
        });
        findViewById(R.id.btnGoWork).setOnClickListener(v -> {
            quickNavigate("公司");
        });

        // 我的位置按钮
        findViewById(R.id.btnMyLocation).setOnClickListener(v -> {
            locateToMyPosition();
        });

        // 缩放按钮
        findViewById(R.id.btnZoomIn).setOnClickListener(v -> {
            aMap.animateCamera(CameraUpdateFactory.zoomIn());
        });
        findViewById(R.id.btnZoomOut).setOnClickListener(v -> {
            aMap.animateCamera(CameraUpdateFactory.zoomOut());
        });

        // 收藏/设置按钮
        findViewById(R.id.btnFavorite).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FavoriteActivity.class);
            startActivity(intent);
        });
        findViewById(R.id.btnSettings).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // 附近POI列表
        rvNearbyPoi = findViewById(R.id.rvNearbyPoi);
        rvNearbyPoi.setLayoutManager(new LinearLayoutManager(this));
        nearbyPoiAdapter = new NearbyPoiAdapter(nearbyPoiList, this::navigateToRoute);
        rvNearbyPoi.setAdapter(nearbyPoiAdapter);

        // 左侧面板隐藏/显示
        initPanelToggle();

        // 地址设置面板
        initAddressSetupPanel();
    }

    /**
     * 初始化左侧面板的隐藏/显示功能
     */
    private void initPanelToggle() {
        leftPanel = findViewById(R.id.leftPanel);
        btnTogglePanel = findViewById(R.id.btnTogglePanel);

        btnTogglePanel.setOnClickListener(v -> togglePanel());
    }

    /**
     * 切换左侧面板的显示/隐藏
     */
    private void togglePanel() {
        // 首次点击时获取面板实际宽度
        if (panelWidth == 0) {
            panelWidth = leftPanel.getWidth();
        }

        if (isPanelVisible) {
            // 隐藏面板：向左平移滑出
            leftPanel.animate()
                    .translationX(-panelWidth)
                    .setDuration(300)
                    .withEndAction(() -> {
                        leftPanel.setVisibility(View.GONE);
                        // 调整地图约束：Start 改为 parent Start
                        ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) mapView.getLayoutParams();
                        lp.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
                        lp.startToEnd = ConstraintLayout.LayoutParams.UNSET;
                        mapView.setLayoutParams(lp);
                        // 切换为展开双箭头（>>），表示可以展开面板
                        btnTogglePanel.setImageResource(R.drawable.ic_panel_toggle_expand);
                    })
                    .start();
            isPanelVisible = false;
        } else {
            // 显示面板：先调整地图约束，再滑入面板
            leftPanel.setVisibility(View.VISIBLE);
            leftPanel.setTranslationX(-panelWidth);
            ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) mapView.getLayoutParams();
            lp.startToStart = ConstraintLayout.LayoutParams.UNSET;
            lp.startToEnd = R.id.leftPanel;
            mapView.setLayoutParams(lp);

            leftPanel.animate()
                    .translationX(0)
                    .setDuration(300)
                    .start();
            // 切换为收起双箭头（<<），表示可以收起面板
            btnTogglePanel.setImageResource(R.drawable.ic_panel_toggle);
            isPanelVisible = true;
        }
    }

    private void checkPermissions() {
        List<String> permissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_PHONE_STATE);
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissions.toArray(new String[0]),
                    LOCATION_PERMISSION_REQUEST);
        }
    }

    private void initLocation() {
        try {
            locationClient = new AMapLocationClient(getApplicationContext());
            locationClient.setLocationListener(this);

            locationOption = new AMapLocationClientOption();
            locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            locationOption.setInterval(2000);
            locationOption.setNeedAddress(true);
            locationOption.setOnceLocation(false);
            locationOption.setMockEnable(true);

            locationClient.setLocationOption(locationOption);
            locationClient.startLocation();
        } catch (Exception e) {
            Log.e(TAG, "初始化定位失败: " + e.getMessage());
        }
    }

    private void initNearbyPoi() {
        // 用一个占位query初始化，后续searchNearbyPoi中通过setQuery更新
        try {
            PoiSearchV2.Query initQuery = new PoiSearchV2.Query("", "", "");
            poiSearch = new PoiSearchV2(this, initQuery);
            poiSearch.setOnPoiSearchListener(this);
        } catch (AMapException e) {
            Log.e(TAG, "初始化POI搜索失败: " + e.getMessage());
        }
    }

    private void searchNearbyPoi() {
        if (currentLocation == null || poiSearch == null) return;

        currentQuery = new PoiSearchV2.Query("停车场|加油站|餐饮|购物",
                "", currentCity);
        currentQuery.setPageSize(20);
        currentQuery.setPageNum(0);
        currentQuery.setLocation(currentLocation);
        currentQuery.setDistanceSort(true);

        poiSearch.setQuery(currentQuery);
        poiSearch.searchPOIAsyn();
        Log.d(TAG, "附近POI搜索: location=" + currentLocation + ", city=" + currentCity);
    }

    private void locateToMyPosition() {
        if (currentLocation != null && aMap != null) {
            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new com.amap.api.maps.model.LatLng(
                            currentLocation.getLatitude(),
                            currentLocation.getLongitude()),
                    15));
        }
    }

    /**
     * 点击地图POI时弹出操作对话框：导航 / 收藏
     */
    private void showPoiActionDialog(SearchResult poi) {
        new AlertDialog.Builder(this)
                .setTitle(poi.getName())
                .setMessage(poi.getAddress())
                .setPositiveButton("导航去这里", (dialog, which) -> {
                    navigateToRoute(poi);
                })
                .setNegativeButton("收藏", (dialog, which) -> {
                    boolean added = FavoriteStore.add(MainActivity.this, poi);
                    if (added) {
                        // 收藏成功后弹出标记编辑对话框
                        int addedIndex = FavoriteStore.loadAll(MainActivity.this).size() - 1;
                        showTagEditDialog(addedIndex, poi.getName());
                    } else {
                        Toast.makeText(MainActivity.this, "该地点已收藏或已达上限(100个)", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("取消", null)
                .show();
    }

    /**
     * 弹出标记编辑对话框，用户可自由输入标记内容（最长50个汉字）
     */
    private void showTagEditDialog(int index, String placeName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("编辑标记 - " + placeName);

        final EditText input = new EditText(this);
        input.setHint("如：美食、历史、常去...（最多50个汉字）");
        input.setSingleLine(true);
        input.setPadding(32, 24, 32, 24);
        input.setTextSize(14);
        input.setFilters(new android.text.InputFilter[]{
                new android.text.InputFilter.LengthFilter(50)
        });

        final TextView tvCharCount = new TextView(this);
        tvCharCount.setText("0/50");
        tvCharCount.setTextSize(11);
        tvCharCount.setPadding(32, 0, 32, 16);
        tvCharCount.setTextColor(0xFF999999);

        input.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int len = s.length();
                tvCharCount.setText(len + "/50");
                if (len >= 50) {
                    tvCharCount.setTextColor(0xFFE74C3C);
                } else if (len > 40) {
                    tvCharCount.setTextColor(0xFFE65100);
                } else {
                    tvCharCount.setTextColor(0xFF999999);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // 加载已有标记
        String existingTag = FavoriteStore.loadAll(this).get(index).getTag();
        if (existingTag != null && !existingTag.isEmpty()) {
            input.setText(existingTag);
            input.setSelection(existingTag.length());
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(input);
        layout.addView(tvCharCount);
        builder.setView(layout);

        builder.setPositiveButton("保存", (dialog, which) -> {
            String tag = input.getText().toString().trim();
            int chineseCharCount = countChineseChars(tag);
            if (chineseCharCount > 50) {
                Toast.makeText(MainActivity.this,
                        "标记内容过长，最多50个汉字", Toast.LENGTH_SHORT).show();
                return;
            }
            FavoriteStore.updateTag(MainActivity.this, index, tag);
            if (!tag.isEmpty()) {
                Toast.makeText(MainActivity.this,
                        "标记已保存: " + tag, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("清空标记", (dialog, which) -> {
            FavoriteStore.updateTag(MainActivity.this, index, "");
            Toast.makeText(MainActivity.this,
                    "标记已清空", Toast.LENGTH_SHORT).show();
        });
        builder.setNeutralButton("取消", (dialog, which) -> {
            // 不做任何操作，仅关闭对话框
        });

        builder.show();
    }

    /**
     * 计算字符串中的汉字数量
     */
    private int countChineseChars(String s) {
        if (s == null || s.isEmpty()) return 0;
        int count = 0;
        for (char c : s.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                count++;
            }
        }
        return count;
    }

    private void quickNavigate(String keyword) {
        if (currentLocation == null) {
            Toast.makeText(this, "正在获取当前位置...", Toast.LENGTH_SHORT).show();
            return;
        }

        // 优先使用本地存储的地址
        AddressStore.AddressInfo savedAddress;
        if ("公司".equals(keyword)) {
            savedAddress = AddressStore.getCompanyAddress(this);
        } else {
            savedAddress = AddressStore.getHomeAddress(this);
        }

        if (savedAddress != null && savedAddress.isValid()) {
            // 有已保存地址，直接导航
            SearchResult result = new SearchResult(savedAddress.name, savedAddress.address,
                    savedAddress.latitude, savedAddress.longitude);
            navigateToRoute(result);
            return;
        }

        // 没有保存地址，打开设置面板
        openAddressSetupPanel(keyword);
    }

    /**
     * 初始化地址设置面板
     */
    private void initAddressSetupPanel() {
        addressSetupPanel = findViewById(R.id.addressSetupPanel);
        tvAddressSetupTitle = findViewById(R.id.tvAddressSetupTitle);
        etAddressInput = findViewById(R.id.etAddressInput);
        tvHomeAddressTitle = findViewById(R.id.tvHomeAddressTitle);
        tvHomeAddressDetail = findViewById(R.id.tvHomeAddressDetail);
        tvCompanyAddressTitle = findViewById(R.id.tvCompanyAddressTitle);
        tvCompanyAddressDetail = findViewById(R.id.tvCompanyAddressDetail);
        btnSaveAddress = findViewById(R.id.btnSaveAddress);

        // 返回按钮
        findViewById(R.id.ivAddressSetupBack).setOnClickListener(v -> closeAddressSetupPanel());

        // 搜索输入框回车搜索
        etAddressInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String keyword = etAddressInput.getText().toString().trim();
                if (!TextUtils.isEmpty(keyword)) {
                    searchAddressForSetup(keyword);
                }
                return true;
            }
            return false;
        });

        // 家地址点击
        findViewById(R.id.llHomeAddress).setOnClickListener(v -> {
            if ("家".equals(currentAddressType)) {
                // 当前设置的是家地址，用家的已保存地址填充
                AddressStore.AddressInfo info = AddressStore.getHomeAddress(MainActivity.this);
                if (info != null && info.isValid()) {
                    etAddressInput.setText(info.address);
                    selectedAddressLat = info.latitude;
                    selectedAddressLng = info.longitude;
                }
            }
        });

        // 公司地址点击
        findViewById(R.id.llCompanyAddress).setOnClickListener(v -> {
            if ("公司".equals(currentAddressType)) {
                AddressStore.AddressInfo info = AddressStore.getCompanyAddress(MainActivity.this);
                if (info != null && info.isValid()) {
                    etAddressInput.setText(info.address);
                    selectedAddressLat = info.latitude;
                    selectedAddressLng = info.longitude;
                }
            }
        });

        // 地图选点按钮
        findViewById(R.id.btnMapPick).setOnClickListener(v -> {
            // 跳转到 MapPickActivity 进行地图选点
            Intent intent = new Intent(MainActivity.this, MapPickActivity.class);
            intent.putExtra("address_type", currentAddressType);
            if (currentLocation != null) {
                intent.putExtra("lat", currentLocation.getLatitude());
                intent.putExtra("lng", currentLocation.getLongitude());
            }
            if (!currentCity.isEmpty()) {
                intent.putExtra("city", currentCity);
            }
            startActivityForResult(intent, 2001);
        });

        // 我的位置按钮 - 获取当前GPS定位并逆地理编码填充到编辑框
        findViewById(R.id.btnMyLocationPick).setOnClickListener(v -> {
            pickMyCurrentLocation();
        });

        // 保存按钮
        btnSaveAddress.setOnClickListener(v -> {
            String addressText = etAddressInput.getText().toString().trim();
            if (TextUtils.isEmpty(addressText)) {
                Toast.makeText(this, "请输入地址", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedAddressLat == 0 && selectedAddressLng == 0) {
                // 没有坐标，用输入的地址做POI搜索
                searchAndSaveAddress(addressText);
            } else {
                saveCurrentAddress();
            }
        });
    }

    /**
     * 打开地址设置面板
     */
    private void openAddressSetupPanel(String type) {
        currentAddressType = type;
        selectedAddressLat = 0;
        selectedAddressLng = 0;
        etAddressInput.setText("");

        if ("公司".equals(type)) {
            tvAddressSetupTitle.setText("设置公司地址");
            etAddressInput.setHint("请输入公司地址");
            tvHomeAddressTitle.setText("家");
            tvCompanyAddressTitle.setText("公司");
        } else {
            tvAddressSetupTitle.setText("设置家庭地址");
            etAddressInput.setHint("请输入家庭地址");
            tvHomeAddressTitle.setText("家");
            tvCompanyAddressTitle.setText("公司");
        }

        // 刷新已保存地址显示
        updateSavedAddressDisplay();

        leftPanel.setVisibility(View.GONE);
        addressSetupPanel.setVisibility(View.VISIBLE);
    }

    /**
     * 关闭地址设置面板
     */
    private void closeAddressSetupPanel() {
        addressSetupPanel.setVisibility(View.GONE);
        leftPanel.setVisibility(View.VISIBLE);
    }

    /**
     * 更新已保存地址的显示
     */
    private void updateSavedAddressDisplay() {
        AddressStore.AddressInfo home = AddressStore.getHomeAddress(this);
        if (home != null && home.isValid()) {
            tvHomeAddressDetail.setText(home.address);
            tvHomeAddressDetail.setTextColor(getResources().getColor(R.color.gray_dark));
        } else {
            tvHomeAddressDetail.setText("未设置");
            tvHomeAddressDetail.setTextColor(getResources().getColor(R.color.gray));
        }

        AddressStore.AddressInfo company = AddressStore.getCompanyAddress(this);
        if (company != null && company.isValid()) {
            tvCompanyAddressDetail.setText(company.address);
            tvCompanyAddressDetail.setTextColor(getResources().getColor(R.color.gray_dark));
        } else {
            tvCompanyAddressDetail.setText("未设置");
            tvCompanyAddressDetail.setTextColor(getResources().getColor(R.color.gray));
        }
    }

    /**
     * 我的位置选点 - 获取当前GPS定位，逆地理编码后填充到编辑框
     */
    private void pickMyCurrentLocation() {
        if (currentLocation == null) {
            Toast.makeText(this, "正在获取当前位置，请稍后再试...", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "正在获取当前位置...", Toast.LENGTH_SHORT).show();

        // 先把当前定位坐标暂存
        selectedAddressLat = currentLocation.getLatitude();
        selectedAddressLng = currentLocation.getLongitude();

        // 逆地理编码获取地址描述
        try {
            if (geocodeSearch == null) {
                geocodeSearch = new GeocodeSearch(this);
                geocodeSearch.setOnGeocodeSearchListener(this);
            }
            LatLonPoint point = new LatLonPoint(currentLocation.getLatitude(), currentLocation.getLongitude());
            RegeocodeQuery query = new RegeocodeQuery(point, 200, GeocodeSearch.AMAP);
            geocodeSearch.getFromLocationAsyn(query);
        } catch (AMapException e) {
            Log.e(TAG, "逆地理编码异常: " + e.getMessage());
            // 逆地理编码失败，直接使用坐标字符串填充
            etAddressInput.setText("我的位置 (" + selectedAddressLat + ", " + selectedAddressLng + ")");
            Toast.makeText(this, "已获取当前位置", Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== GeocodeSearch.OnGeocodeSearchListener ====================

    @Override
    public void onRegeocodeSearched(RegeocodeResult result, int errorCode) {
        if (errorCode == AMapException.CODE_AMAP_SUCCESS && result != null
                && result.getRegeocodeAddress() != null) {
            RegeocodeAddress regeocodeAddress = result.getRegeocodeAddress();
            String formatAddress = regeocodeAddress.getFormatAddress();
            String pickedName = formatAddress;

            // 优先使用具体POI名称
            if (regeocodeAddress.getPois() != null && !regeocodeAddress.getPois().isEmpty()) {
                PoiItem firstPoi = regeocodeAddress.getPois().get(0);
                pickedName = firstPoi.getTitle();
            }

            String displayText = pickedName + " - " + formatAddress;
            etAddressInput.setText(displayText);
            Toast.makeText(this, "已定位: " + pickedName, Toast.LENGTH_SHORT).show();
        } else {
            // 逆地理编码失败，使用坐标
            etAddressInput.setText("我的位置 (" + selectedAddressLat + ", " + selectedAddressLng + ")");
            Toast.makeText(this, "已获取当前位置", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onGeocodeSearched(com.amap.api.services.geocoder.GeocodeResult geocodeResult, int i) {
        // 不需要正向地理编码
    }

    /**
     * 搜索地址（设置面板内）
     */
    private void searchAddressForSetup(String keyword) {
        try {
            PoiSearchV2.Query query = new PoiSearchV2.Query(keyword, "", currentCity);
            query.setPageSize(5);
            query.setPageNum(0);
            if (currentLocation != null) {
                query.setLocation(currentLocation);
            }

            PoiSearchV2 search = new PoiSearchV2(this, query);
            search.setOnPoiSearchListener(new PoiSearchV2.OnPoiSearchListener() {
                @Override
                public void onPoiSearched(PoiResultV2 result, int errorCode) {
                    if (errorCode == AMapException.CODE_AMAP_SUCCESS && result != null
                            && result.getPois() != null && !result.getPois().isEmpty()) {
                        PoiItemV2 poi = result.getPois().get(0);
                        etAddressInput.setText(poi.getTitle() + " - " +
                                poi.getProvinceName() + poi.getCityName() + poi.getAdName() + poi.getSnippet());
                        selectedAddressLat = poi.getLatLonPoint().getLatitude();
                        selectedAddressLng = poi.getLatLonPoint().getLongitude();
                        Toast.makeText(MainActivity.this, "已找到地址", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "未找到该地址", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onPoiItemSearched(PoiItemV2 poiItemV2, int i) {}

                @Override
                public void onVisualSearched(com.amap.api.services.poisearch.VisualSearchResult visualSearchResult, int i) {}
            });
            search.searchPOIAsyn();
        } catch (AMapException e) {
            Log.e(TAG, "地址搜索异常: " + e.getMessage());
            Toast.makeText(this, "搜索失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 搜索并保存地址
     */
    private void searchAndSaveAddress(String keyword) {
        try {
            PoiSearchV2.Query query = new PoiSearchV2.Query(keyword, "", currentCity);
            query.setPageSize(1);
            if (currentLocation != null) {
                query.setLocation(currentLocation);
            }

            PoiSearchV2 search = new PoiSearchV2(this, query);
            search.setOnPoiSearchListener(new PoiSearchV2.OnPoiSearchListener() {
                @Override
                public void onPoiSearched(PoiResultV2 result, int errorCode) {
                    if (errorCode == AMapException.CODE_AMAP_SUCCESS && result != null
                            && result.getPois() != null && !result.getPois().isEmpty()) {
                        PoiItemV2 poi = result.getPois().get(0);
                        selectedAddressLat = poi.getLatLonPoint().getLatitude();
                        selectedAddressLng = poi.getLatLonPoint().getLongitude();
                        saveCurrentAddress();
                    } else {
                        Toast.makeText(MainActivity.this,
                                "未找到该地址，请尝试更详细的关键词", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onPoiItemSearched(PoiItemV2 poiItemV2, int i) {}

                @Override
                public void onVisualSearched(com.amap.api.services.poisearch.VisualSearchResult visualSearchResult, int i) {}
            });
            search.searchPOIAsyn();
        } catch (AMapException e) {
            Log.e(TAG, "搜索保存地址异常: " + e.getMessage());
            Toast.makeText(this, "搜索失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 保存当前选中的地址
     */
    private void saveCurrentAddress() {
        String addressText = etAddressInput.getText().toString().trim();
        if (TextUtils.isEmpty(addressText) || selectedAddressLat == 0 || selectedAddressLng == 0) {
            Toast.makeText(this, "请先搜索或选择地址", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("公司".equals(currentAddressType)) {
            AddressStore.saveCompanyAddress(this, currentAddressType, addressText,
                    selectedAddressLat, selectedAddressLng);
            Toast.makeText(this, "公司地址已保存", Toast.LENGTH_SHORT).show();
        } else {
            AddressStore.saveHomeAddress(this, currentAddressType, addressText,
                    selectedAddressLat, selectedAddressLng);
            Toast.makeText(this, "家庭地址已保存", Toast.LENGTH_SHORT).show();
        }

        // 保存后直接导航
        closeAddressSetupPanel();
        quickNavigate(currentAddressType);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2001 && resultCode == RESULT_OK && data != null) {
            // 从 MapPickActivity 地图选点返回
            String name = data.getStringExtra("name");
            String address = data.getStringExtra("address");
            double lat = data.getDoubleExtra("lat", 0);
            double lng = data.getDoubleExtra("lng", 0);

            if (lat != 0 && lng != 0 && name != null) {
                etAddressInput.setText(name + " - " + address);
                selectedAddressLat = lat;
                selectedAddressLng = lng;
                Toast.makeText(this, "已选择: " + name, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void navigateToRoute(SearchResult destination) {
        if (currentLocation == null) {
            Toast.makeText(this, "正在获取当前位置...", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(MainActivity.this, RouteActivity.class);
        intent.putExtra("start_name", "我的位置");
        intent.putExtra("start_lat", currentLocation.getLatitude());
        intent.putExtra("start_lng", currentLocation.getLongitude());
        intent.putExtra("end_name", destination.getName());
        intent.putExtra("end_lat", destination.getLatitude());
        intent.putExtra("end_lng", destination.getLongitude());
        startActivity(intent);
    }

    // ==================== AMapLocationListener ====================

    @Override
    public void onLocationChanged(AMapLocation location) {
        if (location != null && location.getErrorCode() == 0) {
            currentLocation = new LatLonPoint(location.getLatitude(), location.getLongitude());
            // 保存当前城市名（用于POI搜索city参数）
            if (location.getCity() != null && !location.getCity().isEmpty()) {
                currentCity = location.getCity();
            }

            if (isFirstLocate) {
                isFirstLocate = false;
                // 首次定位，移动地图到当前位置
                if (aMap != null) {
                    aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new com.amap.api.maps.model.LatLng(
                                    location.getLatitude(),
                                    location.getLongitude()),
                            15));
                }
                // 首次定位后搜索附近POI
                searchNearbyPoi();
            }
        } else {
            if (location != null) {
                Log.e(TAG, "定位失败: " + location.getErrorInfo());
            }
        }
    }

    // ==================== PoiSearchV2.OnPoiSearchListener ====================

    @Override
    public void onPoiSearched(PoiResultV2 result, int errorCode) {
        if (errorCode == AMapException.CODE_AMAP_SUCCESS && result != null
                && result.getPois() != null) {
            nearbyPoiList.clear();
            for (PoiItemV2 poi : result.getPois()) {
                SearchResult sr = new SearchResult();
                sr.setName(poi.getTitle());
                sr.setAddress(poi.getProvinceName() + poi.getCityName()
                        + poi.getAdName() + poi.getSnippet());
                sr.setLatitude(poi.getLatLonPoint().getLatitude());
                sr.setLongitude(poi.getLatLonPoint().getLongitude());
                sr.setType(poi.getTypeDes());
                if (currentLocation != null) {
                    float dist = calculateDistance(currentLocation.getLatitude(), currentLocation.getLongitude(),
                            poi.getLatLonPoint().getLatitude(), poi.getLatLonPoint().getLongitude());
                    sr.setDistance(dist);
                }
                nearbyPoiList.add(sr);
            }
            nearbyPoiAdapter.notifyDataSetChanged();
        } else {
            Log.e(TAG, "POI搜索失败, errorCode: " + errorCode);
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
        final double EARTH_RADIUS = 6371000; // 地球半径（米）
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (float) (EARTH_RADIUS * c);
    }

    // ==================== 生命周期 ====================

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) mapView.onDestroy();
        if (locationClient != null) {
            locationClient.stopLocation();
            locationClient.onDestroy();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                initLocation();
            } else {
                Toast.makeText(this, "需要定位权限才能使用导航功能", Toast.LENGTH_LONG).show();
            }
        }
    }
}
