package com.fun.notifyhelper.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.fun.notifyhelper.AlarmAlertActivity;
import com.fun.notifyhelper.R;
import com.fun.notifyhelper.model.AlarmAction;
import com.fun.notifyhelper.storage.AlarmStorage;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";
    private static final String CHANNEL_ID = "alarm_helper_channel";
    private static final String CHANNEL_NAME = "闹钟助手通知";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive triggered");
        if (intent == null) return;

        long alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1);
        if (alarmId == -1) return;

        AlarmAction action = AlarmStorage.getInstance(context).getAlarmById(alarmId);
        if (action == null || !action.isEnabled()) {
            return;
        }

        Log.d(TAG, "Executing alarm action: id=" + action.getId() + ", type=" + action.getActionType() + ", app=" + action.getAppName());

        // 1. Show high-priority notification pointing to AlarmAlertActivity
        showNotificationAndRing(context, action);

        // 2. Launch AlarmAlertActivity directly
        launchAlarmAlertActivity(context, action);

        // 3. Reschedule alarm for NEXT DAY (prevents re-triggering in same minute)
        AlarmScheduler.scheduleAlarmForNextDay(context, action);
    }

    private void showNotificationAndRing(Context context, AlarmAction action) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (defaultSoundUri == null) {
            defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("闹钟触发时的响铃与通知");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 500, 500});
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            channel.setSound(defaultSoundUri, audioAttributes);
            notificationManager.createNotificationChannel(channel);
        }

        Intent alertIntent = new Intent(context, AlarmAlertActivity.class);
        alertIntent.putExtra(AlarmScheduler.EXTRA_ALARM_ID, action.getId());
        alertIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) action.getId(),
                alertIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = action.getTitle();
        if (title == null || title.isEmpty()) {
            if (action.getActionType() == AlarmAction.ACTION_TYPE_OPEN_APP && action.getAppName() != null && !action.getAppName().isEmpty()) {
                title = "闹钟响铃: 即将打开 " + action.getAppName();
            } else {
                title = "闹钟响铃 (" + action.getFormattedTime() + ")";
            }
        }
        String content = action.getActionDescription();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setVibrate(new long[]{0, 500, 500, 500})
                .setFullScreenIntent(pendingIntent, true)
                .setContentIntent(pendingIntent);

        notificationManager.notify((int) action.getId(), builder.build());
    }

    private void launchAlarmAlertActivity(Context context, AlarmAction action) {
        try {
            Intent alertIntent = new Intent(context, AlarmAlertActivity.class);
            alertIntent.putExtra(AlarmScheduler.EXTRA_ALARM_ID, action.getId());
            alertIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(alertIntent);
            Log.d(TAG, "Successfully started AlarmAlertActivity");
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch AlarmAlertActivity directly", e);
        }
    }
}
