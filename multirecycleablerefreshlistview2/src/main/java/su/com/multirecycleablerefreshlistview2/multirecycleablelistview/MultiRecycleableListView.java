package su.com.multirecycleablerefreshlistview2.multirecycleablelistview;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.List;

import su.com.multirecycleablerefreshlistview2.R;

public class MultiRecycleableListView extends ViewGroup {

    private MultiRecycleableAdapter adapter;
    private int offsetY;
    private int usedHeight = 0;
    private int maxLength=0;
    private Scroller scroller = new Scroller(getContext());
    private boolean softEdge=true;//软化边界
    private float softRatio=0.5f;//软化比例
    private List<MultiRecycleableView> recycleableItems = new ArrayList<>();//回收的item列表
    private int topItemPosition = 0;//上方item对应数据中的位置
    private int bottomItemPosition;//下方item对应数据中的位置
    private enum TopState {//顶部状态
        TopNew, TopNormal
    }
    private TopState topState = TopState.TopNormal;//初始状态为普通
    private TopState lastTopState= TopState.TopNormal;//上一个状态
    private enum BottomState {
        BottomNew, BottomNormal
    }
    private BottomState bottomState = BottomState.BottomNormal;
    private BottomState lastBottomState= BottomState.BottomNormal;
    private int widthMode, widthSize, heightMode, heightSize;//测量需要用到
    private boolean isFirstMeasure = true, isFirstLayout = true;//第一次测量和布局

    public void setAdapter(MultiRecycleableAdapter adapter) {
        this.adapter = adapter;
    }

    public MultiRecycleableListView(Context context) {
        this(context, null);
    }

