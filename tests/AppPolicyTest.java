package cn.local.campuswater;
import org.json.*;
import java.util.*;
import java.time.Instant;

public class AppPolicyTest {
  static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
  static long utc(String value){return Instant.parse(value).toEpochMilli();}
  public static void main(String[] args)throws Exception{
    long midnight=utc("2026-10-28T16:00:00Z");
    check(DailySyncPolicy.day(midnight-1).equals("2026-10-28"),"local day before midnight");
    check(DailySyncPolicy.day(midnight).equals("2026-10-29"),"local midnight boundary");
    check(!DailySyncPolicy.due(midnight+1000,"2026-10-29"),"same-day entry uses cache");
    check(DailySyncPolicy.due(midnight,"2026-10-28"),"new day syncs");
    check(DailySyncPolicy.delay(midnight+3600000)==23*3600000L,"schedule next local midnight");
    check(PageNavigation.target(0,.6f,1,-100,1000,4)==1,"slow swipe advances one page");
    check(PageNavigation.target(0,.8f,1,-14000,1000,4)==1,"fast fling still advances only one page");
    check(PageNavigation.target(3,2.2f,-1,14000,1000,4)==2,"reverse fling advances only one page");
    check(PageNavigation.target(1,.95f,1,-100,1000,4)==2,"new gesture advances from selected page during interrupted animation");
    check(PageNavigation.target(3,3.15f,1,-9000,1000,4)==3,"last page cannot wrap");
    check(PageNavigation.target(1,1.05f,0,0,1000,4)==1,"short drag snaps back");
    String[] allowed={"http://ykt.sdmcjn.edu.cn:7248/easytong_webapp/index.html#/home/index","http://authserver.sdmcjn.edu.cn/authserver/login","https://ehall.sdmcjn.edu.cn/publicapp","http://111.14.213.18:7072/uwc_webapp/"};for(String u:allowed)check(SchoolUrlPolicy.allowed(u),"allow known school service");
    String[] blocked={"https://evil.example/","https://evil.sdmcjn.edu.cn/","http://ykt.sdmcjn.edu.cn.evil.example:7248/","http://user@ykt.sdmcjn.edu.cn:7248/","http://ykt.sdmcjn.edu.cn.:7248/","http://ykt.sdmcjn.edu.cn:9999/","file:///data/data/app/","javascript:alert(1)","data:text/html,test","http://111.14.213.18:80/"};for(String u:blocked)check(!SchoolUrlPolicy.allowed(u),"reject unsafe address: "+u);
    Map<Integer,String> times=ReminderPlan.parseTimes(ReminderPlan.DEFAULT_TIMES);check(times.get(1).equals("08:00")&&times.get(3).equals("10:00")&&times.get(5).equals("14:00"),"use supplied large-period times");
    for(String bad:new String[]{"1=24:00","1=08:60","0=08:00","1=08:00\n1=09:00","","1=8:00"}){boolean rejected=false;try{ReminderPlan.parseTimes(bad);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"reject invalid time map");}
    JSONObject calendar=new JSONObject("{\"schoolYearTerm\":\"2026-2027-1\",\"allTeachWeeks\":20,\"weekOfTerm\":9,\"date\":\"2026-10-28\"}");
    JSONArray courses=new JSONArray();for(String section:new String[]{"1-2","3-4","5-6","7-8"})courses.put(new JSONObject().put("name","Test Course "+section).put("classId",section).put("section",section).put("weekday",4).put("position","Test Building").put("classroom","201"));
    JSONObject snapshot=new JSONObject().put("calendar",calendar).put("week",9).put("courses",courses);
    long now=utc("2026-10-28T23:00:00Z");JSONArray plan=ReminderPlan.build("TEST",Arrays.asList(snapshot,snapshot),times,now);
    check(plan.length()==3,"deduplicate snapshots and skip unspecified start times");
    check(plan.getJSONObject(0).getLong("remind")==utc("2026-10-28T23:30:00Z"),"08:00 course reminds at 07:30 local");
    check(plan.getJSONObject(1).getLong("remind")==utc("2026-10-29T01:30:00Z"),"10:00 course reminds at 09:30 local");
    check(plan.getJSONObject(2).getLong("remind")==utc("2026-10-29T05:30:00Z"),"14:00 course reminds at 13:30 local");
    check(plan.getJSONObject(0).getString("room").equals("Test Building 201"),"notification contains class location");
    check(ReminderPlan.build("TEST",Arrays.asList(snapshot),times,utc("2026-10-28T23:31:00Z")).length()==2,"do not send a stale reminder");
    check(!ReminderPlan.build("OTHER",Arrays.asList(snapshot),times,now).getJSONObject(0).getString("id").equals(plan.getJSONObject(0).getString("id")),"reminder identity separates accounts");
    System.out.println("PASS: continuous navigation, daily cache, URL isolation and 30-minute course reminder scheduling");
  }
}
