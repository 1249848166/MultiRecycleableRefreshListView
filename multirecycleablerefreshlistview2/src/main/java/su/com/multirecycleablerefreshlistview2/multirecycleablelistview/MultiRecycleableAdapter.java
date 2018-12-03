package su.com.multirecycleablerefreshlistview2.multirecycleablelistview;

import android.view.ViewGroup;

public interface MultiRecycleableAdapter {
    int getCount();
    Object getItem(int position);
    int getViewType(int position);
    MultiRecycleableView getView(int position, MultiRecycleableView convertView, ViewGroup container);
}
