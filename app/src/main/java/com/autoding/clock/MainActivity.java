package com.autoding.clock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "clock_prefs";
    private SharedPreferences prefs;
    private TextView tvWorkTime, tvOffWorkTime, tvStatus;
    private CheckBox[] dayChecks = new CheckBox[7];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        tvWorkTime = findViewById(R.id.tv_work_time);
        tvOffWorkTime = findViewById(R.id.tv_off_work_time);
        tvStatus = findViewById(R.id.tv_status);

        dayChecks[0] = findViewById(R.id.cb_mon);
        dayChecks[1] = findViewById(R.id.cb_tue);
        dayChecks[2] = findViewById(R.id.cb_wed);
        dayChecks[3] = findViewById(R.id.cb_thu);
        dayChecks[4] = findViewById(R.id.cb_fri);
        dayChecks[5] = findViewById(R.id.cb_sat);
        dayChecks[6] = findViewById(R.id.cb_sun);

        // 加载已保存的设置
        loadSettings();

        // 设置上班时间
        tvWorkTime.setOnClickListener(v -> showTimePicker(true));
        Button btnWork = findViewById(R.id.btn_set_work);
        btnWork.setOnClickListener(v -> showTimePicker(true));

        // 设置下班时间
        tvOffWorkTime.setOnClickListener(v -> showTimePicker(false));
        Button btnOffWork = findViewById(R.id.btn_set_offwork);
        btnOffWork.setOnClickListener(v -> showTimePicker(false));

        // 保存设置
        Button btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> saveSettings());

        // 立即测试打卡
        Button btnTest = findViewById(R.id.btn_test);
        btnTest.setOnClickListener(v -> testClock());

        // 开启无障碍服务
        Button btnAccessibility = findViewById(R.id.btn_accessibility);
        btnAccessibility.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "请找到并开启「自动打卡」无障碍服务", Toast.LENGTH_LONG).show();
        });

        updateStatus();
    }

    private void showTimePicker(final boolean isWorkTime) {
        int hour = prefs.getInt(isWorkTime ? "work_hour" : "offwork_hour", isWorkTime ? 9 : 18);
        int minute = prefs.getInt(isWorkTime ? "work_minute" : "offwork_minute", isWorkTime ? 0 : 0);

        TimePickerDialog dialog = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(isWorkTime ? "work_hour" : "offwork_hour", hourOfDay);
                editor.putInt(isWorkTime ? "work_minute" : "offwork_minute", minute);
                editor.apply();

                if (isWorkTime) {
                    tvWorkTime.setText(String.format("上班时间: %02d:%02d", hourOfDay, minute));
                } else {
                    tvOffWorkTime.setText(String.format("下班时间: %02d:%02d", hourOfDay, minute));
                }
            }
        }, hour, minute, true);
        dialog.show();
    }

    private void loadSettings() {
        int workHour = prefs.getInt("work_hour", 9);
        int workMin = prefs.getInt("work_minute", 0);
        int offHour = prefs.getInt("offwork_hour", 18);
        int offMin = prefs.getInt("offwork_minute", 0);

        tvWorkTime.setText(String.format("上班时间: %02d:%02d", workHour, workMin));
        tvOffWorkTime.setText(String.format("下班时间: %02d:%02d", offHour, offMin));

        // 加载工作日
        for (int i = 0; i < 7; i++) {
            dayChecks[i].setChecked(prefs.getBoolean("day_" + i, i < 5));
        }
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = prefs.edit();
        for (int i = 0; i < 7; i++) {
            editor.putBoolean("day_" + i, dayChecks[i].isChecked());
        }
        editor.apply();

        // 设置闹钟
        ClockScheduler.scheduleAlarms(this);
        Toast.makeText(this, "设置已保存，定时打卡已开启！", Toast.LENGTH_SHORT).show();
        updateStatus();
    }

    private void testClock() {
        // 5秒后触发一次测试打卡
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.setAction("com.autoding.clock.TEST_CLOCK");
        PendingIntent pi = PendingIntent.getBroadcast(this, 9999, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5000, pi);
        Toast.makeText(this, "5秒后将测试打卡，请确保钉钉已登录", Toast.LENGTH_SHORT).show();
    }

    private void updateStatus() {
        StringBuilder sb = new StringBuilder("当前状态:\n");
        sb.append("上班: ").append(prefs.getInt("work_hour", 9)).append(":").append(String.format("%02d", prefs.getInt("work_minute", 0)));
        sb.append("  下班: ").append(prefs.getInt("offwork_hour", 18)).append(":").append(String.format("%02d", prefs.getInt("offwork_minute", 0)));
        sb.append("\n工作日: ");
        String[] days = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        for (int i = 0; i < 7; i++) {
            if (prefs.getBoolean("day_" + i, i < 5)) {
                sb.append(days[i]).append(" ");
            }
        }
        tvStatus.setText(sb.toString());
    }
}
