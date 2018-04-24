package com.devil7.ftpalarm;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Switch;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    Switch btnService;

    EditText serverName;
    EditText port;
    EditText userName;
    EditText password;

    NumberPicker intervalHours;
    NumberPicker intervalMinutes;

    public static String APP_SERVER = "server";
    public static String APP_PORT = "port";
    public static String APP_USERNAME = "username";
    public static String APP_PASSWORD = "password";
    public static String APP_HOUR = "hour";
    public static String APP_MINUTE = "minutes";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setIcon(R.drawable.ic_logo);
            actionBar.setTitle(("  " + getString(R.string.app_name)));
        }

        AssignObjects();
        PrepareObjects();
        LoadSettings();
        AssignEvents();

        ProcessExtras();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);//must store the new intent unless getIntent() will return the old one
        ProcessExtras();
    }

    void ProcessExtras(){
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
        } else if (extras.getBoolean("NotificationClick")) {
            Intent stopIntent = new Intent(getApplicationContext(), RingtoneService.class);
            getApplicationContext().stopService(stopIntent);
            JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
            jobScheduler.cancelAll();
            btnService.setChecked(false);
            EnableControls();
        }
    }

    void AssignObjects(){
        intervalHours = findViewById(R.id.txt_Hour);
        intervalMinutes = findViewById(R.id.txt_Minutes);
        serverName = findViewById(R.id.txt_Server);
        port = findViewById(R.id.txt_Port);
        userName = findViewById(R.id.txt_Username);
        password = findViewById(R.id.txt_Password);
        btnService = findViewById(R.id.switch_Service);
    }

    void PrepareObjects(){
        intervalHours.setMinValue(0);
        intervalHours.setMaxValue(10);
        intervalMinutes.setMinValue(0);
        intervalMinutes.setMaxValue(59);

        JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler.getAllPendingJobs().toArray().length > 0){
            btnService.setChecked(true);
            DisableControls();
        }
    }

    void AssignEvents(){
        btnService.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                EnableDisableService(buttonView, isChecked);
            }
        });
    }

    void DisableControls(){
        serverName.setEnabled(false);
        port.setEnabled(false);
        userName.setEnabled(false);
        password.setEnabled(false);
        intervalHours.setEnabled(false);
        intervalMinutes.setEnabled(false);
    }

    void EnableControls(){
        serverName.setEnabled(true);
        port.setEnabled(true);
        userName.setEnabled(true);
        password.setEnabled(true);
        intervalHours.setEnabled(true);
        intervalMinutes.setEnabled(true);
    }

    void SaveSettings(){
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = preference.edit();
        editor.putString(APP_SERVER, serverName.getText().toString());
        editor.putString(APP_PORT, port.getText().toString());
        editor.putString(APP_USERNAME, userName.getText().toString());
        editor.putString(APP_PASSWORD, password.getText().toString());
        editor.putInt(APP_HOUR, intervalHours.getValue());
        editor.putInt(APP_MINUTE, intervalMinutes.getValue());
        editor.apply();
    }

    void LoadSettings(){
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        serverName.setText(preference.getString(APP_SERVER, "127.0.0.1"));
        port.setText(preference.getString(APP_PORT, "21"));
        userName.setText(preference.getString(APP_USERNAME,""));
        password.setText(preference.getString(APP_PASSWORD,""));
        intervalHours.setValue(preference.getInt(APP_HOUR,0));
        intervalMinutes.setValue(preference.getInt(APP_MINUTE,0));
    }

    boolean CheckSettings(){
        int hours = intervalHours.getValue();
        int minutes = intervalMinutes.getValue();
        int totalMinutes = (hours * 60) + minutes;

        if (totalMinutes < 15){
            return false;
        }

        return true;
    }

    void EnableDisableService(CompoundButton button,boolean isChecked){
        final JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        final Context context = this;
        if(isChecked){

            if(CheckSettings()) {
                SaveSettings();
                DisableControls();
                CheckFTPTask checkFTP = CheckFTPTask.getInstance(false);
                checkFTP.setOnResultListner(new CheckFTPTask.ResultListener() {
                    @Override
                    public void Result(int count) {
                        if (count == 0) {
                            btnService.setChecked(false);
                            Toast.makeText(getApplicationContext(), R.string.toast_service_not_enabled, Toast.LENGTH_LONG).show();
                        } else {
                            int hours = intervalHours.getValue();
                            int minutes = intervalMinutes.getValue();
                            int totalMinutes = (hours * 60) + minutes;

                            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                            pref.edit().putInt("previousCount", count).apply();
                            jobScheduler.schedule(new JobInfo.Builder(0, new ComponentName(context, AlarmService.class))
                                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                                    .setPersisted(true)
                                    //.setMinimumLatency(15000)
                                    .setPeriodic(totalMinutes * (60 * 1000))
                                    .build());
                            Toast.makeText(getApplicationContext(), R.string.toast_service_enabled, Toast.LENGTH_LONG).show();
                        }
                    }
                });
                checkFTP.execute(this);
            }else{
                btnService.setChecked(false);
                Toast.makeText(getApplicationContext(), R.string.toast_service_not_enabled, Toast.LENGTH_LONG).show();
            }
        }else{
            EnableControls();
            jobScheduler.cancelAll();
            Toast.makeText(getApplicationContext(), R.string.toast_service_disabled, Toast.LENGTH_LONG).show();
        }
    }
}
