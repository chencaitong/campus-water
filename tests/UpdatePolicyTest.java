package cn.local.campuswater;
import org.json.*;
import java.util.*;
public class UpdatePolicyTest {
  static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
  public static void main(String[] args)throws Exception{
    long now=10_000_000;
    check(SyncPolicy.due(now,0),"first sync");
    check(!SyncPolicy.due(now,now-3_599_999),"reuse cache during hour");
    check(SyncPolicy.due(now,now-3_600_000),"refresh at hour boundary");
    check(SyncPolicy.delay(now,now-2_000)==3_598_000,"remaining timer");
    check(SyncPolicy.due(now,now+10),"clock rollback does not freeze refresh");
    JSONObject calendar=new JSONObject("{\"schoolYearTerm\":\"2026-2027-1\",\"allTeachWeeks\":20,\"weekOfTerm\":9,\"date\":\"2026-10-28\"}");
    List<String> requests=new ArrayList<>();
    CourseRepository.Transport tx=(endpoint,query)->{requests.add(endpoint+"?"+query);check(endpoint.equals("getMyTimeTableList.do"),"same-term week change must not request calendar again");return new JSONObject().put("code",200).put("timeTable",new JSONObject());};
    JSONObject prev=CourseRepository.load("2026-2027-1",8,"",calendar,tx),next=CourseRepository.load("2026-2027-1",10,"",calendar,tx);
    check(prev.getInt("week")==8&&next.getInt("week")==10,"honour selected past and future week");
    check(requests.get(0).contains("weekOfTerm=8&")&&requests.get(1).contains("weekOfTerm=10&"),"send requested week to server");
    check(CourseRepository.date(calendar,8,1,"yyyy-MM-dd").equals("2026-10-19"),"past week date");
    check(CourseRepository.date(calendar,10,1,"yyyy-MM-dd").equals("2026-11-02"),"future week date");
    check(CourseRepository.date(calendar,9,3,"yyyy-MM-dd").equals("2026-10-28"),"current day date");
    JSONObject old=CourseRepository.load("2026-2027-1",0,"prev",calendar,(ep,q)->ep.equals("getSchoolCalendar.do")?new JSONObject("{\"schoolYearTerm\":\"2025-2026-2\",\"allTeachWeeks\":20,\"weekOfTerm\":0,\"date\":\"2026-06-29\"}"):new JSONObject().put("timeTable",new JSONObject()));
    check(old.getInt("week")==20,"previous semester uses last week when server returns zero");
    check(CourseRepository.date(old.getJSONObject("calendar"),20,1,"yyyy-MM-dd").equals("2026-06-29"),"previous semester date anchor");
    boolean rejected=false;try{CourseRepository.load("2026-2027-1",21,"",calendar,tx);}catch(java.io.IOException e){rejected=true;}check(rejected,"out-of-range week must not silently show a different week");
    System.out.println("PASS: hourly cache boundary/timer, clock changes, past/future week requests, dates, previous term and invalid-week handling");
  }
}
