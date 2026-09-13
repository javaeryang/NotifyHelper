package com.fun.notifyhelper.storage;

import android.content.Context;
import android.content.SharedPreferences;

public class SystemAlarmStorage {

    private static final String PREF_NAME = "system_alarm_config";
    private static final String KEY_ENABLED = "system_alarm_enabled";
    private static final String KEY_PACKAGE_NAME = "system_alarm_target_package";
    private static final String KEY_APP_NAME = "system_alarm_target_app";

    private static SystemAlarmStorage instance;
    private final SharedPreferences preferences;

    private SystemAlarmStorage(Context context) {
        this.preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SystemAlarmStorage getInstance(Context context) {
        if (instance == null) {
            instance = new SystemAlarmStorage(context);
        }
        return instance;
    }

    public boolean isEnabled() {
        return preferences.getBoolean(KEY_ENABLED, false);
    }

    public void setEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public String getTargetPackageName() {
        return preferences.getString(KEY_PACKAGE_NAME, "");
    }

    public void setTargetPackageName(String packageName) {
        preferences.edit().putString(KEY_PACKAGE_NAME, packageName).apply();
    }

    public String getTargetAppName() {
        return preferences.getString(KEY_APP_NAME, "");
    }

    public void setTargetAppName(String appName) {
        preferences.edit().putString(KEY_APP_NAME, appName).apply();
    }
}
