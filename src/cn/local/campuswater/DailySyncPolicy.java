package cn.local.campuswater;
import java.text.SimpleDateFormat;
import java.util.*;
final class DailySyncPolicy {
  static String day(long now){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT);f.setTimeZone(TimeZone.getTimeZone("Asia/Hong_Kong"));return f.format(new Date(now));}
  static boolean due(long now,String lastDay){return !day(now).equals(lastDay);}
  static long delay(long now){Calendar c=Calendar.getInstance(TimeZone.getTimeZone("Asia/Hong_Kong"));c.setTimeInMillis(now);c.add(Calendar.DATE,1);c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);return Math.max(1000,c.getTimeInMillis()-now);}
  private DailySyncPolicy(){}
}
