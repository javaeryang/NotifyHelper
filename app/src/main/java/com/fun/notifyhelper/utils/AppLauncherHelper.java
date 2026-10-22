package com.fun.notifyhelper.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import com.fun.notifyhelper.model.TargetApp;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.DataOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class AppLauncherHelper {

    private static final String TAG = "AppLauncherHelper";

    public static void launchAppsSequentially(Context context, String targetAppsJson) {
        if (targetAppsJson == null || targetAppsJson.isEmpty()) {
            return;
        }

        List<TargetApp> targetApps = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(targetAppsJson);
            for (int i = 0; i < jsonArray.length(); i++) {
                targetApps.add(TargetApp.fromJSONObject(jsonArray.getJSONObject(i)));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse target apps JSON", e);
            return;
        }

        if (targetApps.isEmpty()) {
            return;
        }

        LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);

        new Thread(() -> {
            for (int i = 0; i < targetApps.size(); i++) {
                TargetApp targetApp = targetApps.get(i);
                Log.d(TAG, "Attempting to launch: " + targetApp.getAppName() + " (" + targetApp.getPackageName() + ")");

                // Determine UserHandle and UserId
                UserHandle targetUserHandle = getUserHandleBySerial(userManager, targetApp.getUserSerialNumber());
                int userId = getUserIdFromHandle(targetUserHandle);

                if (userId > 0 || targetApp.getUserSerialNumber() > 0) {
                    // Dual App / Secondary User -> Execute ONLY via Root shell command "am start --user <userId>"
                    int actualUserId = userId > 0 ? userId : 999;
                    Log.d(TAG, "Dual app detected (User " + actualUserId + "). Executing ONLY Root Shell launch.");
                    boolean success = launchViaRootShell(targetApp, actualUserId);
                    if (!success) {
                        Log.e(TAG, "Root shell launch failed for dual app: " + targetApp.getPackageName());
                    }
                } else {
                    // Primary User (User 0) -> Standard LauncherApps / startActivity
                    launchPrimaryApp(context, launcherApps, targetApp, targetUserHandle);
                }

                // If not the last app, wait 5 seconds before launching the next one
                if (i < targetApps.size() - 1) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Sequential launch sleep interrupted", e);
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }).start();
    }

    private static UserHandle getUserHandleBySerial(UserManager userManager, long serialNumber) {
        if (userManager != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            List<UserHandle> userProfiles = userManager.getUserProfiles();
            for (UserHandle profile : userProfiles) {
                if (userManager.getSerialNumberForUser(profile) == serialNumber) {
                    return profile;
                }
            }
        }
        return android.os.Process.myUserHandle();
    }

    private static int getUserIdFromHandle(UserHandle userHandle) {
        if (userHandle != null) {
            try {
                Method getIdentifierMethod = UserHandle.class.getMethod("getIdentifier");
                Object result = getIdentifierMethod.invoke(userHandle);
                if (result != null) {
                    return (int) result;
                }
            } catch (Exception ignore) { }
        }
        return 0;
    }

    private static void launchPrimaryApp(Context context, LauncherApps launcherApps, TargetApp targetApp, UserHandle userHandle) {
        try {
            if (launcherApps != null && targetApp.getComponentName() != null && !targetApp.getComponentName().isEmpty()) {
                ComponentName componentName = ComponentName.unflattenFromString(targetApp.getComponentName());
                if (componentName != null) {
                    UserHandle handle = userHandle != null ? userHandle : android.os.Process.myUserHandle();
                    launcherApps.startMainActivity(componentName, handle, null, null);
                    Log.d(TAG, "Successfully launched primary app via LauncherApps");
                    return;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "LauncherApps failed for primary app, trying fallback startActivity", e);
        }

        // Fallback for primary user
        try {
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(targetApp.getPackageName());
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                context.startActivity(launchIntent);
                Log.d(TAG, "Successfully launched primary app via startActivity");
            }
        } catch (Exception ex) {
            Log.e(TAG, "Primary app launch failed for: " + targetApp.getPackageName(), ex);
        }
    }

    private static boolean launchViaRootShell(TargetApp targetApp, int userId) {
        String component = targetApp.getComponentName();
        if (component == null || component.isEmpty()) {
            component = targetApp.getPackageName() + "/.ui.LauncherUI";
        }

        String command = "am start -n " + component + " -f 0x10008000 --user " + userId;
        Log.d(TAG, "Executing Root Shell Command: " + command);

        Process process = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                Log.d(TAG, "Root shell command executed successfully for dual app!");
                return true;
            } else {
                Log.e(TAG, "Root shell command failed with exit code: " + exitCode);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to execute root shell command: " + command, e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (Exception ignore) {}
            }
            if (process != null) {
                process.destroy();
            }
        }
        return false;
    }
}
