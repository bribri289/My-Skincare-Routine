package com.skincare.app.receivers;
import android.app.*;
import android.content.*;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.skincare.app.SkincareApp;
import com.skincare.app.data.*;
import com.skincare.app.ui.MainActivity;
import java.util.Calendar;

public class ReminderReceiver extends BroadcastReceiver {

    public static void scheduleAll(Context ctx){
        AppData db=AppData.get(ctx);
        AlarmManager am=(AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
        // Cancel all existing
        for(int d=0;d<7;d++){
            Models.DayRoutine dr=db.routine.get(d);
            if(dr==null) continue;
            for(int si=0;si<dr.sessions.size();si++){
                cancelReminder(ctx,am,d,si);
            }
        }
        // Schedule enabled ones
        for(int d=0;d<7;d++){
            Models.DayRoutine dr=db.routine.get(d);
            if(dr==null) continue;
            for(int si=0;si<dr.sessions.size();si++){
                Models.Session s=dr.sessions.get(si);
                if(s.reminderEnabled) scheduleReminder(ctx,am,d,si,s);
            }
        }
    }

    public static void scheduleReminder(Context ctx, AlarmManager am, int dow, int si, Models.Session s){
        if(!s.reminderEnabled) return;
        // Find next occurrence of this day of week at the given time
        Calendar cal=Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY,s.reminderHour);
        cal.set(Calendar.MINUTE,s.reminderMinute);
        cal.set(Calendar.SECOND,0);
        cal.set(Calendar.MILLISECOND,0);
        // Adjust to correct day of week
        int today=cal.get(Calendar.DAY_OF_WEEK)-1; // 0=Sun
        int daysUntil=(dow-today+7)%7;
        if(daysUntil==0&&cal.getTimeInMillis()<=System.currentTimeMillis()) daysUntil=7;
        cal.add(Calendar.DATE,daysUntil);

        Intent intent=new Intent(ctx,ReminderReceiver.class);
        intent.setAction("com.skincare.REMINDER");
        intent.putExtra("dow",dow);
        intent.putExtra("si",si);
        intent.putExtra("sessionName",s.name);
        int reqCode=dow*100+si;
        int flags=PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0);
        PendingIntent pi=PendingIntent.getBroadcast(ctx,reqCode,intent,flags);

        // Samsung Galaxy needs setExactAndAllowWhileIdle for reliable delivery
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),pi);
        }
    }

    public static void cancelReminder(Context ctx, AlarmManager am, int dow, int si){
        Intent intent=new Intent(ctx,ReminderReceiver.class);
        intent.setAction("com.skincare.REMINDER");
        int reqCode=dow*100+si;
        int flags=PendingIntent.FLAG_NO_CREATE|(Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0);
        PendingIntent pi=PendingIntent.getBroadcast(ctx,reqCode,intent,flags);
        if(pi!=null){am.cancel(pi);pi.cancel();}
    }

    @Override public void onReceive(Context ctx, Intent intent){
        String action=intent==null?null:intent.getAction();
        if("android.intent.action.BOOT_COMPLETED".equals(action)){
            // Reschedule all on boot (Samsung clears alarms on reboot)
            ReminderReceiver.scheduleAll(ctx);
            return;
        }
        // Fire notification
        int dow=intent.getIntExtra("dow",0);
        int si=intent.getIntExtra("si",0);
        String sessionName=intent.getStringExtra("sessionName");
        if(sessionName==null) sessionName="Skincare session";
        showNotification(ctx,sessionName,dow,si);
        // Reschedule for next week (exact alarms are one-shot)
        AppData db=AppData.get(ctx);
        Models.DayRoutine dr=db.routine.get(dow);
        if(dr!=null&&si<dr.sessions.size()){
            Models.Session s=dr.sessions.get(si);
            if(s.reminderEnabled){
                AlarmManager am=(AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
                // Schedule 7 days from now
                Calendar cal=Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY,s.reminderHour);
                cal.set(Calendar.MINUTE,s.reminderMinute);
                cal.set(Calendar.SECOND,0);
                cal.set(Calendar.MILLISECOND,0);
                cal.add(Calendar.DATE,7);
                Intent ni=new Intent(ctx,ReminderReceiver.class);
                ni.setAction("com.skincare.REMINDER");
                ni.putExtra("dow",dow);ni.putExtra("si",si);ni.putExtra("sessionName",s.name);
                int flags=PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0);
                PendingIntent pi=PendingIntent.getBroadcast(ctx,dow*100+si,ni,flags);
                if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M)
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),pi);
                else
                    am.setExact(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),pi);
            }
        }
    }

    void showNotification(Context ctx, String sessionName, int dow, int si){
        Intent open=new Intent(ctx,MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags=PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0);
        PendingIntent pi=PendingIntent.getActivity(ctx,dow*100+si+9000,open,flags);
        NotificationCompat.Builder nb=new NotificationCompat.Builder(ctx,SkincareApp.CH_REMINDER)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("✨ Skincare time!")
            .setContentText("Time for your "+sessionName)
            .setStyle(new NotificationCompat.BigTextStyle().bigText("Time for your "+sessionName+". Tap to open your routine."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setVibrate(new long[]{0,300,200,300})
            .setContentIntent(pi);
        NotificationManager nm=(NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(dow*100+si,nb.build());
    }
}
