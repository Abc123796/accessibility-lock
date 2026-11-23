package cn.dsh.alaslock;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

final class Prefs {

    private static final String FILE = "alock";
    private static final String K_ENABLED = "enabled";
    private static final String K_TARGETS = "targets";
    private static final int JOB_ID = 9001;

    private Prefs() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static boolean isEnabled(Context c) {
        return sp(c).getBoolean(K_ENABLED, false);
    }

    static void setEnabled(Context c, boolean v) {
        sp(c).edit().putBoolean(K_ENABLED, v).apply();
    }

    /** 强制保护：连总开关被手动关掉也要拨回来。默认开。 */
    static boolean strict(Context c) {
        return sp(c).getBoolean("strict", true);
    }

    static void setStrict(Context c, boolean v) {
        sp(c).edit().putBoolean("strict", v).apply();
    }

    /** 自动重连：开关看着开着、其实服务已经死了时，自动摘下来再挂回去 */
    static boolean autoRebind(Context c) {
        return sp(c).getBoolean("autoRebind", true);
    }

    static void setAutoRebind(Context c, boolean v) {
        sp(c).edit().putBoolean("autoRebind", v).apply();
    }

    static int rebinds(Context c) {
        return sp(c).getInt("rebinds", 0);
    }

    static int bumpRebind(Context c) {
        int n = rebinds(c) + 1;
        sp(c).edit().putInt("rebinds", n).apply();
        return n;
    }

    static void log(Context c, String line) {
        String ts = new java.text.SimpleDateFormat("MM-dd HH:mm:ss", java.util.Locale.US)
                .format(new java.util.Date());
        String old = sp(c).getString("log", "");
        String s = ts + "  " + line + "\n" + old;
        if (s.length() > 4000) {
            s = s.substring(0, 4000);
        }
        sp(c).edit().putString("log", s).apply();
    }

    static String logText(Context c) {
        return sp(c).getString("log", "");
    }

    static int fixes(Context c) {
        return sp(c).getInt("fixes", 0);
    }

    static int bumpFix(Context c) {
        int n = fixes(c) + 1;
        sp(c).edit().putInt("fixes", n).apply();
        return n;
    }

    static Set<String> targets(Context c) {
        Set<String> s = sp(c).getStringSet(K_TARGETS, Collections.<String>emptySet());
        return new HashSet<String>(s);
    }

    static void setTargets(Context c, Set<String> t) {
        sp(c).edit().putStringSet(K_TARGETS, new HashSet<String>(t)).apply();
    }

    static void startGuardian(Context c) {
        Intent i = new Intent(c, GuardianService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                c.startForegroundService(i);
            } else {
                c.startService(i);
            }
        } catch (Throwable ignored) {
        }
    }

    static void ensureJob(Context c) {
        try {
            JobScheduler js = (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (js == null) {
                return;
            }
            JobInfo.Builder b = new JobInfo.Builder(JOB_ID,
                    new ComponentName(c.getPackageName(), GuardianJobService.class.getName()));
            b.setPeriodic(15L * 60L * 1000L);
            b.setPersisted(true);
            js.schedule(b.build());
        } catch (Throwable ignored) {
        }
    }

    static void cancelJob(Context c) {
        try {
            JobScheduler js = (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (js != null) {
                js.cancel(JOB_ID);
            }
        } catch (Throwable ignored) {
        }
    }
}
