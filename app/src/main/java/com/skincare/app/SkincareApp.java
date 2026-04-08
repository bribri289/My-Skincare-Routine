package com.skincare.app;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class SkincareApp extends Application {
    public static final String CH_REMINDER="skincare_reminder";

    @Override public void onCreate(){
        super.onCreate();
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            NotificationManager nm=getSystemService(NotificationManager.class);
            NotificationChannel ch=new NotificationChannel(CH_REMINDER,"Skincare Reminders",NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Daily skincare session reminders");
            ch.enableVibration(true);
            nm.createNotificationChannel(ch);
        }
    }
}
