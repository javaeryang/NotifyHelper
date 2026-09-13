package com.fun.notifyhelper.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fun.notifyhelper.databinding.ItemAlarmActionBinding;
import com.fun.notifyhelper.model.AlarmAction;

import java.util.ArrayList;
import java.util.List;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder> {

    public interface OnAlarmActionListener {
        void onAlarmClick(AlarmAction action);
        void onAlarmToggle(AlarmAction action, boolean enabled);
        void onAlarmDelete(AlarmAction action);
    }

    private final List<AlarmAction> alarmList = new ArrayList<>();
    private OnAlarmActionListener listener;

    public void setOnAlarmActionListener(OnAlarmActionListener listener) {
        this.listener = listener;
    }

    public void setAlarms(List<AlarmAction> alarms) {
        this.alarmList.clear();
        if (alarms != null) {
            this.alarmList.addAll(alarms);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAlarmActionBinding binding = ItemAlarmActionBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new AlarmViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        AlarmAction action = alarmList.get(position);
        holder.bind(action, listener);
    }

    @Override
    public int getItemCount() {
        return alarmList.size();
    }

    static class AlarmViewHolder extends RecyclerView.ViewHolder {

        private final ItemAlarmActionBinding binding;

        public AlarmViewHolder(@NonNull ItemAlarmActionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(AlarmAction action, OnAlarmActionListener listener) {
            binding.tvTime.setText(action.getFormattedTime());
            
            if (action.getTitle() != null && !action.getTitle().isEmpty()) {
                binding.tvTitle.setText(action.getTitle());
                binding.tvTitle.setVisibility(View.VISIBLE);
            } else {
                binding.tvTitle.setVisibility(View.GONE);
            }

            binding.tvActionDesc.setText(action.getActionDescription());

            // Temporarily detach listener to avoid trigger during bind
            binding.switchEnabled.setOnCheckedChangeListener(null);
            binding.switchEnabled.setChecked(action.isEnabled());

            binding.switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onAlarmToggle(action, isChecked);
                }
            });

            binding.btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAlarmDelete(action);
                }
            });

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAlarmClick(action);
                }
            });
        }
    }
}
