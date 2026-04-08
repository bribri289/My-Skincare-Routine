package com.skincare.app.ui;
import android.os.Bundle;
import android.os.Handler;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.skincare.app.R;
import com.skincare.app.data.*;
import java.util.*;

public class LogSessionActivity extends AppCompatActivity {
    AppData db; Models.SkincareSession session;
    int curStep=0; int secondsLeft=0;
    Handler timerHandler=new Handler();
    Runnable timerRunnable;
    List<Integer> completedIds=new ArrayList<>();

    @Override protected void onCreate(Bundle b){
        super.onCreate(b); setContentView(R.layout.activity_log_session);
        db=AppData.get(this);
        session=(Models.SkincareSession)getIntent().getSerializableExtra("session");
        if(session==null){finish();return;}
        showStep(0);
    }

    void showStep(int idx){
        curStep=idx;
        if(idx>=session.steps.size()){finishSession();return;}
        Models.SessionStep step=session.steps.get(idx);         ((TextView)findViewById(R.id.tv_step_counter)).setText("Step "+(idx+1)+" of "+session.steps.size());
        ((ProgressBar)findViewById(R.id.progress_bar)).setMax(session.steps.size());
        ((ProgressBar)findViewById(R.id.progress_bar)).setProgress(idx+1);
        ((TextView)findViewById(R.id.tv_step_emoji)).setText(step.emoji);
        ((TextView)findViewById(R.id.tv_step_name)).setText(step.name);         ((TextView)findViewById(R.id.tv_step_notes)).setText(step.notes.isEmpty()?"No notes":step.notes);
        // Product
        if(step.productId!=0){
            Models.Product p=db.findProduct(step.productId);
            if(p!=null){                 ((TextView)findViewById(R.id.tv_step_product)).setText(p.emoji+" "+p.name+" — "+p.brand);
                findViewById(R.id.tv_step_product).setVisibility(View.VISIBLE);
            }
        } else findViewById(R.id.tv_step_product).setVisibility(View.GONE);
        // Timer
        secondsLeft=step.durationSeconds>0?step.durationSeconds:60;
        updateTimer();
        if(timerRunnable!=null)timerHandler.removeCallbacks(timerRunnable);
        timerRunnable=new Runnable(){public void run(){if(secondsLeft>0){secondsLeft--;updateTimer();timerHandler.postDelayed(this,1000);}}};
        timerHandler.postDelayed(timerRunnable,1000);
        // Buttons
        findViewById(R.id.btn_done).setOnClickListener(v->{completedIds.add(step.id);showStep(curStep+1);});
        findViewById(R.id.btn_skip).setOnClickListener(v->showStep(curStep+1));
    }

    void updateTimer(){
        int m=secondsLeft/60; int s=secondsLeft%60;         ((TextView)findViewById(R.id.tv_timer)).setText(String.format("%d:%02d",m,s));
    }

    void finishSession(){
        if(timerRunnable!=null)timerHandler.removeCallbacks(timerRunnable);
        // Log entry
        Models.LogEntry log=new Models.LogEntry();
        log.id=db.newId(); log.sessionId=session.id; log.sessionName=session.name;
        log.date=db.todayDate(); log.time=db.nowTime(); log.completed=true;
        log.completedStepIds=completedIds;
        db.logs.add(0,log);
        // Update streak
        Models.SkincareSession s=db.findSession(session.id);
        if(s!=null){s.currentStreak++;s.longestStreak=Math.max(s.longestStreak,s.currentStreak);s.lastCompleted=db.todayDate();}
        db.save();         Toast.makeText(this,"Great job! Routine logged 🌸",Toast.LENGTH_LONG).show();
        finish();
    }

    @Override protected void onDestroy(){
        if(timerRunnable!=null)timerHandler.removeCallbacks(timerRunnable);
        super.onDestroy();
    }
}
