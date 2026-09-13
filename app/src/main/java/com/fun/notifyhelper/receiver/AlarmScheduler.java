package com.fun.notifyhelper.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.fun.notifyhelper.MainActivity;
import com.fun.notifyhelper.model.AlarmAction;
import com.fun.notifyhelper.storage.AlarmStorage;

import java.util.Calendar;
import java.util.List;

public class AlarmScheduler {

    private static final String TAG = "AlarmScheduler";
    public static final String ACTION_ALARM_TRIGGER = "com.fun.notifyhelper.ACTION_ALARM_TRIGGER";
    public static final String EXTRA_ALARM_ID = "extra_alarm_id";

    public static void scheduleAlarm(Context context, AlarmAction action) {
        scheduleAlarmInternal(context, action, false);
    }

    public static void scheduleAlarmForNextDay(Context context, AlarmAction action) {
        scheduleAlarmInternal(context, action, true);
    }

    private static void scheduleAlarmInternal(Context context, AlarmAction action, boolean forceNextDay) {
        if (action == null || !action.isEnabled()) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(ACTION_ALARM_TRIGGER);
        intent.putExtra(EXTRA_ALARM_ID, action.getId());

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        int requestCode = (int) (action.getId() % Integer.MAX_VALUE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags);

        long nowMillis = System.currentTimeMillis();
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(nowMillis);
        target.set(Calendar.HOUR_OF_DAY, action.getHour());
        target.set(Calendar.MINUTE, action.getMinute());
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        long targetMillis;

        if (forceNextDay) {
            // Explicitly rescheduling for tomorrow
            target.add(Calendar.DAY_OF_YEAR, 1);
            targetMillis = target.getTimeInMillis();
        } else {
            targetMillis = target.getTimeInMillis();
            if (targetMillis <= nowMillis - 60000) {
                // Time is earlier than current time today by more than 1 min -> schedule for tomorrow
                target.add(Calendar.DAY_OF_YEAR, 1);
                targetMillis = target.getTimeInMillis();
            } else if (targetMillis <= nowMillis) {
                // Time is within current minute (e.g. 10:42 set at 10:42:15) -> trigger in 2 seconds
                targetMillis = nowMillis + 2000;
            }
        }

        Log.d(TAG, "Scheduling alarm id=" + action.getId() + " (forceNextDay=" + forceNextDay + ") at " + targetMillis + " (in " + ((targetMillis - nowMillis) / 1000) + "s)");

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetMillis, pendingIntent);
                } else {
                    Intent showIntent;
                    if (action.getActionType() == AlarmAction.ACTION_TYPE_OPEN_APP
                            && action.getPackageName() != null && !action.getPackageName().isEmpty()) {
                        showIntent = context.getPackageManager().getLaunchIntentForPackage(action.getPackageName());
                    } else {
                        showIntent = new Intent(context, MainActivity.class);
                    }
                    if (showIntent == null) {
                        showIntent = new Intent(context, MainActivity.class);
                    }
                    showIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    PendingIntent showPendingIntent = PendingIntent.getActivity(context, requestCode, showIntent, flags);

                    AlarmManager.AlarmClockInfo clockInfo = new AlarmManager.AlarmClockInfo(targetMillis, showPendingIntent);
                    alarmManager.setAlarmClock(clockInfo, pendingIntent);
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, targetMillis, pendingIntent);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException scheduling exact alarm, fallback to setAndAllowWhileIdle", e);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetMillis, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, targetMillis, pendingIntent);
            }
        }
    }

    public static void cancelAlarm(Context context, AlarmAction action) {
        if (action == null) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(ACTION_ALARM_TRIGGER);

        int flags = PendingIntent.FLAG_NO_CREATE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        int requestCode = (int) (action.getId() % Integer.MAX_VALUE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags);

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    public static void rescheduleAllAlarms(Context context) {
        List<AlarmAction> list = AlarmStorage.getInstance(context).getAllAlarms();
        for (AlarmAction action : list) {
            if (action.isEnabled()) {
                scheduleAlarm(context, action);
            } else {
                cancelAlarm(context, action);
            }
        }
    }
}
