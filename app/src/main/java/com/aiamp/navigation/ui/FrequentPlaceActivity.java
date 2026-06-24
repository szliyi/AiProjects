package com.aiamp.navigation.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.aiamp.navigation.R;
import com.aiamp.navigation.util.AddressStore;
import com.aiamp.navigation.util.CommuteStore;

/**
 * 常去地点管理页面
 * - 设置家庭地址（跳转地图选点）
 * - 设置公司地址（跳转地图选点）
 * - 常用通勤方式选择（默认/驾车/公交/打车 选项卡）
 * - 上下班时间设置（TimePicker弹窗）
 * - 推测常去地点及推荐路线 Switch
 * - 保存常去地点至云端 Switch
 */
public class FrequentPlaceActivity extends AppCompatActivity {

    private static final int REQUEST_PICK_HOME = 4001;
    private static final int REQUEST_PICK_COMPANY = 4002;

    // 地址显示
    private TextView tvHomeAddress;
    private TextView tvCompanyAddress;

    // 通勤方式选项卡
    private TextView tvModeDefault, tvModeDrive, tvModeTransit, tvModeTaxi;

    // 上下班时间
    private TextView tvWorkTime;
    private TextView tvHomeTime;

    // Switch
    private SwitchCompat swGuessRecommend;
    private SwitchCompat swCloudSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_frequent_place);

        initViews();
        refreshAllLabels();
    }

    private void initViews() {
        // 返回按钮
        findViewById(R.id.ivFreqPlaceBack).setOnClickListener(v -> finish());

        // === 地址设置 ===
        tvHomeAddress = findViewById(R.id.tvHomeAddress);
        tvCompanyAddress = findViewById(R.id.tvCompanyAddress);

        findViewById(R.id.llHomeAddress).setOnClickListener(v -> pickHomeAddress());
        findViewById(R.id.llCompanyAddress).setOnClickListener(v -> pickCompanyAddress());

        // === 通勤方式选项卡 ===
        tvModeDefault = findViewById(R.id.tvModeDefault);
        tvModeDrive = findViewById(R.id.tvModeDrive);
        tvModeTransit = findViewById(R.id.tvModeTransit);
        tvModeTaxi = findViewById(R.id.tvModeTaxi);

        tvModeDefault.setOnClickListener(v -> selectCommuteMode(0));
        tvModeDrive.setOnClickListener(v -> selectCommuteMode(1));
        tvModeTransit.setOnClickListener(v -> selectCommuteMode(2));
        tvModeTaxi.setOnClickListener(v -> selectCommuteMode(3));

        // === 上下班时间 ===
        tvWorkTime = findViewById(R.id.tvWorkTime);
        tvHomeTime = findViewById(R.id.tvHomeTime);

        findViewById(R.id.llWorkTime).setOnClickListener(v -> showTimePicker(true));
        findViewById(R.id.llHomeTime).setOnClickListener(v -> showTimePicker(false));

        // === Switch ===
        swGuessRecommend = findViewById(R.id.swGuessRecommend);
        swCloudSave = findViewById(R.id.swCloudSave);

        swGuessRecommend.setChecked(CommuteStore.isGuessRecommendEnabled(this));
        swCloudSave.setChecked(CommuteStore.isCloudSaveEnabled(this));

        swGuessRecommend.setOnCheckedChangeListener((buttonView, isChecked) -> {
            CommuteStore.setGuessRecommendEnabled(this, isChecked);
            Toast.makeText(this, isChecked ? "推测推荐已开启" : "推测推荐已关闭", Toast.LENGTH_SHORT).show();
        });

        swCloudSave.setOnCheckedChangeListener((buttonView, isChecked) -> {
            CommuteStore.setCloudSaveEnabled(this, isChecked);
            Toast.makeText(this, isChecked ? "云端保存已开启" : "云端保存已关闭", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * 刷新所有标签显示
     */
    private void refreshAllLabels() {
        // 家庭地址
        AddressStore.AddressInfo home = AddressStore.getHomeAddress(this);
        if (home != null && home.isValid()) {
            tvHomeAddress.setText(home.address != null && !home.address.isEmpty() ? home.address : home.name);
        } else {
            tvHomeAddress.setText("设置家庭地址");
        }

        // 公司地址
        AddressStore.AddressInfo company = AddressStore.getCompanyAddress(this);
        if (company != null && company.isValid()) {
            tvCompanyAddress.setText(company.address != null && !company.address.isEmpty() ? company.address : company.name);
        } else {
            tvCompanyAddress.setText("设置公司地址");
        }

        // 通勤方式
        updateCommuteModeTabs(CommuteStore.getCommuteMode(this));

        // 上下班时间
        tvWorkTime.setText(CommuteStore.getWorkTimeText(this));
        tvHomeTime.setText(CommuteStore.getHomeTimeText(this));
    }

    // ==================== 地图选点 ====================

    private void pickHomeAddress() {
        Intent intent = new Intent(this, MapPickActivity.class);
        intent.putExtra("lat", 39.904989);
        intent.putExtra("lng", 116.405285);
        startActivityForResult(intent, REQUEST_PICK_HOME);
    }

    private void pickCompanyAddress() {
        Intent intent = new Intent(this, MapPickActivity.class);
        intent.putExtra("lat", 39.904989);
        intent.putExtra("lng", 116.405285);
        startActivityForResult(intent, REQUEST_PICK_COMPANY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        String name = data.getStringExtra("name");
        String address = data.getStringExtra("address");
        double lat = data.getDoubleExtra("lat", 0);
        double lng = data.getDoubleExtra("lng", 0);

        if (name == null || lat == 0 || lng == 0) return;

        if (requestCode == REQUEST_PICK_HOME) {
            AddressStore.saveHomeAddress(this, name, address, lat, lng);
            Toast.makeText(this, "家庭地址已设置", Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQUEST_PICK_COMPANY) {
            AddressStore.saveCompanyAddress(this, name, address, lat, lng);
            Toast.makeText(this, "公司地址已设置", Toast.LENGTH_SHORT).show();
        }

        refreshAllLabels();
    }

    // ==================== 通勤方式 ====================

    private void selectCommuteMode(int mode) {
        CommuteStore.setCommuteMode(this, mode);
        updateCommuteModeTabs(mode);
        Toast.makeText(this, "通勤方式: " + CommuteStore.getCommuteModeText(this), Toast.LENGTH_SHORT).show();
    }

    private void updateCommuteModeTabs(int mode) {
        // 重置所有选项卡样式
        resetTabStyle(tvModeDefault);
        resetTabStyle(tvModeDrive);
        resetTabStyle(tvModeTransit);
        resetTabStyle(tvModeTaxi);

        // 选中当前选项卡
        switch (mode) {
            case 1:
                setTabSelected(tvModeDrive);
                break;
            case 2:
                setTabSelected(tvModeTransit);
                break;
            case 3:
                setTabSelected(tvModeTaxi);
                break;
            default:
                setTabSelected(tvModeDefault);
                break;
        }
    }

    private void setTabSelected(TextView tv) {
        tv.setBackgroundResource(R.drawable.bg_commute_tab_selected);
        tv.setTextColor(0xFF1565C0);
    }

    private void resetTabStyle(TextView tv) {
        tv.setBackgroundResource(R.drawable.bg_commute_tab_normal);
        tv.setTextColor(0xFF666666);
    }

    // ==================== 时间选择 ====================

    private void showTimePicker(boolean isWorkTime) {
        // 使用自定义时间选择对话框
        int currentHour = isWorkTime ? CommuteStore.getWorkTimeHour(this) : CommuteStore.getHomeTimeHour(this);
        int currentMinute = isWorkTime ? CommuteStore.getWorkTimeMinute(this) : CommuteStore.getHomeTimeMinute(this);
        String title = isWorkTime ? "设置上班时间" : "设置回家时间";

        // 构建小时数组 (00-23)
        final String[] hours = new String[24];
        for (int i = 0; i < 24; i++) {
            hours[i] = String.format("%02d", i);
        }
        // 构建分钟数组 (00-59)
        final String[] minutes = new String[60];
        for (int i = 0; i < 60; i++) {
            minutes[i] = String.format("%02d", i);
        }

        // 使用自定义布局
        final android.widget.NumberPicker npHour = new android.widget.NumberPicker(this);
        npHour.setMinValue(0);
        npHour.setMaxValue(23);
        npHour.setValue(currentHour);
        npHour.setFormatter(value -> String.format("%02d", value));

        final android.widget.NumberPicker npMinute = new android.widget.NumberPicker(this);
        npMinute.setMinValue(0);
        npMinute.setMaxValue(59);
        npMinute.setValue(currentMinute);
        npMinute.setFormatter(value -> String.format("%02d", value));

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        layout.setGravity(android.view.Gravity.CENTER);
        layout.setPadding(32, 24, 32, 24);

        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        layout.addView(npHour, lp);

        TextView tvColon = new TextView(this);
        tvColon.setText(" : ");
        tvColon.setTextSize(20);
        tvColon.setTextColor(0xFF333333);
        layout.addView(tvColon);

        layout.addView(npMinute, lp);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setView(layout)
                .setPositiveButton("确定", (dialog, which) -> {
                    int hour = npHour.getValue();
                    int minute = npMinute.getValue();
                    if (isWorkTime) {
                        CommuteStore.setWorkTimeHour(this, hour);
                        CommuteStore.setWorkTimeMinute(this, minute);
                        tvWorkTime.setText(CommuteStore.getWorkTimeText(this));
                    } else {
                        CommuteStore.setHomeTimeHour(this, hour);
                        CommuteStore.setHomeTimeMinute(this, minute);
                        tvHomeTime.setText(CommuteStore.getHomeTimeText(this));
                    }
                    Toast.makeText(this, title + ": " + String.format("%02d:%02d", hour, minute),
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
