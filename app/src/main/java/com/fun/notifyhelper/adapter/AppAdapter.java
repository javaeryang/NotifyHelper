package com.fun.notifyhelper.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fun.notifyhelper.databinding.ItemAppInfoBinding;
import com.fun.notifyhelper.model.AppInfo;

import java.util.ArrayList;
import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.AppViewHolder> {

    public interface OnAppClickListener {
        void onAppClick(AppInfo appInfo, int position);
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

    public List<AppInfo> getApps() {
        return appList;
    }

    public List<AppInfo> getSelectedApps() {
        List<AppInfo> selected = new ArrayList<>();
        for (AppInfo app : appList) {
            if (app.isSelected()) {
                selected.add(app);
            }
        }
        return selected;
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
        holder.bind(appInfo, position, listener);
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

        public void bind(AppInfo appInfo, int position, OnAppClickListener listener) {
            binding.tvAppName.setText(appInfo.getAppName());
            binding.tvPackageName.setText(appInfo.getPackageName());
            binding.ivAppIcon.setImageDrawable(appInfo.getIcon());
            binding.checkboxApp.setChecked(appInfo.isSelected());

            if (appInfo.getUserSerialNumber() > 0) {
                binding.tvUserInfo.setVisibility(View.VISIBLE);
                binding.tvUserInfo.setText("双开/工作空间 (User " + appInfo.getUserSerialNumber() + ")");
            } else {
                binding.tvUserInfo.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                appInfo.setSelected(!appInfo.isSelected());
                binding.checkboxApp.setChecked(appInfo.isSelected());
                if (listener != null) {
                    listener.onAppClick(appInfo, position);
                }
            });
        }
    }
}
