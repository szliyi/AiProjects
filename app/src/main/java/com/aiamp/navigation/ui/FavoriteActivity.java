package com.aiamp.navigation.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aiamp.navigation.R;
import com.aiamp.navigation.adapter.FavoriteAdapter;
import com.aiamp.navigation.model.SearchResult;
import com.aiamp.navigation.util.FavoriteStore;

import java.util.List;

/**
 * 我的收藏页面
 * - 列表展示所有收藏地点
 * - 地图选点添加收藏
 * - 管理模式：多选删除
 * - 点击收藏项直接导航
 */
public class FavoriteActivity extends AppCompatActivity {

    private static final int REQUEST_MAP_PICK = 3001;

    private RecyclerView rvFavorites;
    private FavoriteAdapter adapter;
    private TextView tvEditMode;
    private View layoutBatchBar;
    private TextView tvSelectedCount;
    private View layoutEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        initViews();
        loadFavorites();
    }

    private void initViews() {
        // 返回按钮
        findViewById(R.id.ivFavoriteBack).setOnClickListener(v -> finish());

        // 管理模式切换
        tvEditMode = findViewById(R.id.tvEditMode);
        tvEditMode.setOnClickListener(v -> toggleEditMode());

        // 地图选点添加收藏
        findViewById(R.id.tvAddFromMap).setOnClickListener(v -> openMapPick());

        // 批量删除栏
        layoutBatchBar = findViewById(R.id.layoutBatchBar);
        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        findViewById(R.id.tvBatchDelete).setOnClickListener(v -> batchDelete());

        // 空状态
        layoutEmpty = findViewById(R.id.layoutEmpty);

        // 收藏列表
        rvFavorites = findViewById(R.id.rvFavorites);
        rvFavorites.setLayoutManager(new LinearLayoutManager(this));

        adapter = new FavoriteAdapter();
        adapter.setOnFavoriteClickListener((item, position) -> {
            if (adapter.isEditMode()) {
                // 管理模式：更新选中计数
                updateBatchBar();
            } else {
                // 普通模式：弹出选择菜单（导航 / 编辑标记）
                showItemActionDialog(item, position);
            }
        });

        // 长按收藏项 → 弹出删除确认
        adapter.setOnFavoriteLongClickListener((item, position) -> {
            showDeleteConfirmDialog(item, position);
        });

        // 点击标记标签 → 编辑标记
        adapter.setOnTagClickListener((item, position) -> {
            showTagEditDialog(position, item.getName());
        });

        rvFavorites.setAdapter(adapter);
    }

    private void loadFavorites() {
        List<SearchResult> list = FavoriteStore.loadAll(this);
        adapter.setItems(list);

        if (list.isEmpty()) {
            rvFavorites.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            tvEditMode.setVisibility(View.GONE);
        } else {
            rvFavorites.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
            tvEditMode.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 切换管理模式
     */
    private void toggleEditMode() {
        boolean newMode = !adapter.isEditMode();
        adapter.setEditMode(newMode);

        if (newMode) {
            tvEditMode.setText("取消");
            layoutBatchBar.setVisibility(View.VISIBLE);
            updateBatchBar();
        } else {
            tvEditMode.setText("管理");
            layoutBatchBar.setVisibility(View.GONE);
        }
    }

    /**
     * 更新批量操作栏
     */
    private void updateBatchBar() {
        int count = adapter.getSelectedCount();
        tvSelectedCount.setText("已选择 " + count + " 项");
        findViewById(R.id.tvBatchDelete).setEnabled(count > 0);
    }

    /**
     * 批量删除选中项
     */
    private void batchDelete() {
        List<Integer> selected = adapter.getSelectedIndices();
        if (selected.isEmpty()) {
            Toast.makeText(this, "请先选择要删除的收藏", Toast.LENGTH_SHORT).show();
            return;
        }

        FavoriteStore.removeAll(this, selected);
        adapter.clearSelection();

        Toast.makeText(this, "已删除 " + selected.size() + " 个收藏", Toast.LENGTH_SHORT).show();

        // 重新加载
        loadFavorites();
        if (adapter.isEditMode() && FavoriteStore.loadAll(this).isEmpty()) {
            // 全部删除后自动退出管理模式
            adapter.setEditMode(false);
            tvEditMode.setText("管理");
            tvEditMode.setVisibility(View.GONE);
            layoutBatchBar.setVisibility(View.GONE);
        }
    }

    /**
     * 打开地图选点页面添加收藏
     */
    private void openMapPick() {
        Intent intent = new Intent(this, MapPickActivity.class);
        // 传递默认位置（北京）
        intent.putExtra("lat", 39.904989);
        intent.putExtra("lng", 116.405285);
        startActivityForResult(intent, REQUEST_MAP_PICK);
    }

    /**
     * 点击收藏项弹出操作菜单：导航 / 编辑标记
     */
    private void showItemActionDialog(SearchResult item, int position) {
        String existingTag = item.getTag();
        String tagInfo = (existingTag != null && !existingTag.isEmpty())
                ? "\n标记: " + existingTag : "";

        new AlertDialog.Builder(this)
                .setTitle(item.getName())
                .setMessage(item.getAddress() + tagInfo)
                .setPositiveButton("导航去这里", (dialog, which) -> {
                    navigateToPlace(item);
                })
                .setNegativeButton("编辑标记", (dialog, which) -> {
                    showTagEditDialog(position, item.getName());
                })
                .setNeutralButton("取消", null)
                .show();
    }

    /**
     * 长按收藏项弹出删除确认对话框
     */
    private void showDeleteConfirmDialog(SearchResult item, int position) {
        new AlertDialog.Builder(this)
                .setTitle("删除收藏")
                .setMessage("确定要删除收藏「" + item.getName() + "」吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    FavoriteStore.remove(this, position);
                    loadFavorites();
                    Toast.makeText(FavoriteActivity.this,
                            "已删除: " + item.getName(), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 导航到收藏地点
     */
    private void navigateToPlace(SearchResult place) {
        Intent intent = new Intent(this, RouteActivity.class);
        intent.putExtra("start_name", "我的位置");
        intent.putExtra("start_lat", 0);
        intent.putExtra("start_lng", 0);
        intent.putExtra("end_name", place.getName());
        intent.putExtra("end_lat", place.getLatitude());
        intent.putExtra("end_lng", place.getLongitude());
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MAP_PICK && resultCode == RESULT_OK && data != null) {
            // 从地图选点返回，添加到收藏
            String name = data.getStringExtra("name");
            String address = data.getStringExtra("address");
            double lat = data.getDoubleExtra("lat", 0);
            double lng = data.getDoubleExtra("lng", 0);

            if (lat != 0 && lng != 0 && name != null) {
                SearchResult place = new SearchResult(name, address, lat, lng);
                boolean added = FavoriteStore.add(this, place);
                if (added) {
                    loadFavorites();
                    // 收藏成功后弹出标记编辑对话框
                    int addedIndex = FavoriteStore.loadAll(this).size() - 1;
                    showTagEditDialog(addedIndex, name);
                } else {
                    Toast.makeText(this, "该地点已收藏或已达上限(100个)", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * 弹出标记编辑对话框，用户可自由输入标记内容（最长50个汉字）
     * @param index 收藏列表中的索引
     * @param placeName 地点名称，用于提示
     */
    private void showTagEditDialog(int index, String placeName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("编辑标记 - " + placeName);

        // 自定义输入框布局
        final EditText input = new EditText(this);
        input.setHint("如：美食、历史、常去...（最多50个汉字）");
        input.setSingleLine(true);
        input.setPadding(32, 24, 32, 24);
        input.setTextSize(14);
        input.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(50)
        });

        // 字符计数提示
        final TextView tvCharCount = new TextView(this);
        tvCharCount.setText("0/50");
        tvCharCount.setTextSize(11);
        tvCharCount.setPadding(32, 0, 32, 16);
        tvCharCount.setTextColor(0xFF999999);

        input.addTextChangedListener(new TextWatcher() {
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
            public void afterTextChanged(Editable s) {}
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
                Toast.makeText(FavoriteActivity.this,
                        "标记内容过长，最多50个汉字", Toast.LENGTH_SHORT).show();
                return;
            }
            FavoriteStore.updateTag(FavoriteActivity.this, index, tag);
            loadFavorites();
            if (!tag.isEmpty()) {
                Toast.makeText(FavoriteActivity.this,
                        "标记已保存: " + tag, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("清空标记", (dialog, which) -> {
            FavoriteStore.updateTag(FavoriteActivity.this, index, "");
            loadFavorites();
            Toast.makeText(FavoriteActivity.this,
                    "标记已清空", Toast.LENGTH_SHORT).show();
        });
        builder.setNeutralButton("取消", (dialog, which) -> {
            // 不做任何操作，仅关闭对话框
        });

        builder.show();
    }

    /**
     * 计算字符串中的汉字数量（中文字符范围）
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
}
