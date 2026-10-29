package cn.local.campuswater;
import android.content.*;

public final class CourseReminderReceiver extends BroadcastReceiver {
  @Override public void onReceive(Context context,Intent intent){
    if(CourseReminderManager.ACTION.equals(intent.getAction())){CourseReminderManager.deliver(context,intent.getData()==null?null:intent.getData().getLastPathSegment());}
    else CourseReminderManager.schedule(context);
  }
}
