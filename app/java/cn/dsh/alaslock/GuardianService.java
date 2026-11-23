package cn.dsh.alaslock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import java.util.Set;

public class GuardianService extends Service {

    public static volatile boolean running = false;

    private static final String CHANNEL = "guard";
    private static final int NOTIF_ID = 1001;
    private static final long TICK_MS = 3000L;

    private Handler handler;
    private int verifyRound = 0;
    private boolean reportedIgnored = false;
    private boolean warned = false;
    private boolean forcePending = false;
    private int deadHits = 0;
    private long lastRebindAt = 0L;
    private static final long REBIND_COOLDOWN_MS = 60_000L;
    private String lastText = "";

    private final Runnable verifyRunnable = new Runnable() {
        @Override
        public void run() {
            Set<String> targets = Prefs.targets(GuardianService.this);
            Set<String> bound = boundServices();
            if (bound == null) {
                return; // 取不到系统真实状态，不瞎报
            }
            if (bound.containsAll(targets)) {
                reportedIgnored = false;
                return;
            }
            verifyRound++;
            if (verifyRound < 2 && handler != null) {
                handler.postDelayed(this, 3000L);
                return;
            }
            if (!reportedIgnored) {
                reportedIgnored = true;
                update("写回了但系统没认 · 可能被「受限设置」拦");
            }
        }
    };

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            try {
                if (!forcePending) {
                    repair();
                }
            } catch (Throwable t) {
                Log.w("ALock", "repair failed", t);
            }
            if (handler != null) {
                handler.postDelayed(this, TICK_MS);
            }
        }
    };

    private final Runnable forceRunnable = new Runnable() {
        @Override
        public void run() {
            forceRebind();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        running = true;
        handler = new Handler(Looper.getMainLooper());
        createChannel();
        startForeground(NOTIF_ID, notification("启动中"));
        Prefs.ensureJob(this);
        handler.postDelayed(tick, 1200L);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!Prefs.isEnabled(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (intent != null && intent.getBooleanExtra("force", false) && handler != null) {
            handler.post(forceRunnable);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private boolean canWrite() {
        return checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS")
                == PackageManager.PERMISSION_GRANTED;
    }

    private void repair() {
        Set<String> targets = Prefs.targets(this);
        if (targets.isEmpty()) {
            update("还没选择要守护的服务");
            return;
        }

        String cur = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        boolean accOn = "1".equals(Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_ENABLED));
        boolean strict = Prefs.strict(this);

        int action = LockLogic.action(cur, accOn, targets, strict);

        if (action == LockLogic.NONE) {
            if (!accOn) {
                update("总开关被关（非强制模式，不拨回）");
            } else {
                checkAlive(targets);
            }
            return;
        }

        if (!canWrite()) {
            if (!warned) {
                warned = true;
                update("缺授权，掉线后无法自动恢复");
            }
            return;
        }

        try {
            if ((action & LockLogic.LIST) != 0) {
                Settings.Secure.putString(getContentResolver(),
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                        LockLogic.withTargets(cur, targets));
            }
            if ((action & LockLogic.SWITCH) != 0) {
                Settings.Secure.putString(getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_ENABLED, "1");
            }
            int n = Prefs.bumpFix(this);
            Log.i("ALock", "restored: action=" + action);
            update("已恢复 · 累计 " + n + " 次");
            scheduleVerify();
        } catch (Throwable t) {
            Log.w("ALock", "write secure settings failed", t);
        }
    }

    /**
     * 开关看着是开的，服务可能已经死了。
     * 用系统实际绑定列表比对，连续两次都缺才动手，并加 60 秒冷却，
     * 避免「每 3 秒无脑重连」把目标 App 反复杀一遍。
     */
    private void checkAlive(java.util.Set<String> targets) {
        String ok = "正常 · 守护 " + targets.size() + " 个服务";
        if (!Prefs.autoRebind(this)) {
            update(ok);
            return;
        }
        java.util.Set<String> bound = boundServices();
        if (bound == null) {
            update(ok);          // 系统不告诉我们，就当正常，不瞎拨
            return;
        }
        if (bound.containsAll(targets)) {
            deadHits = 0;
            update(ok);
            return;
        }
        deadHits++;
        if (deadHits < 2) {
            update("疑似假死 · 观察中");
            return;
        }
        long now = android.os.SystemClock.elapsedRealtime();
        if (now - lastRebindAt < REBIND_COOLDOWN_MS) {
            update("疑似假死 · 冷却中");
            return;
        }
        autoRebind(targets);
    }

    private void autoRebind(java.util.Set<String> targets) {
        lastRebindAt = android.os.SystemClock.elapsedRealtime();
        deadHits = 0;
        String cur = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        try {
            forcePending = true;
            Settings.Secure.putString(getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    LockLogic.withoutTargets(cur, targets));
            int n = Prefs.bumpRebind(this);
            Prefs.log(this, "自动重连 #" + n + "（开关开着但服务没绑上）");
            Log.i("ALock", "auto rebind #" + n);
            update("自动重连 · 第 " + n + " 次");
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    forcePending = false;
                    repair();
                }
            }, 1500L);
        } catch (Throwable t) {
            forcePending = false;
            Log.w("ALock", "auto rebind failed", t);
        }
    }

    /** 系统当前真正绑上的无障碍服务；取不到时返回 null 表示「不知道」。 */
    private Set<String> boundServices() {
        try {
            android.view.accessibility.AccessibilityManager am =
                    (android.view.accessibility.AccessibilityManager)
                            getSystemService(ACCESSIBILITY_SERVICE);
            if (am == null) {
                return null;
            }
            java.util.List<android.accessibilityservice.AccessibilityServiceInfo> list =
                    am.getEnabledAccessibilityServiceList(
                            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
            if (list == null || list.isEmpty()) {
                return null;
            }
            Set<String> out = new java.util.LinkedHashSet<String>();
            for (android.accessibilityservice.AccessibilityServiceInfo i : list) {
                android.content.pm.ServiceInfo si =
                        i.getResolveInfo() == null ? null : i.getResolveInfo().serviceInfo;
                if (si != null) {
                    out.add(si.packageName + "/" + si.name);
                }
            }
            return out;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 写回之后复核：如果系统压根没把服务绑起来，说明被「受限设置」之类的拦截了，
     * 这时光靠写设置是没用的，得明确告诉用户。
     */
    private void scheduleVerify() {
        if (handler == null) {
            return;
        }
        verifyRound = 0;
        handler.postDelayed(verifyRunnable, 3000L);
    }

    /**
     * 把目标服务从无障碍列表里摘掉、停 1.5 秒再挂回去，逼系统重新绑定。
     * 用于「开关看着还是开的，但服务其实已经死了」的情况。
     */
    private void forceRebind() {
        Set<String> targets = Prefs.targets(this);
        if (targets.isEmpty()) {
            update("没选服务，无法重连");
            return;
        }
        if (!canWrite()) {
            update("缺授权，无法重连");
            return;
        }
        String cur = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        try {
            forcePending = true;
            Settings.Secure.putString(getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    LockLogic.withoutTargets(cur, targets));
            update("重连中…");
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    forcePending = false;
                    repair();
                }
            }, 1500L);
        } catch (Throwable t) {
            forcePending = false;
            Log.w("ALock", "force rebind failed", t);
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm == null) {
            return;
        }
        NotificationChannel ch = new NotificationChannel(CHANNEL, "守护状态",
                NotificationManager.IMPORTANCE_LOW);
        ch.setShowBadge(false);
        nm.createNotificationChannel(ch);
    }

    private Notification notification(String text) {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent open = PendingIntent.getActivity(this, 1,
                new Intent(this, MainActivity.class), flags);
        PendingIntent pause = PendingIntent.getBroadcast(this, 2,
                new Intent(this, StopReceiver.class), flags);

        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            b = new Notification.Builder(this, CHANNEL);
        } else {
            b = new Notification.Builder(this);
        }
        b.setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentTitle("无障碍锁")
                .setContentText(text)
                .setContentIntent(open)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false);
        try {
            b.addAction(android.R.drawable.ic_media_pause, "暂停", pause);
        } catch (Throwable ignored) {
        }
        return b.build();
    }

    private void update(String text) {
        if (text.equals(lastText)) {
            return;
        }
        lastText = text;
        try {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.notify(NOTIF_ID, notification(text));
            }
        } catch (Throwable ignored) {
        }
    }
}
