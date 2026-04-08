package com.skincare.app.ui;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.skincare.app.R;
import com.skincare.app.data.*;
import com.skincare.app.receivers.ReminderReceiver;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    AppData db;
    int curTab=0; // 0=tracker,1=routine,2=products

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        db=AppData.get(this);
        // Request notification permission (Android 13+)
        if(Build.VERSION.SDK_INT>=33){
            ActivityCompat.requestPermissions(this,new String[]{"android.permission.POST_NOTIFICATIONS"},1);
        }
        setupTabs();
        showTab(0);
        ReminderReceiver.scheduleAll(this);
    }

    @Override protected void onResume(){
        super.onResume();
        refreshCurrentTab();
    }

    void setupTabs(){
        int[] tabIds={R.id.tab_tracker,R.id.tab_routine,R.id.tab_products};
        for(int i=0;i<tabIds.length;i++){
            final int idx=i;
            findViewById(tabIds[i]).setOnClickListener(v->showTab(idx));
        }
    }

    void showTab(int idx){
        curTab=idx;
        int[] tabIds={R.id.tab_tracker,R.id.tab_routine,R.id.tab_products};
        int[] viewIds={R.id.view_tracker,R.id.view_routine,R.id.view_products};
        for(int i=0;i<3;i++){
            boolean active=i==idx;
            View tab=findViewById(tabIds[i]);
            tab.setBackgroundResource(active?R.drawable.tab_active_bg:android.R.color.transparent);
            ((TextView)tab).setTextColor(active?0xFF1A1A1A:0xFF9B9B8F);
            findViewById(viewIds[i]).setVisibility(active?View.VISIBLE:View.GONE);
        }
        refreshCurrentTab();
    }

    void refreshCurrentTab(){
        if(curTab==0) buildTracker();
        else if(curTab==1) buildRoutineEditor();
        else buildProducts();
    }

    // ── TRACKER ───────────────────────────────────────────────────────────────
    void buildTracker(){
        Calendar today=Calendar.getInstance();
        String todayKey=AppData.dateKey(today);
        int dow=today.get(Calendar.DAY_OF_WEEK)-1;

        // Stats
        int[] prog=db.getProgress(todayKey,dow);
        int streak=db.calcStreak();
        ((TextView)findViewById(R.id.stat_today)).setText(prog[1]>0?(prog[0]*100/prog[1])+"%":"0%");
        ((TextView)findViewById(R.id.stat_steps)).setText(String.valueOf(prog[0]));
        ((TextView)findViewById(R.id.streak_num)).setText(String.valueOf(streak));

        // Week grid
        buildWeekGrid(today, todayKey);

        // Day card
        buildDayCard(todayKey, dow, today);
    }

    void buildWeekGrid(Calendar today, String todayKey){
        LinearLayout grid=findViewById(R.id.week_grid);
        grid.removeAllViews();
        String[] DS={"S","M","T","W","T","F","S"};
        Calendar weekStart=(Calendar)today.clone();
        weekStart.set(Calendar.DAY_OF_WEEK,Calendar.SUNDAY);
        for(int i=0;i<7;i++){
            Calendar day=(Calendar)weekStart.clone();
            day.add(Calendar.DATE,i);
            String key=AppData.dateKey(day);
            int d=day.get(Calendar.DAY_OF_WEEK)-1;
            int[] p=db.getProgress(key,d);
            boolean isToday=key.equals(todayKey);
            boolean full=p[1]>0&&p[0]==p[1];
            boolean partial=p[0]>0&&!full;

            LinearLayout col=new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(android.view.Gravity.CENTER);
            col.setPadding(4,10,4,8);
            col.setBackground(getDrawable(isToday?R.drawable.week_day_today:R.drawable.week_day_bg));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1);
            lp.setMargins(3,0,3,0); col.setLayoutParams(lp);

            TextView lbl=new TextView(this); lbl.setText(DS[i]); lbl.setTextSize(10);
            lbl.setGravity(android.view.Gravity.CENTER); lbl.setTextColor(0xFF9B9B8F); col.addView(lbl);

            TextView dt=new TextView(this); dt.setText(String.valueOf(day.get(Calendar.DAY_OF_MONTH)));
            dt.setTextSize(13); dt.setGravity(android.view.Gravity.CENTER);
            dt.setTextColor(isToday?0xFF1D9E75:0xFF1A1A1A); col.addView(dt);

            View dot=new View(this);
            LinearLayout.LayoutParams dlp=new LinearLayout.LayoutParams(8,8); dlp.gravity=android.view.Gravity.CENTER; dlp.topMargin=4;
            dot.setLayoutParams(dlp);
            dot.setBackgroundResource(full?R.drawable.dot_green:partial?R.drawable.dot_orange:R.drawable.dot_empty);
            col.addView(dot);
            grid.addView(col);
        }
    }

    void buildDayCard(String dateKey, int dow, Calendar today){
        LinearLayout card=findViewById(R.id.day_card);
        card.removeAllViews();
        Models.DayRoutine dr=db.routine.get(dow);
        if(dr==null) return;
        Models.CheckData cd=db.getChecks(dateKey);

        for(int si=0;si<dr.sessions.size();si++){
            Models.Session sess=dr.sessions.get(si);
            if(sess.steps.isEmpty()) continue;
            final int fsi=si;

            // Session header
            LinearLayout sessHeader=new LinearLayout(this);
            sessHeader.setOrientation(LinearLayout.HORIZONTAL);
            sessHeader.setGravity(android.view.Gravity.CENTER_VERTICAL);
            sessHeader.setPadding(0,16,0,8);

            TextView sessLbl=new TextView(this); sessLbl.setText(sess.name.toUpperCase());
            sessLbl.setTextSize(11); sessLbl.setTextColor(0xFF9B9B8F);
            LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1);
            sessLbl.setLayoutParams(slp); sessHeader.addView(sessLbl);

            if(sess.reminderEnabled){
                String ap=sess.reminderHour<12?"AM":"PM";
                int h=sess.reminderHour%12; if(h==0)h=12;
                TextView time=new TextView(this);
                time.setText(String.format("🔔 %d:%02d %s",h,sess.reminderMinute,ap));
                time.setTextSize(11); time.setTextColor(0xFF1D9E75); sessHeader.addView(time);
            }
            card.addView(sessHeader);

            // Steps
            for(int i=0;i<sess.steps.size();i++){
                Models.Step step=sess.steps.get(i);
                final String ck=si+"_"+i;
                boolean checked=Boolean.TRUE.equals(cd.checks.get(ck));
                Models.Product prod=db.findProduct(step.productId);

                LinearLayout row=new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                row.setPadding(8,7,8,7);
                row.setBackground(getDrawable(R.drawable.step_row_bg));
                LinearLayout.LayoutParams rlp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
                rlp.setMargins(0,0,0,4); row.setLayoutParams(rlp);

                View chk=new View(this);
                LinearLayout.LayoutParams clp=new LinearLayout.LayoutParams(20,20); clp.setMargins(0,0,12,0);
                chk.setLayoutParams(clp);
                chk.setBackgroundResource(checked?R.drawable.check_on:R.drawable.check_off);

                LinearLayout info=new LinearLayout(this);
                info.setOrientation(LinearLayout.VERTICAL);
                info.setLayoutParams(new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));

                TextView nm=new TextView(this); nm.setText(step.name); nm.setTextSize(13);
                nm.setTextColor(checked?0xFFAFAFA0:0xFF1A1A1A);
                if(checked) nm.setPaintFlags(nm.getPaintFlags()|android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                info.addView(nm);

                if(prod!=null&&!prod.brand.isEmpty()){
                    TextView pb=new TextView(this); pb.setText(prod.brand);
                    pb.setTextSize(11); pb.setTextColor(0xFF9B9B8F); info.addView(pb);
                }

                row.addView(chk); row.addView(info);
                final int fi=i;
                row.setOnClickListener(v->{
                    cd.checks.put(ck,!Boolean.TRUE.equals(cd.checks.get(ck)));
                    db.save(); buildDayCard(dateKey,dow,today);
                });
                card.addView(row);
            }
        }

        // Notes
        TextView notesLbl=new TextView(this); notesLbl.setText("SKIN NOTES");
        notesLbl.setTextSize(11); notesLbl.setTextColor(0xFF9B9B8F);
        LinearLayout.LayoutParams nlp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        nlp.setMargins(0,16,0,6); notesLbl.setLayoutParams(nlp);
        card.addView(notesLbl);

        EditText notes=new EditText(this);
        notes.setHint("How does your skin feel? Any reactions?");
        notes.setText(cd.notes); notes.setTextSize(13);
        notes.setBackground(getDrawable(R.drawable.input_bg));
        notes.setPadding(12,10,12,10); notes.setMinLines(2);
        notes.setOnFocusChangeListener((v,f)->{if(!f){cd.notes=notes.getText().toString();db.save();}});
        card.addView(notes);
    }

    // ── ROUTINE EDITOR ────────────────────────────────────────────────────────
    void buildRoutineEditor(){
        LinearLayout ed=findViewById(R.id.routine_editor);
        ed.removeAllViews();
        String[] dayNames={"Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"};
        for(int d=0;d<7;d++){
            Models.DayRoutine dr=db.routine.get(d);
            if(dr==null) continue;
            final int fd=d;

            // Day section header
            LinearLayout dayHdr=new LinearLayout(this);
            dayHdr.setOrientation(LinearLayout.HORIZONTAL);
            dayHdr.setGravity(android.view.Gravity.CENTER_VERTICAL);
            dayHdr.setPadding(0,16,0,8);

            TextView dayLbl=new TextView(this); dayLbl.setText(dayNames[d]);
            dayLbl.setTextSize(15); dayLbl.setTextColor(0xFF1A1A1A);
            dayLbl.setTypeface(null,android.graphics.Typeface.BOLD);
            dayLbl.setLayoutParams(new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));
            dayHdr.addView(dayLbl);

            Button addSess=new Button(this); addSess.setText("+ Session");
            addSess.setBackground(getDrawable(R.drawable.btn_outline)); addSess.setTextSize(12);
            addSess.setTextColor(0xFF6B6A66); addSess.setPadding(16,6,16,6);
            addSess.setOnClickListener(v->openEditSession(fd,-1));
            dayHdr.addView(addSess);
            ed.addView(dayHdr);

            for(int si=0;si<dr.sessions.size();si++){
                Models.Session sess=dr.sessions.get(si);
                final int fsi=si;

                LinearLayout sessCard=new LinearLayout(this);
                sessCard.setOrientation(LinearLayout.VERTICAL);
                sessCard.setBackground(getDrawable(R.drawable.card_white));
                sessCard.setPadding(14,12,14,12);
                LinearLayout.LayoutParams sclp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
                sclp.setMargins(0,0,0,8); sessCard.setLayoutParams(sclp);

                LinearLayout sessHdr=new LinearLayout(this);
                sessHdr.setOrientation(LinearLayout.HORIZONTAL);
                sessHdr.setGravity(android.view.Gravity.CENTER_VERTICAL);

                TextView sName=new TextView(this); sName.setText(sess.name);
                sName.setTextSize(13); sName.setTextColor(0xFF1A1A1A);
                sName.setTypeface(null,android.graphics.Typeface.BOLD);
                sName.setLayoutParams(new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));
                sessHdr.addView(sName);

                // Reminder badge
                if(sess.reminderEnabled){
                    String ap=sess.reminderHour<12?"AM":"PM";
                    int h=sess.reminderHour%12; if(h==0)h=12;
                    TextView rb=new TextView(this);
                    rb.setText(String.format("🔔 %d:%02d %s",h,sess.reminderMinute,ap));
                    rb.setTextSize(11); rb.setTextColor(0xFF1D9E75);
                    rb.setBackground(getDrawable(R.drawable.chip_green));
                    rb.setPadding(8,4,8,4);
                    LinearLayout.LayoutParams rblp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
                    rblp.setMargins(0,0,8,0); rb.setLayoutParams(rblp);
                    sessHdr.addView(rb);
                }

                Button editBtn=new Button(this); editBtn.setText("Edit");
                editBtn.setBackground(getDrawable(R.drawable.btn_outline)); editBtn.setTextSize(11);
                editBtn.setTextColor(0xFF6B6A66); editBtn.setPadding(12,4,12,4);
                editBtn.setOnClickListener(v->openEditSession(fd,fsi));
                sessHdr.addView(editBtn);
                sessCard.addView(sessHdr);

                // Step count
                TextView stepCount=new TextView(this);
                stepCount.setText(sess.steps.size()+" steps");
                stepCount.setTextSize(12); stepCount.setTextColor(0xFF9B9B8F);
                LinearLayout.LayoutParams tclp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
                tclp.setMargins(0,4,0,0); stepCount.setLayoutParams(tclp);
                sessCard.addView(stepCount);
                ed.addView(sessCard);
            }
        }
    }

    void openEditSession(int dow, int si){
        Intent i=new Intent(this,EditSessionActivity.class);
        i.putExtra("dow",dow);
        i.putExtra("si",si);
        startActivity(i);
    }

    // ── PRODUCTS ──────────────────────────────────────────────────────────────
    void buildProducts(){
        LinearLayout grid=findViewById(R.id.product_grid);
        grid.removeAllViews();

        Button addBtn=new Button(this); addBtn.setText("+ Add Product");
        addBtn.setBackground(getDrawable(R.drawable.btn_green));
        addBtn.setTextColor(0xFFFFFFFF); addBtn.setTextSize(14);
        LinearLayout.LayoutParams ablp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        ablp.setMargins(0,0,0,16); addBtn.setLayoutParams(ablp);
        addBtn.setOnClickListener(v->startActivity(new Intent(this,EditProductActivity.class)));
        grid.addView(addBtn);

        for(int i=0;i<db.products.size();i++){
            Models.Product p=db.products.get(i);
            final int fi=i;
            LinearLayout card=new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackground(getDrawable(R.drawable.card_white));
            card.setPadding(14,12,14,12);
            LinearLayout.LayoutParams clp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
            clp.setMargins(0,0,0,8); card.setLayoutParams(clp);

            TextView name=new TextView(this); name.setText(p.name);
            name.setTextSize(14); name.setTextColor(0xFF1A1A1A);
            name.setTypeface(null,android.graphics.Typeface.BOLD); card.addView(name);

            if(!p.brand.isEmpty()){
                TextView brand=new TextView(this); brand.setText(p.brand);
                brand.setTextSize(12); brand.setTextColor(0xFF9B9B8F); card.addView(brand);
            }

            if(!p.concerns.isEmpty()){
                TextView con=new TextView(this); con.setText(p.concerns);
                con.setTextSize(12); con.setTextColor(0xFF6B6A66);
                LinearLayout.LayoutParams conlp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
                conlp.setMargins(0,4,0,0); con.setLayoutParams(conlp); card.addView(con);
            }

            if(!p.notes.isEmpty()){
                TextView notes=new TextView(this); notes.setText(p.notes);
                notes.setTextSize(11); notes.setTextColor(0xFF9B9B8F); card.addView(notes);
            }

            LinearLayout actions=new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams alp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
            alp.setMargins(0,8,0,0); actions.setLayoutParams(alp);

            Button edit=new Button(this); edit.setText("Edit");
            edit.setBackground(getDrawable(R.drawable.btn_outline)); edit.setTextSize(12);
            edit.setTextColor(0xFF6B6A66); edit.setPadding(12,6,12,6);
            LinearLayout.LayoutParams elp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
            elp.setMargins(0,0,8,0); edit.setLayoutParams(elp);
            edit.setOnClickListener(v->{
                Intent in=new Intent(this,EditProductActivity.class);
                in.putExtra("productIndex",fi); startActivity(in);
            });

            Button del=new Button(this); del.setText("Delete");
            del.setBackground(getDrawable(R.drawable.btn_outline)); del.setTextSize(12);
            del.setTextColor(0xFFA32D2D); del.setPadding(12,6,12,6);
            del.setOnClickListener(v->{
                new android.app.AlertDialog.Builder(this).setTitle("Delete product?")
                    .setPositiveButton("Delete",(d,w)->{db.products.remove(fi);db.save();buildProducts();})
                    .setNegativeButton("Cancel",null).show();
            });

            actions.addView(edit); actions.addView(del);
            card.addView(actions);
            grid.addView(card);
        }
    }
}
