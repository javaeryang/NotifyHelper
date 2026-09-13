package com.fun.notifyhelper.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fun.notifyhelper.databinding.ItemAppInfoBinding;
import com.fun.notifyhelper.model.AppInfo;

import java.util.ArrayList;
import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.AppViewHolder> {

    public interface OnAppClickListener {
        void onAppClick(AppInfo appInfo);
    }

    private final List<AppInfo> appList = new ArrayList<>();
    private OnAppClickListener listener;

    public void setOnAppClickListener(OnAppClickListener listener) {
        this.listener = listener;
    }

    public void setApps(List<AppInfo> apps) {
        this.appList.clear();
        if (apps != null) {
            this.appList.addAll(apps);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAppInfoBinding binding = ItemAppInfoBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new AppViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppInfo appInfo = appList.get(position);
        holder.bind(appInfo, listener);
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    static class AppViewHolder extends RecyclerView.ViewHolder {

        private final ItemAppInfoBinding binding;

        public AppViewHolder(@NonNull ItemAppInfoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(AppInfo appInfo, OnAppClickListener listener) {
            binding.tvAppName.setText(appInfo.getAppName());
            binding.tvPackageName.setText(appInfo.getPackageName());
            binding.ivAppIcon.setImageDrawable(appInfo.getIcon());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAppClick(appInfo);
                }
            });
        }
    }
}
