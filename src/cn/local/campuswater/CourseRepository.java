package cn.local.campuswater;

import android.webkit.CookieManager;
import org.json.*;
import java.net.*;
import java.io.*;
import java.util.*;

class CourseRepository {
  static final String API="http://ehall.sdmcjn.edu.cn/publicapp/sys/mykbxt/api/";
  static JSONObject get(String endpoint,String query)throws Exception{
    URL url=new URL(API+endpoint+(query.isEmpty()?"":"?"+query));
    for(int redirects=0;redirects<9;redirects++){
      if(Thread.currentThread().isInterrupted())throw new InterruptedException();
      if(!SchoolUrlPolicy.allowed(url.toString()))throw new IOException("学校服务跳转地址异常");
      HttpURLConnection c=(HttpURLConnection)url.openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(15000);c.setReadTimeout(20000);
      c.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/143.0.0.0 Mobile Safari/537.36");c.setRequestProperty("Accept","application/json");c.setRequestProperty("X-Requested-With","XMLHttpRequest");
      String cookies=CookieManager.getInstance().getCookie(url.toString());if(cookies!=null)c.setRequestProperty("Cookie",cookies);
      try{
        int status=c.getResponseCode();if(Thread.currentThread().isInterrupted())throw new InterruptedException();
        for(Map.Entry<String,List<String>> h:c.getHeaderFields().entrySet())if(h.getKey()!=null&&h.getKey().equalsIgnoreCase("Set-Cookie"))for(String value:h.getValue())CookieManager.getInstance().setCookie(url.toString(),value);
        if(status>=300&&status<400){String next=c.getHeaderField("Location");if(next==null)throw new IOException("学校服务跳转失败");url=new URL(url,next);continue;}
        if(status!=200)throw new IOException("学校课表服务暂不可用（"+status+"）");
        ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(out.size()+n>4*1024*1024)throw new IOException("课表数据超过读取范围");out.write(b,0,n);}}
        String body=out.toString("UTF-8").trim();if(body.startsWith("\uFEFF"))body=body.substring(1);
        if(!body.startsWith("{"))throw new IOException("学校认证已失效，请在“我的”中重新登录");
        JSONObject j=new JSONObject(body);if(j.optInt("code")!=200)throw new IOException(j.optString("msg","学校未返回有效课表"));return j;
      }finally{c.disconnect();}
    }throw new IOException("学校认证跳转次数过多，请重新登录");
  }
  interface Transport {JSONObject get(String endpoint,String query)throws Exception;}
  static JSONObject load(String term,int week,String jump,JSONObject knownCalendar)throws Exception{return load(term,week,jump,knownCalendar,CourseRepository::get);}
  static JSONObject load(String term,int week,String jump,JSONObject knownCalendar,Transport transport)throws Exception{
    JSONObject calendar;
    if(jump.isEmpty()&&week>0&&knownCalendar!=null&&term.equals(knownCalendar.optString("schoolYearTerm")))calendar=new JSONObject(knownCalendar.toString());
    else{String q="termJump="+URLEncoder.encode(jump,"UTF-8")+"&schoolYearTerm="+URLEncoder.encode(term,"UTF-8");calendar=transport.get("getSchoolCalendar.do",q);}
    String actualTerm=calendar.getString("schoolYearTerm");int max=calendar.optInt("allTeachWeeks",0);if(max<1)throw new IOException("学校尚未公布该学期教学周");
    int actualWeek=week>0&&jump.isEmpty()?week:calendar.optInt("weekOfTerm",0);if(actualWeek<1)actualWeek=max;
    if(actualWeek>max)throw new IOException("所选周次超过该学期教学周范围");
    JSONObject result=transport.get("getMyTimeTableList.do","weekOfTerm="+actualWeek+"&schoolYearTerm="+URLEncoder.encode(actualTerm,"UTF-8"));
    JSONObject snapshot=new JSONObject();snapshot.put("calendar",calendar);snapshot.put("week",actualWeek);snapshot.put("term",actualTerm);snapshot.put("courses",normalize(result.opt("timeTable")));snapshot.put("synced",System.currentTimeMillis());return snapshot;
  }
  static String date(JSONObject calendar,int week,int day,String pattern)throws Exception{
    java.text.SimpleDateFormat parser=new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.CHINA);parser.setLenient(false);parser.setTimeZone(TimeZone.getTimeZone("Asia/Hong_Kong"));
    Calendar c=Calendar.getInstance(parser.getTimeZone());c.setTime(parser.parse(calendar.getString("date")));int delta=(c.get(Calendar.DAY_OF_WEEK)+5)%7;
    int anchor=calendar.optInt("weekOfTerm",0);if(anchor<1)anchor=calendar.optInt("allTeachWeeks",1);
    c.add(Calendar.DATE,-delta+(week-anchor)*7+day-1);java.text.SimpleDateFormat fmt=new java.text.SimpleDateFormat(pattern,Locale.CHINA);fmt.setTimeZone(parser.getTimeZone());return fmt.format(c.getTime());
  }
  static JSONArray normalize(Object table)throws JSONException{
    JSONArray out=new JSONArray();Set<String> seen=new HashSet<>();walk(table,0,out,seen);return out;
  }
  static void walk(Object item,int day,JSONArray out,Set<String> seen)throws JSONException{
    if(item instanceof JSONArray){JSONArray a=(JSONArray)item;for(int i=0;i<a.length();i++)walk(a.opt(i),day,out,seen);return;}
    if(!(item instanceof JSONObject))return;JSONObject j=(JSONObject)item;
    if(j.has("name")&&j.has("section")){
      int weekday=j.optInt("weekday",day);String name=j.optString("name").trim(),section=j.optString("section").trim();if(name.isEmpty()||section.isEmpty()||weekday<1||weekday>7)return;
      String key=weekday+"|"+j.optString("classId")+"|"+name+"|"+section+"|"+j.optString("classroom")+"|"+j.optString("bjmc");
      if(seen.add(key)){JSONObject copy=new JSONObject(j.toString());copy.put("weekday",weekday);out.put(copy);}return;
    }
    Iterator<String> keys=j.keys();while(keys.hasNext()){String key=keys.next();int childDay=day;if(day==0&&key.matches("[1-7]"))childDay=Integer.parseInt(key);walk(j.opt(key),childDay,out,seen);}
  }
}
