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

        if (launcherApps == null || userManager == null) {
            Log.e(TAG, "LauncherApps or UserManager is null, cannot launch apps");
            return;
        }

        new Thread(() -> {
            for (int i = 0; i < targetApps.size(); i++) {
                TargetApp targetApp = targetApps.get(i);
                Log.d(TAG, "Attempting to launch: " + targetApp.getAppName() + " (" + targetApp.getPackageName() + ")");

                boolean launched = false;
                try {
                    UserHandle targetUserHandle = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        List<UserHandle> userProfiles = userManager.getUserProfiles();
                        for (UserHandle profile : userProfiles) {
                            if (userManager.getSerialNumberForUser(profile) == targetApp.getUserSerialNumber()) {
                                targetUserHandle = profile;
                                break;
                            }
                        }

                        if (targetUserHandle == null) {
                            Log.w(TAG, "User profile not found for serial: " + targetApp.getUserSerialNumber() + ", falling back to primary");
                            targetUserHandle = android.os.Process.myUserHandle();
                        }

                        if (targetApp.getComponentName() != null && !targetApp.getComponentName().isEmpty()) {
                            ComponentName componentName = ComponentName.unflattenFromString(targetApp.getComponentName());
                            if (componentName != null) {
                                try {
                                    launcherApps.startMainActivity(componentName, targetUserHandle, null, null);
                                    launched = true;
                                    Log.d(TAG, "Successfully launched via LauncherApps");
                                } catch (SecurityException se) {
                                    Log.w(TAG, "SecurityException: INTERACT_ACROSS_USERS block triggered. Falling back to Advanced Methods.", se);
                                    launched = launchFallbackAsUser(context, targetApp, targetUserHandle);
                                }
                            }
                        }
                    }

                    if (!launched) {
                        launchFallback(context, targetApp);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to launch app: " + targetApp.getPackageName(), e);
                    launchFallback(context, targetApp);
                }

                // If this is not the last app, wait 5 seconds before launching the next one
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

    private static boolean launchFallbackAsUser(Context context, TargetApp targetApp, UserHandle userHandle) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return false;
        }
        
        int userId = -1;
        if (userHandle != null) {
            try {
                Method getIdentifierMethod = UserHandle.class.getMethod("getIdentifier");
                Object result = getIdentifierMethod.invoke(userHandle);
                if (result != null) {
                    userId = (int) result;
                }
            } catch (Exception ignore) { }
        }

        Log.d(TAG, "Extracted actual UserId: " + userId + " from UserHandle (SerialNumber: " + targetApp.getUserSerialNumber() + ")");

        // 终极武器：如果有 Root 权限，直接通过 shell 命令强制拉起
        if (userId > 0) { // Not the primary user (0)
            boolean rootSuccess = launchViaRootShell(targetApp, userId);
            if (rootSuccess) {
                return true;
            }
            Log.w(TAG, "Root shell execution failed or not permitted, trying alternative fallbacks...");
        }

        // 如果没有 Root 或者 Root 失败，尝试前面的隐式跨用户方案
        try {
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(targetApp.getPackageName());
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

                if (userId == 999 || targetApp.getUserSerialNumber() == 10) {
                    // For MIUI Dual Apps specifically (XSpace)
                    Log.d(TAG, "Attempting MIUI XSpace specific intent extra fallback for userId: " + userId);
                    launchIntent.putExtra("miui.intent.extra.EXPLICIT_SELECTED", true);
                    launchIntent.putExtra("miui.intent.extra.XSPACE", true);

                    try {
                        Method putExtraUserMethod = Intent.class.getMethod("putExtra", String.class, int.class);
                        putExtraUserMethod.invoke(launchIntent, "android.intent.extra.USER_ID", userId);
                    } catch (Exception e) {
                        // ignore
                    }

                    context.startActivity(launchIntent);
                    Log.d(TAG, "Successfully launched via MIUI XSpace Intent flag");
                    return true;
                }

                // Reflection to call startActivityAsUser
                Method startActivityAsUserMethod = Context.class.getMethod("startActivityAsUser", Intent.class, UserHandle.class);
                startActivityAsUserMethod.invoke(context, launchIntent, userHandle);
                Log.d(TAG, "Successfully launched via reflection startActivityAsUser for " + targetApp.getPackageName());
                return true;
            }
        } catch (Exception ex) {
            Log.e(TAG, "Reflection launchFallbackAsUser failed for: " + targetApp.getPackageName(), ex);
        }
        return false;
    }
    
    private static boolean launchViaRootShell(TargetApp targetApp, int userId) {
        String component = targetApp.getComponentName();
        if (component == null || component.isEmpty()) {
            return false;
        }
        
        Log.d(TAG, "Attempting Root Shell launch for User " + userId + " Component: " + component);
        String command = "am start -n " + component + " -f 0x10008000 --user " + userId;
        
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
                Log.d(TAG, "Root shell launched successfully!");
                return true;
            } else {
                Log.e(TAG, "Root shell launch failed with exit code: " + exitCode);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to execute root shell command", e);
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

    private static void launchFallback(Context context, TargetApp targetApp) {
        try {
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(targetApp.getPackageName());
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                context.startActivity(launchIntent);
                Log.d(TAG, "Successfully launched via primary fallback (standard startActivity) for " + targetApp.getPackageName());
            }
        } catch (Exception ex) {
            Log.e(TAG, "Final fallback launch failed for: " + targetApp.getPackageName(), ex);
        }
    }
}
