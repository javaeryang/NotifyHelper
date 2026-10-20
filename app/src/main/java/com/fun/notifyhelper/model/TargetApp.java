package com.fun.notifyhelper.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class TargetApp implements Serializable {
    private String appName;
    private String packageName;
    private String componentName;
    private long userSerialNumber;

    public TargetApp() {
    }

    public TargetApp(String appName, String packageName, String componentName, long userSerialNumber) {
        this.appName = appName;
        this.packageName = packageName;
        this.componentName = componentName;
        this.userSerialNumber = userSerialNumber;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public long getUserSerialNumber() {
        return userSerialNumber;
    }

    public void setUserSerialNumber(long userSerialNumber) {
        this.userSerialNumber = userSerialNumber;
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("appName", appName);
        json.put("packageName", packageName);
        json.put("componentName", componentName);
        json.put("userSerialNumber", userSerialNumber);
        return json;
    }

    public static TargetApp fromJSONObject(JSONObject json) {
        TargetApp app = new TargetApp();
        app.setAppName(json.optString("appName", ""));
        app.setPackageName(json.optString("packageName", ""));
        app.setComponentName(json.optString("componentName", ""));
        app.setUserSerialNumber(json.optLong("userSerialNumber", 0));
        return app;
    }
}
