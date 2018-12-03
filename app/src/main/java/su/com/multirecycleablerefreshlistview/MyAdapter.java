package su.com.multirecycleablerefreshlistview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import su.com.multirecycleablerefreshlistview2.multirecycleablelistview.MultiRecycleableAdapter;
import su.com.multirecycleablerefreshlistview2.multirecycleablelistview.MultiRecycleableView;

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
