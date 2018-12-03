# MultiRecycleableRefreshListView
## 模仿官方RecyclerView的一个多类型可复用listview
这两天打算自己实现多种类可回收控件，原因是安卓自带的RecyclerView虽然也实现了回收复用，据说多种类型回收复用也可以（事实上我也是这样用的，但是懒得看源码，也不知道类型大于等于二以后是不是真的能够很好的回收。）所以为了刨根问底，我决定自己实现RecyclerView的功能，并在此基础上将多类型视图的复用机制塞进控件里面，这样在外部就不用写那么多选择判断语句了，减小耦合性。
实际上我最后没能减少这一部分耦合，因为从一开始就陷入了固定架构中。
## 先说我是怎么实现复用的吧。
因为不知道官方具体复用方式，自己也没有太大概念，又懒得看源码，所以我直接拿来adapter的结构二话不说就是干。
public interface MultiRecycleableAdapter {
    int getCount();
    Object getItem(int position);
    int getViewType(int position);
    MultiRecycleableView getView(int position, MultiRecycleableView convertView, ViewGroup container);
}
上面是我定义的adapter结构，和官方的有些出入。可以看到getView方法我返回的不是view，而是封装了view的一个对象（因为我想通过封装的方式在内部实现view类型解开耦合。然而我失败了）
然后接下来就是真正的工作了，怎样根据上面adapter的结构来设计这个可以回收的控件？？？
从这个adapter的结构可以看出RecyclerView内部的回收机制吗？答案是可以！
因为getView可以获得视图，这时候就有两种情况了，一种是这个视图原本就存在，另一种是不存在，就要重新创建一个。假设已经存在了，直接拿来复用。如果不存在，创建一个。问题是假设要创建一个新的，这个是的视图的类型怎么确定？这时我们就看到getViewType的作用了。
毫无疑问，不管是getView还是getViewType等等adapter里面定义的方法，都是在外部实现，然后给内部用的。这个外部指的是安卓程序猿，而内部指的是中间件（这个可以多类型复用的控件）的开发者。这样就很有趣了，因为我们写的组建并非是完全品，而是预留一部分自定义的代码交由安卓开发人员填充代码。
## 接下来，正式写这个控件。
首先这里必须对安卓自定义view的绘制和布局有一定的了解。首先经过测量每一个view的大小，然后布局每一个view到父view里面。由于viewGroup的父类就是view，所以很有意思的是，他的测量和布局是一种类似于递归的形式。
幸运的是安卓提供了方式让我们去重写测量和布局的方法。但是事实上我们通常能看到的案例都是静态测量和布局。什么意思呢？就是测量后再布局，只需要一次后就不去管了。
可怕的是要实现这个多类型回收控件，必须要动态的去测量和布局。
我的方法是，他肯定要有一次静态的测量和布局，然后之后的动态测量和布局根据屏幕的滑动来确定。
1.比如，刚开始显示的时候，测量在可视范围内的view并正确的在屏幕中布局他们。（这里有一个问题是，控件本身是一个容器，内部的item的坐标系必须根据控件顶部和左边边界来确定，而不是屏幕边界，否则当别人使用这个控件的时候如果不把控件设为全屏大小，内部测量和布局的逻辑肯定是出错的。）
2.然后当手指向上滑动时，控件的内容要向上滚动（换句话说，控件向下滚动），然后底部可视范围之外的item就要从不可见状态变为可见，添加进容器里面。这时两种情况，第一种是这个item已经存在，第二种是item不存在于回收复用列表里面。对于第二种，直接创建一个。但是重点来了，因为我们要考虑将这部分的代码交由安卓程序猿来实现，所以这时getview就派上用场了。还记得getview里面有传入convertview这个被回收的view吧，那就对了。如果我们在内部的复用列表里面没有通过getviewtype找到这个类型的可回收的convertview，那么内部调用getview是就传入null。如果找到了，那么把他传进去。而当他从内部传入外部时，外部会判断他是否为null，null的话创建一个由外部自己定义的view。不过不管是不是null，都必须会处理的一件事便是绑定数据。
以上就是通过adapter结构可以看到的复用逻辑。其实这个复用方式并不难，真正难的是如何处理好回收工作（这里分两部分，一个是复用列表的维护，另一个是容器视图列表的维护）
3.刚才是上滑，可以确定的是第一次的时候可视范围内肯定会有几个item，然后上滑后增加几个。但是当下滑的时候，这些之前已经加载出来的item怎么复用呢？答案是直接复用。因为上滑时已经创建好了当前状态下所有需要的模版，下滑时不管怎样那些模版肯定已经存在了，所以不用判断是否存在，直接复用。
貌似这个控件其实就只需要这三个逻辑，事实上就是这样。但是有很多细节的坑。
比如怎样标记最顶部可是范围内的item和最底部可视范围内的item？我这里采用两个变量储存，用topItemPosition和bottomItemPosition来储存可视范围内最顶部item的真实数据位置和可视范围最底部item的数据位置（这个是数据位置，和视图位置不一样）。
再比如，如何判断一个item从不可视状态转化为可视？我的方法是通过判断item的边界和容器边界的关系（这时，scrollY和top属性的区别就要清楚了，当然也可以不用scroll属性直接用top，但是实际上scroll更好用些，因为他不仅仅是一个距离属性，因为他有一套配套的测距和回滚计算）。此外，就是必须将数据的比较转化成状态。因为数值比较关系是不停触发的，而状态是当改变时才触发，所以必须先转化为状态，除非你想要一个动作增加几十几百个item。。。。
接下来是具体实现：
```java
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
上面是会用到的属性，下面是第一次测量和布局
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
```
这一部分是静态测量和布局，因为只处理第一次显示时的item大小和排布
下面是屏幕滑动逻辑，动态测量和布局就是在这里实现的。
首先，事件分配，按下不拦截，只拦截判断滑动（安卓的触摸事件是配套的，因此一旦按下事件没法捕捉，后面的滑动和抬起就捕捉不到，所以一般不拦截按下，而是对滑动判断是否拦截。）
```java
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
```
然后是触摸事件。因为按下没有拦截，所以理论上控件和他内部的item都接受的到按下事件。只有滑动时控件才拦截了事件，这是很正确的逻辑。
```java
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
```
上面触摸屏幕逻辑里不但处理了两种不同的控件滚动方式（超出边界速度减小，不超出边界速度不变，超出后松开回滚），还在每个事件里判断item的状态，而具体的代码就是这个回收的核心逻辑了。
```java
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
```
其实是分了上滑和下滑，只需要处理上滑就行，因为下滑逻辑相似，而且更简单。后面我还做了个收尾的动作，实际上这是必须的，因为当滑动的很快的时候（疯狂的随机上下滑动，极致的疯狂，想把屏幕都滑烂的冲动）这时候还是会有一定小的几率会出现视图叠加的情况。。。（为什么没有正确回收？很可能是有没考虑到的地方吧。不过后面再回收一次，保证不出错。）

