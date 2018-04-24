package com.devil7.ftpalarm;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class AlarmService extends JobService {

    PowerManager.WakeLock fullWakeLock;

    @Override
    public boolean onStartJob(final JobParameters params) {
        CheckFTPTask checkFTP = CheckFTPTask.getInstance(false);
        checkFTP.setOnResultListner(new CheckFTPTask.ResultListener() {
            @Override
            public void Result(int count) {
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                int prevCount = pref.getInt("previousCount",0);
                Log.i("Alarm Service","Previous Count : " + prevCount + " Current Count : " + count);
                if (prevCount != count){

                    PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
                    fullWakeLock = powerManager.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "FTP Alarm - Wake Lock");
                    fullWakeLock.acquire(60000);

                    KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                    KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("TAG");
                    keyguardLock.disableKeyguard();

                    Intent startIntent = new Intent(getApplicationContext(), RingtoneService.class);

                    //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    //    getApplicationContext().startForegroundService(startIntent);
                    //} else {
                        getApplicationContext().startService(startIntent);
                    //}

                    JobScheduler jobScheduler =
                            (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
                    jobScheduler.cancelAll();

                } else {
                    pref.edit().putInt("previousCount",count).apply();
                }
            }
        });
        checkFTP.execute(this);
        return true;
    }

    @Override
    public boolean onStopJob(final JobParameters params) {
        return true;
    }

}
