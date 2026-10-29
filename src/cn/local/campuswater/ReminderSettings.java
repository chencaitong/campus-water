package cn.local.campuswater;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.*;

final class ReminderSettings {
  static void notifications(MainActivity a){
    if(Build.VERSION.SDK_INT>=33&&a.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){a.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},300);return;}
    Intent settings=new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,a.getPackageName()).putExtra(Settings.EXTRA_CHANNEL_ID,CourseReminderManager.CHANNEL);try{a.startActivity(settings);}catch(Exception e){Toast.makeText(a,"请在系统设置中允许本应用通知",Toast.LENGTH_LONG).show();}
  }
  static void show(MainActivity a){
    CourseReminderManager.channel(a);ScrollView scroll=new ScrollView(a);LinearLayout box=a.col();box.setPadding(a.dp(20),a.dp(8),a.dp(20),a.dp(16));scroll.addView(box);
    Switch enabled=new Switch(a);enabled.setText("课前30分钟提醒");enabled.setChecked(CourseReminderManager.enabled(a));box.addView(enabled);
    box.addView(a.text("提醒显示科目、上课时间和地点；锁屏默认隐藏课程详情。",13,false));
    box.addView(a.text("按课程开始的节次填写时间",17,true));
    box.addView(a.text("已按你提供的大课时间设置：第1–2节08:00、第3–4节10:00、第5–6节14:00。每行格式为“开始节次=HH:mm”，其他节次可补充。",13,false));
    EditText times=new EditText(a);times.setText(CourseReminderManager.times(a));times.setSingleLine(false);times.setMinLines(4);times.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);box.addView(times);
    box.addView(a.button("允许通知",()->notifications(a)));a.gap(box,8);
    box.addView(a.button("发送测试通知",()->{if(!CourseReminderManager.notificationsAllowed(a)){notifications(a);return;}CourseReminderManager.post(a,"test","课前提醒测试","通知已正常开启。正式提醒将显示科目、上课时间和地点。");Toast.makeText(a,"测试通知已发送",Toast.LENGTH_SHORT).show();}));
    box.addView(a.text("提醒基于已同步课表；未填写时间的节次不会安排。学校调课后请手动刷新。使用普通定时通知，不要求额外的准时提醒权限。系统省电时可能延迟，无法保证正好提前30分钟；强行停止应用也会停止提醒。",12,false));
    AlertDialog dialog=new AlertDialog.Builder(a).setTitle("课前提醒设置").setView(scroll).setNegativeButton("取消",null).setPositiveButton("保存",null).create();
    dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button->{try{ReminderPlan.parseTimes(times.getText().toString());CourseReminderManager.prefs(a).edit().putBoolean("enabled",enabled.isChecked()).putString("times",times.getText().toString().trim()).apply();CourseReminderManager.schedule(a);if(!enabled.isChecked())((NotificationManager)a.getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();if(a.courseScreen!=null)a.courseScreen.updateReminderStatus();dialog.dismiss();if(enabled.isChecked()&&!CourseReminderManager.notificationsAllowed(a))notifications(a);}catch(IllegalArgumentException e){times.setError(e.getMessage());}}));dialog.show();
  }
  private ReminderSettings(){}
}