这样就实现了回收复用了，根据控件的大小和item的大小，可复用列表和可视的item会维持在一个尽量小的数目里面。
复用的好处就是，当你有一万条数据，没必要创建一万个view，只需要根据数据对应view的特征合理的复用他们，避免了内存溢出。但是代价是，增加了一些性能负担。
既然通过减小内存增加了性能（虽然比起性能的降低，内存的优化效果更巨大），那么就要弥补回来。。。。对的，这时候就又了Holder！！！
Holder的作用就是将布局中找到的子view保存起来，下一次就不用再去findview了，findview是很耗费性能的。
recycle和Holder的结合是很完美的。
那么，要怎么使用这个控件呢？和官方给的RecyclerView基本一样。。。
前面说了，我尝试将判断viewtype的耦合部分塞进控件内部，但是失败了。。。所以这个控件并不比安卓官方的RecyclerView好，因为没有解决viewtype带来的耦合问题。就当作对RecyclerView的一种学习吧。
## 在外部具体使用：
```java
public class MyAdapter implements MultiRecycleableAdapter {

    Context context;
    List<MultiRecycleableView> list;

    public MyAdapter(Context context, List<MultiRecycleableView> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public int getViewType(int position) {
        if(position==0){
            return 0;
        }else if(position==1){
            return 1;
        }else if((position-2)%5==0){
            return 2;
        }else{
            return 3;
        }
    }

    @Override
    public MultiRecycleableView getView(int position, MultiRecycleableView convertView,ViewGroup container) {
        MultiRecycleableView multiRecycleableView=null;
        if(convertView!=null){//有复用
            multiRecycleableView=convertView;
        }else{//没有复用
            multiRecycleableView=new MultiRecycleableView();
            LayoutInflater inflater=LayoutInflater.from(context);
            Holder holder=new Holder();
            //找出所有子view存放进holder，牺牲空间换时间
            if(getViewType(position)==0){
                multiRecycleableView.view=inflater.inflate(R.layout.item_top1,container,false);
                holder.head1=multiRecycleableView.view.findViewById(R.id.head1);
                holder.head2=multiRecycleableView.view.findViewById(R.id.head2);
            }else if(getViewType(position)==1){
                multiRecycleableView.view=inflater.inflate(R.layout.item_top2,container,false);
                holder.head=multiRecycleableView.view.findViewById(R.id.head);
            }else if(getViewType(position)==2){
                multiRecycleableView.view=inflater.inflate(R.layout.item_ad,container,false);
                holder.ad1=multiRecycleableView.view.findViewById(R.id.ad1);
                holder.ad2=multiRecycleableView.view.findViewById(R.id.ad2);
                holder.ad3=multiRecycleableView.view.findViewById(R.id.ad3);
            }else{
                multiRecycleableView.view=inflater.inflate(R.layout.item_content,container,false);
                holder.content=multiRecycleableView.view.findViewById(R.id.content);
            }
            multiRecycleableView.tag=holder;
            ViewGroup.LayoutParams params=multiRecycleableView.view.getLayoutParams();
            params.height=500;
        }
        //取出holder设置数据
        Holder holder= (Holder) multiRecycleableView.tag;
        if(getViewType(position)==0){
            holder.head1.setImageResource(R.mipmap.ic_launcher);
            holder.head2.setImageResource(R.mipmap.ic_launcher);
        }else if(getViewType(position)==1){
            holder.head.setImageResource(R.mipmap.ic_launcher);
        }else if(getViewType(position)==2){
            holder.ad1.setImageResource(R.mipmap.ic_launcher);
            holder.ad2.setImageResource(R.mipmap.ic_launcher);
            holder.ad3.setImageResource(R.mipmap.ic_launcher);
        }else{
            holder.content.setText("text content in position "+position);
        }
        enterAnimate(multiRecycleableView.view,500);
        return multiRecycleableView;
    }

    class Holder{//存放所有可能用到的view
        TextView content;
        ImageView ad1,ad2,ad3;
        ImageView head,head1,head2;
    }

    private void enterAnimate(View v,long duration){
        Animation animation=new ScaleAnimation(0,1,0,1,
                Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);
        animation.setDuration(duration);
        v.startAnimation(animation);
    }
}
```
继承adapter并实现内部预留给外部重写的方法。然后我们需要这个控件，给控件设置adapter
```java
public class MainActivity extends AppCompatActivity {

    MultiRecycleableListView multiRecycleableListView;
    List<MultiRecycleableView> list;
    MyAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        multiRecycleableListView=findViewById(R.id.multirecycleablelistview);
        list=new ArrayList<>();
        for(int i=0;i<50;i++){
            list.add(new MultiRecycleableView());
        }
        adapter=new MyAdapter(this,list);
        multiRecycleableListView.setAdapter(adapter);
    }
}
```
就是这么简单，和RecyclerView一样，甚至更简单，因为省去了LayoutManager和ItemDecoration等内部一系列复杂的功能。
