package com.skincare.app.ui;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import com.skincare.app.R;
import com.skincare.app.data.*;
import java.util.Calendar;

public class TodayFragment extends Fragment {
    @Override public View onCreateView(LayoutInflater inf,ViewGroup c,Bundle b){
        View v=inf.inflate(R.layout.fragment_today,c,false);
        AppData db=AppData.get(requireContext());
        // Greeting
        int hr=Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String g=hr<12?"Good morning ☀️":hr<17?"Good afternoon ✨":"Good evening 🌙";
        ((TextView)v.findViewById(R.id.tv_greeting)).setText(g);         String[] months={"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};         String[] days={"Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"};
        Calendar cal=Calendar.getInstance();         ((TextView)v.findViewById(R.id.tv_date)).setText(days[cal.get(Calendar.DAY_OF_WEEK)-1]+", "+months[cal.get(Calendar.MONTH)]+" "+cal.get(Calendar.DATE));
        // Stats
        int done=0;
        for(Models.SkincareSession s:db.sessions) if(db.completedToday(s.id)) done++;         ((TextView)v.findViewById(R.id.tv_sessions_done)).setText(done+"/"+db.sessions.size());
        int maxStreak=0; for(Models.SkincareSession s:db.sessions) maxStreak=Math.max(maxStreak,s.currentStreak);         ((TextView)v.findViewById(R.id.tv_streak)).setText(maxStreak+" days");
        // Today sessions
        buildTodaySessions(v,db);
        return v;
    }

    void buildTodaySessions(View v,AppData db){
        LinearLayout list=v.findViewById(R.id.today_sessions); list.removeAllViews();
        LayoutInflater inf=LayoutInflater.from(getContext());
        if(db.sessions.isEmpty()){
            TextView empty=new TextView(getContext());             empty.setText("No routines yet! Go to Routines tab to create your first one.");
            empty.setTextColor(0xFF6B5F7A); empty.setTextSize(14); empty.setGravity(android.view.Gravity.CENTER);
            empty.setPadding(32,40,32,40); list.addView(empty); return;
        }
        for(Models.SkincareSession s:db.sessions){
            View item=inf.inflate(R.layout.item_session_today,list,false);
            ((TextView)item.findViewById(R.id.tv_emoji)).setText(s.emoji);
            ((TextView)item.findViewById(R.id.tv_name)).setText(s.name);
            ((TextView)item.findViewById(R.id.tv_type)).setText(s.type);
            boolean done=db.completedToday(s.id);             ((TextView)item.findViewById(R.id.tv_streak)).setText("🔥 "+s.currentStreak+" days");
            View checkIcon=item.findViewById(R.id.check_icon);
            checkIcon.setBackgroundResource(done?R.drawable.circle_pink:R.drawable.circle_bg);
            checkIcon.setVisibility(done?View.VISIBLE:View.INVISIBLE);
            if(s.reminderEnabled){
                TextView tvTime=item.findViewById(R.id.tv_reminder_time);                 String ap=s.reminderHour<12?"AM":"PM"; int rh=s.reminderHour%12; if(rh==0)rh=12;                 tvTime.setText("⏰ "+String.format("%d:%02d %s",rh,s.reminderMinute,ap));
                tvTime.setVisibility(View.VISIBLE);
            }
            Button btn=item.findViewById(R.id.btn_start);             btn.setText(done?"✓ Done":"Start");
            btn.setBackgroundResource(done?R.drawable.btn_secondary_bg:R.drawable.btn_primary_bg);
            if(!done) btn.setOnClickListener(x->{
                Intent i=new Intent(getActivity(),LogSessionActivity.class);                 i.putExtra("session",s); startActivity(i);
            });
            list.addView(item);
        }
    }
}
