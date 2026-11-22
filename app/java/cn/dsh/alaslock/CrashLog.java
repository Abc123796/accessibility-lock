package cn.dsh.alaslock;

import android.content.Context;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;

/** 把未捕获异常写到外部私有目录，方便事后排查（不上传）。 */
final class CrashLog {

    private CrashLog() {
    }

    static void install(final Context c) {
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            try {
                File dir = c.getExternalFilesDir(null);
                if (dir != null) {
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    FileWriter w = new FileWriter(new File(dir, "crash.txt"), false);
                    w.write("thread: " + t.getName() + "\n");
                    w.write("shizuku-ping: " + safe() + "\n");
                    w.write(sw.toString());
                    w.close();
                }
            } catch (Throwable ignored) {
            }
            if (prev != null) {
                prev.uncaughtException(t, e);
            }
        });
    }

    private static String safe() {
        try {
            return String.valueOf(ShizukuShell.available());
        } catch (Throwable t) {
            return "err:" + t.getClass().getSimpleName();
        }
    }
}
