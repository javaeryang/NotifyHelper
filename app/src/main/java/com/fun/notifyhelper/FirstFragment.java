package com.fun.notifyhelper;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.fun.notifyhelper.adapter.AlarmAdapter;
import com.fun.notifyhelper.databinding.FragmentFirstBinding;
import com.fun.notifyhelper.model.AlarmAction;
import com.fun.notifyhelper.receiver.AlarmScheduler;
import com.fun.notifyhelper.storage.AlarmStorage;
import com.fun.notifyhelper.storage.SystemAlarmStorage;

import java.util.List;

public class FirstFragment extends Fragment implements AlarmAdapter.OnAlarmActionListener {

    private FragmentFirstBinding binding;
    private AlarmAdapter adapter;
    private boolean isSelectingForSystemAlarm = false;

    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                checkAndPromptPermissions();
                loadAlarms();
            });

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new AlarmAdapter();
        adapter.setOnAlarmActionListener(this);
        binding.rvAlarms.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvAlarms.setAdapter(adapter);

        binding.btnGrantPermission.setOnClickListener(v -> requestPermissions());

        setupSystemAlarmCard();

        getParentFragmentManager().setFragmentResultListener(
                AppPickerFragment.REQUEST_KEY_SELECT_APP,
                getViewLifecycleOwner(),
                (requestKey, result) -> {
                    if (isSelectingForSystemAlarm) {
                        isSelectingForSystemAlarm = false;
                        String pkgName = result.getString(AppPickerFragment.EXTRA_PACKAGE_NAME, "");
                        String appName = result.getString(AppPickerFragment.EXTRA_APP_NAME, "");
                        SystemAlarmStorage storage = SystemAlarmStorage.getInstance(requireContext());
                        storage.setTargetPackageName(pkgName);
                        storage.setTargetAppName(appName);
                        updateSystemAlarmCardUI();
                        Toast.makeText(requireContext(), "已设定系统闹钟联动应用: " + appName, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void setupSystemAlarmCard() {
        SystemAlarmStorage storage = SystemAlarmStorage.getInstance(requireContext());

        binding.switchSystemAlarm.setOnCheckedChangeListener(null);
        binding.switchSystemAlarm.setChecked(storage.isEnabled());

        binding.switchSystemAlarm.setOnCheckedChangeListener((buttonView, isChecked) -> {
            storage.setEnabled(isChecked);
            updateSystemAlarmCardUI();
            if (isChecked && !hasNotificationListenerPermission()) {
                requestNotificationListenerPermission();
            }
        });

        binding.btnSelectSystemAlarmApp.setOnClickListener(v -> {
            isSelectingForSystemAlarm = true;
            NavHostFragment.findNavController(FirstFragment.this)
                    .navigate(R.id.AppPickerFragment);
        });

        binding.btnGrantNotifListener.setOnClickListener(v -> requestNotificationListenerPermission());

        updateSystemAlarmCardUI();
    }

    private void updateSystemAlarmCardUI() {
        SystemAlarmStorage storage = SystemAlarmStorage.getInstance(requireContext());
        boolean enabled = storage.isEnabled();

        binding.layoutSystemAlarmDetails.setVisibility(enabled ? View.VISIBLE : View.GONE);

        if (enabled) {
            String appName = storage.getTargetAppName();
            String pkgName = storage.getTargetPackageName();
            if (appName != null && !appName.isEmpty()) {
                binding.tvSystemAlarmApp.setText("响铃时打开: " + appName);
            } else if (pkgName != null && !pkgName.isEmpty()) {
                binding.tvSystemAlarmApp.setText("响铃时打开: " + pkgName);
            } else {
                binding.tvSystemAlarmApp.setText("响铃时打开: 未选择");
            }

            if (!hasNotificationListenerPermission()) {
                binding.btnGrantNotifListener.setVisibility(View.VISIBLE);
            } else {
                binding.btnGrantNotifListener.setVisibility(View.GONE);
            }
        }
    }

    private boolean hasNotificationListenerPermission() {
        String enabledListeners = Settings.Secure.getString(requireContext().getContentResolver(), "enabled_notification_listeners");
        return enabledListeners != null && enabledListeners.contains(requireContext().getPackageName());
    }

    private void requestNotificationListenerPermission() {
        try {
            Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "无法打开通知监听设置页，请手动在系统设置中搜索开启", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkAndPromptPermissions();
        updateSystemAlarmCardUI();
        loadAlarms();
    }

    private void loadAlarms() {
        List<AlarmAction> list = AlarmStorage.getInstance(requireContext()).getAllAlarms();
        if (list.isEmpty()) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.rvAlarms.setVisibility(View.GONE);
        } else {
            binding.layoutEmpty.setVisibility(View.GONE);
            binding.rvAlarms.setVisibility(View.VISIBLE);
            adapter.setAlarms(list);
        }
    }

    private void checkAndPromptPermissions() {
        boolean hasExactAlarmPermission = true;
        boolean hasNotificationPermission = true;
        boolean hasOverlayPermission = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                hasExactAlarmPermission = false;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                hasNotificationPermission = false;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(requireContext())) {
                hasOverlayPermission = false;
            }
        }

        if (!hasExactAlarmPermission || !hasNotificationPermission || !hasOverlayPermission) {
            binding.cardPermissionWarning.setVisibility(View.VISIBLE);
            if (!hasExactAlarmPermission) {
                binding.tvPermissionTitle.setText("需要精确闹钟权限");
                binding.tvPermissionDesc.setText("为保证在后台或休眠时准时响铃，请允许设定精确闹钟。");
            } else if (!hasNotificationPermission) {
                binding.tvPermissionTitle.setText("需要通知权限");
                binding.tvPermissionDesc.setText("为弹出响铃通知与全屏卡片，请允许发送通知。");
            } else {
                binding.tvPermissionTitle.setText("建议开启后台弹出/悬浮窗权限");
                binding.tvPermissionDesc.setText("允许悬浮窗/后台弹出界面权限，可确保闹钟响铃时直接打开目标应用。");
            }
        } else {
            binding.cardPermissionWarning.setVisibility(View.GONE);
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                return;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                    startActivity(intent);
                    return;
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                    startActivity(intent);
                    return;
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(requireContext())) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                    startActivity(intent);
                }
            }
        }
    }

    @Override
    public void onAlarmClick(AlarmAction action) {
        Bundle bundle = new Bundle();
        bundle.putLong(AddEditAlarmFragment.ARG_ALARM_ID, action.getId());
        NavHostFragment.findNavController(FirstFragment.this)
                .navigate(R.id.action_FirstFragment_to_AddEditAlarmFragment, bundle);
    }

    @Override
    public void onAlarmToggle(AlarmAction action, boolean enabled) {
        action.setEnabled(enabled);
        AlarmStorage.getInstance(requireContext()).saveAlarm(action);
        if (enabled) {
            AlarmScheduler.scheduleAlarm(requireContext(), action);
            Toast.makeText(requireContext(), "闹钟已开启", Toast.LENGTH_SHORT).show();
        } else {
            AlarmScheduler.cancelAlarm(requireContext(), action);
            Toast.makeText(requireContext(), "闹钟已关闭", Toast.LENGTH_SHORT).show();
        }
        loadAlarms();
    }

    @Override
    public void onAlarmDelete(AlarmAction action) {
        AlarmScheduler.cancelAlarm(requireContext(), action);
        AlarmStorage.getInstance(requireContext()).deleteAlarm(action.getId());
        Toast.makeText(requireContext(), "闹钟已删除", Toast.LENGTH_SHORT).show();
        loadAlarms();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
