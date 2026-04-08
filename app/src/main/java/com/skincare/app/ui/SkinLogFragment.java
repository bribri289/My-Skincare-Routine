package com.skincare.app.ui;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import com.skincare.app.R;
import com.skincare.app.data.*;
import java.util.Collections;

public class SkinLogFragment extends Fragment {
    @Override public View onCreateView(LayoutInflater inf,ViewGroup c,Bundle b){
        View v=inf.inflate(R.layout.fragment_skin_log,c,false);
        AppData db=AppData.get(requireContext());
        buildLog(v,db);
        v.findViewById(R.id.btn_add_skin_entry).setOnClickListener(x->showAddEntry(v,db));
        return v;
    }
    @Override public void onResume(){super.onResume();View v=getView();if(v!=null)buildLog(v,AppData.get(requireContext()));}

    void buildLog(View v,AppData db){
        LinearLayout list=v.findViewById(R.id.skin_list); list.removeAllViews();
        if(db.skinLogs.isEmpty()){
            TextView empty=new TextView(getContext());
            empty.setText("No skin entries yet. Track how your skin looks and feels each day.");
            empty.setTextColor(0xFF6B5F7A); empty.setTextSize(14); empty.setGravity(android.view.Gravity.CENTER);
            empty.setPadding(32,80,32,80); list.addView(empty); return;
        }
        Collections.sort(db.skinLogs,(a,b2)->b2.date.compareTo(a.date));
        LayoutInflater inf=LayoutInflater.from(getContext());
        for(Models.SkinEntry e:db.skinLogs){
            View item=inf.inflate(R.layout.item_skin_entry,list,false);
            ((TextView)item.findViewById(R.id.tv_date)).setText(e.date);             String[] stars={"","⭐","⭐⭐","⭐⭐⭐","⭐⭐⭐⭐","⭐⭐⭐⭐⭐"};
            ((TextView)item.findViewById(R.id.tv_rating)).setText(stars[Math.min(5,Math.max(1,e.overallRating))]);
            StringBuilder concerns=new StringBuilder();             if(e.acne)concerns.append("Acne  ");if(e.dryness)concerns.append("Dry  ");             if(e.oiliness)concerns.append("Oily  ");if(e.redness)concerns.append("Redness  ");             if(e.sensitivity)concerns.append("Sensitive");             ((TextView)item.findViewById(R.id.tv_concerns)).setText(concerns.length()>0?concerns.toString().trim():"No concerns");
            if(!e.notes.isEmpty()){((TextView)item.findViewById(R.id.tv_notes)).setText(e.notes);item.findViewById(R.id.tv_notes).setVisibility(View.VISIBLE);}
            else item.findViewById(R.id.tv_notes).setVisibility(View.GONE);
            list.addView(item);
        }
    }

    void showAddEntry(View v,AppData db){
        AlertDialog.Builder builder=new AlertDialog.Builder(requireContext());         builder.setTitle("How does your skin look today?");
        View dialog=LayoutInflater.from(getContext()).inflate(R.layout.dialog_skin_entry,null);
        builder.setView(dialog);         builder.setPositiveButton("Save",(d,w)->{
            Models.SkinEntry e=new Models.SkinEntry();
            e.id=db.newId(); e.date=db.todayDate();
            SeekBar rating=dialog.findViewById(R.id.seekbar_rating);
            e.overallRating=Math.max(1,rating.getProgress());
            e.acne=((CheckBox)dialog.findViewById(R.id.cb_acne)).isChecked();
            e.dryness=((CheckBox)dialog.findViewById(R.id.cb_dryness)).isChecked();
            e.oiliness=((CheckBox)dialog.findViewById(R.id.cb_oiliness)).isChecked();
            e.redness=((CheckBox)dialog.findViewById(R.id.cb_redness)).isChecked();
            e.sensitivity=((CheckBox)dialog.findViewById(R.id.cb_sensitivity)).isChecked();
            e.notes=((EditText)dialog.findViewById(R.id.et_notes)).getText().toString().trim();
            db.skinLogs.add(0,e); db.save();
            buildLog(v,db);
        });         builder.setNegativeButton("Cancel",null);
        builder.show();
    }
}
