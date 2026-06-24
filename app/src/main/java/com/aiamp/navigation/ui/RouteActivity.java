package com.aiamp.navigation.ui;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aiamp.navigation.R;
import com.aiamp.navigation.adapter.RoutePlanAdapter;
import com.aiamp.navigation.model.RouteInfo;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RouteSearch;

import java.util.ArrayList;
import java.util.List;

/**
 * 路线规划页面 - 显示多路线方案对比
 */
public class RouteActivity extends AppCompatActivity implements
        RouteSearch.OnRouteSearchListener {

    private static final String TAG = "RouteActivity";
    private static final int REQUEST_PICK_START = 1001;

    private MapView mapView;
    private AMap aMap;

    // 起终点信息
    private LatLonPoint startPoint, endPoint;
    private String startName, endName;

    // UI组件
    private TextView tvStartPoint, tvEndPoint;
    private RecyclerView rvRoutes;
    private RoutePlanAdapter routeAdapter;
    private List<RouteInfo> routeList = new ArrayList<>();
    private TextView tvRouteCount;
    private Button btnStartNavi;
    private LinearLayout btnSimNavi;

    // 路线规划
    private RouteSearch routeSearch;
    private DriveRouteResult driveRouteResult;
    private int selectedRouteIndex = 0;

    // 出行方式切换Tab
    private TextView tabDrive, tabBus, tabWalk;

    // 左侧面板隐藏/显示
    private LinearLayout leftPanel;
    private ImageButton btnTogglePanel;
    private boolean isPanelVisible = true;
    private float panelWidth = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route);

        getIntentData();
        initMap(savedInstanceState);
        initViews();
        searchDriveRoute();
    }

    private void getIntentData() {
        Intent intent = getIntent();
        startName = intent.getStringExtra("start_name");
        if (startName == null) startName = "我的位置";
        double startLat = intent.getDoubleExtra("start_lat", 39.904989);
        double startLng = intent.getDoubleExtra("start_lng", 116.405285);
        startPoint = new LatLonPoint(startLat, startLng);

        endName = intent.getStringExtra("end_name");
        if (endName == null) endName = "目的地";
        double endLat = intent.getDoubleExtra("end_lat", 39.915119);
        double endLng = intent.getDoubleExtra("end_lng", 116.403963);
        endPoint = new LatLonPoint(endLat, endLng);
    }

    private void initMap(Bundle savedInstanceState) {
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        aMap = mapView.getMap();
        if (aMap != null) {
            aMap.getUiSettings().setZoomControlsEnabled(false);
            aMap.getUiSettings().setAllGesturesEnabled(true);

            // 添加起终点标记
            addStartEndMarkers();

            // 缩放地图显示起终点
            LatLng startLatLng = new LatLng(startPoint.getLatitude(), startPoint.getLongitude());
            LatLng endLatLng = new LatLng(endPoint.getLatitude(), endPoint.getLongitude());
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startLatLng, 13));
        }
    }

    private void addStartEndMarkers() {
        if (aMap == null) return;

        // 起点标记
        LatLng start = new LatLng(startPoint.getLatitude(), startPoint.getLongitude());
        aMap.addMarker(new MarkerOptions()
                .position(start)
                .title("起点: " + startName)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        // 终点标记
        LatLng end = new LatLng(endPoint.getLatitude(), endPoint.getLongitude());
        aMap.addMarker(new MarkerOptions()
                .position(end)
                .title("终点: " + endName)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
    }

    private void initViews() {
        // 起终点文本
        tvStartPoint = findViewById(R.id.tvStartPoint);
        tvStartPoint.setText(startName);
        tvStartPoint.setOnClickListener(v -> {
            // 点击起点 → 跳转搜索页选择起点
            Intent intent = new Intent(RouteActivity.this, SearchActivity.class);
            intent.putExtra("lat", startPoint.getLatitude());
            intent.putExtra("lng", startPoint.getLongitude());
            intent.putExtra("search_mode", "pick_start");  // 标记为"选择起点"模式
            startActivityForResult(intent, REQUEST_PICK_START);
        });

        tvEndPoint = findViewById(R.id.tvEndPoint);
        tvEndPoint.setText(endName);
        tvEndPoint.setOnClickListener(v -> {
            // 点击终点 → 跳转搜索页选择终点
            Intent intent = new Intent(RouteActivity.this, SearchActivity.class);
            intent.putExtra("lat", endPoint.getLatitude());
            intent.putExtra("lng", endPoint.getLongitude());
            intent.putExtra("search_mode", "pick_end");
            startActivityForResult(intent, REQUEST_PICK_START);
        });

        // 交换按钮
        findViewById(R.id.ivSwap).setOnClickListener(v -> {
            // 交换起终点
            LatLonPoint temp = startPoint;
            startPoint = endPoint;
            endPoint = temp;

            String tempName = startName;
            startName = endName;
            endName = tempName;

            tvStartPoint.setText(startName);
            tvEndPoint.setText(endName);

            // 重新搜索路线
            aMap.clear();
            addStartEndMarkers();
            searchDriveRoute();
        });

        // 关闭按钮
        findViewById(R.id.ivClose).setOnClickListener(v -> finish());

        // 出行方式切换Tab
        tabDrive = findViewById(R.id.tabDrive);
        tabBus = findViewById(R.id.tabBus);
        tabWalk = findViewById(R.id.tabWalk);

        tabDrive.setOnClickListener(v -> switchTravelMode(0));
        tabBus.setOnClickListener(v -> switchTravelMode(1));
        tabWalk.setOnClickListener(v -> switchTravelMode(2));

        // 路线列表
        rvRoutes = findViewById(R.id.rvRoutes);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.HORIZONTAL, false);
        rvRoutes.setLayoutManager(layoutManager);
        routeAdapter = new RoutePlanAdapter(routeList, position -> {
            selectRoute(position);
        });
        rvRoutes.setAdapter(routeAdapter);

        // 路线数量
        tvRouteCount = findViewById(R.id.tvRouteCount);

        // 模拟导航按钮
        btnSimNavi = findViewById(R.id.btnSimNavi);
        btnSimNavi.setOnClickListener(v -> startNavigation(true));

        // 开始导航按钮
        btnStartNavi = findViewById(R.id.btnStartNavi);
        btnStartNavi.setOnClickListener(v -> startNavigation(false));

        // 左侧面板隐藏/显示
        initPanelToggle();
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
        if (panelWidth == 0) {
            panelWidth = leftPanel.getWidth();
        }

        if (isPanelVisible) {
            leftPanel.animate()
                    .translationX(-panelWidth)
                    .setDuration(300)
                    .withEndAction(() -> {
                        leftPanel.setVisibility(View.GONE);
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

    private void switchTravelMode(int mode) {
        // 重置tab样式
        tabDrive.setBackgroundResource(R.drawable.bg_tab_unselected);
        tabBus.setBackgroundResource(R.drawable.bg_tab_unselected);
        tabWalk.setBackgroundResource(R.drawable.bg_tab_unselected);
        tabDrive.setTextColor(getColor(R.color.primary));
        tabBus.setTextColor(getColor(R.color.primary));
        tabWalk.setTextColor(getColor(R.color.primary));

        switch (mode) {
            case 0: // 驾车
                tabDrive.setBackgroundResource(R.drawable.bg_tab_selected);
                tabDrive.setTextColor(getColor(R.color.white));
                searchDriveRoute();
                break;
            case 1: // 公交
                tabBus.setBackgroundResource(R.drawable.bg_tab_selected);
                tabBus.setTextColor(getColor(R.color.white));
                Toast.makeText(this, "公交路线规划开发中", Toast.LENGTH_SHORT).show();
                break;
            case 2: // 步行
                tabWalk.setBackgroundResource(R.drawable.bg_tab_selected);
                tabWalk.setTextColor(getColor(R.color.white));
                Toast.makeText(this, "步行路线规划开发中", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void searchDriveRoute() {
        try {
            routeSearch = new RouteSearch(this);
            routeSearch.setRouteSearchListener(this);

            RouteSearch.FromAndTo fromAndTo = new RouteSearch.FromAndTo(startPoint, endPoint);
            RouteSearch.DriveRouteQuery query = new RouteSearch.DriveRouteQuery(fromAndTo,
                    RouteSearch.DRIVING_MULTI_STRATEGY_FASTEST_SHORTEST_AVOID_CONGESTION, null, null, "");

            routeSearch.calculateDriveRouteAsyn(query);
        } catch (AMapException e) {
            Log.e(TAG, "路线搜索异常: " + e.getMessage());
            Toast.makeText(this, "路线规划失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void selectRoute(int position) {
        selectedRouteIndex = position;
        for (int i = 0; i < routeList.size(); i++) {
            routeList.get(i).setSelected(i == position);
        }
        routeAdapter.notifyDataSetChanged();

        // 高亮选中路线
        highlightSelectedRoute();
    }

    private void highlightSelectedRoute() {
        if (driveRouteResult == null || aMap == null) return;

        List<DrivePath> paths = driveRouteResult.getPaths();
        if (paths == null || selectedRouteIndex >= paths.size()) return;

        aMap.clear();
        addStartEndMarkers();

        // 绘制所有路线
        int[] routeColors = {Color.parseColor("#2196F3"), Color.parseColor("#4CAF50"),
                Color.parseColor("#FF9800"), Color.parseColor("#9C27B0")};

        for (int i = 0; i < paths.size(); i++) {
            DrivePath path = paths.get(i);
            List<LatLng> latLngs = parseDrivePathPolyline(path);
            if (latLngs.isEmpty()) continue;

            PolylineOptions options = new PolylineOptions()
                    .addAll(latLngs)
                    .width(i == selectedRouteIndex ? 18 : 10)
                    .color(i == selectedRouteIndex ? routeColors[i % routeColors.length] :
                            Color.argb(120, Color.red(routeColors[i % routeColors.length]),
                                    Color.green(routeColors[i % routeColors.length]),
                                    Color.blue(routeColors[i % routeColors.length])));

            aMap.addPolyline(options);
        }
    }

    /**
     * 解析 DrivePath 的所有 polyline 坐标点
     * 新版高德SDK中 getPolyline() 返回编码字符串，需要解码
     */
    private List<LatLng> parseDrivePathPolyline(DrivePath path) {
        List<LatLng> allPoints = new ArrayList<>();
        if (path.getSteps() == null) return allPoints;

        for (int i = 0; i < path.getSteps().size(); i++) {
            Object polylineObj = path.getSteps().get(i).getPolyline();
            if (polylineObj == null) continue;

            if (polylineObj instanceof String) {
                // 新版SDK：返回编码字符串，需解码
                List<LatLng> stepPoints = decodePolyline((String) polylineObj);
                if (stepPoints != null) {
                    allPoints.addAll(stepPoints);
                }
            } else if (polylineObj instanceof List) {
                // 旧版兼容：直接返回List<LatLonPoint>
                @SuppressWarnings("unchecked")
                List<LatLonPoint> stepPoints = (List<LatLonPoint>) polylineObj;
                for (LatLonPoint p : stepPoints) {
                    allPoints.add(new LatLng(p.getLatitude(), p.getLongitude()));
                }
            }
        }
        return allPoints;
    }

    /**
     * 解码高德polyline编码字符串为经纬度列表
     * 高德polyline编码格式类似Google的折线编码算法
     */
    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> points = new ArrayList<>();
        if (encoded == null || encoded.isEmpty()) return points;

        try {
            // 高德新版SDK polyline格式：分号分隔的 "lng,lat" 坐标对
            if (encoded.contains(";")) {
                String[] coords = encoded.split(";");
                for (String coord : coords) {
                    String[] parts = coord.split(",");
                    if (parts.length >= 2) {
                        double lng = Double.parseDouble(parts[0]);
                        double lat = Double.parseDouble(parts[1]);
                        points.add(new LatLng(lat, lng));
                    }
                }
            } else {
                // 尝试Google风格的编码折线算法解码
                int len = encoded.length();
                int index = 0;
                int lat = 0, lng = 0;

                while (index < len) {
                    int result = 0;
                    int shift = 0;
                    int b;
                    do {
                        b = encoded.charAt(index++) - 63;
                        result |= (b & 0x1f) << shift;
                        shift += 5;
                    } while (b >= 0x20);
                    int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                    lat += dlat;

                    result = 0;
                    shift = 0;
                    do {
                        b = encoded.charAt(index++) - 63;
                        result |= (b & 0x1f) << shift;
                        shift += 5;
                    } while (b >= 0x20);
                    int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                    lng += dlng;

                    points.add(new LatLng(lat * 1e-5, lng * 1e-5));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "polyline解码失败: " + e.getMessage());
        }
        return points;
    }

    private void startNavigation(boolean isSimulation) {
        if (driveRouteResult == null || driveRouteResult.getPaths() == null
                || driveRouteResult.getPaths().isEmpty()) {
            Toast.makeText(this, "请先选择路线", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(RouteActivity.this, NavigationActivity.class);
        intent.putExtra("start_name", startName);
        intent.putExtra("start_lat", startPoint.getLatitude());
        intent.putExtra("start_lng", startPoint.getLongitude());
        intent.putExtra("end_name", endName);
        intent.putExtra("end_lat", endPoint.getLatitude());
        intent.putExtra("end_lng", endPoint.getLongitude());
        intent.putExtra("route_index", selectedRouteIndex);
        intent.putExtra("is_simulation", isSimulation);
        startActivity(intent);
    }

    // ==================== RouteSearch.OnRouteSearchListener ====================

    @Override
    public void onDriveRouteSearched(DriveRouteResult result, int errorCode) {
        if (errorCode == AMapException.CODE_AMAP_SUCCESS && result != null
                && result.getPaths() != null) {
            driveRouteResult = result;
            routeList.clear();

            List<DrivePath> paths = result.getPaths();
            String[] tags = {"推荐路线", "最快路线", "最短路线", "备选路线"};

            for (int i = 0; i < paths.size(); i++) {
                DrivePath path = paths.get(i);
                RouteInfo info = new RouteInfo();
                info.setRouteId(i);
                info.setTag(i < tags.length ? tags[i] : "路线" + (i + 1));
                info.setDuration((int) path.getDuration());
                info.setDistance((int) path.getDistance());
                info.setTrafficLights(path.getTotalTrafficlights());
                info.setToll(path.getTolls());
                info.setSelected(i == 0);
                routeList.add(info);
            }

            routeAdapter.notifyDataSetChanged();
            tvRouteCount.setText("共" + routeList.size() + "条路线");
            selectedRouteIndex = 0;

            // 在地图上绘制路线
            highlightSelectedRoute();
        } else {
            Toast.makeText(this, "路线规划失败, 请重试", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "路线搜索失败, errorCode: " + errorCode);
        }
    }

    // 预留其他路线类型接口
    @Override public void onBusRouteSearched(com.amap.api.services.route.BusRouteResult busRouteResult, int i) {}
    @Override public void onWalkRouteSearched(com.amap.api.services.route.WalkRouteResult walkRouteResult, int i) {}
    @Override public void onRideRouteSearched(com.amap.api.services.route.RideRouteResult rideRouteResult, int i) {}

    // ==================== 起终点选择回调 ====================

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_START && resultCode == RESULT_OK && data != null) {
            String pickedName = data.getStringExtra("picked_name");
            double pickedLat = data.getDoubleExtra("picked_lat", 0);
            double pickedLng = data.getDoubleExtra("picked_lng", 0);
            String searchMode = data.getStringExtra("search_mode");

            if (pickedName == null || pickedLat == 0) return;

            if ("pick_start".equals(searchMode)) {
                startName = pickedName;
                startPoint = new LatLonPoint(pickedLat, pickedLng);
                tvStartPoint.setText(startName);
            } else {
                endName = pickedName;
                endPoint = new LatLonPoint(pickedLat, pickedLng);
                tvEndPoint.setText(endName);
            }

            // 重新规划路线
            aMap.clear();
            addStartEndMarkers();
            searchDriveRoute();
        }
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
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }
}
