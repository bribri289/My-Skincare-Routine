package com.skincare.app.data;
import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.*;

public class AppData {
    private static AppData inst;
    private static final String PREFS="skincare_prefs";
    private final SharedPreferences prefs;
    private final Gson gson=new Gson();

    public Map<Integer,Models.DayRoutine> routine=new HashMap<>();
    public List<Models.Product> products=new ArrayList<>();
    public Map<String,Models.CheckData> checks=new HashMap<>();

    private AppData(Context ctx){
        prefs=ctx.getApplicationContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        load();
        if(routine.isEmpty()) seed();
    }

    public static synchronized AppData get(Context ctx){
        if(inst==null) inst=new AppData(ctx);
        return inst;
    }

    public void save(){
        prefs.edit()
            .putString("routine",gson.toJson(routine))
            .putString("products",gson.toJson(products))
            .putString("checks",gson.toJson(checks))
            .apply();
    }

    private void load(){
        String rj=prefs.getString("routine",null);
        String pj=prefs.getString("products",null);
        String cj=prefs.getString("checks",null);
        if(rj!=null){
            Type t=new TypeToken<Map<Integer,Models.DayRoutine>>(){}.getType();
            routine=gson.fromJson(rj,t);
        }
        if(pj!=null){
            Type t=new TypeToken<List<Models.Product>>(){}.getType();
            products=gson.fromJson(pj,t);
        }
        if(cj!=null){
            Type t=new TypeToken<Map<String,Models.CheckData>>(){}.getType();
            checks=gson.fromJson(cj,t);
        }
    }

    public Models.CheckData getChecks(String dateKey){
        if(!checks.containsKey(dateKey)){checks.put(dateKey,new Models.CheckData());checks.get(dateKey).dateKey=dateKey;}
        return checks.get(dateKey);
    }

    public int[] getProgress(String dateKey, int dow){
        Models.DayRoutine day=routine.get(dow);
        if(day==null) return new int[]{0,0};
        Models.CheckData cd=getChecks(dateKey);
        int done=0,total=0;
        for(int si=0;si<day.sessions.size();si++){
            Models.Session s=day.sessions.get(si);
            for(int i=0;i<s.steps.size();i++){
                total++;
                if(Boolean.TRUE.equals(cd.checks.get(si+"_"+i))) done++;
            }
        }
        return new int[]{done,total};
    }

    public int calcStreak(){
        int n=0;
        Calendar cal=Calendar.getInstance();
        for(int i=0;i<60;i++){
            String key=dateKey(cal);
            int dow=cal.get(Calendar.DAY_OF_WEEK)-1;
            int[] p=getProgress(key,dow);
            if(i==0&&p[0]==0){cal.add(Calendar.DATE,-1);continue;}
            if(p[1]>0&&p[0]==p[1]) n++;
            else if(i>0) break;
            cal.add(Calendar.DATE,-1);
        }
        return n;
    }

    public static String dateKey(Calendar cal){
        return cal.get(Calendar.YEAR)+"-"+
            String.format("%02d",cal.get(Calendar.MONTH)+1)+"-"+
            String.format("%02d",cal.get(Calendar.DAY_OF_MONTH));
    }

    public Models.Product findProduct(String id){
        if(id==null||id.isEmpty()) return null;
        for(Models.Product p:products) if(p.id.equals(id)) return p;
        return null;
    }

    public List<Models.Session> getAllSessionsWithReminders(){
        List<Models.Session> result=new ArrayList<>();
        for(Models.DayRoutine dr:routine.values())
            for(Models.Session s:dr.sessions)
                if(s.reminderEnabled) result.add(s);
        return result;
    }

    private void seed(){
        String[] dayNames={"Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"};
        // Default products
        String[][] prods={
            {"p1","CeraVe Foaming Face Wash","CeraVe","face","Ceramides, Niacinamide","Cleansing","AM cleanser"},
            {"p2","CeraVe BP Face Wash","CeraVe","face","Benzoyl Peroxide 4%","Acne","Mon & Thu PM"},
            {"p3","CeraVe Retinol Serum","CeraVe","face","Encapsulated Retinol","Texture","Tue & Fri PM"},
            {"p4","CeraVe Vitamin C Serum","CeraVe","face","Vitamin C","Brightening","AM after cleanser"},
            {"p5","CeraVe AM Moisturizer SPF 30","CeraVe","face","SPF 30, Ceramides","Sun protection","Every morning"},
            {"p6","CeraVe PM Moisturizer","CeraVe","face","Ceramides, Hyaluronic Acid","Moisture","Every PM"},
            {"p7","Dove Body Wash","Dove","body","Mild surfactants","Moisture","Daily"},
        };
        for(String[] p:prods){
            Models.Product prod=new Models.Product();
            prod.id=p[0];prod.name=p[1];prod.brand=p[2];prod.type=p[3];
            prod.ingredients=p[4];prod.concerns=p[5];prod.notes=p[6];
            products.add(prod);
        }

        // Build routine for each day
        for(int d=0;d<7;d++){
            Models.DayRoutine dr=new Models.DayRoutine();
            dr.label=dayNames[d];
            // Morning session
            Models.Session am=new Models.Session();
            am.id="am_"+d; am.name="Morning — face";
            am.reminderEnabled=true; am.reminderHour=7; am.reminderMinute=0;
            am.steps.add(step("Foaming Face Wash","p1"));
            am.steps.add(step("Vitamin C Serum","p4"));
            am.steps.add(step("AM Moisturizer SPF 30","p5"));
            dr.sessions.add(am);
            // Evening session
            Models.Session pm=new Models.Session();
            pm.id="pm_"+d; pm.name="Evening — face";
            pm.reminderEnabled=true; pm.reminderHour=20; pm.reminderMinute=0;
            pm.steps.add(step("Foaming Face Wash","p1"));
            pm.steps.add(step("PM Moisturizer","p6"));
            dr.sessions.add(pm);
            // Body session
            Models.Session body=new Models.Session();
            body.id="body_"+d; body.name="Body";
            body.reminderEnabled=false;
            body.steps.add(step("Dove Body Wash","p7"));
            dr.sessions.add(body);
            routine.put(d,dr);
        }
        save();
    }

    private Models.Step step(String name, String pid){
        Models.Step s=new Models.Step(); s.name=name; s.productId=pid; return s;
    }
}
