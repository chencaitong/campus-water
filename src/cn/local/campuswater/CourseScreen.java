package cn.local.campuswater;

import android.app.AlertDialog;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.text.*;
import java.util.*;

class CourseScreen {
  final MainActivity a;final LinearLayout body;final String owner;JSONObject snapshot;boolean grid=true,cancelled=false,busy=false;int selectedDay;
  TextView status;Thread worker;int generation=0;
  TextView reminderStatus;
  final android.os.Handler dailyHandler=new android.os.Handler();
  final Runnable dailyRefresh=()->enter();
  final int[] colors={Color.rgb(218,240,251),Color.rgb(225,233,252),Color.rgb(216,241,234),Color.rgb(233,229,250)};
  CourseScreen(MainActivity activity){a=activity;body=a.body;owner=a.account();Calendar c=Calendar.getInstance(TimeZone.getTimeZone("Asia/Hong_Kong"));selectedDay=(c.get(Calendar.DAY_OF_WEEK)+5)%7+1;try{snapshot=new JSONObject(a.getSharedPreferences("courses",0).getString(owner+"_latest",""));}catch(Exception ignored){}}
  void cancel(){cancelled=true;generation++;leave();if(worker!=null)worker.interrupt();}
  void leave(){dailyHandler.removeCallbacks(dailyRefresh);}
  void enter(){
    updateReminderStatus();
    leave();if(!active()||owner.isEmpty()||!a.selected.equals("课表")||!a.foreground)return;
    String previous=a.getSharedPreferences("courses",0).getString(owner+"_lastRequestDay","");
    if(!busy&&DailySyncPolicy.due(System.currentTimeMillis(),previous))load("",0,"");
    dailyHandler.postDelayed(dailyRefresh,DailySyncPolicy.delay(System.currentTimeMillis()));
  }
  void updateReminderStatus(){if(reminderStatus!=null)reminderStatus.setText(CourseReminderManager.status(a));}
  boolean active(){return !cancelled&&!a.isFinishing()&&this==a.courseScreen&&owner.equals(a.account());}
  void render(){draw();if(!owner.isEmpty())load("",0,"");}
  LinearLayout card(int color){LinearLayout box=a.col();box.setPadding(a.dp(18),a.dp(16),a.dp(18),a.dp(16));box.setBackground(a.shape(color,24));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=a.dp(16);body.addView(box,lp);return box;}
  LinearLayout row(){LinearLayout r=new LinearLayout(a);r.setGravity(Gravity.CENTER_VERTICAL);return r;}
  Button light(String label,Runnable action){Button b=a.button(label,action);b.setTextSize(14);b.setTextColor(a.teal);b.setBackground(a.shape(a.pale,16));return b;}
  void draw(){
    body.removeAllViews();LinearLayout top=row();top.addView(a.text("课程表",28,true),new LinearLayout.LayoutParams(0,-2,1));Button refresh=light("手动刷新",()->load(term(),week(),""));top.addView(refresh,new LinearLayout.LayoutParams(a.dp(100),a.dp(42)));body.addView(top);a.gap(body,16);
    LinearLayout hero=card(a.pale);String[] termParts=term().split("-");String semester=termParts.length==3?termParts[0]+"—"+termParts[1]+" · 第"+termParts[2]+"学期":term();hero.addView(a.text(snapshot==null?"学校课程安排":semester,16,true));
    LinearLayout pager=row();hero.addView(pager);Button prev=light("‹",()->move(-1));pager.addView(prev,new LinearLayout.LayoutParams(a.dp(45),a.dp(50)));
    Button middle=a.button(snapshot==null?"待同步周次":"第 "+week()+" 周",()->chooseWeek());pager.addView(middle,new LinearLayout.LayoutParams(0,a.dp(50),1));Button next=light("›",()->move(1));pager.addView(next,new LinearLayout.LayoutParams(a.dp(45),a.dp(50)));
    status=a.text(snapshot==null?"登录后同步学校课程":"每天自动同步 · 缓存 "+new SimpleDateFormat("MM-dd HH:mm",Locale.CHINA).format(new Date(snapshot.optLong("synced"))),12,false);hero.addView(status);
    LinearLayout controls=row();Button now=light("返回本周",()->load("",0,""));controls.addView(now,new LinearLayout.LayoutParams(0,a.dp(42),1));Button mode=light(grid?"每日课程":"周课表",()->{grid=!grid;draw();});LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(0,a.dp(42),1);mp.leftMargin=a.dp(8);controls.addView(mode,mp);hero.addView(controls);
    refresh.setEnabled(!busy);prev.setEnabled(!busy&&snapshot!=null);next.setEnabled(!busy&&snapshot!=null);middle.setEnabled(snapshot!=null&&!busy);now.setEnabled(!busy);
    reminderStatus=a.text(CourseReminderManager.status(a),12,false);hero.addView(reminderStatus);hero.addView(light("课前提醒设置",()->ReminderSettings.show(a)));
    if(owner.isEmpty()){LinearLayout c=card(Color.WHITE);c.addView(a.text("先登录学校账号",21,true));c.addView(a.text("课程数据使用与今日校园一致的学校身份认证。",14,false));c.addView(a.button("前往登录",()->{a.selected="我的";a.home();a.loadLogin();}));return;}
    if(snapshot==null){LinearLayout c=card(Color.WHITE);c.addView(a.text("尚未同步课程",20,true));c.addView(a.text("点击“同步”读取真实课程；不会显示示例课程。",14,false));c.addView(light("重新验证登录",()->{a.selected="我的";a.home();a.loadLogin();}));return;}
    LinearLayout days=row();body.addView(days);String[] dayNames={"一","二","三","四","五","六","日"};
    for(int d=1;d<=7;d++){final int day=d;String date=date(d,"M/d");TextView t=a.text("周"+dayNames[d-1]+"\n"+date,12,true);t.setGravity(Gravity.CENTER);t.setTextColor(a.teal);boolean activeDay=d==selectedDay;t.setTextColor(activeDay?Color.WHITE:a.teal);t.setBackground(a.shape(activeDay?a.teal:Color.TRANSPARENT,16));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,a.dp(58),1);lp.setMargins(a.dp(1),0,a.dp(1),0);days.addView(t,lp);t.setOnClickListener(v->{selectedDay=day;grid=false;draw();});}
    a.gap(body,10);if(grid)drawGrid();else drawAgenda();
  }
  String term(){return snapshot==null?"":snapshot.optString("term");}int week(){return snapshot==null?0:snapshot.optInt("week");}
  int maxWeek(){return snapshot==null?1:Math.max(1,snapshot.optJSONObject("calendar").optInt("allTeachWeeks",1));}
  String date(int day,String pattern){try{return CourseRepository.date(snapshot.getJSONObject("calendar"),week(),day,pattern);}catch(Exception e){return "";}}
  static int[] section(JSONObject c){java.util.regex.Matcher m=java.util.regex.Pattern.compile("\\d+").matcher(c.optString("section"));int first=0,last=0;while(m.find()){int n=Integer.parseInt(m.group());if(first==0)first=n;last=n;}return new int[]{first,Math.max(first,last)};}
  List<JSONObject> courses(int day){List<JSONObject> list=new ArrayList<>();JSONArray array=snapshot.optJSONArray("courses");if(array!=null)for(int i=0;i<array.length();i++){JSONObject c=array.optJSONObject(i);if(c!=null&&c.optInt("weekday")==day)list.add(c);}Collections.sort(list,(x,y)->Integer.compare(section(x)[0],section(y)[0]));return list;}
  String room(JSONObject c){String r=(c.optString("position","")+" "+c.optString("classroom","")).trim();return r.isEmpty()?"教室未公布":r;}
  int color(JSONObject c){return colors[(c.optString("name").hashCode()&0x7fffffff)%colors.length];}
  void detail(JSONObject c){String teacher=c.optString("teacherName",c.optString("teacher",""));new AlertDialog.Builder(a).setTitle(c.optString("name")).setMessage(date(c.optInt("weekday"),"yyyy年M月d日")+"\n第 "+c.optString("section")+" 节\n"+room(c)+"\n"+c.optString("bjmc","")+(teacher.isEmpty()?"":"\n教师："+teacher)).setPositiveButton("关闭",null).show();}
  void drawGrid(){
    LinearLayout wrapper=a.col();wrapper.setPadding(a.dp(5),a.dp(12),a.dp(5),a.dp(12));wrapper.setBackground(a.shape(Color.WHITE,22));body.addView(wrapper);
    LinearLayout table=new LinearLayout(a);wrapper.addView(table);int rows=12;for(int d=1;d<=7;d++)for(JSONObject c:courses(d))rows=Math.min(24,Math.max(rows,section(c)[1]));
    int unit=a.dp(55);LinearLayout nums=a.col();table.addView(nums,new LinearLayout.LayoutParams(a.dp(22),rows*unit));for(int i=1;i<=rows;i++){TextView n=a.text(String.valueOf(i),11,false);n.setGravity(Gravity.CENTER);n.setTextColor(Color.rgb(126,151,157));nums.addView(n,new LinearLayout.LayoutParams(-1,unit));}
    int width=(a.getResources().getDisplayMetrics().widthPixels-a.dp(40+10+22))/7;
    for(int day=1;day<=7;day++){
      FrameLayout column=new FrameLayout(a);table.addView(column,new LinearLayout.LayoutParams(0,rows*unit,1));List<JSONObject> list=courses(day);List<Integer> lanes=new ArrayList<>();List<Integer> assigned=new ArrayList<>();
      for(JSONObject c:list){int[] sec=section(c);int lane=0;while(lane<lanes.size()&&lanes.get(lane)>=sec[0])lane++;if(lane==lanes.size())lanes.add(sec[1]);else lanes.set(lane,sec[1]);assigned.add(lane);}
      int laneCount=Math.max(1,lanes.size());for(int i=0;i<list.size();i++){JSONObject c=list.get(i);int[] sec=section(c);if(sec[0]<1||sec[1]>24)continue;TextView cell=a.text(c.optString("name")+"\n"+room(c),11,true);cell.setPadding(a.dp(3),a.dp(6),a.dp(3),a.dp(4));cell.setTextColor(a.teal);cell.setBackground(a.shape(color(c),10));cell.setMaxLines(Math.max(3,(sec[1]-sec[0]+1)*4));cell.setEllipsize(android.text.TextUtils.TruncateAt.END);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(Math.max(a.dp(10),width/laneCount-a.dp(3)),(sec[1]-sec[0]+1)*unit-a.dp(5));lp.topMargin=(sec[0]-1)*unit;lp.leftMargin=assigned.get(i)*width/laneCount+a.dp(1);column.addView(cell,lp);cell.setOnClickListener(v->detail(c));}
    }
    body.addView(a.text("点击课程查看详情 · 点击日期切换每日课程",12,false));
  }
  void drawAgenda(){
    body.addView(a.text(date(selectedDay,"M月d日")+" · 当日课程",20,true));List<JSONObject> list=courses(selectedDay);
    if(list.isEmpty()){LinearLayout c=card(Color.WHITE);c.addView(a.text("当天暂无课程",22,true));c.addView(a.text("该日期在学校返回的课表中没有课程安排。",14,false));return;}
    for(JSONObject c:list){LinearLayout box=card(Color.WHITE);LinearLayout stripe=row();box.addView(stripe);TextView badge=a.text("第 "+c.optString("section")+" 节",13,true);badge.setPadding(a.dp(10),a.dp(6),a.dp(10),a.dp(6));badge.setBackground(a.shape(color(c),11));stripe.addView(badge);box.addView(a.text(c.optString("name"),21,true));box.addView(a.text(room(c),15,false));box.addView(a.text(c.optString("bjmc",""),13,false));box.setOnClickListener(v->detail(c));}
  }
  void chooseWeek(){String[] weeks=new String[maxWeek()];for(int i=0;i<weeks.length;i++)weeks[i]="第 "+(i+1)+" 周";new AlertDialog.Builder(a).setTitle("选择教学周").setSingleChoiceItems(weeks,week()-1,(dialog,index)->{dialog.dismiss();load(term(),index+1,"");}).setNegativeButton("取消",null).show();}
  void move(int delta){int next=week()+delta;load(term(),next,next<1?"prev":next>maxWeek()?"next":"");}
  void load(String term,int week,String jump){
    if(owner.isEmpty()||!active())return;a.getSharedPreferences("courses",0).edit().putString(owner+"_lastRequestDay",DailySyncPolicy.day(System.currentTimeMillis())).apply();generation++;final int token=generation;if(worker!=null)worker.interrupt();
    if(week>0&&jump.isEmpty())try{String cached=a.getSharedPreferences("courses",0).getString(owner+"_"+term+"_"+week,"");if(!cached.isEmpty())snapshot=new JSONObject(cached);}catch(Exception ignored){}
    busy=true;draw();status.setText(week>0&&jump.isEmpty()?"正在读取第 "+week+" 周课表…":"正在同步学校课表…");
    final JSONObject known=snapshot==null?null:snapshot.optJSONObject("calendar");
    worker=new Thread(()->{try{JSONObject result=CourseRepository.load(term,week,jump,known);a.runOnUiThread(()->{if(!active()||token!=generation)return;busy=false;snapshot=result;a.getSharedPreferences("courses",0).edit().putString(owner+"_latest",result.toString()).putString(owner+"_"+result.optString("term")+"_"+result.optInt("week"),result.toString()).apply();CourseReminderManager.schedule(a);draw();status.setText("已同步 · "+new SimpleDateFormat("MM-dd HH:mm",Locale.CHINA).format(new Date()));});}catch(Exception e){a.runOnUiThread(()->{if(!active()||token!=generation)return;busy=false;draw();status.setText((snapshot==null?"":"保留缓存课表 · ")+(e.getMessage()==null?"同步失败，请重试":e.getMessage()));});}},"school-course-sync");worker.start();
  }
}

