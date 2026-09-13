package com.fun.notifyhelper.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.fun.notifyhelper.storage.SystemAlarmStorage;

public class SystemAlarmBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "SysAlarmBroadcastRecv";
    private static long lastTriggerTime = 0;
    private static final long COOLDOWN_MS = 5000;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        SystemAlarmStorage storage = SystemAlarmStorage.getInstance(context);
        if (!storage.isEnabled()) {
            return;
        }

        String targetPackage = storage.getTargetPackageName();
        if (targetPackage == null || targetPackage.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastTriggerTime < COOLDOWN_MS) {
            Log.d(TAG, "Ignored duplicate system alarm broadcast trigger within cooldown");
            return;
        }
        lastTriggerTime = now;

        Log.d(TAG, "System alarm broadcast received: " + intent.getAction() + ", launching target: " + targetPackage);

        try {
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(targetPackage);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                context.startActivity(launchIntent);
                Log.d(TAG, "Successfully launched target app: " + targetPackage);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch target app from broadcast receiver: " + targetPackage, e);
        }
    }
}
