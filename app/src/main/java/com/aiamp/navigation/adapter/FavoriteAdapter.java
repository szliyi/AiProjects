package com.aiamp.navigation.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.aiamp.navigation.R;
import com.aiamp.navigation.model.SearchResult;

import java.util.ArrayList;
import java.util.List;

/**
 * 收藏列表适配器
 * 支持普通模式（点击导航）和管理模式（多选删除）
 */
public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.ViewHolder> {

    private List<SearchResult> items = new ArrayList<>();
    private List<Integer> selectedIndices = new ArrayList<>();
    private boolean isEditMode = false;
    private OnFavoriteClickListener listener;
    private OnTagClickListener tagClickListener;

    public interface OnFavoriteClickListener {
        void onItemClick(SearchResult item, int position);
    }

    public interface OnFavoriteLongClickListener {
        void onItemLongClick(SearchResult item, int position);
    }

    public interface OnTagClickListener {
        void onTagClick(SearchResult item, int position);
    }

    private OnFavoriteLongClickListener longClickListener;

    public void setOnFavoriteClickListener(OnFavoriteClickListener listener) {
        this.listener = listener;
    }

    public void setOnFavoriteLongClickListener(OnFavoriteLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setOnTagClickListener(OnTagClickListener listener) {
        this.tagClickListener = listener;
    }

    public void setItems(List<SearchResult> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        if (!editMode) {
            selectedIndices.clear();
        }
        notifyDataSetChanged();
    }

    public boolean isEditMode() {
        return isEditMode;
    }

    public List<Integer> getSelectedIndices() {
        return new ArrayList<>(selectedIndices);
    }

    public int getSelectedCount() {
        return selectedIndices.size();
    }

    public void toggleSelect(int position) {
        if (selectedIndices.contains(position)) {
            selectedIndices.remove(Integer.valueOf(position));
        } else {
            selectedIndices.add(position);
        }
        notifyItemChanged(position);
    }

    public void clearSelection() {
        selectedIndices.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchResult item = items.get(position);

        holder.tvName.setText(item.getName());

        // 显示用户自定义标记
        String tag = item.getTag();
        android.util.Log.d("FavoriteAdapter", "onBind position=" + position + " name=" + item.getName() + " tag=" + tag);
        if (tag != null && !tag.isEmpty()) {
            holder.tvTag.setVisibility(View.VISIBLE);
            holder.tvTag.setText(tag);
            // 点击标记标签编辑标记
            holder.tvTag.setOnClickListener(v -> {
                if (tagClickListener != null) {
                    tagClickListener.onTagClick(item, position);
                }
            });
        } else {
            holder.tvTag.setVisibility(View.GONE);
            holder.tvTag.setOnClickListener(null);
        }

        holder.tvAddress.setText(item.getAddress());

        // 显示经纬度
        String coordText = String.format("经纬度: %.4f, %.4f", item.getLatitude(), item.getLongitude());
        holder.tvCoords.setText(coordText);

        // 编辑模式：显示复选框，隐藏导航箭头
        if (isEditMode) {
            holder.cbSelect.setVisibility(View.VISIBLE);
            holder.ivNavigate.setVisibility(View.GONE);
            holder.cbSelect.setChecked(selectedIndices.contains(position));

            // 整行点击切换选中
            holder.itemView.setOnClickListener(v -> {
                toggleSelect(position);
                if (listener != null) {
                    listener.onItemClick(item, position);
                }
            });
        } else {
            holder.cbSelect.setVisibility(View.GONE);
            holder.ivNavigate.setVisibility(View.VISIBLE);

            // 整行点击导航
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item, position);
                }
            });

            // 长按删除
            holder.itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onItemLongClick(item, position);
                }
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbSelect;
        ImageView ivFavoriteIcon, ivNavigate;
        TextView tvName, tvTag, tvAddress, tvCoords;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelect = itemView.findViewById(R.id.cbSelect);
            ivFavoriteIcon = itemView.findViewById(R.id.ivFavoriteIcon);
            ivNavigate = itemView.findViewById(R.id.ivNavigateArrow);
            tvName = itemView.findViewById(R.id.tvFavoriteName);
            tvTag = itemView.findViewById(R.id.tvFavoriteTag);
            tvAddress = itemView.findViewById(R.id.tvFavoriteAddress);
            tvCoords = itemView.findViewById(R.id.tvFavoriteCoords);
        }
    }
}
