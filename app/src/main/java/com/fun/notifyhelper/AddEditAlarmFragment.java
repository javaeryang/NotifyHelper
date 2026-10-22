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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

public class AddEditAlarmFragment extends Fragment {

    public static final String ARG_ALARM_ID = "extra_alarm_id";

    private FragmentAddEditAlarmBinding binding;
    private long alarmId = -1;
    private AlarmAction currentAlarm;
    private String selectedTargetAppsJson = "[]";

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
            selectedTargetAppsJson = savedInstanceState.getString("selectedTargetAppsJson", "[]");
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
                selectedTargetAppsJson = currentAlarm.getTargetAppsJson();
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
            binding.switchRandom.setChecked(currentAlarm.isRandom());
            if (currentAlarm.isRandom()) {
                binding.layoutRandomInputs.setVisibility(View.VISIBLE);
                binding.etRandomMin.setText(String.valueOf(currentAlarm.getRandomMinMinutes()));
                binding.etRandomMax.setText(String.valueOf(currentAlarm.getRandomMaxMinutes()));
            } else {
                binding.layoutRandomInputs.setVisibility(View.GONE);
                binding.etRandomMin.setText("5");
                binding.etRandomMax.setText("30");
            }
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
            binding.switchRandom.setChecked(false);
            binding.layoutRandomInputs.setVisibility(View.GONE);
            binding.etRandomMin.setText("5");
            binding.etRandomMax.setText("30");
            if (binding.rbActionPlayAudio.isChecked()) {
                showPlayAudioSection();
            } else {
                binding.rbActionOpenApp.setChecked(true);
                showOpenAppSection();
            }
        }

        binding.switchRandom.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.layoutRandomInputs.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

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
                    selectedTargetAppsJson = result.getString(AppPickerFragment.EXTRA_TARGET_APPS_JSON, "[]");
                    updateAppDisplay();
                }
        );
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("selectedHour", selectedHour);
        outState.putInt("selectedMinute", selectedMinute);
        outState.putString("selectedTargetAppsJson", selectedTargetAppsJson);
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
        if (selectedTargetAppsJson != null && !selectedTargetAppsJson.isEmpty() && !selectedTargetAppsJson.equals("[]")) {
            try {
                JSONArray array = new JSONArray(selectedTargetAppsJson);
                int count = array.length();
                if (count > 0) {
                    JSONObject firstApp = array.getJSONObject(0);
                    String firstAppName = firstApp.optString("appName", "");
                    if (firstApp.optLong("userSerialNumber", 0) > 0) {
                        firstAppName += "(双开)";
                    }
                    if (count == 1) {
                        binding.tvSelectedAppName.setText(firstAppName);
                        binding.tvSelectedPackageName.setText("包名: " + firstApp.optString("packageName", ""));
                    } else {
                        binding.tvSelectedAppName.setText(firstAppName + " 等 " + count + " 个应用");
                        binding.tvSelectedPackageName.setText("将在响铃时顺次打开");
                    }
                    return;
                }
            } catch (JSONException e) {
                // ignore
            }
        }
        binding.tvSelectedAppName.setText("未选择应用");
        binding.tvSelectedPackageName.setText("包名: -");
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
        if (binding.etRandomMin.hasFocus()) binding.etRandomMin.clearFocus();
        if (binding.etRandomMax.hasFocus()) binding.etRandomMax.clearFocus();

        int actionType = binding.rbActionOpenApp.isChecked() ?
                AlarmAction.ACTION_TYPE_OPEN_APP : AlarmAction.ACTION_TYPE_PLAY_AUDIO;

        if (actionType == AlarmAction.ACTION_TYPE_OPEN_APP &&
                (selectedTargetAppsJson == null || selectedTargetAppsJson.equals("[]"))) {
            Toast.makeText(requireContext(), "请选取需要打开的目标应用", Toast.LENGTH_SHORT).show();
            return;
        }
        
        boolean isRandom = binding.switchRandom.isChecked();
        int minRandom = 5;
        int maxRandom = 30;
        
        if (isRandom) {
            String minStr = binding.etRandomMin.getText() != null ? binding.etRandomMin.getText().toString().trim() : "";
            String maxStr = binding.etRandomMax.getText() != null ? binding.etRandomMax.getText().toString().trim() : "";
            
            try {
                minRandom = Integer.parseInt(minStr);
                maxRandom = Integer.parseInt(maxStr);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "请输入有效的随机间隔数字", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (minRandom <= 0) {
                Toast.makeText(requireContext(), "最小间隔必须大于0分钟", Toast.LENGTH_SHORT).show();
                return;
            }
            if (maxRandom < minRandom) {
                Toast.makeText(requireContext(), "最大间隔不能小于最小间隔", Toast.LENGTH_SHORT).show();
                return;
            }
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
        alarmToSave.setBaseHour(hour);
        alarmToSave.setBaseMinute(minute);
        alarmToSave.setEnabled(true);
        alarmToSave.setActionType(actionType);
        alarmToSave.setTargetAppsJson(selectedTargetAppsJson);
        alarmToSave.setRandom(isRandom);
        alarmToSave.setRandomMinMinutes(minRandom);
        alarmToSave.setRandomMaxMinutes(maxRandom);

        // Cancel existing scheduled alarm if editing
        if (currentAlarm != null) {
            AlarmScheduler.cancelAlarm(requireContext(), currentAlarm);
        }

        // Save to Storage
        AlarmStorage.getInstance(requireContext()).saveAlarm(alarmToSave);

        // Schedule new alarm
        AlarmScheduler.scheduleAlarm(requireContext(), alarmToSave);

        String msg = isRandom ? "闹钟已保存并启用 (" + alarmToSave.getFormattedTime() + "，开启每日随机)"
                              : "闹钟已保存并启用 (" + alarmToSave.getFormattedTime() + ")";
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();

        NavHostFragment.findNavController(AddEditAlarmFragment.this).navigateUp();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
