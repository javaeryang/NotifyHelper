package com.fun.notifyhelper.storage;

import android.content.Context;
import android.content.SharedPreferences;

public class SystemAlarmStorage {

    private static final String PREF_NAME = "system_alarm_config";
    private static final String KEY_ENABLED = "system_alarm_enabled";
    private static final String KEY_TARGET_APPS_JSON = "system_alarm_target_apps_json";

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

    public String getTargetAppsJson() {
        // Fallback for old single package name
        String appsJson = preferences.getString(KEY_TARGET_APPS_JSON, "[]");
        if (appsJson.equals("[]")) {
            String legacyPkg = preferences.getString("system_alarm_target_package", "");
            String legacyName = preferences.getString("system_alarm_target_app", "");
            if (!legacyPkg.isEmpty()) {
                try {
                    com.fun.notifyhelper.model.TargetApp legacyApp = new com.fun.notifyhelper.model.TargetApp(
                            legacyName, legacyPkg, "", 0
                    );
                    org.json.JSONArray array = new org.json.JSONArray();
                    array.put(legacyApp.toJSONObject());
                    appsJson = array.toString();
                    setTargetAppsJson(appsJson); // migrate to new key
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        return appsJson;
    }

    public void setTargetAppsJson(String json) {
        preferences.edit().putString(KEY_TARGET_APPS_JSON, json).apply();
    }
}
