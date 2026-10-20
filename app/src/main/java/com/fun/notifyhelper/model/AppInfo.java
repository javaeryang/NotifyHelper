package com.fun.notifyhelper.model;

import android.graphics.drawable.Drawable;

public class AppInfo {
    private String appName;
    private String packageName;
    private String componentName;
    private Drawable icon;
    private long userSerialNumber;
    private boolean isSelected;

    public AppInfo(String appName, String packageName, String componentName, Drawable icon, long userSerialNumber) {
        this.appName = appName;
        this.packageName = packageName;
        this.componentName = componentName;
        this.icon = icon;
        this.userSerialNumber = userSerialNumber;
        this.isSelected = false;
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

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    public long getUserSerialNumber() {
        return userSerialNumber;
    }

    public void setUserSerialNumber(long userSerialNumber) {
        this.userSerialNumber = userSerialNumber;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
