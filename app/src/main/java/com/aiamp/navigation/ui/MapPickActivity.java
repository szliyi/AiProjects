package com.aiamp.navigation.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.aiamp.navigation.R;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.Projection;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.LatLng;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;

/**
 * 地图选点页面 - 滑动地图选择位置，屏幕中心十字标记即为选中点
 * 用户手指滑动地图时，屏幕中心始终对应选中位置
 * 通过逆地理编码获取地址信息，返回给调用方
 */
public class MapPickActivity extends AppCompatActivity implements
        AMap.OnCameraChangeListener, GeocodeSearch.OnGeocodeSearchListener {

    private static final String TAG = "MapPickActivity";
    private static final long REVERSE_GEOCODE_DELAY_MS = 500; // 地图停止滑动后延迟500ms再查地址

    private MapView mapView;
    private AMap aMap;
    private TextView tvPickedAddress;
    private TextView tvConfirm;
    private GeocodeSearch geocodeSearch;

    private LatLng pickedLatLng;
    private String pickedAddress = "";
    private String pickedName = "";

    private double initLat, initLng;
    private String currentCity = "";

    // 逆地理编码防抖：地图滑动停止后才查询
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private final Runnable debounceRunnable = new Runnable() {
        @Override
        public void run() {
            if (aMap != null) {
                LatLng center = aMap.getCameraPosition().target;
                pickedLatLng = center;
                reverseGeocode(center);
            }
        }
    };

    // 是否正在逆地理编码中（避免重复请求）
    private boolean isGeocoding = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_pick);

        // 获取初始位置
        initLat = getIntent().getDoubleExtra("lat", 39.904989);
        initLng = getIntent().getDoubleExtra("lng", 116.405285);
        currentCity = getIntent().getStringExtra("city");
        if (currentCity == null) currentCity = "";

        pickedLatLng = new LatLng(initLat, initLng);

        initMap(savedInstanceState);
        initViews();
    }

    private void initMap(Bundle savedInstanceState) {
        mapView = findViewById(R.id.mapPickView);
        mapView.onCreate(savedInstanceState);

        aMap = mapView.getMap();
        if (aMap != null) {
            setupMap();
        }
    }

    private void setupMap() {
        aMap.setMapType(AMap.MAP_TYPE_NORMAL);

        UiSettings uiSettings = aMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(false);
        uiSettings.setMyLocationButtonEnabled(false);
        uiSettings.setCompassEnabled(true);
        uiSettings.setScaleControlsEnabled(true);
        uiSettings.setAllGesturesEnabled(true);
        uiSettings.setScrollGesturesEnabled(true);

        // 设置地图移动监听（滑动地图时中心点变化）
        aMap.setOnCameraChangeListener(this);

        // 移动地图到初始位置
        aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pickedLatLng, 16));

        // 初始化逆地理编码
        try {
            geocodeSearch = new GeocodeSearch(this);
            geocodeSearch.setOnGeocodeSearchListener(this);
        } catch (Exception e) {
            Log.e(TAG, "初始化GeocodeSearch失败: " + e.getMessage());
        }

        // 首次进入时逆地理编码获取初始位置地址
        reverseGeocode(pickedLatLng);
    }

    private void initViews() {
        tvPickedAddress = findViewById(R.id.tvPickedAddress);

        // 返回按钮
        findViewById(R.id.ivMapPickBack).setOnClickListener(v -> finish());

        // 确认按钮
        tvConfirm = findViewById(R.id.tvMapPickConfirm);
        tvConfirm.setOnClickListener(v -> confirmPick());

        tvPickedAddress.setText("正在获取地址...");
    }

    // ==================== AMap.OnCameraChangeListener ====================

    /**
     * 地图相机变化时触发（手指滑动地图过程中持续回调）
     */
    @Override
    public void onCameraChange(com.amap.api.maps.model.CameraPosition cameraPosition) {
        // 地图正在移动，取消之前的防抖任务
        debounceHandler.removeCallbacks(debounceRunnable);
        // 显示"移动地图选择位置"提示
        if (!isGeocoding) {
            tvPickedAddress.setText("移动地图选择位置...");
        }
    }

    /**
     * 地图相机变化结束时触发（手指抬起、地图滑动停止）
     */
    @Override
    public void onCameraChangeFinish(com.amap.api.maps.model.CameraPosition cameraPosition) {
        // 地图停止滑动，延迟后取屏幕中心坐标进行逆地理编码
        tvPickedAddress.setText("正在获取地址...");
        debounceHandler.removeCallbacks(debounceRunnable);
        debounceHandler.postDelayed(debounceRunnable, REVERSE_GEOCODE_DELAY_MS);
    }

    // ==================== 逆地理编码 ====================

    /**
     * 逆地理编码 - 根据坐标获取地址信息
     */
    private void reverseGeocode(LatLng latLng) {
        if (geocodeSearch == null) {
            try {
                geocodeSearch = new GeocodeSearch(this);
                geocodeSearch.setOnGeocodeSearchListener(this);
            } catch (Exception e) {
                tvPickedAddress.setText("地址解析服务初始化失败");
                return;
            }
        }

        isGeocoding = true;
        LatLonPoint point = new LatLonPoint(latLng.latitude, latLng.longitude);
        RegeocodeQuery query = new RegeocodeQuery(point, 500, GeocodeSearch.AMAP);
        geocodeSearch.getFromLocationAsyn(query);
    }

    /**
     * 确认选点，返回结果给调用方
     */
    private void confirmPick() {
        if (pickedLatLng == null) {
            Toast.makeText(this, "请移动地图选择位置", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pickedAddress.isEmpty()) {
            Toast.makeText(this, "正在获取地址信息，请稍候...", Toast.LENGTH_SHORT).show();
            return;
        }

        android.content.Intent data = new android.content.Intent();
        data.putExtra("name", pickedName.isEmpty() ? pickedAddress : pickedName);
        data.putExtra("address", pickedAddress);
        data.putExtra("lat", pickedLatLng.latitude);
        data.putExtra("lng", pickedLatLng.longitude);
        setResult(RESULT_OK, data);
        finish();
    }

    // ==================== GeocodeSearch.OnGeocodeSearchListener ====================

    @Override
    public void onRegeocodeSearched(RegeocodeResult result, int errorCode) {
        isGeocoding = false;

        if (errorCode == AMapException.CODE_AMAP_SUCCESS && result != null
                && result.getRegeocodeAddress() != null) {
            RegeocodeAddress regeocodeAddress = result.getRegeocodeAddress();
            String formatAddress = regeocodeAddress.getFormatAddress();

            // 优先使用具体POI名称
            if (regeocodeAddress.getPois() != null && !regeocodeAddress.getPois().isEmpty()) {
                com.amap.api.services.core.PoiItem firstPoi = regeocodeAddress.getPois().get(0);
                pickedName = firstPoi.getTitle();
                pickedAddress = formatAddress;
            } else {
                pickedName = formatAddress;
                pickedAddress = formatAddress;
            }

            tvPickedAddress.setText(pickedAddress);
            tvConfirm.setAlpha(1.0f);
        } else {
            // 逆地理编码失败，使用坐标作为地址
            String coordStr = String.format("纬度:%.6f, 经度:%.6f",
                    pickedLatLng.latitude, pickedLatLng.longitude);
            pickedName = coordStr;
            pickedAddress = coordStr;
            tvPickedAddress.setText("选中位置 (" + coordStr + ")");
            tvConfirm.setAlpha(1.0f);
        }
    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {
        // 正向地理编码，本页面不使用
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
        debounceHandler.removeCallbacks(debounceRunnable);
        if (mapView != null) mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }
}
