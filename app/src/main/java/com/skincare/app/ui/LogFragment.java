package com.skincare.app.ui;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import com.skincare.app.R;
import com.skincare.app.data.*;
import java.util.Collections;

public class LogFragment extends Fragment {
    @Override public View onCreateView(LayoutInflater inf,ViewGroup c,Bundle b){
        View v=inf.inflate(R.layout.fragment_log,c,false);
        buildLog(v,AppData.get(requireContext()));
        return v;
    }
    @Override public void onResume(){super.onResume();View v=getView();if(v!=null)buildLog(v,AppData.get(requireContext()));}

    void buildLog(View v,AppData db){
        LinearLayout list=v.findViewById(R.id.log_list); list.removeAllViews();
        if(db.logs.isEmpty()){
            TextView empty=new TextView(getContext());
            empty.setText("No sessions logged yet. Complete a routine to see your history here.");
            empty.setTextColor(0xFF6B5F7A); empty.setTextSize(14); empty.setGravity(android.view.Gravity.CENTER);
            empty.setPadding(32,80,32,80); list.addView(empty); return;
        }
        Collections.sort(db.logs,(a,b2)->b2.date.compareTo(a.date));         String lastDate="";
        LayoutInflater inf=LayoutInflater.from(getContext());
        for(Models.LogEntry l:db.logs){
            if(!l.date.equals(lastDate)){
                TextView dateHeader=new TextView(getContext());
                dateHeader.setText(l.date); dateHeader.setTextColor(0xFFF472B6);
                dateHeader.setTextSize(12); dateHeader.setTypeface(null,android.graphics.Typeface.BOLD);
                dateHeader.setPadding(0,16,0,8); list.addView(dateHeader);
                lastDate=l.date;
            }
            View item=inf.inflate(R.layout.item_log,list,false);
            ((TextView)item.findViewById(R.id.tv_session_name)).setText(l.sessionName);
            ((TextView)item.findViewById(R.id.tv_time)).setText(l.time);             ((TextView)item.findViewById(R.id.tv_condition)).setText(l.skinCondition.isEmpty()?"":l.skinCondition);
            if(!l.notes.isEmpty()){((TextView)item.findViewById(R.id.tv_notes)).setText(l.notes);item.findViewById(R.id.tv_notes).setVisibility(View.VISIBLE);}
            else item.findViewById(R.id.tv_notes).setVisibility(View.GONE);
            list.addView(item);
        }
    }
}
