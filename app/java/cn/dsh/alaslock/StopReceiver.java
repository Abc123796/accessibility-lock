package cn.dsh.alaslock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StopReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context c, Intent i) {
        Prefs.setEnabled(c, false);
        Prefs.cancelJob(c);
        c.stopService(new Intent(c, GuardianService.class));
    }
}
