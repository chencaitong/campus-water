package cn.local.campuswater;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import org.json.*;
import java.util.*;

final class CourseReminderManager {
  static final String CHANNEL="course_reminders_v1",ACTION="cn.local.campuswater.COURSE_REMINDER";
  static SharedPreferences prefs(Context c){return c.getSharedPreferences("course_reminders",0);}
  static boolean enabled(Context c){return prefs(c).getBoolean("enabled",true);}
  static String times(Context c){return prefs(c).getString("times",ReminderPlan.DEFAULT_TIMES);}
  static boolean notificationsAllowed(Context c){NotificationManager manager=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);NotificationChannel channel=manager.getNotificationChannel(CHANNEL);return (Build.VERSION.SDK_INT<33||c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)&&manager.areNotificationsEnabled()&&(channel==null||channel.getImportance()!=NotificationManager.IMPORTANCE_NONE);}
  static JSONArray stored(Context c){try{return new JSONArray(prefs(c).getString("plan","[]"));}catch(Exception e){return new JSONArray();}}
  static PendingIntent alarm(Context c,String id){Intent intent=new Intent(c,CourseReminderReceiver.class).setAction(ACTION).setData(Uri.parse("campuswater://course-reminder/"+id));return PendingIntent.getBroadcast(c,0,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
  static void channel(Context c){NotificationChannel channel=new NotificationChannel(CHANNEL,"课前提醒",NotificationManager.IMPORTANCE_HIGH);channel.setDescription("上课前30分钟提醒课程、时间和教室");channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);}
  static void cancel(Context c){AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);JSONArray old=stored(c);for(int i=0;i<old.length();i++){JSONObject row=old.optJSONObject(i);if(row!=null)am.cancel(alarm(c,row.optString("id")));}prefs(c).edit().remove("plan").apply();}
  static synchronized void schedule(Context c){
    cancel(c);channel(c);String owner=c.getSharedPreferences("account",0).getString("id","");if(!enabled(c)||owner.isEmpty()||!notificationsAllowed(c))return;
    try{List<JSONObject> snapshots=new ArrayList<>();for(Map.Entry<String,?> entry:c.getSharedPreferences("courses",0).getAll().entrySet()){
      if(!entry.getKey().startsWith(owner+"_")||entry.getKey().endsWith("_latest")||!(entry.getValue() instanceof String))continue;
      try{JSONObject snapshot=new JSONObject((String)entry.getValue());if(snapshot.has("calendar")&&snapshot.has("courses"))snapshots.add(snapshot);}catch(Exception ignored){}
    }
    JSONArray plan=ReminderPlan.build(owner,snapshots,ReminderPlan.parseTimes(times(c)),System.currentTimeMillis());prefs(c).edit().putString("plan",plan.toString()).commit();
    AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
    for(int i=0;i<plan.length();i++){JSONObject row=plan.getJSONObject(i);PendingIntent intent=alarm(c,row.getString("id"));am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,row.getLong("remind"),intent);}
    }catch(Exception ignored){prefs(c).edit().remove("plan").apply();}
  }
  static String status(Context c){
    if(!enabled(c))return "课前提醒已关闭";
    if(!notificationsAllowed(c))return "课前提醒：请允许通知";
    int count=stored(c).length();return "课前30分钟 · 已安排 "+count+" 节"+" · 普通通知";
  }
  static void deliver(Context c,String id){
    if(!enabled(c)||!notificationsAllowed(c)||id==null)return;String owner=c.getSharedPreferences("account",0).getString("id","");if(owner.isEmpty())return;
    JSONArray plan=stored(c);for(int i=0;i<plan.length();i++){JSONObject row=plan.optJSONObject(i);if(row==null||!id.equals(row.optString("id"))||!owner.equals(row.optString("owner")))continue;
      long now=System.currentTimeMillis();if(row.optLong("start")<=now||row.optLong("remind")>now+1500)return;
      Set<String> delivered=new HashSet<>(prefs(c).getStringSet("delivered",Collections.emptySet()));if(delivered.contains(id))return;
      delivered.add(id);if(delivered.size()>512)delivered=new HashSet<>(Collections.singleton(id));prefs(c).edit().putStringSet("delivered",delivered).commit();
      post(c,id,row.optString("name")+" · 即将上课",row.optString("date")+" "+row.optString("clock")+"（第"+row.optString("section")+"节）\n地点："+row.optString("room"));return;
    }
  }
  static void post(Context c,String tag,String title,String content){
    channel(c);if(!notificationsAllowed(c))return;
    Intent open=new Intent(c,MainActivity.class).putExtra("targetPage","课表").addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
    PendingIntent action=PendingIntent.getActivity(c,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    int icon=c.getResources().getIdentifier("ic_notification","drawable",c.getPackageName());
    Notification hidden=new Notification.Builder(c,CHANNEL).setSmallIcon(icon).setContentTitle("课前提醒").setContentText("解锁查看课程安排").build();
    Notification notification=new Notification.Builder(c,CHANNEL).setSmallIcon(icon).setContentTitle(title).setContentText(content).setStyle(new Notification.BigTextStyle().bigText(content)).setContentIntent(action).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).setPublicVersion(hidden).setCategory(Notification.CATEGORY_REMINDER).build();
    ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(tag,0,notification);
  }
  private CourseReminderManager(){}
}
