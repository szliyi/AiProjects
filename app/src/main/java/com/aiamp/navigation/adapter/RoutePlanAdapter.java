package com.aiamp.navigation.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aiamp.navigation.R;
import com.aiamp.navigation.model.RouteInfo;

import java.util.List;

/**
 * 路线方案列表适配器
 */
public class RoutePlanAdapter extends RecyclerView.Adapter<RoutePlanAdapter.ViewHolder> {

    private List<RouteInfo> routeList;
    private OnRouteClickListener listener;

    public interface OnRouteClickListener {
        void onRouteClick(int position);
    }

    public RoutePlanAdapter(List<RouteInfo> routeList, OnRouteClickListener listener) {
        this.routeList = routeList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_route_plan, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RouteInfo route = routeList.get(position);

        holder.tvTag.setText(route.getTag());
        holder.tvDuration.setText(route.getDurationText());
        holder.tvDistance.setText(route.getDistanceText());
        holder.tvTrafficLights.setText(route.getTrafficLights() + "个红绿灯");

        if (route.getToll() > 0) {
            holder.tvToll.setText("¥" + (int) route.getToll());
            holder.tvToll.setVisibility(View.VISIBLE);
        } else {
            holder.tvToll.setText("免费");
            holder.tvToll.setVisibility(View.VISIBLE);
        }

        // 选中状态
        holder.ivSelected.setVisibility(route.isSelected() ? View.VISIBLE : View.INVISIBLE);

        // 选中状态的边框高亮
        if (route.isSelected()) {
            holder.itemView.setBackgroundResource(R.drawable.bg_route_selected);
        } else {
            holder.itemView.setBackgroundResource(android.R.color.transparent);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRouteClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return routeList != null ? routeList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTag, tvDuration, tvDistance, tvTrafficLights, tvToll;
        ImageView ivSelected;

        ViewHolder(View itemView) {
            super(itemView);
            tvTag = itemView.findViewById(R.id.tvRouteTag);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvTrafficLights = itemView.findViewById(R.id.tvTrafficLights);
            tvToll = itemView.findViewById(R.id.tvToll);
            ivSelected = itemView.findViewById(R.id.ivRouteSelected);
        }
    }
}
