package cn.local.campuswater;
import android.animation.ValueAnimator;
import android.content.Context;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import java.util.ArrayList;

/** Keeps all four pages mounted; new touches interrupt settling immediately. */
final class PageSwipeLayout extends FrameLayout {
  interface Listener {void onSelected(int index);}
  private final FrameLayout content;
  private final ArrayList<View> pages=new ArrayList<>();
  private final PageSwipeGesture gesture;
  private final Listener listener;
  private VelocityTracker velocity;
  private ValueAnimator animator;
  private float position,startPosition,startX;
  private int selectedIndex,gesturePage;
  private boolean interrupted;
  PageSwipeLayout(Context context,Listener listener){
    super(context);this.listener=listener;float density=getResources().getDisplayMetrics().density;
    gesture=new PageSwipeGesture(Math.max(ViewConfiguration.get(context).getScaledTouchSlop(),12*density),32*density,24*density);
    content=new FrameLayout(context);addView(content,new LayoutParams(-1,-1));
  }
  void addPage(View page){pages.add(page);content.addView(page,new LayoutParams(-1,-1));}
  void showPage(int index,boolean animate){
    index=Math.max(0,Math.min(pages.size()-1,index));cancelAnimation();selectedIndex=index;listener.onSelected(index);
    if(!animate||getWidth()==0){position=index;placePages();return;}
    animator=ValueAnimator.ofFloat(position,index);animator.setDuration(Math.min(320,160+(int)(Math.abs(position-index)*70)));
    animator.setInterpolator(new DecelerateInterpolator(1.4f));
    animator.addUpdateListener(value->{position=(float)value.getAnimatedValue();placePages();});animator.start();
  }
  void cancelAnimation(){if(animator!=null){animator.cancel();animator=null;}}
  private void placePages(){
    for(int i=0;i<pages.size();i++){View page=pages.get(i);float offset=i-position;
      page.setTranslationX(offset*getWidth());page.setVisibility(Math.abs(offset)<1.05f?VISIBLE:INVISIBLE);
      page.setImportantForAccessibility(Math.abs(offset)<.5f?IMPORTANT_FOR_ACCESSIBILITY_AUTO:IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
    }
  }
  @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);placePages();}
  @Override public boolean dispatchTouchEvent(MotionEvent event){
    switch(event.getActionMasked()){
      case MotionEvent.ACTION_DOWN:
        interrupted=animator!=null&&animator.isRunning();cancelAnimation();
        startX=event.getX();startPosition=position;gesturePage=selectedIndex;gesture.begin(startX,event.getY(),getWidth());
        if(velocity!=null)velocity.recycle();velocity=VelocityTracker.obtain();break;
      case MotionEvent.ACTION_MOVE:
        if(gesture.move(event.getX(),event.getY()))super.requestDisallowInterceptTouchEvent(false);break;
      case MotionEvent.ACTION_POINTER_DOWN:gesture.cancel();showPage(gesturePage,true);break;
    }
    if(velocity!=null)velocity.addMovement(event);
    boolean handled=super.dispatchTouchEvent(event);
    if(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL){if(velocity!=null){velocity.recycle();velocity=null;}}
    return handled;
  }
  @Override public boolean onInterceptTouchEvent(MotionEvent event){
    switch(event.getActionMasked()){
      case MotionEvent.ACTION_DOWN:return interrupted&&event.getY()<getHeight()-100*getResources().getDisplayMetrics().density;
      case MotionEvent.ACTION_MOVE:return gesture.move(event.getX(),event.getY());
      case MotionEvent.ACTION_CANCEL:gesture.cancel();return false;
      default:return false;
    }
  }
  @Override public boolean onTouchEvent(MotionEvent event){
    switch(event.getActionMasked()){
      case MotionEvent.ACTION_MOVE:
        if(gesture.move(event.getX(),event.getY())){
          float next=startPosition-(event.getX()-startX)/Math.max(1,getWidth()),last=pages.size()-1;
          next=Math.max(gesturePage-1,Math.min(gesturePage+1,next));
          position=next<0?next*.18f:next>last?last+(next-last)*.18f:next;placePages();
        }break;
      case MotionEvent.ACTION_UP:
        int direction=gesture.finish(event.getX(),event.getY());float speed=0;
        if(velocity!=null){velocity.computeCurrentVelocity(1000);speed=velocity.getXVelocity();}
        showPage(PageNavigation.target(gesturePage,position,direction,speed,getWidth(),pages.size()),true);break;
      case MotionEvent.ACTION_CANCEL:gesture.cancel();showPage(gesturePage,true);break;
    }
    return true;
  }
  @Override protected void onDetachedFromWindow(){cancelAnimation();if(velocity!=null){velocity.recycle();velocity=null;}super.onDetachedFromWindow();}
}
