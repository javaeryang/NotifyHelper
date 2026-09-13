package com.fun.notifyhelper;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.fun.notifyhelper.model.AlarmAction;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class AlarmActionTest {

    @Test
    public void testAlarmActionSerialization() throws JSONException {
        AlarmAction action = new AlarmAction(
                1001L,
                "测试早晨应用打开",
                8,
                30,
                true,
                AlarmAction.ACTION_TYPE_OPEN_APP,
                "com.tencent.mm",
                "微信",
                ""
        );

        JSONObject json = action.toJSONObject();
        AlarmAction restored = AlarmAction.fromJSONObject(json);

        assertEquals(1001L, restored.getId());
        assertEquals("测试早晨应用打开", restored.getTitle());
        assertEquals(8, restored.getHour());
        assertEquals(30, restored.getMinute());
        assertTrue(restored.isEnabled());
        assertEquals(AlarmAction.ACTION_TYPE_OPEN_APP, restored.getActionType());
        assertEquals("com.tencent.mm", restored.getPackageName());
        assertEquals("微信", restored.getAppName());
        assertEquals("08:30", restored.getFormattedTime());
        assertEquals("打开应用: 微信", restored.getActionDescription());
    }
}
