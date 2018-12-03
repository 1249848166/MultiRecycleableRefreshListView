package su.com.multirecycleablerefreshlistview2.multirecycleablelistview;

import android.view.View;

public class MultiRecycleableView {
    public View view;//视图
    public boolean isOutOfLayout;//是否不可见
    public Object tag;//标签
    public int dataPosition;//在数据中的位置
    public int viewPosition;//在视图中的位置
    public boolean isBottom;//是否在控件底部
    public boolean isTop;//是否在控件顶部
    public int viewType;//视图类型
    public int left;//左边界位置
    public int top;//顶部边界位置
    public int right;//右边界位置
    public int bottom;//底部边界位置
}
