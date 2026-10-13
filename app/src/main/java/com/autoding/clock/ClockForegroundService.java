package com.autoding.clock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

public class ClockForegroundService extends Service {

    private static final String CHANNEL_ID = "clock_service_channel";
    private static final int NOTIFICATION_ID = 10001;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 执行打卡
        ClockAccessibilityService service = ClockAccessibilityService.getInstance();
        boolean isOffWork = intent != null && intent.getBooleanExtra("is_offwork", false);

        if (service != null) {
            service.startClocking(isOffWork);
        }

        // 打卡完成后停止服务
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                stopSelf();
            }
        }, 30000); // 30秒后停止

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "自动打卡服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("保持后台运行以执行自动打卡");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("自动打卡正在运行")
                .setContentText("到点将自动执行打卡操作")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
}
