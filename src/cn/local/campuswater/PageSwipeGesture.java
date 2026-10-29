package cn.local.campuswater;

/** Direction locking keeps scrolling, taps and system back separate from paging. */
final class PageSwipeGesture {
  private final float slop, distance, edge;
  private float startX, startY;
  private boolean eligible, horizontal;

  PageSwipeGesture(float slop,float distance,float edge){this.slop=slop;this.distance=distance;this.edge=edge;}
  void begin(float x,float y,float width){startX=x;startY=y;horizontal=false;eligible=x>=edge&&x<=width-edge;}
  void cancel(){eligible=false;horizontal=false;}
  boolean move(float x,float y){
    if(!eligible)return false;
    if(horizontal)return true;
    float dx=Math.abs(x-startX),dy=Math.abs(y-startY);
    if(dy>slop&&dy>=dx){cancel();return false;}
    if(dx>slop&&dx>dy*1.5f)horizontal=true;
    return horizontal;
  }
  int finish(float x,float y){
    float dx=x-startX,dy=Math.abs(y-startY);
    int direction=eligible&&horizontal&&Math.abs(dx)>=distance&&Math.abs(dx)>dy*1.5f?(dx<0?1:-1):0;
    cancel();return direction;
  }
}
