package cn.local.campuswater;
final class PageNavigation {
  static int target(float start,float position,int direction,float velocity,int width,int count){
    if(count<1)return 0;int result=Math.round(start)+(direction>0?1:direction<0?-1:0);
    return Math.max(0,Math.min(count-1,result));
  }
  private PageNavigation(){}
}
