package com.fun.notifyhelper.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.fun.notifyhelper.storage.SystemAlarmStorage;
import com.fun.notifyhelper.utils.AppLauncherHelper;

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

        String targetAppsJson = storage.getTargetAppsJson();
        if (targetAppsJson == null || targetAppsJson.isEmpty() || targetAppsJson.equals("[]")) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastTriggerTime < COOLDOWN_MS) {
            Log.d(TAG, "Ignored duplicate system alarm broadcast trigger within cooldown");
            return;
        }
        lastTriggerTime = now;

        Log.d(TAG, "System alarm broadcast received: " + intent.getAction() + ", launching targets");
        AppLauncherHelper.launchAppsSequentially(context.getApplicationContext(), targetAppsJson);
    }
}
