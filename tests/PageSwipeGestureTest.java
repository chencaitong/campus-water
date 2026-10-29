package cn.local.campuswater;

public class PageSwipeGestureTest {
  static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
  public static void main(String[] args){
    PageSwipeGesture shortSwipe=new PageSwipeGesture(12,32,24);shortSwipe.begin(200,300,400);shortSwipe.move(160,302);check(shortSwipe.finish(160,302)==1,"32dp threshold accepts a short intentional swipe");
    PageSwipeGesture g=new PageSwipeGesture(12,72,24);
    g.begin(250,300,400);check(g.move(200,310),"claim horizontal drag");check(g.finish(130,312)==1,"left goes to next page");
    g.begin(100,300,400);check(g.move(150,299),"claim right drag");check(g.finish(220,302)==-1,"right goes to previous page");
    g.begin(200,300,400);check(!g.move(205,305),"tap jitter stays with child");check(g.finish(205,305)==0,"tap does not change page");
    g.begin(200,300,400);g.move(170,300);check(g.finish(150,300)==0,"short drag does not change page");
    g.begin(200,300,400);check(!g.move(202,350),"vertical scroll stays with child");check(!g.move(50,355),"vertical gesture cannot turn into paging");check(g.finish(50,355)==0,"vertical scroll never changes page");
    g.begin(10,300,400);check(!g.move(180,300),"left edge stays with system back");check(g.finish(220,300)==0,"left edge cannot page");
    g.begin(390,300,400);check(!g.move(200,300),"right edge stays with system back");check(g.finish(100,300)==0,"right edge cannot page");
    g.begin(200,300,400);g.move(150,300);g.cancel();check(g.finish(50,300)==0,"cancel or multiple pointers cannot page");
    g.begin(200,300,400);g.move(150,300);check(g.finish(40,500)==0,"diagonal finish rejected");
    g.begin(200,300,400);g.move(100,300);check(g.finish(210,300)==0,"reversed drag below threshold rejected");
    g.begin(200,300,400);g.move(100,300);check(g.finish(100,300)==1,"new gesture recovers after cancellation");check(g.finish(100,300)==0,"one gesture switches once");
    System.out.println("PASS: swipe direction, tap/short drag, vertical lock, edge back, cancellation, diagonals and single dispatch");
  }
}
