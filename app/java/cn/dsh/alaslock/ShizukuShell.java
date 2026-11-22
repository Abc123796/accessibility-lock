package cn.dsh.alaslock;

import android.content.pm.PackageManager;
import android.os.IBinder;
import android.util.Log;

import moe.shizuku.server.IRemoteProcess;
import moe.shizuku.server.IShizukuService;
import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuBinderWrapper;

/**
 * 用 Shizuku 以 shell 身份执行命令，从而免电脑给本应用授权。
 * 只做一件事：pm grant ... WRITE_SECURE_SETTINGS
 */
final class ShizukuShell {

    static final int REQ = 0x5A1;
    private static final String TAG = "ALock";

    private ShizukuShell() {
    }

    static boolean available() {
        try {
            return Shizuku.pingBinder();
        } catch (Throwable t) {
            return false;
        }
    }

    static boolean granted() {
        try {
            return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            return false;
        }
    }

    static void request() {
        try {
            Shizuku.requestPermission(REQ);
        } catch (Throwable t) {
            Log.w(TAG, "shizuku request failed", t);
        }
    }

    static boolean exec(String... cmd) {
        try {
            IBinder b = Shizuku.getBinder();
            if (b == null) {
                return false;
            }
            IShizukuService svc = IShizukuService.Stub.asInterface(new ShizukuBinderWrapper(b));
            IRemoteProcess proc = svc.newProcess(cmd, null, null);
            if (proc == null) {
                return false;
            }
            int code = proc.waitFor();
            Log.i(TAG, "shizuku exec exit=" + code);
            return code == 0;
        } catch (Throwable t) {
            Log.w(TAG, "shizuku exec failed", t);
            return false;
        }
    }

    static boolean grantSelf() {
        return exec("pm", "grant", "cn.dsh.alaslock", "android.permission.WRITE_SECURE_SETTINGS");
    }
}
