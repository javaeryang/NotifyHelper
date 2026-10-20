package com.fun.notifyhelper.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
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
    private String targetAppsJson; // JSON array of TargetApp
    private String audioPath;

    public AlarmAction() {
        this.id = System.currentTimeMillis();
        this.enabled = true;
        this.actionType = ACTION_TYPE_OPEN_APP;
        this.title = "";
        this.targetAppsJson = "[]";
        this.audioPath = "";
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

    public String getTargetAppsJson() {
        return targetAppsJson;
    }

    public void setTargetAppsJson(String targetAppsJson) {
        this.targetAppsJson = targetAppsJson;
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
            if (targetAppsJson != null && !targetAppsJson.isEmpty() && !targetAppsJson.equals("[]")) {
                try {
                    JSONArray array = new JSONArray(targetAppsJson);
                    int count = array.length();
                    if (count > 0) {
                        JSONObject firstApp = array.getJSONObject(0);
                        String firstAppName = firstApp.optString("appName", "");
                        if (count == 1) {
                            return "打开应用: " + firstAppName;
                        } else {
                            return "打开应用: " + firstAppName + " 等 " + count + " 个应用";
                        }
                    }
                } catch (JSONException e) {
                    return "打开应用 (格式错误)";
                }
            }
            return "打开应用 (未设定)";
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
        json.put("targetAppsJson", targetAppsJson);
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
        action.setTargetAppsJson(json.optString("targetAppsJson", "[]"));
        
        // Backward compatibility for old single package name
        if (json.has("packageName") && !json.optString("packageName").isEmpty() && action.getTargetAppsJson().equals("[]")) {
            try {
                TargetApp legacyApp = new TargetApp(
                        json.optString("appName", ""),
                        json.optString("packageName", ""),
                        "",
                        0
                );
                JSONArray array = new JSONArray();
                array.put(legacyApp.toJSONObject());
                action.setTargetAppsJson(array.toString());
            } catch (JSONException e) {
                // ignore
            }
        }
        
        action.setAudioPath(json.optString("audioPath", ""));
        return action;
    }
}
