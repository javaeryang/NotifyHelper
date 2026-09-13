package com.fun.notifyhelper.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Locale;

public class AlarmAction implements Serializable {

    public static final int ACTION_TYPE_OPEN_APP = 1;
    public static final int ACTION_TYPE_PLAY_AUDIO = 2;

    private long id;
    private String title;
    private int hour;
    private int minute;
    private boolean enabled;
    private int actionType;
    private String packageName;
    private String appName;
    private String audioPath;

    public AlarmAction() {
        this.id = System.currentTimeMillis();
        this.enabled = true;
        this.actionType = ACTION_TYPE_OPEN_APP;
        this.title = "";
        this.packageName = "";
        this.appName = "";
        this.audioPath = "";
    }

    public AlarmAction(long id, String title, int hour, int minute, boolean enabled, int actionType, String packageName, String appName, String audioPath) {
        this.id = id;
        this.title = title;
        this.hour = hour;
        this.minute = minute;
        this.enabled = enabled;
        this.actionType = actionType;
        this.packageName = packageName;
        this.appName = appName;
        this.audioPath = audioPath;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getActionType() {
        return actionType;
    }

    public void setActionType(int actionType) {
        this.actionType = actionType;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAudioPath() {
        return audioPath;
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }

    public String getFormattedTime() {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
    }

    public String getActionDescription() {
        if (actionType == ACTION_TYPE_OPEN_APP) {
            if (appName != null && !appName.isEmpty()) {
                return "打开应用: " + appName;
            } else if (packageName != null && !packageName.isEmpty()) {
                return "打开应用: " + packageName;
            } else {
                return "打开应用 (未设定)";
            }
        } else if (actionType == ACTION_TYPE_PLAY_AUDIO) {
            return "播放音频/铃声";
        }
        return "未知动作";
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("title", title);
        json.put("hour", hour);
        json.put("minute", minute);
        json.put("enabled", enabled);
        json.put("actionType", actionType);
        json.put("packageName", packageName);
        json.put("appName", appName);
        json.put("audioPath", audioPath);
        return json;
    }

    public static AlarmAction fromJSONObject(JSONObject json) {
        AlarmAction action = new AlarmAction();
        action.setId(json.optLong("id", System.currentTimeMillis()));
        action.setTitle(json.optString("title", ""));
        action.setHour(json.optInt("hour", 8));
        action.setMinute(json.optInt("minute", 0));
        action.setEnabled(json.optBoolean("enabled", true));
        action.setActionType(json.optInt("actionType", ACTION_TYPE_OPEN_APP));
        action.setPackageName(json.optString("packageName", ""));
        action.setAppName(json.optString("appName", ""));
        action.setAudioPath(json.optString("audioPath", ""));
        return action;
    }
}
