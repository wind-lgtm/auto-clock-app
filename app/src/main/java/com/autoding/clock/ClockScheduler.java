package com.autoding.clock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;
import java.util.Random;

public class ClockScheduler {

    private static final String TAG = "ClockScheduler";
    private static final String PREFS_NAME = "clock_prefs";
    private static final int REQUEST_WORK = 1001;
    private static final int REQUEST_OFFWORK = 1002;
    private static final int EARLY_MINUTES = 5; // 上班提前5分钟
    private static final int LATE_MINUTES = 3;  // 下班延后3分钟

    /**
     * 设置所有闹钟
     */
    public static void scheduleAlarms(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // 取消已有的闹钟
        cancelAlarms(context);

        // 设置上班闹钟
        int workHour = prefs.getInt("work_hour", 9);
        int workMin = prefs.getInt("work_minute", 0);
        scheduleDayAlarms(context, am, workHour, workMin, true, prefs);

        // 设置下班闹钟
        int offHour = prefs.getInt("offwork_hour", 18);
        int offMin = prefs.getInt("offwork_minute", 0);
        scheduleDayAlarms(context, am, offHour, offMin, false, prefs);

        Log.d(TAG, "Alarms scheduled: work=" + workHour + ":" + workMin + " offwork=" + offHour + ":" + offMin);
    }

    /**
     * 为每一天设置闹钟
     */
    private static void scheduleDayAlarms(Context context, AlarmManager am, int hour, int minute,
                                         boolean isWork, SharedPreferences prefs) {
        Random random = new Random();
        Calendar calendar = Calendar.getInstance();

        for (int day = 0; day < 7; day++) {
            if (!prefs.getBoolean("day_" + day, day < 5)) {
                continue; // 这天不上班
            }

            Calendar target = Calendar.getInstance();
            // 计算下一个目标日期
            int currentDayOfWeek = target.get(Calendar.DAY_OF_WEEK);
            // Calendar: SUNDAY=1, MONDAY=2, ..., SATURDAY=7
            // 我们的 day: 0=周一, 1=周二, ..., 6=周日
            int targetDayOfWeek = day + 2; // 0->2(MONDAY), 6->1(SUNDAY)
            if (targetDayOfWeek > 7) targetDayOfWeek = 1;

            int daysDiff = targetDayOfWeek - currentDayOfWeek;
            if (daysDiff <= 0) {
                daysDiff += 7;
            }

            target.add(Calendar.DAY_OF_MONTH, daysDiff);
            target.set(Calendar.HOUR_OF_DAY, hour);
            target.set(Calendar.MINUTE, minute);
            target.set(Calendar.SECOND, 0);
            target.set(Calendar.MILLISECOND, 0);

            // 加上随机偏移
            int offset = isWork ? -random.nextInt(EARLY_MINUTES) : random.nextInt(LATE_MINUTES);
            target.add(Calendar.MINUTE, offset);

            // 如果时间已过，则设为下周
            if (target.getTimeInMillis() <= System.currentTimeMillis()) {
                target.add(Calendar.DAY_OF_MONTH, 7);
            }

            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.setAction(isWork ? "com.autoding.clock.WORK_CLOCK" : "com.autoding.clock.OFFWORK_CLOCK");
            intent.putExtra("is_offwork", !isWork);

            int requestCode = isWork ? (REQUEST_WORK + day) : (REQUEST_OFFWORK + day);
            PendingIntent pi = PendingIntent.getBroadcast(context, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.getTimeInMillis(), pi);
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, target.getTimeInMillis(), pi);
                }
                Log.d(TAG, "Scheduled " + (isWork ? "work" : "offwork") + " for day " + day + " at " + target.getTime());
            } catch (SecurityException e) {
                Log.e(TAG, "Failed to schedule alarm: " + e.getMessage());
            }
        }
    }

    /**
     * 取消所有闹钟
     */
    public static void cancelAlarms(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);

        for (int i = 0; i < 7; i++) {
            PendingIntent pi1 = PendingIntent.getBroadcast(context, REQUEST_WORK + i, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            PendingIntent pi2 = PendingIntent.getBroadcast(context, REQUEST_OFFWORK + i, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            am.cancel(pi1);
            am.cancel(pi2);
        }
    }
}
