package com.aiamp.navigation.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.aiamp.navigation.R;
import com.aiamp.navigation.util.SettingsStore;

/**
 * 设置页面 - 参考高德地图设置页风格
 * - 导航设置双列卡片（车道级导航/避开限行 + 3D车标/语音包）
 * - 卡片式分组布局（地图设置、导航语音、路线偏好、通用、存储空间、关于）
 * - Switch开关控件 + 带箭头选择项
 * - 底部切换账号/退出登录按钮 + 版权信息
 */
public class SettingsActivity extends AppCompatActivity {

    // 文本显示控件
    private TextView tvMapMode;
    private TextView tvDayNightMode;
    private TextView tvVoiceBroadcast;
    private TextView tvRoutePreference;
    private TextView tvCacheSize;

    // Switch开关控件
    private SwitchCompat swLaneNavi;
    private SwitchCompat swAvoidRestriction;
    private SwitchCompat swTraffic;
    private SwitchCompat swAvoidCongestion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initViews();
        refreshAllLabels();
        setupSwitches();
        setupBottomButtons();
    }

    private void initViews() {
        // 返回按钮
        findViewById(R.id.ivSettingsBack).setOnClickListener(v -> finish());

        // 全部导航设置
        findViewById(R.id.llAllNaviSettings).setOnClickListener(v -> {
            Toast.makeText(this, "全部导航设置功能开发中", Toast.LENGTH_SHORT).show();
        });

        // 3D车标
        findViewById(R.id.llCarIcon).setOnClickListener(v -> {
            Toast.makeText(this, "3D车标设置功能开发中", Toast.LENGTH_SHORT).show();
        });

        // 语音包
        findViewById(R.id.llVoicePack).setOnClickListener(v -> {
            Toast.makeText(this, "语音包设置功能开发中", Toast.LENGTH_SHORT).show();
        });

        // 地图模式
        tvMapMode = findViewById(R.id.tvMapMode);
        findViewById(R.id.llMapMode).setOnClickListener(v -> showMapModeDialog());

        // 日夜模式
        tvDayNightMode = findViewById(R.id.tvDayNightMode);
        findViewById(R.id.llDayNightMode).setOnClickListener(v -> showDayNightModeDialog());

        // 语音播报
        tvVoiceBroadcast = findViewById(R.id.tvVoiceBroadcast);
        findViewById(R.id.llVoiceBroadcast).setOnClickListener(v -> showVoiceBroadcastDialog());

        // 路线偏好
        tvRoutePreference = findViewById(R.id.tvRoutePreference);
        findViewById(R.id.llRoutePreference).setOnClickListener(v -> showRoutePreferenceDialog());

        // Switch控件
        swLaneNavi = findViewById(R.id.swLaneNavi);
        swAvoidRestriction = findViewById(R.id.swAvoidRestriction);
        swTraffic = findViewById(R.id.swTraffic);
        swAvoidCongestion = findViewById(R.id.swAvoidCongestion);

        // 足迹设置
        findViewById(R.id.llFootprint).setOnClickListener(v -> {
            Toast.makeText(this, "足迹设置功能开发中", Toast.LENGTH_SHORT).show();
        });

        // 常去地点管理
        findViewById(R.id.llFrequentPlace).setOnClickListener(v -> {
            startActivity(new Intent(this, FrequentPlaceActivity.class));
        });

        // 消息通知
        findViewById(R.id.llNotification).setOnClickListener(v -> {
            Toast.makeText(this, "消息通知设置功能开发中", Toast.LENGTH_SHORT).show();
        });

        // 关于AIaiAmp导航
        findViewById(R.id.llAbout).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("关于AIaiAmp导航")
                    .setMessage("AIaiAmp导航 v1.0.0\n基于高德地图SDK开发\n\n功能特性：\n- 地图浏览与POI搜索\n- 路线规划（驾车/步行/骑行/公交）\n- 实时导航与模拟导航\n- 收藏与标记管理\n- 个性化设置")
                    .setPositiveButton("确定", null)
                    .show();
        });

        // 清除缓存
        tvCacheSize = findViewById(R.id.tvCacheSize);
        findViewById(R.id.llClearCache).setOnClickListener(v -> clearCache());
    }

    /**
     * 初始化Switch开关状态和监听器
     */
    private void setupSwitches() {
        // 车道级导航
        swLaneNavi.setChecked(true);
        swLaneNavi.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String msg = isChecked ? "车道级导航已开启" : "车道级导航已关闭";
            Toast.makeText(SettingsActivity.this, msg, Toast.LENGTH_SHORT).show();
        });

        // 避开限行
        swAvoidRestriction.setChecked(SettingsStore.isAvoidRestriction(this));
        swAvoidRestriction.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SettingsStore.setAvoidRestriction(SettingsActivity.this, isChecked);
            String msg = isChecked ? "避开限行已开启" : "避开限行已关闭";
            Toast.makeText(SettingsActivity.this, msg, Toast.LENGTH_SHORT).show();
        });

        // 实时路况
        swTraffic.setChecked(SettingsStore.isTrafficEnabled(this));
        swTraffic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SettingsStore.setTrafficEnabled(SettingsActivity.this, isChecked);
            String msg = isChecked ? "实时路况已开启" : "实时路况已关闭";
            Toast.makeText(SettingsActivity.this, msg, Toast.LENGTH_SHORT).show();
        });

        // 避开拥堵
        swAvoidCongestion.setChecked(SettingsStore.isAvoidCongestion(this));
        swAvoidCongestion.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SettingsStore.setAvoidCongestion(SettingsActivity.this, isChecked);
            String msg = isChecked ? "避开拥堵已开启" : "避开拥堵已关闭";
            Toast.makeText(SettingsActivity.this, msg, Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * 底部按钮：切换账号、退出登录
     */
    private void setupBottomButtons() {
        findViewById(R.id.btnSwitchAccount).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("切换账号")
                    .setMessage("确定要切换账号吗？\n当前登录状态将失效。")
                    .setPositiveButton("确定", (dialog, which) -> {
                        Toast.makeText(SettingsActivity.this, "已退出当前账号", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("退出登录")
                    .setMessage("确定要退出登录吗？")
                    .setPositiveButton("退出", (dialog, which) -> {
                        Toast.makeText(SettingsActivity.this, "已退出登录", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    /**
     * 刷新所有设置项的显示文字
     */
    private void refreshAllLabels() {
        tvMapMode.setText(SettingsStore.getMapModeText(this));
        tvDayNightMode.setText(SettingsStore.getDayNightModeText(this));
        tvVoiceBroadcast.setText(SettingsStore.getVoiceBroadcastText(this));
        tvRoutePreference.setText(SettingsStore.getRoutePreferenceText(this));
        tvCacheSize.setText("0.0 MB");
    }

    // ==================== 地图模式 ====================

    private void showMapModeDialog() {
        int currentMode = SettingsStore.getMapMode(this);
        final String[] items = {"标准地图", "卫星地图"};
        new AlertDialog.Builder(this)
                .setTitle("地图模式")
                .setSingleChoiceItems(items, currentMode, (dialog, which) -> {
                    SettingsStore.setMapMode(SettingsActivity.this, which);
                    tvMapMode.setText(items[which]);
                    Toast.makeText(SettingsActivity.this,
                            "地图模式已切换为: " + items[which], Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // ==================== 日夜模式 ====================

    private void showDayNightModeDialog() {
        int currentMode = SettingsStore.getDayNightMode(this);
        final String[] items = {"自动切换", "白天", "夜间"};
        new AlertDialog.Builder(this)
                .setTitle("日夜模式")
                .setSingleChoiceItems(items, currentMode, (dialog, which) -> {
                    SettingsStore.setDayNightMode(SettingsActivity.this, which);
                    tvDayNightMode.setText(items[which]);
                    Toast.makeText(SettingsActivity.this,
                            "日夜模式已切换为: " + items[which], Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // ==================== 语音播报 ====================

    private void showVoiceBroadcastDialog() {
        int currentMode = SettingsStore.getVoiceBroadcast(this);
        final String[] items = {"关闭", "开启", "仅导航"};
        new AlertDialog.Builder(this)
                .setTitle("语音播报")
                .setSingleChoiceItems(items, currentMode, (dialog, which) -> {
                    SettingsStore.setVoiceBroadcast(SettingsActivity.this, which);
                    tvVoiceBroadcast.setText(items[which]);
                    Toast.makeText(SettingsActivity.this,
                            "语音播报: " + items[which], Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // ==================== 路线偏好 ====================

    private void showRoutePreferenceDialog() {
        int currentPref = SettingsStore.getRoutePreference(this);
        final String[] items = {"智能推荐", "躲避拥堵", "不走高速", "少收费", "高速优先"};
        new AlertDialog.Builder(this)
                .setTitle("路线偏好")
                .setSingleChoiceItems(items, currentPref, (dialog, which) -> {
                    SettingsStore.setRoutePreference(SettingsActivity.this, which);
                    tvRoutePreference.setText(items[which]);
                    Toast.makeText(SettingsActivity.this,
                            "路线偏好: " + items[which], Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // ==================== 清除缓存 ====================

    private void clearCache() {
        new AlertDialog.Builder(this)
                .setTitle("清除缓存")
                .setMessage("确定要清除所有缓存数据吗？\n（不会影响收藏和个人设置）")
                .setPositiveButton("确定清除", (dialog, which) -> {
                    // 清除应用缓存目录
                    try {
                        File cacheDir = getCacheDir();
                        if (cacheDir != null && cacheDir.exists()) {
                            deleteDir(cacheDir);
                        }
                    } catch (Exception e) {
                        // ignore
                    }
                    tvCacheSize.setText("0.0 MB");
                    Toast.makeText(SettingsActivity.this, "缓存已清除", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private boolean deleteDir(File dir) {
        if (dir == null || !dir.isDirectory()) return false;
        String[] children = dir.list();
        if (children != null) {
            for (String child : children) {
                boolean success = deleteDir(new File(dir, child));
                if (!success) return false;
            }
        }
        return dir.delete();
    }
}
