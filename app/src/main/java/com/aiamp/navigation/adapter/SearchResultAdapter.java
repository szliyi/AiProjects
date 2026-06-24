package com.aiamp.navigation.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aiamp.navigation.R;
import com.aiamp.navigation.model.SearchResult;

import java.util.List;

/**
 * 搜索结果列表适配器
 */
public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

    private List<SearchResult> resultList;
    private OnResultClickListener listener;

    public interface OnResultClickListener {
        void onResultClick(SearchResult result);
    }

    public SearchResultAdapter(List<SearchResult> resultList, OnResultClickListener listener) {
        this.resultList = resultList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchResult result = resultList.get(position);
        holder.tvName.setText(result.getName());
        holder.tvAddress.setText(result.getAddress());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onResultClick(result);
            }
        });

        holder.btnGoRoute.setOnClickListener(v -> {
            if (listener != null) {
                listener.onResultClick(result);
            }
        });
    }

    @Override
    public int getItemCount() {
        return resultList != null ? resultList.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAddress;
        LinearLayout btnGoRoute;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvResultName);
            tvAddress = itemView.findViewById(R.id.tvResultAddress);
            btnGoRoute = itemView.findViewById(R.id.btnGoRoute);
        }
    }
}
