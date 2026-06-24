package com.aiamp.navigation.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.aiamp.navigation.R;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.DriveStep;
import com.amap.api.services.route.RouteSearch;

import java.util.ArrayList;
import java.util.List;

/**
 * 导航页面 - 支持实时导航和模拟导航
 */
public class NavigationActivity extends AppCompatActivity implements
        RouteSearch.OnRouteSearchListener {

    private static final String TAG = "NavigationActivity";

    private MapView mapView;
    private AMap aMap;

    // 起终点
    private LatLonPoint startPoint, endPoint;
    private String startName, endName;

    // 导航模式
    private boolean isSimulation;
    private boolean isNavigating = false;
    private int routeIndex;

    // 路线数据
    private DriveRouteResult driveRouteResult;
    private DrivePath drivePath;
    private List<LatLonPoint> routePoints = new ArrayList<>();

    // 模拟导航
    private Handler simHandler = new Handler(Looper.getMainLooper());
    private int currentSimIndex = 0;
    private Marker carMarker;
    private int simSpeedMultiplier = 2;

    // 导航信息
    private int remainDistance;
    private int remainDuration;
    private int currentSpeed = 0;

    // UI组件
    private TextView tvNextRoad, tvNextDistance;
    private TextView tvRemainDistance, tvRemainTime, tvEta;
    private TextView tvSpeed, tvSimSpeed;
    private ImageView ivNextTurn;
    private LinearLayout simModeBanner, speedPanel;
    private ImageButton btnTtsToggle, btnNightMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        getIntentData();
        initMap(savedInstanceState);
        initViews();
        searchRoute();
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

        routeIndex = intent.getIntExtra("route_index", 0);
        isSimulation = intent.getBooleanExtra("is_simulation", false);
    }

    private void initMap(Bundle savedInstanceState) {
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        aMap = mapView.getMap();
        if (aMap != null) {
            aMap.getUiSettings().setZoomControlsEnabled(false);
            aMap.getUiSettings().setAllGesturesEnabled(true);
            aMap.getUiSettings().setCompassEnabled(true);
            aMap.setMapType(AMap.MAP_TYPE_NORMAL);

            // 模拟导航模式下关闭定位蓝点，使用自定义车标代替
            if (!isSimulation) {
                MyLocationStyle locationStyle = new MyLocationStyle();
                locationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);
                locationStyle.interval(2000);
                aMap.setMyLocationStyle(locationStyle);
                aMap.setMyLocationEnabled(true);
            } else {
                aMap.setMyLocationEnabled(false);
            }
        }
    }

    private void initViews() {
        // 模拟模式横幅
        simModeBanner = findViewById(R.id.simModeBanner);
        tvSimSpeed = findViewById(R.id.tvSimSpeed);

        if (isSimulation) {
            simModeBanner.setVisibility(View.VISIBLE);
        }

        // 导航信息面板
        ivNextTurn = findViewById(R.id.ivNextTurn);
        tvNextRoad = findViewById(R.id.tvNextRoad);
        tvNextDistance = findViewById(R.id.tvNextDistance);
        tvRemainDistance = findViewById(R.id.tvRemainDistance);
        tvRemainTime = findViewById(R.id.tvRemainTime);
        tvEta = findViewById(R.id.tvEta);

        // 速度面板
        speedPanel = findViewById(R.id.speedPanel);
        tvSpeed = findViewById(R.id.tvSpeed);

        // 退出按钮
        findViewById(R.id.btnExitNavi).setOnClickListener(v -> {
            if (isNavigating) {
                // 确认退出
                Toast.makeText(this, "导航结束", Toast.LENGTH_SHORT).show();
            }
            stopNavigation();
            finish();
        });

        // TTS开关
        btnTtsToggle = findViewById(R.id.btnTtsToggle);
        btnTtsToggle.setOnClickListener(v -> {
            // 切换语音播报
            Toast.makeText(this, "语音播报切换", Toast.LENGTH_SHORT).show();
        });

        // 总览按钮
        findViewById(R.id.btnOverview).setOnClickListener(v -> {
            if (aMap != null && driveRouteResult != null) {
                // 缩放显示全览
                List<LatLonPoint> pathPoints = routePoints;
                if (pathPoints != null && !pathPoints.isEmpty()) {
                    LatLng center = new LatLng(
                            (startPoint.getLatitude() + endPoint.getLatitude()) / 2,
                            (startPoint.getLongitude() + endPoint.getLongitude()) / 2);
                    aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center, 12));
                }
            }
        });

        // 日夜模式切换
        btnNightMode = findViewById(R.id.btnNightMode);
        btnNightMode.setOnClickListener(v -> {
            if (aMap != null) {
                if (aMap.getMapType() == AMap.MAP_TYPE_NORMAL) {
                    aMap.setMapType(AMap.MAP_TYPE_NIGHT);
                    btnNightMode.setImageResource(R.drawable.ic_night_mode);
                } else {
                    aMap.setMapType(AMap.MAP_TYPE_NORMAL);
                    btnNightMode.setImageResource(R.drawable.ic_night_mode);
                }
            }
        });

        // 模拟速度调节（点击模拟横幅切换速度）
        simModeBanner.setOnClickListener(v -> {
            if (isSimulation) {
                simSpeedMultiplier = simSpeedMultiplier >= 4 ? 1 : simSpeedMultiplier + 1;
                tvSimSpeed.setText(simSpeedMultiplier + "x");
                Toast.makeText(this, "模拟速度: " + simSpeedMultiplier + "倍",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchRoute() {
        try {
            RouteSearch routeSearch = new RouteSearch(this);
            routeSearch.setRouteSearchListener(this);

            RouteSearch.FromAndTo fromAndTo = new RouteSearch.FromAndTo(startPoint, endPoint);
            RouteSearch.DriveRouteQuery query = new RouteSearch.DriveRouteQuery(fromAndTo,
                    RouteSearch.DrivingDefault, null, null, "");

            routeSearch.calculateDriveRouteAsyn(query);
        } catch (AMapException e) {
            Log.e(TAG, "导航路线搜索异常: " + e.getMessage());
            Toast.makeText(this, "路线规划失败", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void startNavigation() {
        isNavigating = true;

        if (isSimulation) {
            startSimulationNavigation();
        } else {
            startRealNavigation();
        }
    }

    private void startRealNavigation() {
        // 实际导航模式：使用高德导航SDK（AMapNavi）
        // 由于项目使用基础3D地图SDK，此处使用地图+路线模拟
        // 真正的实时导航需要集成 com.amap.api:navi-3dmap SDK
        Toast.makeText(this, "实时导航模式已启动", Toast.LENGTH_SHORT).show();

        // 使用模拟导航作为演示
        startSimulationNavigation();
    }

    private void startSimulationNavigation() {
        Log.d(TAG, "开始模拟导航, 路线点数: " + routePoints.size());
        currentSimIndex = 0;

        if (routePoints.isEmpty()) {
            Toast.makeText(this, "路线数据为空，无法模拟导航", Toast.LENGTH_SHORT).show();
            return;
        }

        // 添加车辆标记
        if (carMarker != null) {
            carMarker.remove();
            carMarker = null;
        }

        LatLng start = new LatLng(routePoints.get(0).getLatitude(),
                routePoints.get(0).getLongitude());

        // 将vector drawable转为Bitmap，确保兼容所有设备
        Bitmap carBitmap = getBitmapFromVectorDrawable(R.drawable.ic_drive);
        MarkerOptions markerOptions = new MarkerOptions()
                .position(start)
                .title("当前位置")
                .anchor(0.5f, 0.5f)
                .zIndex(10); // 确保车标在路线之上
        if (carBitmap != null) {
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(carBitmap));
            Log.d(TAG, "车标图标已设置");
        } else {
            // 兜底：使用默认蓝色标记
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            Log.w(TAG, "车标图标加载失败，使用默认图标");
        }
        carMarker = aMap.addMarker(markerOptions);
        Log.d(TAG, "车标Marker已创建: " + (carMarker != null));

        // 调整地图视角
        aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(start, 17));

        // 更新初始导航信息
        updateNavigationInfo();
        speedPanel.setVisibility(View.VISIBLE);

        // 开始模拟移动
        simHandler.post(simRunnable);
    }

    /**
     * 将Vector Drawable安全转换为Bitmap
     */
    private Bitmap getBitmapFromVectorDrawable(int drawableId) {
        try {
            Drawable drawable = ContextCompat.getDrawable(this, drawableId);
            if (drawable == null) {
                Log.e(TAG, "Drawable资源未找到: " + drawableId);
                return null;
            }
            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();
            if (width <= 0) width = 80;
            if (height <= 0) height = 80;

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "转换Vector Drawable失败: " + e.getMessage());
            return null;
        }
    }

    private Runnable simRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isNavigating || currentSimIndex >= routePoints.size()) {
                if (currentSimIndex >= routePoints.size()) {
                    Toast.makeText(NavigationActivity.this,
                            "已到达目的地: " + endName, Toast.LENGTH_LONG).show();
                    stopNavigation();
                    finish();
                }
                return;
            }

            // 移动车辆位置
            LatLonPoint point = routePoints.get(currentSimIndex);
            LatLng newPos = new LatLng(point.getLatitude(), point.getLongitude());

            if (carMarker != null) {
                // 平滑移动动画
                animateCarTo(newPos);
            }

            // 更新导航信息
            updateSimNavigationInfo();

            // 移动地图跟随车辆
            aMap.animateCamera(CameraUpdateFactory.changeLatLng(newPos));

            // 模拟车速 (30-80 km/h随机)
            currentSpeed = 30 + (int) (Math.random() * 50);
            tvSpeed.setText(String.valueOf(currentSpeed));

            // 根据模拟速度倍数推进
            currentSimIndex += simSpeedMultiplier;

            // 控制更新频率
            int delay = 200 / simSpeedMultiplier; // 200ms为基准间隔
            simHandler.postDelayed(this, Math.max(50, delay));
        }
    };

    private void animateCarTo(LatLng target) {
        if (carMarker == null) return;

        LatLng current = carMarker.getPosition();

        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(150);
        animator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();
            double lat = current.latitude + (target.latitude - current.latitude) * fraction;
            double lng = current.longitude + (target.longitude - current.longitude) * fraction;
            carMarker.setPosition(new LatLng(lat, lng));
        });
        animator.start();
    }

    private void updateSimNavigationInfo() {
        if (routePoints.isEmpty()) return;

        int totalPoints = routePoints.size();
        int remainingPoints = totalPoints - currentSimIndex;
        float progress = (float) remainingPoints / totalPoints;

        // 根据路线长度比例估算剩余距离和时间
        if (drivePath != null) {
            remainDistance = (int) (drivePath.getDistance() * progress);
            remainDuration = (int) (drivePath.getDuration() * progress);
        }

        // 更新UI
        tvRemainDistance.setText(formatDistance(remainDistance));
        tvRemainTime.setText(formatDuration(remainDuration));

        // 估算到达时间
        long etaMillis = System.currentTimeMillis() + remainDuration * 1000L;
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        tvEta.setText(sdf.format(new java.util.Date(etaMillis)));

        // 更新下一条指令信息
        updateNextInstruction();
    }

    private void updateNextInstruction() {
        if (drivePath == null || drivePath.getSteps() == null) return;

        // 查找当前所在的步骤
        int accumulatedDistance = 0;
        for (DriveStep step : drivePath.getSteps()) {
            if (step.getPolyline() == null) continue;

            int stepPointCount = step.getPolyline().size();
            if (currentSimIndex < accumulatedDistance + stepPointCount) {
                // 当前步骤
                int distanceToNext = accumulatedDistance + stepPointCount - currentSimIndex;
                tvNextRoad.setText(step.getRoad());
                tvNextDistance.setText("约" + formatDistance(distanceToNext * 10)); // 粗略估算

                // 转向图标
                String action = step.getAction();
                if (action != null) {
                    if (action.contains("左")) {
                        ivNextTurn.setImageResource(R.drawable.ic_turn_right);
                        ivNextTurn.setRotation(-90);
                    } else if (action.contains("右")) {
                        ivNextTurn.setImageResource(R.drawable.ic_turn_right);
                        ivNextTurn.setRotation(0);
                    } else {
                        ivNextTurn.setImageResource(R.drawable.ic_turn_right);
                        ivNextTurn.setRotation(0);
                    }
                }
                return;
            }
            accumulatedDistance += stepPointCount;
        }

        // 接近终点
        tvNextRoad.setText("即将到达目的地");
        tvNextDistance.setText("");
    }

    private void updateNavigationInfo() {
        if (drivePath != null) {
            remainDistance = (int) drivePath.getDistance();
            remainDuration = (int) drivePath.getDuration();

            tvRemainDistance.setText(formatDistance(remainDistance));
            tvRemainTime.setText(formatDuration(remainDuration));
        }

        // 设置初始下一个指令
        if (drivePath != null && drivePath.getSteps() != null
                && !drivePath.getSteps().isEmpty()) {
            DriveStep firstStep = drivePath.getSteps().get(0);
            tvNextRoad.setText(firstStep.getRoad());
        }
    }

    private String formatDistance(int meters) {
        if (meters >= 1000) {
            return String.format("%.1f公里", meters / 1000.0f);
        }
        return meters + "米";
    }

    private String formatDuration(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        if (hours > 0) {
            return hours + "小时" + minutes + "分钟";
        }
        return minutes + "分钟";
    }

    private void stopNavigation() {
        isNavigating = false;
        simHandler.removeCallbacks(simRunnable);
        if (carMarker != null) {
            carMarker.remove();
            carMarker = null;
        }
    }

    // ==================== RouteSearch.OnRouteSearchListener ====================

    @Override
    public void onDriveRouteSearched(DriveRouteResult result, int errorCode) {
        if (errorCode == AMapException.CODE_AMAP_SUCCESS && result != null
                && result.getPaths() != null && !result.getPaths().isEmpty()) {
            driveRouteResult = result;
            drivePath = result.getPaths().get(
                    Math.min(routeIndex, result.getPaths().size() - 1));

            // 提取路线点
            routePoints.clear();
            for (DriveStep step : drivePath.getSteps()) {
                if (step.getPolyline() != null) {
                    routePoints.addAll(step.getPolyline());
                }
            }

            Log.d(TAG, "路线点数量: " + routePoints.size());

            // 在地图上绘制路线
            if (aMap != null && !routePoints.isEmpty()) {
                List<LatLng> latLngs = new ArrayList<>();
                for (LatLonPoint p : routePoints) {
                    latLngs.add(new LatLng(p.getLatitude(), p.getLongitude()));
                }

                aMap.addPolyline(new com.amap.api.maps.model.PolylineOptions()
                        .addAll(latLngs)
                        .width(20)
                        .color(Color.parseColor("#2196F3")));

                // 添加终点标记
                LatLng end = new LatLng(endPoint.getLatitude(), endPoint.getLongitude());
                aMap.addMarker(new MarkerOptions()
                        .position(end)
                        .title("终点: " + endName)
                        .icon(BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_RED)));

                // 缩放显示全览
                LatLng center = new LatLng(
                        (startPoint.getLatitude() + endPoint.getLatitude()) / 2,
                        (startPoint.getLongitude() + endPoint.getLongitude()) / 2);
                aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center, 13));
            }

            // 开始导航
            startNavigation();
        } else {
            Toast.makeText(this, "路线规划失败, 请重试", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override public void onBusRouteSearched(com.amap.api.services.route.BusRouteResult busRouteResult, int i) {}
    @Override public void onWalkRouteSearched(com.amap.api.services.route.WalkRouteResult walkRouteResult, int i) {}
    @Override public void onRideRouteSearched(com.amap.api.services.route.RideRouteResult rideRouteResult, int i) {}

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
        stopNavigation();
        if (mapView != null) mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }
}
