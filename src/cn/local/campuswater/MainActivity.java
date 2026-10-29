package cn.local.campuswater;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.*;

public class MainActivity extends Activity {
  final int ink=Color.rgb(24,44,47), teal=Color.rgb(44,109,127), pale=Color.rgb(219,240,244), bg=Color.rgb(239,246,246);
  PageSwipeLayout mainPager; final LinearLayout[] pageRoots=new LinearLayout[4],pageBodies=new LinearLayout[4]; TextView[] navItems;
  CourseScreen courseScreen; LinearLayout root, body; WebView web; String selected="控制";
  int dp(float n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
  GradientDrawable shape(int c,int r){GradientDrawable d=new GradientDrawable(); d.setColor(c);d.setCornerRadius(dp(r));return d;}
  TextView text(String s,int size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(ink);if(bold)t.setTypeface(null,Typeface.BOLD);t.setPadding(0,dp(5),0,dp(5));return t;}
  LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
  LinearLayout card(int c){LinearLayout l=col();l.setPadding(dp(18),dp(16),dp(18),dp(16));l.setBackground(shape(c,24));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(16);body.addView(l,p);return l;}
  Button button(String s,Runnable fn){Button b=new Button(this);b.setText(s);b.setTextSize(16);b.setTextColor(Color.WHITE);b.setAllCaps(false);b.setBackground(shape(teal,17));b.setPadding(dp(16),dp(8),dp(16),dp(8));b.setOnClickListener(v->fn.run());return b;}
  void gap(LinearLayout l,int h){Space s=new Space(this);l.addView(s,new LinearLayout.LayoutParams(1,dp(h)));}
  @Override public void onCreate(Bundle b){super.onCreate(b);
    WebView.setWebContentsDebuggingEnabled(false);WaterSessionStore.read(this);
    try{JSONObject old=new JSONObject(prefs().getString("cardSession","{}"));old.remove("PerCode");old.remove("accInfo");prefs().edit().putString("cardSession",old.toString()).apply();}catch(Exception ignored){}
    if(Build.VERSION.SDK_INT>=33)setRecentsScreenshotEnabled(false);
    getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);home();
  }
  void home(){
    if(courseScreen!=null){courseScreen.cancel();courseScreen=null;}
    handler.removeCallbacksAndMessages(null);requestId++;if(engine!=null){engine.destroy();engine=null;}if(!syncScreen())stopDataSync();
    if(web!=null){web.stopLoading();web.destroy();web=null;}

    if("课表".equals(getIntent().getStringExtra("targetPage"))){selected="课表";getIntent().removeExtra("targetPage");}
    String requestedPage=selected;
    mainPager=new PageSwipeLayout(this,index->selectNativePage(index));FrameLayout shell=mainPager;shell.setBackgroundColor(bg);setContentView(shell);
    for(int pageIndex=0;pageIndex<PAGE_NAMES.length;pageIndex++){
    selected=PAGE_NAMES[pageIndex];root=col();pageRoots[pageIndex]=root;mainPager.addPage(root);
    if(dataWeb!=null&&pageIndex==0){if(dataWeb.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)dataWeb.getParent()).removeView(dataWeb);root.addView(dataWeb,new LinearLayout.LayoutParams(1,1));}
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    body=col();pageBodies[pageIndex]=body;body.setPadding(dp(20),dp(25),dp(20),dp(115));scroll.addView(body);
    TextView title=text(selected.equals("控制")?"饮水控制中心":selected.equals("记录")?"用水记录":selected.equals("课表")?"课程表":"我的",28,true);body.addView(title);gap(body,18);
    if(selected.equals("控制")){
      LinearLayout a=card(pale);a.addView(text("账号",13,false));accountLabel=text(prefs().getString("name","未登录"),21,true);a.addView(accountLabel);a.addView(text("山东医学高等专科学校",14,false));a.addView(text(account().isEmpty()?"请在“我的”中登录学校账号":"当前登录账号："+account(),12,false));gap(a,10);
      View line=new View(this);line.setBackgroundColor(Color.rgb(189,216,220));a.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));gap(a,10);balanceLabel=text("可用额度  — —",22,true);a.addView(balanceLabel);walletName=text("请登录后刷新钱包余额",13,false);a.addView(walletName);walletPicker=new Spinner(this);a.addView(walletPicker);syncStatus=text("",12,false);controlSyncStatus=syncStatus;a.addView(syncStatus);a.addView(button("刷新余额",()->syncAccount()));showWallets();
      LinearLayout d=card(Color.WHITE);d.addView(text("设备",20,true));d.addView(text("在设备上输入用水码，按 # 确认",14,false));gap(d,10);
      LinearLayout box=col();box.setPadding(dp(15),dp(14),dp(15),dp(14));box.setBackground(shape(Color.rgb(209,242,247),20));d.addView(box);
      box.addView(text("生活用水",21,true));box.addView(text("用水码 · 有效时间以服务返回为准",13,false));gap(box,12);codeButton=button("获取用水码",()->openWater());box.addView(codeButton);inlineInfo=text("",13,false);box.addView(inlineInfo);addCodeActions(box);
      LinearLayout t=card(Color.WHITE);t.addView(text("今天概览",20,true));todayLabel=text("已识别用水支出：— —",20,true);t.addView(todayLabel);t.addView(text("以学校已同步的消费账单为准",13,false));updateToday();
    }else if(selected.equals("记录")){
      LinearLayout c=card(Color.WHITE);c.addView(text("用水消费记录",20,true));c.addView(text("按当前账号保存学校账单，显示每笔金额和交易时间。",14,false));
      syncStatus=text("",13,false);recordSyncStatus=syncStatus;c.addView(syncStatus);c.addView(button("同步学校账单",()->syncAccount()));gap(c,8);
      Button filter=button(showAllRecords?"切换为用水账单":"查看全部消费",()->{showAllRecords=!showAllRecords;home();});c.addView(filter);
      c.addView(text("用水分类按学校交易类型和设备名称识别；若有遗漏，可查看全部消费核对。",12,false));
      recordList=col();body.addView(recordList);renderRecords();
    }else if(selected.equals("课表")){courseScreen=new CourseScreen(this);courseScreen.draw();
    }else{
      LinearLayout c=card(Color.WHITE);c.addView(text("学校账号登录",20,true));loginState=text(account().isEmpty()?"使用本人学校账号登录":("已登录："+prefs().getString("name",account())),16,true);c.addView(loginState);
      c.addView(text("使用与今日校园相同的学校统一认证账号登录，登录后连接一卡通与课程表。本应用不保存密码。",13,false));
      c.addView(button(account().isEmpty()?"今日校园学校账号登录":"重新验证学校登录",()->loadLogin()));gap(c,8);c.addView(button("退出并切换账号",()->logout()));gap(c,8);c.addView(button("清除本机记录与课表",()->confirmClearHistory()));
      loginContainer=col();body.addView(loginContainer);

    }
    }
    selected=requestedPage;addFloatingNav(shell);mainPager.showPage(java.util.Arrays.asList(PAGE_NAMES).indexOf(selected),false);

  }
  WebView engine; Handler handler=new Handler(); int attempt=0; boolean requested=false; Button codeButton,regenerateButton,stopButton; TextView inlineInfo; String operation="query"; int requestId=0;
  void addCodeActions(LinearLayout parent){
    LinearLayout row=new LinearLayout(this);gap(parent,8);parent.addView(row);
    regenerateButton=button("重新获取",()->startWater("regenerate"));stopButton=button("停止使用",()->startWater("stop"));
    regenerateButton.setTextSize(14);stopButton.setTextSize(14);stopButton.setTextColor(teal);stopButton.setBackground(shape(Color.rgb(233,246,249),15));
    LinearLayout.LayoutParams a=new LinearLayout.LayoutParams(0,dp(46),1);a.rightMargin=dp(8);row.addView(regenerateButton,a);row.addView(stopButton,new LinearLayout.LayoutParams(0,dp(46),1));
  }
  void captureSession(WebView w){
    String u=w.getUrl();if(u==null||!u.startsWith("http://111.14.213.18:7072/"))return;final int generation=authGeneration;final String owner=account();
    w.evaluateJavascript("JSON.stringify({uwcToken:localStorage.getItem('uwcToken'),uwcAccNum:localStorage.getItem('uwcAccNum'),uwcEpid:localStorage.getItem('uwcEpid')})",r->{try{if(generation!=authGeneration||!owner.equals(account())||owner.isEmpty()||isFinishing())return;String value=new JSONTokener(r).nextValue().toString();JSONObject j=new JSONObject(value);if(!j.isNull("uwcToken")&&!j.isNull("uwcAccNum")&&!j.isNull("uwcEpid"))WaterSessionStore.save(this,value);}catch(Exception ignored){}});
  }
  void finishInline(String title,String info){codeButton.setText(title);codeButton.setEnabled(true);if(regenerateButton!=null)regenerateButton.setEnabled(true);if(stopButton!=null)stopButton.setEnabled(true);inlineInfo.setText(info);}
  void openWater(){startWater("query");}
  void startWater(String action){
    if(account().isEmpty()){finishInline("获取用水码","请先在“我的”中登录学校账号");return;}
    operation=action;requestId++;regenerateButton.setEnabled(false);stopButton.setEnabled(false);
    handler.removeCallbacksAndMessages(null);if(engine!=null){engine.destroy();engine=null;}
    attempt=0;requested=false;codeButton.setEnabled(false);codeButton.setText(action.equals("stop")?"停止中…":"获取中…");inlineInfo.setText("正在连接学校服务");
    engine=newSchoolWeb();
    engine.setWebViewClient(new SchoolWebClient(){@Override public void onPageFinished(WebView w,String u){if(attempt==0)handler.postDelayed(()->pollCode(),1500);}});
    engine.loadUrl("http://ykt.sdmcjn.edu.cn:8081/water");
    handler.postDelayed(()->{if(engine!=null&&attempt==0)finishInline("点击重试","连接超时，请检查网络");},30000);
  }
  void pollCode(){
    if(engine==null||codeButton==null)return;
    attempt++;
    String js="(function(){var root=document.querySelector('#app');function find(v){if(!v)return null;if(v.$options&&v.$options.name==='waterCode')return v;var cs=v.$children||[];for(var i=0;i<cs.length;i++){var r=find(cs[i]);if(r)return r;}return null;}var v=find(root&&root.__vue__);if(!v&&root&&root.__vue__&&root.__vue__.$router&&localStorage.getItem('uwcToken')&&!window.__waterNavigated){window.__waterNavigated=true;root.__vue__.$router.replace('/waterCode');}window.__waterVM=v;var toast=document.querySelector('.van-toast__text');return JSON.stringify({ready:!!v,auth:!!localStorage.getItem('uwcToken'),acc:!!localStorage.getItem('uwcAccNum'),ep:!!localStorage.getItem('uwcEpid'),code:v?v.ranCode:'',minutes:v?v.validTime:0,total:v?v.ranValidTime:0,left:v?v.ranGetTimes:null,uses:v?v.ranValidTimes:null,interval:v?v.interval:0,done:!!window.__nativeDone,ok:!!window.__nativeActionOK,msg:toast?toast.innerText:''});})()";
    String saved=WaterSessionStore.read(this);
    js="(function(){if(location.host==='111.14.213.18:7072'){var saved="+saved+";Object.keys(saved).forEach(function(k){if(saved[k]&&!localStorage.getItem(k))localStorage.setItem(k,saved[k]);});}return "+js+";})()";
    final int activeRequest=requestId;
    engine.evaluateJavascript(js,result->{
      if(activeRequest!=requestId||isFinishing())return;
      try{
        String decoded=new JSONTokener(result).nextValue().toString();JSONObject state=new JSONObject(decoded);
        String code=state.optString("code","");String msg=state.optString("msg","");
        if(!operation.equals("query")){
          if(state.optBoolean("ready")&&state.has("left")&&!state.isNull("left")&&!requested){
            if(operation.equals("regenerate")){
              if(state.optInt("left",0)<=0){finishInline(code.isEmpty()?"获取用水码":code,"今日获取次数已用完");return;}
              if(state.optDouble("interval",0)>0){finishInline(code.isEmpty()?"获取用水码":code,"请等待 "+(int)Math.ceil(state.optDouble("interval",0))+" 秒再重新获取");return;}
            }
            if(operation.equals("stop")&&code.isEmpty()){finishInline("获取用水码","当前没有正在使用的用水码");return;}
            requested=true;
            String method=operation.equals("stop")?"handleImmediateEnd":"getCode";
            engine.evaluateJavascript("(async function(){var v=window.__waterVM,old=v.ranCode,stamp=v.ranApplyTime;try{await v."+method+"();window.__nativeActionOK="+(operation.equals("stop")?"!v.ranCode&&v.immediateEndFlag":"!!v.ranCode&&(v.ranCode!==old||v.ranApplyTime!==stamp)")+";}catch(e){window.__nativeActionOK=false;}window.__nativeDone=true;})()",null);
          }
          if(state.optBoolean("done")){
            if(!state.optBoolean("ok")){finishInline(code.isEmpty()?"获取用水码":code,msg.isEmpty()?"操作未成功，请稍后重试":msg);return;}
            if(operation.equals("stop")){finishInline("获取用水码","已停止使用，原用水码已结束");return;}
            operation="query";
          }
        }
        if(operation.equals("query")&&!code.isEmpty()&&!code.equals("null")&&state.optDouble("minutes",0)>0){
          captureSession(engine);finishInline(code,"剩余约 "+(int)Math.ceil(state.optDouble("minutes",0))+" 分钟 · 可用 "+state.optInt("uses",0)+" 次");return;
        }
        if(state.optBoolean("ready")&&(!state.optBoolean("auth")||!state.optBoolean("acc")||!state.optBoolean("ep"))){if(attempt>20){finishInline("点击重试","认证未完成，请在“我的”中打开生活用水后返回");return;}}
        if(operation.equals("query")&&state.has("left")&&!state.isNull("left")){
          int left=state.optInt("left",0);
          if(left<=0){finishInline("今日次数已用完","请明天再试");return;}
          if(!requested){
            requested=true;
            inlineInfo.setText("正在生成用水码…");engine.evaluateJavascript("window.__waterVM.getCode();void(0)",null);
          }
        }
        if(!msg.isEmpty()&&!msg.contains("加载")){inlineInfo.setText(msg);}
        if(attempt>=50){finishInline("点击重试",msg.isEmpty()?"获取失败，请在“我的”中检查登录状态":msg);return;}
      }catch(Exception e){if(attempt>=50){finishInline("点击重试","连接未完成，请稍后重试或在“我的”中检查登录");return;}}
      handler.postDelayed(()->pollCode(),700);
    });
  }
  TextView accountLabel,balanceLabel,walletName,syncStatus,todayLabel,loginState,controlSyncStatus,recordSyncStatus;
  Spinner walletPicker; LinearLayout recordList,loginContainer; boolean showAllRecords=false; int recordLimit=300;
  WebView dataWeb; Handler dataHandler=new Handler(); volatile int dataGeneration=0,authGeneration=0;String dataKnown="{}";
  android.content.SharedPreferences prefs(){return getSharedPreferences("account",0);}
  String account(){return prefs().getString("id","");}
  RecordStore records(){return new RecordStore(this);}
  void showWallets(){
    if(balanceLabel==null||walletPicker==null)return;
    try{
      JSONArray wallets=new JSONArray(prefs().getString("wallets","[]"));
      if(wallets.length()==0){balanceLabel.setText("可用额度  — —");walletName.setText("请登录并刷新余额");walletPicker.setVisibility(View.GONE);return;}
      String[] names=new String[wallets.length()];for(int i=0;i<wallets.length();i++){JSONObject w=wallets.getJSONObject(i);names[i]=w.optString("alias","")+" "+w.optString("ewalletName","钱包");}
      walletPicker.setVisibility(View.VISIBLE);ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names);walletPicker.setAdapter(adapter);
      walletPicker.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){} public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){try{JSONObject w=wallets.getJSONObject(pos);balanceLabel.setText("可用额度  ¥"+new java.math.BigDecimal(w.getString("ewalletMoney")).setScale(2,java.math.RoundingMode.HALF_UP).toPlainString());walletName.setText("上次同步："+prefs().getString("updated",""));}catch(Exception e){balanceLabel.setText("余额暂不可用");}}});
    }catch(Exception e){balanceLabel.setText("余额暂不可用");}
  }
  void renderRecords(){
    if(recordList==null)return;recordList.removeAllViews();String acc=account();
    if(acc.isEmpty()){recordList.addView(text("请先在“我的”中登录",16,false));return;}
    JSONArray rows;try(RecordStore store=records()){rows=store.list(acc,showAllRecords,recordLimit);}
    if(rows.length()==0){recordList.addView(text(showAllRecords?"暂无已同步消费账单":"暂无已识别的用水账单",16,false));return;}
    for(int i=0;i<rows.length();i++)try{JSONObject row=rows.getJSONObject(i);LinearLayout c=col();c.setPadding(dp(16),dp(10),dp(16),dp(10));c.setBackground(shape(Color.WHITE,18));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(10);recordList.addView(c,lp);
      c.addView(text((row.optDouble("signedAmount",-1)>0?"退回 +¥":"支出 ¥")+row.optString("amount"),20,true));c.addView(text(row.optString("date")+"  "+row.optString("time"),14,false));c.addView(text(row.optString("fee")+" · "+row.optString("location"),13,false));
    }catch(Exception ignored){}
    if(rows.length()>=recordLimit)recordList.addView(button("显示更多记录",()->{recordLimit+=300;renderRecords();}));
  }
  void updateToday(){
    if(todayLabel==null)return;if(account().isEmpty()){todayLabel.setText("登录后查看今日用水支出");return;}
    String date=new java.text.SimpleDateFormat("yyyy-MM-dd",java.util.Locale.CHINA).format(new java.util.Date());java.math.BigDecimal total=java.math.BigDecimal.ZERO;
    try(RecordStore store=records()){JSONArray rows=store.list(account(),false,20000);for(int i=0;i<rows.length();i++){JSONObject r=rows.getJSONObject(i);if(date.equals(r.optString("date"))&&r.optDouble("signedAmount")<0)total=total.add(new java.math.BigDecimal(r.getString("amount")));}}catch(Exception ignored){}
    todayLabel.setText("已识别用水支出：¥"+total.setScale(2,java.math.RoundingMode.HALF_UP).toPlainString());
  }
  WebView newSchoolWeb(){WebView w=new WebView(this);WebSettings s=w.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setUserAgentString("Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Mobile Safari/537.36");s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setAllowFileAccessFromFileURLs(false);s.setAllowUniversalAccessFromFileURLs(false);s.setSaveFormData(false);s.setSavePassword(false);s.setJavaScriptCanOpenWindowsAutomatically(false);CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(w,false);return w;}
  boolean cardOrigin(WebView w){String u=w.getUrl();if(u==null)return false;android.net.Uri uri=android.net.Uri.parse(u);return "ykt.sdmcjn.edu.cn".equals(uri.getHost())&&uri.getPort()==7248;}
  void captureCard(WebView w){
    if(!cardOrigin(w))return;final int generation=authGeneration;
    w.evaluateJavascript("(function(){var s={};['AccNum','CardEpid','Epid','AccName','PerCode','accInfo','CardAccNum','IsScp'].forEach(function(k){var v=localStorage.getItem(k);if(v!==null)s[k]=v;});return JSON.stringify(s);})()",r->{try{if(generation!=authGeneration||w!=web||isFinishing())return;JSONObject j=new JSONObject(new JSONTokener(r).nextValue().toString());String id=j.optString("AccNum","");if(id.isEmpty()||id.equals("null"))return;String name=j.optString("AccName","");if(name.isEmpty()&&j.has("accInfo"))name=new JSONObject(j.getString("accInfo")).optString("accName",id);if(name.isEmpty())name=id;
      if(!id.equals(account())){prefs().edit().clear().apply();getSharedPreferences("water",0).edit().clear().apply();}
      j.remove("PerCode");j.remove("accInfo");prefs().edit().putString("id",id).putString("name",name).putString("cardSession",j.toString()).apply();if(loginState!=null)loginState.setText("已登录："+name+"（"+id+"）\n可返回“控制”刷新余额与用水码");
    }catch(Exception ignored){}});
  }
  static final String COURSE_URL="http://ehall.sdmcjn.edu.cn/publicapp/sys/mykbxt/mobile/mySchoolTimetable/index.html";
  static final String CARD_ENTRY="http://ykt.sdmcjn.edu.cn:8081/homeIndexA";
  static final String AUTH_URL="http://authserver.sdmcjn.edu.cn/authserver/login?service="+android.net.Uri.encode(COURSE_URL);
  static final String[] PAGE_NAMES={"控制","记录","课表","我的"};
  void selectNativePage(int index){
    selected=PAGE_NAMES[index];root=pageRoots[index];body=pageBodies[index];syncStatus=index==0?controlSyncStatus:recordSyncStatus;updateNav();
    if(courseScreen!=null){if(index==2)courseScreen.enter();else courseScreen.leave();}
    if(syncScreen()){if(index==0){showWallets();updateToday();}else renderRecords();autoSyncAccount();}
    else{stopDataSync();hourlyHandler.removeCallbacks(hourlyRefresh);}
  }
  void updateNav(){if(navItems==null)return;for(int i=0;i<navItems.length;i++)if(navItems[i]!=null){boolean active=PAGE_NAMES[i].equals(selected);navItems[i].setTextColor(active?teal:Color.rgb(116,137,143));navItems[i].setBackground(active?shape(pale,23):null);}}
  void addFloatingNav(FrameLayout shell){
    LinearLayout nav=new LinearLayout(this);nav.setPadding(dp(7),dp(7),dp(7),dp(7));GradientDrawable panel=shape(Color.WHITE,30);panel.setStroke(dp(1),Color.rgb(219,234,237));nav.setBackground(panel);nav.setElevation(dp(12));
    FrameLayout.LayoutParams position=new FrameLayout.LayoutParams(-1,dp(74),Gravity.BOTTOM);position.setMargins(dp(20),0,dp(20),dp(16));shell.addView(nav,position);
    navItems=new TextView[4];String[] names=PAGE_NAMES;String[] symbols={"⌂","≡","▦","●"};
    for(int i=0;i<names.length;i++){final int pageIndex=i;final String name=names[i];TextView item=text(symbols[i]+"\n"+name,14,true);item.setGravity(Gravity.CENTER);item.setLineSpacing(dp(2),1);item.setTextColor(selected.equals(name)?teal:Color.rgb(116,137,143));if(selected.equals(name))item.setBackground(shape(pale,23));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);lp.setMargins(dp(2),0,dp(2),0);nav.addView(item,lp);navItems[i]=item;item.setOnClickListener(v->{if(mainPager!=null&&web==null)mainPager.showPage(pageIndex,true);else{selected=name;home();}});}
  }
  boolean allowedSchoolPage(android.net.Uri u){return SchoolUrlPolicy.allowed(u.toString());}
  void showSchoolPortal(String url,String title,boolean signingIn){
    mainPager=null;
    handler.removeCallbacksAndMessages(null);stopDataSync();requestId++;if(engine!=null){engine.destroy();engine=null;}
    if(web!=null){WebView old=web;captureCard(old);captureSession(old);web=null;old.destroy();}
    FrameLayout shell=new FrameLayout(this);shell.setBackgroundColor(bg);root=col();root.setPadding(dp(10),dp(8),dp(10),dp(104));shell.addView(root,new FrameLayout.LayoutParams(-1,-1));setContentView(shell);
    LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);root.addView(heading);TextView label=text(title,23,true);heading.addView(label,new LinearLayout.LayoutParams(0,-2,1));Button reload=button("刷新",()->{if(web!=null)web.reload();});heading.addView(reload,new LinearLayout.LayoutParams(dp(70),dp(42)));
    TextView state=text(signingIn?"使用今日校园对应的学校统一身份认证":"学校官方课程表 · 与本应用共享学校认证",12,false);state.setPadding(dp(8),dp(8),dp(8),dp(10));root.addView(state);loginState=state;
    web=newSchoolWeb();WebView portal=web;root.addView(portal,new LinearLayout.LayoutParams(-1,0,1));addFloatingNav(shell);
    final int generation=authGeneration;final boolean[] cardStarted={false};
    portal.setWebViewClient(new SchoolWebClient(){
      public boolean shouldOverrideUrlLoading(WebView w,WebResourceRequest req){android.net.Uri u=req.getUrl();String scheme=u.getScheme();if(("http".equals(scheme)||"https".equals(scheme))&&allowedSchoolPage(u))return false;state.setText("页面请求打开学校以外的应用，已保留当前页面");return true;}
      public void onPageFinished(WebView w,String u){
        if(generation!=authGeneration||web!=portal)return;CookieManager.getInstance().flush();android.net.Uri address=android.net.Uri.parse(u);String host=address.getHost();
        if("authserver.sdmcjn.edu.cn".equals(host)){state.setText("学校官方认证页 · 请使用与今日校园一致的账号密码");return;}
        if(signingIn&&"ehall.sdmcjn.edu.cn".equals(host)&&!cardStarted[0]){cardStarted[0]=true;state.setText("学校认证成功，正在连接一卡通…");w.loadUrl(CARD_ENTRY);return;}
        if(cardOrigin(w)&&address.getPath()!=null&&address.getPath().endsWith("/getAuthInfo.html"))w.evaluateJavascript("(function(){if(window.urlParams&&urlParams.IDNo&&urlParams.etToken&&window.modNumMap&&!modNumMap[urlParams.jumpMod]){var u=location.href;if(/([?&])jumpMod=/.test(u))u=u.replace(/([?&])jumpMod=[^&]*/,'$1jumpMod=homeIndex');else u+='&jumpMod=homeIndex';location.replace(u);}})()",null);
        captureCard(w);captureSession(w);if(!signingIn)state.setText("可在学校课表内切换学期、周次并查看课程详情");
        for(int delay:new int[]{1200,3000,6000,10000})dataHandler.postDelayed(()->{if(generation==authGeneration&&web==portal){captureCard(portal);captureSession(portal);}},delay);
      }
      public void onReceivedError(WebView w,WebResourceRequest req,WebResourceError e){if(req.isForMainFrame())state.setText("学校服务连接失败，请检查网络后点击刷新");}
      public void onReceivedSslError(WebView w,android.webkit.SslErrorHandler h,android.net.http.SslError e){h.cancel();state.setText("学校认证证书校验失败，已停止连接");}
    });portal.loadUrl(url);
  }

  void loadLogin(){showSchoolPortal(AUTH_URL,"学校统一认证",true);}
  void confirmClearHistory(){new AlertDialog.Builder(this).setTitle("清除本机缓存？").setMessage("删除本应用保存的所有账号历史账单和课表，不影响学校服务器记录，也不会退出当前登录。").setNegativeButton("取消",null).setPositiveButton("清除",(dialog,which)->{
    CourseReminderManager.cancel(this);if(courseScreen!=null)courseScreen.cancel();stopDataSync();getSharedPreferences("courses",0).edit().clear().apply();
    try(RecordStore store=records()){store.clear();}home();Toast.makeText(this,"本机记录和课表已清除",Toast.LENGTH_SHORT).show();
  }).show();}
  void logout(){
    CourseReminderManager.cancel(this);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancelAll();
    authGeneration++;requestId++;showAllRecords=false;recordLimit=300;stopDataSync();handler.removeCallbacksAndMessages(null);if(engine!=null){engine.destroy();engine=null;}if(web!=null){web.clearCache(true);web.clearHistory();web.clearFormData();web.destroy();web=null;}
    prefs().edit().clear().apply();getSharedPreferences("water",0).edit().clear().apply();WebStorage.getInstance().deleteAllData();new java.io.File(getFilesDir(),"school-session-import.json").delete();
    CookieManager.getInstance().removeAllCookies(ok->{CookieManager.getInstance().flush();selected="我的";home();});
  }
  void stopDataSync(){dataGeneration++;dataHandler.removeCallbacksAndMessages(null);if(dataWeb!=null){if(dataWeb.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)dataWeb.getParent()).removeView(dataWeb);dataWeb.destroy();dataWeb=null;}}
  final Handler hourlyHandler=new Handler();boolean foreground=false;
  final Runnable hourlyRefresh=()->autoSyncAccount();
  boolean syncScreen(){return selected.equals("控制")||selected.equals("记录");}
  void scheduleHourly(){hourlyHandler.removeCallbacks(hourlyRefresh);if(foreground&&syncScreen()&&!account().isEmpty())hourlyHandler.postDelayed(hourlyRefresh,Math.max(1000,SyncPolicy.delay(System.currentTimeMillis(),prefs().getLong("lastRequestMs",0))));}
  void autoSyncAccount(){
    if(account().isEmpty()||!syncScreen()){scheduleHourly();return;}
    if(dataWeb==null&&SyncPolicy.due(System.currentTimeMillis(),prefs().getLong("lastRequestMs",0)))syncAccount();
    else if(dataWeb==null&&syncStatus!=null)syncStatus.setText("每小时自动同步 · 可手动刷新"+(prefs().getString("updated","").isEmpty()?"":" · 上次 "+prefs().getString("updated","")));
    scheduleHourly();
  }
  @Override protected void onResume(){super.onResume();foreground=true;CourseReminderManager.schedule(this);autoSyncAccount();if(courseScreen!=null&&selected.equals("课表"))courseScreen.enter();}
  @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] grants){super.onRequestPermissionsResult(code,permissions,grants);if(code==300){CourseReminderManager.schedule(this);if(courseScreen!=null)courseScreen.updateReminderStatus();}}
  @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);if("课表".equals(intent.getStringExtra("targetPage"))){intent.removeExtra("targetPage");if(mainPager!=null&&web==null)mainPager.showPage(2,true);else{selected="课表";home();}}}
  @Override protected void onPause(){foreground=false;if(courseScreen!=null)courseScreen.leave();hourlyHandler.removeCallbacks(hourlyRefresh);super.onPause();}
  void syncAccount(){
    if(account().isEmpty()){if(syncStatus!=null)syncStatus.setText("请先在“我的”中登录学校账号");return;}
    if(dataWeb!=null){if(syncStatus!=null)syncStatus.setText("正在同步，请稍候…");return;}
    prefs().edit().putLong("lastRequestMs",System.currentTimeMillis()).apply();scheduleHourly();
    stopDataSync();final int generation=dataGeneration;final String owner=account();dataKnown=knownBillDetails();if(syncStatus!=null)syncStatus.setText("正在同步学校钱包与消费账单…");
    dataWeb=newSchoolWeb();dataWeb.setAlpha(0);dataWeb.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);root.addView(dataWeb,new LinearLayout.LayoutParams(1,1));dataWeb.setWebViewClient(new SchoolWebClient(){public void onPageFinished(WebView w,String u){if(generation==dataGeneration)pollAccount(generation,owner,0);}});
    dataWeb.loadUrl("http://ykt.sdmcjn.edu.cn:7248/easytong_webapp/index.html#/home/index");
    dataHandler.postDelayed(()->{if(generation==dataGeneration&&dataWeb!=null){if(syncStatus!=null)syncStatus.setText("同步超时，保留上次记录；请稍后重试");stopDataSync();}},90000);
  }
  String rawSyncScript(){try(java.io.InputStream in=getResources().openRawResource(getResources().getIdentifier("account_sync","raw",getPackageName()));java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toString("UTF-8");}catch(Exception e){return "";}}
  String knownBillDetails(){JSONObject known=new JSONObject();try(RecordStore store=records()){JSONArray rows=store.list(account(),true,20000);for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);if(row.optBoolean("detailChecked")&&!row.optString("id").isEmpty()){JSONObject d=new JSONObject();d.put("detailChecked",true);d.put("location",row.optString("location"));d.put("water",row.optBoolean("water"));known.put(row.getString("id"),d);}}}catch(Exception ignored){}return known.toString();}
  void pollAccount(int generation,String owner,int tries){
    if(generation!=dataGeneration||dataWeb==null||!owner.equals(account()))return;
    if(!cardOrigin(dataWeb)){if(tries<90)dataHandler.postDelayed(()->pollAccount(generation,owner,tries+1),750);return;}
    String saved=prefs().getString("cardSession","{}");String script="(function(){var saved="+saved+";Object.keys(saved).forEach(function(k){if(!localStorage.getItem(k))localStorage.setItem(k,saved[k]);});if(!window.__knownBillDetails)window.__knownBillDetails="+dataKnown+";"+rawSyncScript()+";return JSON.stringify(window.__accountResult||{state:'waiting'});})()";
    dataWeb.evaluateJavascript(script,r->{if(generation!=dataGeneration||!owner.equals(account()))return;
      try{JSONObject result=new JSONObject(new JSONTokener(r).nextValue().toString());String state=result.optString("state");if(state.equals("loading")&&syncStatus!=null)syncStatus.setText("正在同步"+result.optString("stage","钱包")+" · 已读取 "+result.optInt("loaded",0)+" 笔");if(state.equals("loading")&&result.has("wallets")){prefs().edit().putString("wallets",result.getJSONArray("wallets").toString()).apply();if(selected.equals("控制"))showWallets();}if(state.equals("error")){if(syncStatus!=null)syncStatus.setText(result.optString("message")+"，可在“我的”中重新登录");stopDataSync();return;}
        if(state.equals("ok")){
          if(!owner.equals(result.optString("account"))){if(syncStatus!=null)syncStatus.setText("账号状态已改变，请重新登录");stopDataSync();return;}
          JSONArray rows=result.getJSONArray("records");String updated=new java.text.SimpleDateFormat("MM-dd HH:mm",java.util.Locale.CHINA).format(new java.util.Date());prefs().edit().putString("wallets",result.getJSONArray("wallets").toString()).putString("updated",updated).putString("cardSession",result.getJSONObject("session").toString()).apply();
          new Thread(()->{try(RecordStore store=records()){synchronized(RecordStore.class){if(generation!=dataGeneration||!owner.equals(account()))return;store.merge(owner,rows);}runOnUiThread(()->{if(generation!=dataGeneration||!owner.equals(account()))return;prefs().edit().putLong("lastSuccessMs",System.currentTimeMillis()).apply();if(syncStatus!=null)syncStatus.setText("已同步 · "+updated+(result.optBoolean("complete")?"":" · 更多历史待同步"));if(selected.equals("控制")){showWallets();updateToday();}if(selected.equals("记录"))renderRecords();stopDataSync();});}catch(Exception e){runOnUiThread(()->{if(generation==dataGeneration&&syncStatus!=null)syncStatus.setText("保存账单失败，请重试");});}},"save-school-bills").start();return;
        }
      }catch(Exception ignored){}
      if(tries<90)dataHandler.postDelayed(()->pollAccount(generation,owner,tries+1),750);
    });
  }

  @Override public void onBackPressed(){
    if(web!=null){if(selected.equals("我的")||selected.equals("课表")){selected="控制";home();return;}if(web.canGoBack())web.goBack();else home();return;}
    if(!selected.equals("控制")){if(mainPager!=null)mainPager.showPage(0,true);else{selected="控制";home();}return;}
    finish();
  }
  @Override protected void onDestroy(){hourlyHandler.removeCallbacksAndMessages(null);if(courseScreen!=null)courseScreen.cancel();stopDataSync();handler.removeCallbacksAndMessages(null);requestId++;if(engine!=null){engine.destroy();engine=null;}if(web!=null){web.destroy();web=null;}super.onDestroy();}
}






