package cn.local.campuswater;
import org.json.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.security.MessageDigest;
import java.util.regex.*;

final class ReminderPlan {
  static final String DEFAULT_TIMES="1=08:00\n3=10:00\n5=14:00";
  static Map<Integer,String> parseTimes(String value){
    Map<Integer,String> result=new TreeMap<>();
    for(String line:value.split("\\r?\\n")){
      if(line.trim().isEmpty())continue;Matcher m=Pattern.compile("^\\s*(\\d{1,2})\\s*=\\s*([01]\\d|2[0-3]):([0-5]\\d)\\s*$").matcher(line);
      if(!m.matches())throw new IllegalArgumentException("请按“1=08:00”格式填写，每行一个开始节次");
      int section=Integer.parseInt(m.group(1));if(section<1||section>24||result.containsKey(section))throw new IllegalArgumentException("节次须为1至24且不能重复");
      result.put(section,m.group(2)+":"+m.group(3));
    }
    if(result.isEmpty())throw new IllegalArgumentException("请至少填写一个上课时间");return result;
  }
  static int firstSection(String value){Matcher m=Pattern.compile("\\d+").matcher(value);return m.find()?Integer.parseInt(m.group()):0;}
  static JSONArray build(String owner,Collection<JSONObject> snapshots,Map<Integer,String> times,long now)throws Exception{
    Map<String,JSONObject> unique=new HashMap<>();SimpleDateFormat parser=new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.ROOT);parser.setTimeZone(TimeZone.getTimeZone("Asia/Hong_Kong"));parser.setLenient(false);
    for(JSONObject snapshot:snapshots){JSONArray courses=snapshot.optJSONArray("courses");if(courses==null)continue;
      for(int i=0;i<courses.length();i++){JSONObject course=courses.getJSONObject(i);int day=course.optInt("weekday");if(day<1||day>7)continue;
        String clock=times.get(firstSection(course.optString("section")));if(clock==null)continue;
        String date=CourseRepository.date(snapshot.getJSONObject("calendar"),snapshot.getInt("week"),day,"yyyy-MM-dd");
        long start=parser.parse(date+" "+clock).getTime(),remind=start-30*60*1000L;if(remind<=now||remind>now+90L*24*60*60*1000)continue;
        String room=(course.optString("position","")+" "+course.optString("classroom","")).trim();if(room.isEmpty())room="教室未公布";
        String identity=owner+"|"+date+"|"+course.optString("classId")+"|"+course.optString("name")+"|"+course.optString("section")+"|"+room;
        byte[] digest=MessageDigest.getInstance("SHA-256").digest(identity.getBytes("UTF-8"));StringBuilder id=new StringBuilder();for(byte b:digest)id.append(String.format(Locale.ROOT,"%02x",b&255));
        JSONObject row=new JSONObject();row.put("id",id.toString());row.put("owner",owner);row.put("name",course.optString("name"));row.put("room",room);row.put("section",course.optString("section"));row.put("date",date);row.put("clock",clock);row.put("start",start);row.put("remind",remind);unique.put(id.toString(),row);
      }
    }
    List<JSONObject> rows=new ArrayList<>(unique.values());Collections.sort(rows,(a,b)->Long.compare(a.optLong("remind"),b.optLong("remind")));JSONArray out=new JSONArray();for(JSONObject row:rows){if(out.length()>=128)break;out.put(row);}return out;
  }
  private ReminderPlan(){}
}
