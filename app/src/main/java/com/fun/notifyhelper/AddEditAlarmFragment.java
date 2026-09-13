package com.fun.notifyhelper;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.fun.notifyhelper.databinding.FragmentAddEditAlarmBinding;
import com.fun.notifyhelper.model.AlarmAction;
import com.fun.notifyhelper.receiver.AlarmScheduler;
import com.fun.notifyhelper.storage.AlarmStorage;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Calendar;

public class AddEditAlarmFragment extends Fragment {

    public static final String ARG_ALARM_ID = "extra_alarm_id";

    private FragmentAddEditAlarmBinding binding;
    private long alarmId = -1;
    private AlarmAction currentAlarm;
    private String selectedPackageName = "";
    private String selectedAppName = "";

    private int selectedHour = -1;
    private int selectedMinute = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAddEditAlarmBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.timePicker.setIs24HourView(true);

        if (savedInstanceState != null) {
            selectedHour = savedInstanceState.getInt("selectedHour", -1);
            selectedMinute = savedInstanceState.getInt("selectedMinute", -1);
            selectedPackageName = savedInstanceState.getString("selectedPackageName", "");
            selectedAppName = savedInstanceState.getString("selectedAppName", "");
        }

        if (getArguments() != null) {
            alarmId = getArguments().getLong(ARG_ALARM_ID, -1);
        }

        if (alarmId > 0 && currentAlarm == null) {
            currentAlarm = AlarmStorage.getInstance(requireContext()).getAlarmById(alarmId);
        }

