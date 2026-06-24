package com.aiamp.navigation.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aiamp.navigation.R;
import com.aiamp.navigation.model.SearchResult;

import java.util.List;

/**
 * 附近POI列表适配器
 */
public class NearbyPoiAdapter extends RecyclerView.Adapter<NearbyPoiAdapter.ViewHolder> {

    private List<SearchResult> poiList;
    private OnPoiClickListener listener;

    public interface OnPoiClickListener {
        void onPoiClick(SearchResult poi);
    }

    public NearbyPoiAdapter(List<SearchResult> poiList, OnPoiClickListener listener) {
        this.poiList = poiList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nearby_poi, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchResult poi = poiList.get(position);
        holder.tvName.setText(poi.getName());
        holder.tvAddress.setText(poi.getAddress());
        holder.tvDistance.setText(poi.getDistanceText());

        // 根据POI类型设置图标
        setPoiIcon(holder.ivIcon, poi.getType());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPoiClick(poi);
            }
        });
    }

    private void setPoiIcon(ImageView iv, String type) {
        if (type == null) {
            iv.setImageResource(R.drawable.ic_poi_default);
            return;
        }

        if (type.contains("餐饮") || type.contains("美食")) {
            iv.setImageResource(R.drawable.ic_poi_default);
        } else if (type.contains("加油")) {
            iv.setImageResource(R.drawable.ic_poi_default);
        } else if (type.contains("停车")) {
            iv.setImageResource(R.drawable.ic_poi_default);
        } else if (type.contains("购物") || type.contains("商场")) {
            iv.setImageResource(R.drawable.ic_poi_default);
        } else {
            iv.setImageResource(R.drawable.ic_poi_default);
        }
    }

    @Override
    public int getItemCount() {
        return poiList != null ? poiList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon, ivGoNav;
        TextView tvName, tvDistance, tvAddress;

        ViewHolder(View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivPoiIcon);
            ivGoNav = itemView.findViewById(R.id.ivGoNav);
            tvName = itemView.findViewById(R.id.tvPoiName);
            tvDistance = itemView.findViewById(R.id.tvPoiDistance);
            tvAddress = itemView.findViewById(R.id.tvPoiAddress);
        }
    }
}
