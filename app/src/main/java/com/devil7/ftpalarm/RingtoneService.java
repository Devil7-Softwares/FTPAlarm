package com.devil7.ftpalarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class RingtoneService extends Service {
    private Ringtone ringtone;

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        String channelId = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = createNotificationChannel();
        }

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.putExtra("NotificationClick",true);
        PendingIntent pIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getApplicationContext(), channelId);


        mBuilder.setContentIntent(pIntent);
        mBuilder.setSmallIcon(R.drawable.ic_launcher);
        mBuilder.setContentTitle("FTP Alarm");
        mBuilder.setContentText("New file/directory added to FTP Server. Click here to turn off the alarm.");
        mBuilder.setPriority(Notification.PRIORITY_MAX);

        startForeground(1,mBuilder.build());
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(){
        String channelId = "my_service";
        String channelName = "My Background Service";
        NotificationChannel chan = new NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setDescription("Alarm Notification");
        chan.setImportance(NotificationManager.IMPORTANCE_HIGH);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(chan);
        return channelId;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alert == null){
            alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (alert == null){
                alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
        }

        ringtone = RingtoneManager.getRingtone(getApplicationContext(), alert);
        ringtone.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int volume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        if(volume == 0){
            volume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        }
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volume,AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);

        if(ringtone != null){
            ringtone.play();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        ringtone.stop();
    }
}

