package cn.dsh.alaslock;

import android.app.job.JobParameters;
import android.app.job.JobService;

public class GuardianJobService extends JobService {

    @Override
    public boolean onStartJob(JobParameters params) {
        if (Prefs.isEnabled(this) && !GuardianService.running) {
            Prefs.startGuardian(this);
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}
