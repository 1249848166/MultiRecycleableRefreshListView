package su.com.multirecycleablerefreshlistview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import su.com.multirecycleablerefreshlistview2.multirecycleablelistview.MultiRecycleableListView;
import su.com.multirecycleablerefreshlistview2.multirecycleablelistview.MultiRecycleableView;

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