        if (selectedHour == -1) {
            if (currentAlarm != null) {
                selectedHour = currentAlarm.getHour();
                selectedMinute = currentAlarm.getMinute();
                selectedPackageName = currentAlarm.getPackageName();
                selectedAppName = currentAlarm.getAppName();
            } else {
                Calendar now = Calendar.getInstance();
                selectedHour = now.get(Calendar.HOUR_OF_DAY);
                selectedMinute = now.get(Calendar.MINUTE);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding.timePicker.setHour(selectedHour);
            binding.timePicker.setMinute(selectedMinute);
        } else {
            binding.timePicker.setCurrentHour(selectedHour);
            binding.timePicker.setCurrentMinute(selectedMinute);
        }

        if (currentAlarm != null) {
            binding.etTitle.setText(currentAlarm.getTitle());
            if (currentAlarm.getActionType() == AlarmAction.ACTION_TYPE_PLAY_AUDIO) {
                binding.rbActionPlayAudio.setChecked(true);
                showPlayAudioSection();
            } else {
                binding.rbActionOpenApp.setChecked(true);
                showOpenAppSection();
            }
            binding.btnDeleteAlarm.setVisibility(View.VISIBLE);
            binding.btnDeleteAlarm.setOnClickListener(v -> deleteCurrentAlarm());
        } else {
            binding.btnDeleteAlarm.setVisibility(View.GONE);
            if (binding.rbActionPlayAudio.isChecked()) {
                showPlayAudioSection();
            } else {
                binding.rbActionOpenApp.setChecked(true);
                showOpenAppSection();
            }
        }

        binding.timePicker.setOnTimeChangedListener((view1, hourOfDay, minute) -> {
            selectedHour = hourOfDay;
            selectedMinute = minute;
        });

        updateAppDisplay();

        binding.rgActionType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_action_open_app) {
                showOpenAppSection();
            } else if (checkedId == R.id.rb_action_play_audio) {
                showPlayAudioSection();
            }
        });

        binding.btnSelectApp.setOnClickListener(v -> {
            binding.timePicker.clearFocus();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                selectedHour = binding.timePicker.getHour();
                selectedMinute = binding.timePicker.getMinute();
            } else {
                selectedHour = binding.timePicker.getCurrentHour();
                selectedMinute = binding.timePicker.getCurrentMinute();
            }
            NavHostFragment.findNavController(AddEditAlarmFragment.this)
                    .navigate(R.id.action_AddEditAlarmFragment_to_AppPickerFragment);
        });

        binding.btnSaveAlarm.setOnClickListener(v -> saveAlarm());

        getParentFragmentManager().setFragmentResultListener(
                AppPickerFragment.REQUEST_KEY_SELECT_APP,
                getViewLifecycleOwner(),
                (requestKey, result) -> {
                    selectedPackageName = result.getString(AppPickerFragment.EXTRA_PACKAGE_NAME, "");
                    selectedAppName = result.getString(AppPickerFragment.EXTRA_APP_NAME, "");
                    updateAppDisplay();
                }
        );
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("selectedHour", selectedHour);
        outState.putInt("selectedMinute", selectedMinute);
        outState.putString("selectedPackageName", selectedPackageName);
        outState.putString("selectedAppName", selectedAppName);
    }

    private void showOpenAppSection() {
        binding.cardOpenApp.setVisibility(View.VISIBLE);
        binding.cardPlayAudio.setVisibility(View.GONE);
    }

    private void showPlayAudioSection() {
        binding.cardOpenApp.setVisibility(View.GONE);
        binding.cardPlayAudio.setVisibility(View.VISIBLE);
    }

    private void updateAppDisplay() {
        if (selectedAppName != null && !selectedAppName.isEmpty()) {
            binding.tvSelectedAppName.setText(selectedAppName);
        } else {
            binding.tvSelectedAppName.setText("未选择应用");
        }

        if (selectedPackageName != null && !selectedPackageName.isEmpty()) {
            binding.tvSelectedPackageName.setText("包名: " + selectedPackageName);
        } else {
            binding.tvSelectedPackageName.setText("包名: -");
        }
    }

    private void deleteCurrentAlarm() {
        if (currentAlarm == null) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("确认删除")
                .setMessage("确定要删除此闹钟动作吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    AlarmScheduler.cancelAlarm(requireContext(), currentAlarm);
                    AlarmStorage.getInstance(requireContext()).deleteAlarm(currentAlarm.getId());
                    Toast.makeText(requireContext(), "闹钟已删除", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(AddEditAlarmFragment.this).navigateUp();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void saveAlarm() {
        binding.timePicker.clearFocus();

        int actionType = binding.rbActionOpenApp.isChecked() ?
                AlarmAction.ACTION_TYPE_OPEN_APP : AlarmAction.ACTION_TYPE_PLAY_AUDIO;

        if (actionType == AlarmAction.ACTION_TYPE_OPEN_APP &&
                (selectedPackageName == null || selectedPackageName.isEmpty())) {
            Toast.makeText(requireContext(), "请选取需要打开的目标应用", Toast.LENGTH_SHORT).show();
            return;
        }

        int hour;
        int minute;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hour = binding.timePicker.getHour();
            minute = binding.timePicker.getMinute();
        } else {
            hour = binding.timePicker.getCurrentHour();
            minute = binding.timePicker.getCurrentMinute();
        }

        String title = binding.etTitle.getText() != null ? binding.etTitle.getText().toString().trim() : "";

        AlarmAction alarmToSave = currentAlarm != null ? currentAlarm : new AlarmAction();
        alarmToSave.setTitle(title);
        alarmToSave.setHour(hour);
        alarmToSave.setMinute(minute);
        alarmToSave.setEnabled(true);
        alarmToSave.setActionType(actionType);
        alarmToSave.setPackageName(selectedPackageName);
        alarmToSave.setAppName(selectedAppName);

        // Cancel existing scheduled alarm if editing
        if (currentAlarm != null) {
            AlarmScheduler.cancelAlarm(requireContext(), currentAlarm);
        }

        // Save to Storage
        AlarmStorage.getInstance(requireContext()).saveAlarm(alarmToSave);

        // Schedule new alarm
        AlarmScheduler.scheduleAlarm(requireContext(), alarmToSave);

        Toast.makeText(requireContext(), "闹钟已保存并启用 (" + alarmToSave.getFormattedTime() + ")", Toast.LENGTH_SHORT).show();

        NavHostFragment.findNavController(AddEditAlarmFragment.this).navigateUp();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
