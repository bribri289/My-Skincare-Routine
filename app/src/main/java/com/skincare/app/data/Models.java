package com.skincare.app.data;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Models {

    public static class Product implements Serializable {
        public String id="", name="", brand="", type="face";
        public String ingredients="", concerns="", notes="";
    }

    public static class Step implements Serializable {
        public String name="";
        public String productId="";
    }

    public static class Session implements Serializable {
        public String id="", name="";
        public List<Step> steps=new ArrayList<>();
        // Reminder: exact daily time
        public boolean reminderEnabled=false;
        public int reminderHour=8, reminderMinute=0;
        // Which days of week (0=Sun..6=Sat)
        public boolean[] reminderDays={true,true,true,true,true,true,true};
    }

    public static class DayRoutine implements Serializable {
        public String label="";
        public List<Session> sessions=new ArrayList<>();
    }

    public static class CheckData implements Serializable {
        public String dateKey="";
        public java.util.Map<String,Boolean> checks=new java.util.HashMap<>();
        public String notes="";
    }
}
