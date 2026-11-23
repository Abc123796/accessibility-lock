package cn.dsh.alaslock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context c, Intent i) {
        if (!Prefs.isEnabled(c) || !Prefs.bootStart(c)) {
            return;
        }
        Prefs.ensureJob(c);
        Prefs.startGuardian(c);
    }
}
