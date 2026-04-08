package com.skincare.app.ui;
import android.app.AlarmManager;
import android.content.Context;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.skincare.app.R;
import com.skincare.app.data.*;
import com.skincare.app.receivers.ReminderReceiver;

public class EditSessionActivity extends AppCompatActivity {
    AppData db; int dow, si; boolean isNew;
    Models.Session session; Models.DayRoutine dayRoutine;
    LinearLayout stepsContainer;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        setContentView(R.layout.activity_edit_session);
        db=AppData.get(this);
        dow=getIntent().getIntExtra("dow",0);
        si=getIntent().getIntExtra("si",-1);
        isNew=si<0;
        dayRoutine=db.routine.get(dow);
        if(dayRoutine==null){finish();return;}
        if(isNew){session=new Models.Session();session.id="s_"+System.currentTimeMillis();}
        else{if(si>=dayRoutine.sessions.size()){finish();return;}session=dayRoutine.sessions.get(si);}
        setup();
    }

    void setup(){
        ((TextView)findViewById(R.id.tv_title)).setText(isNew?"New Session":"Edit Session");
        findViewById(R.id.btn_back).setOnClickListener(v->finish());

        EditText etName=findViewById(R.id.et_name); etName.setText(session.name);

        // Reminder toggle
        Switch swRem=findViewById(R.id.sw_reminder); swRem.setChecked(session.reminderEnabled);
        LinearLayout remTimeRow=findViewById(R.id.rem_time_row);
        remTimeRow.setVisibility(session.reminderEnabled?View.VISIBLE:View.GONE);
        swRem.setOnCheckedChangeListener((btn,chk)->{
            session.reminderEnabled=chk;
            remTimeRow.setVisibility(chk?View.VISIBLE:View.GONE);
        });

        // Time picker
        TimePicker tp=findViewById(R.id.time_picker);
        tp.setIs24HourView(false); tp.setHour(session.reminderHour); tp.setMinute(session.reminderMinute);

        // Day chips for reminder
        LinearLayout daysRow=findViewById(R.id.reminder_days_row);
        String[] DN={"S","M","T","W","T","F","S"};
        for(int i=0;i<7;i++){
            final int idx=i;
            TextView chip=new TextView(this); chip.setText(DN[i]);
            chip.setPadding(20,10,20,10); chip.setTextColor(0xFF1A1A1A); chip.setTextSize(12);
            chip.setBackground(getDrawable(session.reminderDays[i]?R.drawable.chip_green:R.drawable.chip_outline));
            chip.setOnClickListener(v->{
                session.reminderDays[idx]=!session.reminderDays[idx];
                chip.setBackground(getDrawable(session.reminderDays[idx]?R.drawable.chip_green:R.drawable.chip_outline));
            });
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(4,0,4,0); chip.setLayoutParams(lp); daysRow.addView(chip);
        }

        stepsContainer=findViewById(R.id.steps_container);
        rebuildSteps();

        findViewById(R.id.btn_add_step).setOnClickListener(v->{
            Models.Step s=new Models.Step(); s.name="New step";
            session.steps.add(s); rebuildSteps();
        });

        // Save
        findViewById(R.id.btn_save).setOnClickListener(v->{
            String name=etName.getText().toString().trim();
            if(name.isEmpty()){etName.setError("Required");return;}
            session.name=name;
            session.reminderEnabled=swRem.isChecked();
            session.reminderHour=tp.getHour();
            session.reminderMinute=tp.getMinute();
            if(isNew) dayRoutine.sessions.add(session);
            else dayRoutine.sessions.set(si,session);
            db.save();
            // Reschedule reminders
            ReminderReceiver.scheduleAll(this);
            Toast.makeText(this,"Session saved!",Toast.LENGTH_SHORT).show();
            finish();
        });

        View btnDel=findViewById(R.id.btn_delete); btnDel.setVisibility(isNew?View.GONE:View.VISIBLE);
        btnDel.setOnClickListener(v->{
            new android.app.AlertDialog.Builder(this).setTitle("Delete session?")
                .setPositiveButton("Delete",(d,w)->{
                    dayRoutine.sessions.remove(si); db.save();
                    ReminderReceiver.scheduleAll(this); finish();
                }).setNegativeButton("Cancel",null).show();
        });
    }

    void rebuildSteps(){
        stepsContainer.removeAllViews();
        for(int i=0;i<session.steps.size();i++){
            Models.Step step=session.steps.get(i);
            final int idx=i;
            LinearLayout row=new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setBackground(getDrawable(R.drawable.card_white));
            row.setPadding(12,10,12,10);
            LinearLayout.LayoutParams rlp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
            rlp.setMargins(0,0,0,6); row.setLayoutParams(rlp);

            EditText etStep=new EditText(this); etStep.setText(step.name);
            etStep.setTextSize(13); etStep.setBackground(null); etStep.setHint("Step name");
            etStep.setLayoutParams(new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));
            etStep.setOnFocusChangeListener((v,f)->{if(!f)step.name=etStep.getText().toString();});

            // Product spinner
            Spinner ps=new Spinner(this);
            java.util.List<String> pLabels=new java.util.ArrayList<>();
            java.util.List<String> pIds=new java.util.ArrayList<>();
            pLabels.add("No product"); pIds.add("");
            for(Models.Product p:db.products){pLabels.add(p.name);pIds.add(p.id);}
            ArrayAdapter<String> pa=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,pLabels);
            pa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); ps.setAdapter(pa);
            for(int j=0;j<pIds.size();j++) if(pIds.get(j).equals(step.productId)){ps.setSelection(j);break;}
            ps.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
                public void onItemSelected(AdapterView<?> p,View v,int pos,long id){step.productId=pIds.get(pos);}
                public void onNothingSelected(AdapterView<?> p){}
            });
            LinearLayout.LayoutParams pslp=new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1);
            pslp.setMargins(8,0,0,0); ps.setLayoutParams(pslp);

            Button del=new Button(this); del.setText("✕");
            del.setBackground(getDrawable(android.R.color.transparent)); del.setTextColor(0xFFAFAFA0);
            del.setTextSize(14); del.setPadding(8,0,0,0);
            del.setOnClickListener(v->{session.steps.remove(idx);rebuildSteps();});

            row.addView(etStep); row.addView(ps); row.addView(del);
            stepsContainer.addView(row);
        }
    }
}
