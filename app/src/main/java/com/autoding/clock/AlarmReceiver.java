package com.autoding.clock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm triggered!");

        String action = intent.getAction();
        boolean isOffWork = intent.getBooleanExtra("is_offwork", false);

        // 启动前台服务执行打卡
        Intent serviceIntent = new Intent(context, ClockForegroundService.class);
        serviceIntent.putExtra("is_offwork", isOffWork);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }

        // 重新调度下一次闹钟
        ClockScheduler.scheduleAlarms(context);
    }
}
