package com.autoding.clock;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;
import java.util.Random;

public class ClockAccessibilityService extends AccessibilityService {

    private static final String TAG = "ClockAccessibilityService";
    private static final String DINGTALK_PACKAGE = "com.alibaba.android.rimet";
    private static ClockAccessibilityService instance;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isClocking = false;
    private int retryCount = 0;
    private static final int MAX_RETRY = 3;
    private static final long STEP_DELAY = 2500; // 每步等待时间
    private boolean isOffWork = false; // 区分上班/下班打卡

    public static ClockAccessibilityService getInstance() {
        return instance;
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        AccessibilityServiceInfo info = getServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        info.notificationTimeout = 100;
        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (isClocking && event.getPackageName() != null &&
                event.getPackageName().toString().equals(DINGTALK_PACKAGE)) {
            // 监控钉钉界面变化
        }
    }

    @Override
    public void onInterrupt() {
        isClocking = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
    }

    /**
     * 开始打卡流程
     */
    public void startClocking(boolean isOffWork) {
        if (isClocking) {
            Log.d(TAG, "Already clocking, skip");
            return;
        }
        this.isOffWork = isOffWork;
        isClocking = true;
        retryCount = 0;

        // 启动钉钉
        launchDingTalk();
    }

    private void launchDingTalk() {
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage(DINGTALK_PACKAGE);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                Log.d(TAG, "Launched DingTalk");
                // 等待钉钉启动后，开始查找打卡入口
                handler.postDelayed(this::navigateToWorkbench, STEP_DELAY * 2);
            } else {
                Log.e(TAG, "DingTalk not installed");
                isClocking = false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch DingTalk: " + e.getMessage());
            isClocking = false;
        }
    }

    /**
     * 导航到工作台
     */
    private void navigateToWorkbench() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            retry("Root node is null, waiting...");
            return;
        }

        // 尝试点击底部"工作台"Tab
        if (clickByText(root, "工作台")) {
            Log.d(TAG, "Clicked 工作台");
            handler.postDelayed(this::findAttendance, STEP_DELAY);
        } else {
            retry("Cannot find 工作台");
        }
    }

    /**
     * 查找考勤打卡入口
     */
    private void findAttendance() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            retry("Root node is null at findAttendance");
            return;
        }

        // 尝试点击"考勤打卡"
        if (clickByText(root, "考勤打卡")) {
            Log.d(TAG, "Clicked 考勤打卡");
            handler.postDelayed(this::doClockIn, STEP_DELAY * 2);
        } else {
            retry("Cannot find 考勤打卡");
        }
    }

    /**
     * 执行打卡操作
     */
    private void doClockIn() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            retry("Root node is null at doClockIn");
            return;
        }

        // 尝试点击打卡按钮（上班/下班）
        String targetText = isOffWork ? "下班打卡" : "上班打卡";
        if (clickByText(root, targetText)) {
            Log.d(TAG, "Clicked " + targetText + ", clock in success!");
            handler.postDelayed(() -> isClocking = false, 2000);
        } else if (clickByText(root, "打卡")) {
            Log.d(TAG, "Clicked 打卡, clock in success!");
            handler.postDelayed(() -> isClocking = false, 2000);
        } else {
            retry("Cannot find clock button");
        }
    }

    private void retry(String reason) {
        retryCount++;
        Log.d(TAG, "Retry " + retryCount + ": " + reason);
        if (retryCount > MAX_RETRY) {
            Log.e(TAG, "Max retry reached, giving up");
            isClocking = false;
            return;
        }
        handler.postDelayed(this::navigateToWorkbench, STEP_DELAY);
    }

    /**
     * 通过文本查找并点击节点
     */
    private boolean clickByText(AccessibilityNodeInfo root, String text) {
        if (root == null) return false;

        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(text);
        for (AccessibilityNodeInfo node : nodes) {
            // 尝试直接点击
            if (node.isClickable()) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return true;
            }
            // 尝试点击父节点
            AccessibilityNodeInfo parent = node.getParent();
            while (parent != null) {
                if (parent.isClickable()) {
                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    return true;
                }
                parent = parent.getParent();
            }
        }
        return false;
    }

    /**
     * 通过ID查找并点击
     */
    private boolean clickById(String viewId) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;

        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(viewId);
        for (AccessibilityNodeInfo node : nodes) {
            if (node.isClickable()) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return true;
            }
            AccessibilityNodeInfo parent = node.getParent();
            while (parent != null) {
                if (parent.isClickable()) {
                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    return true;
                }
                parent = parent.getParent();
            }
        }
        return false;
    }
}
