package com.fun.notifyhelper.service;

import android.app.Notification;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.fun.notifyhelper.storage.SystemAlarmStorage;
import com.fun.notifyhelper.utils.AppLauncherHelper;

import java.util.Arrays;
import java.util.List;

public class SystemAlarmNotificationListener extends NotificationListenerService {

    private static final String TAG = "SysAlarmNotifListener";
    private static long lastTriggerTime = 0;
    private static final long COOLDOWN_MS = 5000;

    private static final List<String> KNOWN_CLOCK_PACKAGES = Arrays.asList(
            "com.google.android.deskclock",
            "com.android.deskclock",
            "com.sec.android.app.clockpackage",
            "com.samsung.sec.android.clockpackage",
            "com.miui.calculator", // MIUI clock package
            "com.android.alarmclock",
            "com.huawei.deskclock",
            "com.coloros.alarmclock",
            "com.vivo.alarmclock",
            "com.oppo.alarmclock",
            "com.zte.handset.alarmclock",
            "com.asus.alarm"
    );

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;

        SystemAlarmStorage storage = SystemAlarmStorage.getInstance(this);
        if (!storage.isEnabled()) {
            return;
        }

        String targetAppsJson = storage.getTargetAppsJson();
        if (targetAppsJson == null || targetAppsJson.isEmpty() || targetAppsJson.equals("[]")) {
            return;
        }

        String pkgName = sbn.getPackageName();
        if (pkgName == null || pkgName.equals(getPackageName())) {
            return;
        }

        Notification notification = sbn.getNotification();
        if (notification == null) return;

        boolean isAlarm = false;

        // 1. Category check
        if (Notification.CATEGORY_ALARM.equals(notification.category)) {
            isAlarm = true;
        }

        // 2. Clock package check
        if (!isAlarm && KNOWN_CLOCK_PACKAGES.contains(pkgName.toLowerCase())) {
            isAlarm = true; // Broaden detection for clocks
        }

        if (isAlarm) {
            long now = System.currentTimeMillis();
            if (now - lastTriggerTime < COOLDOWN_MS) {
                Log.d(TAG, "Ignored duplicate system alarm notification trigger within cooldown");
                return;
            }
            lastTriggerTime = now;

            Log.d(TAG, "System alarm notification detected from package: " + pkgName + ", launching targets");
            AppLauncherHelper.launchAppsSequentially(getApplicationContext(), targetAppsJson);
        }
    }
}
