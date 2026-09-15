package com.fun.notifyhelper.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.fun.notifyhelper.model.AlarmAction;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AlarmStorage {

    private static final String PREF_NAME = "notify_helper_alarms";
    private static final String KEY_ALARM_LIST = "alarm_list_json";

    private static AlarmStorage instance;
    private final SharedPreferences preferences;

    private AlarmStorage(Context context) {
        this.preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized AlarmStorage getInstance(Context context) {
        if (instance == null) {
            instance = new AlarmStorage(context);
        }
        return instance;
    }

    public List<AlarmAction> getAllAlarms() {
        List<AlarmAction> list = new ArrayList<>();
        String jsonStr = preferences.getString(KEY_ALARM_LIST, null);
        if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
                JSONArray jsonArray = new JSONArray(jsonStr);
                for (int i = 0; i < jsonArray.length(); i++) {
                    list.add(AlarmAction.fromJSONObject(jsonArray.getJSONObject(i)));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        Collections.sort(list, (a1, a2) -> {
            int t1 = a1.getHour() * 60 + a1.getMinute();
            int t2 = a2.getHour() * 60 + a2.getMinute();
            return Integer.compare(t1, t2);
        });
        return list;
    }

    public AlarmAction getAlarmById(long id) {
        List<AlarmAction> list = getAllAlarms();
        for (AlarmAction action : list) {
            if (action.getId() == id) {
                return action;
            }
        }
        return null;
    }

    public void saveAlarm(AlarmAction alarmAction) {
        List<AlarmAction> list = getAllAlarms();
        boolean updated = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId() == alarmAction.getId()) {
                list.set(i, alarmAction);
                updated = true;
                break;
            }
        }
        if (!updated) {
            list.add(alarmAction);
        }
        saveAllAlarms(list);
    }

    public void deleteAlarm(long id) {
        List<AlarmAction> list = getAllAlarms();
        List<AlarmAction> newList = new ArrayList<>();
        for (AlarmAction action : list) {
            if (action.getId() != id) {
                newList.add(action);
            }
        }
        saveAllAlarms(newList);
    }

    private void saveAllAlarms(List<AlarmAction> list) {
        Collections.sort(list, (a1, a2) -> {
            int t1 = a1.getHour() * 60 + a1.getMinute();
            int t2 = a2.getHour() * 60 + a2.getMinute();
            return Integer.compare(t1, t2);
        });

        JSONArray jsonArray = new JSONArray();
        for (AlarmAction action : list) {
            try {
                jsonArray.put(action.toJSONObject());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        preferences.edit().putString(KEY_ALARM_LIST, jsonArray.toString()).apply();
    }
}
