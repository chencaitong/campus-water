package cn.local.campuswater;
import org.json.*;
public class CourseModelTest{
  static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
  public static void main(String[] args)throws Exception{
    JSONObject first=new JSONObject("{\"name\":\"人体解剖学\",\"section\":\"3-4\",\"classId\":\"A\",\"classroom\":\"101\",\"bjmc\":\"一班\"}");
    JSONObject clash=new JSONObject(first.toString()).put("classId","B").put("classroom","102");
    JSONObject table=new JSONObject().put("3",new JSONObject().put("3",first).put("4",first).put("5",clash));
    JSONArray rows=CourseRepository.normalize(table);check(rows.length()==2,"Deduplicate repeated periods without dropping overlapping classes");check(rows.getJSONObject(0).getInt("weekday")==3,"day fallback");
    JSONArray list=new JSONArray().put(new JSONObject(first.toString()).put("weekday",1)).put(new JSONObject(first.toString()).put("weekday",7));
    check(CourseRepository.normalize(list).length()==2,"array schedules");
    check(CourseRepository.normalize(new JSONArray().put(new JSONObject(first.toString()).put("weekday",9))).length()==0,"Reject invalid weekdays");
    int[] sec=CourseScreen.section(first);check(sec[0]==3&&sec[1]==4,"section range");
    sec=CourseScreen.section(new JSONObject().put("section","第9-10节"));check(sec[0]==9&&sec[1]==10,"section labels");
    check(CourseRepository.normalize(new JSONObject()).length()==0,"empty schedule");
    System.out.println("PASS: real normalization code, repeated periods, clashes, weekday mapping, range parsing, empty weeks");
  }
}
