package com.skincare.app.ui;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import com.skincare.app.R;
import com.skincare.app.data.*;

public class SessionsFragment extends Fragment {
    @Override public View onCreateView(LayoutInflater inf,ViewGroup c,Bundle b){
        View v=inf.inflate(R.layout.fragment_sessions,c,false);
        buildList(v,AppData.get(requireContext()));
        v.findViewById(R.id.btn_new_session).setOnClickListener(x->startActivity(new Intent(getActivity(),EditSessionActivity.class)));
        return v;
    }
    @Override public void onResume(){super.onResume();View v=getView();if(v!=null)buildList(v,AppData.get(requireContext()));}

    void buildList(View v,AppData db){
        LinearLayout list=v.findViewById(R.id.sessions_list); list.removeAllViews();
        LayoutInflater inf=LayoutInflater.from(getContext());
        if(db.sessions.isEmpty()){
            TextView empty=new TextView(getContext());
            empty.setText("No routines yet. Tap + to create your first skincare routine.");
            empty.setTextColor(0xFF6B5F7A); empty.setTextSize(14); empty.setGravity(android.view.Gravity.CENTER);
            empty.setPadding(32,60,32,60); list.addView(empty); return;
        }
        for(Models.SkincareSession s:db.sessions){
            View item=inf.inflate(R.layout.item_session,list,false);
            ((TextView)item.findViewById(R.id.tv_emoji)).setText(s.emoji);
            ((TextView)item.findViewById(R.id.tv_name)).setText(s.name);             ((TextView)item.findViewById(R.id.tv_type)).setText(s.type+" · "+s.steps.size()+" steps");
            if(s.reminderEnabled){                 String ap=s.reminderHour<12?"AM":"PM"; int rh=s.reminderHour%12; if(rh==0)rh=12;                 ((TextView)item.findViewById(R.id.tv_reminder)).setText("⏰ "+String.format("%d:%02d %s",rh,s.reminderMinute,ap));
                item.findViewById(R.id.tv_reminder).setVisibility(View.VISIBLE);
            } else item.findViewById(R.id.tv_reminder).setVisibility(View.GONE);             ((TextView)item.findViewById(R.id.tv_streak)).setText("🔥 "+s.currentStreak+" day streak");             item.setOnClickListener(x->{Intent i=new Intent(getActivity(),EditSessionActivity.class);i.putExtra("session",s);startActivity(i);});             item.findViewById(R.id.btn_start).setOnClickListener(x->{Intent i=new Intent(getActivity(),LogSessionActivity.class);i.putExtra("session",s);startActivity(i);});
            list.addView(item);
        }
    }
}
