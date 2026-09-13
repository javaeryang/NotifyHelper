package com.fun.notifyhelper;

import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.fun.notifyhelper.databinding.ActivityAlarmAlertBinding;
import com.fun.notifyhelper.model.AlarmAction;
import com.fun.notifyhelper.receiver.AlarmScheduler;
import com.fun.notifyhelper.storage.AlarmStorage;

public class AlarmAlertActivity extends AppCompatActivity {

    private static final String TAG = "AlarmAlertActivity";
    private ActivityAlarmAlertBinding binding;
    private Ringtone ringtone;
    private Vibrator vibrator;
    private AlarmAction alarmAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupWindowFlags();

        binding = ActivityAlarmAlertBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        long alarmId = getIntent().getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1);
        if (alarmId > 0) {
            alarmAction = AlarmStorage.getInstance(this).getAlarmById(alarmId);
        }

        setupUI();
        startRingingAndVibrating();

        binding.btnStopAlarm.setOnClickListener(v -> dismissAndExecute());

        binding.btnExecuteAction.setOnClickListener(v -> dismissAndExecute());
    }

    private void dismissAndExecute() {
        stopAndDismiss();
        if (alarmAction != null && alarmAction.getActionType() == AlarmAction.ACTION_TYPE_OPEN_APP) {
            String pkgName = alarmAction.getPackageName();
            if (pkgName != null && !pkgName.isEmpty()) {
                launchTargetApp(pkgName);
            }
        }
    }

    private void setupWindowFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null) {
                km.requestDismissKeyguard(this, null);
            }
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            );
        }
    }

    private void setupUI() {
        if (alarmAction != null) {
            binding.tvAlarmTime.setText(alarmAction.getFormattedTime());

            String title = alarmAction.getTitle();
            if (title != null && !title.isEmpty()) {
                binding.tvAlarmTitle.setText(title);
            } else {
                binding.tvAlarmTitle.setText("闹钟响铃中...");
            }

            binding.tvAlarmActionDesc.setText(alarmAction.getActionDescription());

            if (alarmAction.getActionType() == AlarmAction.ACTION_TYPE_OPEN_APP
                    && alarmAction.getPackageName() != null && !alarmAction.getPackageName().isEmpty()) {
                String appName = alarmAction.getAppName();
                if (appName != null && !appName.isEmpty()) {
                    binding.btnExecuteAction.setText("打开 " + appName);
                } else {
                    binding.btnExecuteAction.setText("打开目标应用");
                }
                binding.btnExecuteAction.setVisibility(View.VISIBLE);
            } else {
                binding.btnExecuteAction.setVisibility(View.GONE);
            }
        } else {
            binding.tvAlarmTime.setText("闹钟响铃");
            binding.tvAlarmTitle.setText("时间到了！");
            binding.tvAlarmActionDesc.setText("");
            binding.btnExecuteAction.setVisibility(View.GONE);
        }
    }

    private void startRingingAndVibrating() {
        try {
            Uri alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alertUri == null) {
                alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            ringtone = RingtoneManager.getRingtone(getApplicationContext(), alertUri);
            if (ringtone != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ringtone.setLooping(true);
                }
                ringtone.play();
            }

            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = new long[]{0, 800, 800};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
                } else {
                    vibrator.vibrate(pattern, 0);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting ringtone or vibration", e);
        }
    }

    private void stopAndDismiss() {
        try {
            if (ringtone != null && ringtone.isPlaying()) {
                ringtone.stop();
            }
            if (vibrator != null) {
                vibrator.cancel();
            }

            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && alarmAction != null) {
                nm.cancel((int) alarmAction.getId());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping ringtone/vibration", e);
        }
        finish();
    }

    private void launchTargetApp(String packageName) {
        try {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                startActivity(launchIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch target app: " + packageName, e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAndDismiss();
        binding = null;
    }
}