    public MultiRecycleableListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MultiRecycleableListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context,attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray typedArray=null;
        try {
            typedArray=context.obtainStyledAttributes(attrs, R.styleable.MultiRecycleableListView);
            int count=typedArray.getIndexCount();
            for(int i=0;i<count;i++) {
                if(typedArray.getIndex(i)==R.styleable.MultiRecycleableListView_softEdge) {
                    softEdge = typedArray.getBoolean(typedArray.getIndex(i), false);
                }else if(typedArray.getIndex(i)==R.styleable.MultiRecycleableListView_softRatio){
                    softRatio=typedArray.getFloat(typedArray.getIndex(i),0.5f);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(typedArray!=null)
                typedArray.recycle();
        }
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (adapter == null) return;
        if (!isFirstMeasure) {
            return;
        }
        widthMode = MeasureSpec.getMode(widthMeasureSpec);
        widthSize = MeasureSpec.getSize(widthMeasureSpec);
        heightMode = MeasureSpec.getMode(heightMeasureSpec);
        heightSize = MeasureSpec.getSize(heightMeasureSpec);
        measureAllChildren();
        isFirstMeasure = false;
    }

    private void measureAllChildren() {
        int len = adapter.getCount();
        usedHeight = 0;
        int index = 0;
        while (index < len && usedHeight < heightSize) {
            MultiRecycleableView child = adapter.getView(index, null,this);
            measureAItem(widthMode, widthSize, heightMode, heightSize, child.view);//测量一个view
            usedHeight += child.view.getMeasuredHeight();
            child.left = 0;
            child.right = child.view.getMeasuredWidth();
            child.top = usedHeight - child.view.getMeasuredHeight();
            child.bottom = usedHeight;
            child.isOutOfLayout = false;
            child.dataPosition = index;
            child.viewPosition = index;
            child.viewType=adapter.getViewType(index);
            child.isTop = false;
            child.isBottom = false;
            if (index == 0) {
                child.isTop = true;
                child.isBottom = false;
            }
            if (usedHeight >= heightSize) {
                child.isTop = false;
                child.isBottom = true;
            }
            addViewToContainer(child.view);
            //添加进回收集合
            addViewToRecyclers(child);
            index++;
        }
        maxLength=usedHeight;//当前最大访问的视图长度（用来防止上滑后下滑再上滑导致usedHeight错误增加）
        bottomItemPosition = index - 1;
        setMeasuredDimension(widthSize, heightSize);
    }

    void measureAItem(int widthMode, int widthSize, int heightMode, int heightSize,View child) {
        int wSpec, hSpec;
        LayoutParams params = child.getLayoutParams();
        if (params == null)
            params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        switch (params.width) {
            case LayoutParams.MATCH_PARENT:
                if (widthMode == MeasureSpec.AT_MOST || widthMode == MeasureSpec.EXACTLY) {
                    wSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);
                } else {
                    wSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
                }
                break;
            case LayoutParams.WRAP_CONTENT:
                if (widthMode == MeasureSpec.AT_MOST || widthMode == MeasureSpec.EXACTLY) {
                    wSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.AT_MOST);
                } else {
                    wSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
                }
                break;
            default:
                wSpec = MeasureSpec.makeMeasureSpec(params.width, MeasureSpec.EXACTLY);
                break;
        }
        switch (params.height) {
            case LayoutParams.MATCH_PARENT:
                if (heightMode == MeasureSpec.AT_MOST || heightMode == MeasureSpec.EXACTLY) {
                    hSpec = MeasureSpec.makeMeasureSpec(heightSize - usedHeight, MeasureSpec.EXACTLY);
                } else {
                    hSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
                }
                break;
            case LayoutParams.WRAP_CONTENT:
                if (heightMode == MeasureSpec.AT_MOST || heightMode == MeasureSpec.EXACTLY) {
                    hSpec = MeasureSpec.makeMeasureSpec(heightSize - usedHeight, MeasureSpec.AT_MOST);
                } else {
                    hSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
                }
                break;
            default:
                hSpec = MeasureSpec.makeMeasureSpec(params.height, MeasureSpec.EXACTLY);
                break;
        }
        child.measure(wSpec, hSpec);
    }

    @Override
    protected void onLayout(boolean changed, final int l, final int t, final int r, final int b) {
        if (adapter == null) return;
        if (!isFirstLayout) {
            return;
        }
        if (!changed) return;
        layoutAllChildren(l, t, r, b);
        isFirstLayout = false;
    }

    private void layoutAllChildren(int l, int t, int r, int b) {
        for (int i = 0; i < recycleableItems.size(); i++) {
            MultiRecycleableView child = recycleableItems.get(i);
            layoutAItem(child);
        }
    }

    private void layoutAItem(MultiRecycleableView child) {
        child.view.layout(child.left, child.top, child.right, child.bottom);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean flag = false;
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                flag = false;
                break;
            case MotionEvent.ACTION_MOVE:
                flag = true;
                break;
        }
        return flag;
    }

    private int touchX, touchY, lastTouchX, lastTouchY;
    private int scrollX, scrollY;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        boolean flag = true;
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = (int) ev.getX();
                lastTouchY = (int) ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                touchX = (int) ev.getX();
                touchY = (int) ev.getY();
                scrollX = touchX - lastTouchX;
                scrollY = touchY - lastTouchY;
                lastTouchX = touchX;
                lastTouchY = touchY;
                float curScrollY = getScrollY();
                if (-curScrollY > 0 || -curScrollY < -usedHeight + getMeasuredHeight()) {
                    if(softEdge) {
                        scrollBy(0, (int) (-scrollY * softRatio));
                    }
                } else {
                    scrollBy(0, -scrollY);
                }
                try {
                    if (scrollY < 0) {//向上滑动，下面可视范围外的view变为可见，上面可见view变不可见
                        detactViewState(true);
                    } else {//向下滑动，上面可视范围外的view变为可见，下面可见view变不可见
                        detactViewState(false);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (-getScrollY() > 0) {
                    scroller.startScroll(0, getScrollY(), 0,
                            0 - getScrollY(), 300);
                } else if (-getScrollY() < getMeasuredHeight() - usedHeight) {
                    scroller.startScroll(0, getScrollY(), 0,
                            0 - getScrollY() - getMeasuredHeight() + usedHeight, 300);
                }
                break;
        }
        offsetY = getScrollY();
        invalidate();
        return flag;
    }

    private void detactViewState(boolean isUp) {
        if(isUp){//上滑
            if(getTopView()==null) return;
            if(getTopView().bottom-getScrollY()<=0){
                //最顶部可见item离开可视范围状态
                topState= TopState.TopNew;
            }else{
                //普通状态
                topState= TopState.TopNormal;
            }
            if(lastTopState!=topState){//顶部当前状态和先前状态不一致，触发事件
                lastTopState=topState= TopState.TopNormal;
                if(isItemOutOfLayout(getTopView())){
                    getTopView().isOutOfLayout=true;
                    removeViewFromContainer(getTopView().view);
                    getTopView().dataPosition=-1;
                }
                topItemPosition+=1;
            }
            if(bottomItemPosition>=adapter.getCount()-1) return;
            if(getBottomView()==null) return;
            if(getBottomView().bottom-getScrollY()<=getMeasuredHeight()){
                //最底部可见item的底边向上离开控件底部边界，设置状态为新的item进入
                bottomState = BottomState.BottomNew;
            }else{
                //否则为普通状态
                bottomState = BottomState.BottomNormal;
            }
            if(lastBottomState!=bottomState){//底部当前状态和先前状态不一致，触发事件
                lastBottomState=bottomState= BottomState.BottomNormal;
                MultiRecycleableView newView=getRecycleableView(bottomItemPosition+1);
                if(newView!=null){//找到可复用的item，复用
                    newView=adapter.getView(bottomItemPosition+1,newView,this);//将当前view传入adapter，设置新的数据
                    newView.isTop=false;//不是顶部
                    newView.isBottom=true;//是底部
                    newView.left=0;//设置左位置
                    newView.dataPosition=bottomItemPosition+1;
                    measureAItem(widthMode,widthSize,heightMode,heightSize,newView.view);//测量
                    newView.right=newView.view.getMeasuredWidth();//设置右位置
                    newView.top= getBottomView().bottom;//设置上位置
                    newView.isOutOfLayout=false;
                    newView.viewType=adapter.getViewType(bottomItemPosition+1);
                    newView.isTop=false;
                    newView.bottom= getBottomView().bottom+newView.view.getMeasuredHeight();//设置下位置
                    addViewToContainer(newView.view);//添加进container
                    layoutAItem(newView);//布局
                    newView.viewPosition=getChildCount()-1;
                    if(Math.abs(getScrollY())+getMeasuredHeight()>=maxLength) {
                        usedHeight += newView.view.getMeasuredHeight();//使用长度增加
                        maxLength=usedHeight;//防止usedHeight错误增加
                    }
                    getBottomView().isBottom=false;//上一个item不再是底部
                }else{//没找到可复用的item，从adapter那里获取一个新的已经绑定数据的item
                    newView=adapter.getView(bottomItemPosition+1,null,this);//获取一个新的item
                    newView.viewType=adapter.getViewType(bottomItemPosition+1);
                    newView.isBottom=true;//是底部
                    newView.isTop=false;//不是顶部
                    newView.isOutOfLayout=false;
                    newView.top= getBottomView().bottom;//设置顶部位置
                    measureAItem(widthMode,widthSize,heightMode,heightSize,newView.view);//测量
                    newView.bottom= getBottomView().bottom+newView.view.getMeasuredHeight();//设置底部位置
                    newView.left=0;//设置左边位置
                    newView.right=newView.view.getMeasuredWidth();//设置右边位置
                    newView.dataPosition=bottomItemPosition+1;//设置数据位置
                    layoutAItem(newView);//布局
                    addViewToContainer(newView.view);//添加进container
                    newView.viewPosition=getChildCount()-1;
                    if(Math.abs(getScrollY())+getMeasuredHeight()>=maxLength) {
                        usedHeight += newView.view.getMeasuredHeight();//使用长度增加
                        maxLength=usedHeight;
                    }
                    getBottomView().isBottom=false;//上一个item不再是底部
                    addViewToRecyclers(newView);//添加进复用列表
                }
                bottomItemPosition+=1;
            }
        }else{//下滑
            if(getBottomView()==null) return;
            if(getBottomView().top-getScrollY()>=getMeasuredHeight()){
                bottomState= BottomState.BottomNew;
            }else{
                bottomState= BottomState.BottomNormal;
            }
            if(lastBottomState!=bottomState){
                lastBottomState=bottomState= BottomState.BottomNormal;
                if(isItemOutOfLayout(getBottomView())){
                    getBottomView().isOutOfLayout=true;
                    removeViewFromContainer(getBottomView().view);
                    getBottomView().dataPosition=-1;
                }
                bottomItemPosition-=1;
            }
            if(topItemPosition<=0) return;
            if(getTopView()==null) return;
            if(getTopView().top-getScrollY()>=0){
                topState = TopState.TopNew;
            }else{
                topState = TopState.TopNormal;
            }
            if(lastTopState!=topState&&topState== TopState.TopNew){
                lastTopState=topState= TopState.TopNormal;
                MultiRecycleableView newView=getRecycleableView(topItemPosition-1);
                newView=adapter.getView(topItemPosition-1,newView,this);
                newView.isTop=true;
                newView.isBottom=false;
                newView.left=0;
                newView.dataPosition=topItemPosition-1;
                measureAItem(widthMode,widthSize,heightMode,heightSize,newView.view);
                newView.right=newView.view.getMeasuredWidth();
                newView.top= getTopView().top-newView.view.getMeasuredHeight();
                newView.isOutOfLayout=false;
                newView.viewType=adapter.getViewType(topItemPosition-1);
                newView.bottom= getTopView().top;
                layoutAItem(newView);
                addViewToContainer(newView.view);
                newView.viewPosition=0;
                getTopView().isTop=false;
                topItemPosition-=1;
            }
        }
        //事后处理，避免意外没有清除
        for(int i = 0; i< recycleableItems.size(); i++){
            if(isItemOutOfLayout(recycleableItems.get(i))){
                removeViewFromContainer(recycleableItems.get(i).view);
                recycleableItems.get(i).isOutOfLayout=true;
                recycleableItems.get(i).dataPosition=-1;
            }
        }
    }

    private synchronized MultiRecycleableView getRecycleableView(int position){
        for(MultiRecycleableView view: recycleableItems){
            if(view.isOutOfLayout&&view.viewType==adapter.getViewType(position)){
                return view;
            }
        }
        return null;
    }

    private synchronized void addViewToContainer(View v){
        this.addView(v);
    }

    private synchronized void removeViewFromContainer(View v){
        this.removeView(v);
    }

    private synchronized void addViewToRecyclers(MultiRecycleableView v){
        recycleableItems.add(v);
    }

    private synchronized boolean isItemOutOfLayout(MultiRecycleableView v) {
        if (v.bottom - getScrollY() < 0 || v.top - getScrollY() > getMeasuredHeight()) {
            return true;
        }
        return false;
    }

    //获取顶部视图
    private synchronized MultiRecycleableView getTopView() {
        for (MultiRecycleableView multiRecycleableView : recycleableItems) {
            if (multiRecycleableView.dataPosition==topItemPosition) {
                return multiRecycleableView;
            }
        }
        return null;
    }

    //获取底部视图
    private synchronized MultiRecycleableView getBottomView() {
        for (MultiRecycleableView multiRecycleableView : recycleableItems) {
            if (multiRecycleableView.dataPosition==bottomItemPosition) {
                return multiRecycleableView;
            }
        }
        return null;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (scroller.computeScrollOffset()) {
            scrollTo(0, scroller.getCurrY());
            offsetY = getScrollY();
            postInvalidate();
        }
    }

    public int getOffsetY(){
        return offsetY;
    }

    public void setSoftEdge(boolean softEdge) {
        this.softEdge = softEdge;
    }

    public void setSoftRatio(float softRatio) {
        this.softRatio = softRatio;
    }
}
