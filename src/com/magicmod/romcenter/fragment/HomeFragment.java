package com.magicmod.romcenter.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.magicmod.romcenter.R;
import com.magicmod.romcenter.adapter.OtaAdapter;
import com.magicmod.romcenter.utils.Constants;
import com.magicmod.romcenter.utils.UtilTools;

import java.util.ArrayList;

public class HomeFragment extends Fragment{
    private static final String TAG = "HomeFragment";
    private static final boolean DBG = Constants.DEBUG;
    
    private static class ItemInfo {
        public String title;
        public String summary;
    }

    private ListView mListView;
    private ArrayList<ItemInfo> mItemList = new ArrayList<HomeFragment.ItemInfo>();
    private Adapt mAdapt;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        String titles[] = this.getResources().getStringArray(R.array.home_item_title);
        String summarys[] = this.getResources().getStringArray(R.array.home_item_values);
        mItemList.clear();
        for (int i=0; i<titles.length; i++) {
            ItemInfo item = new ItemInfo();
            item.title = titles[i];
            item.summary = summarys[i];
            mItemList.add(item);
        }
        //mAdapt = new Adapt();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.home_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mListView = (ListView) this.getActivity().findViewById(R.id.home_fragment_lists);
        if (mAdapt == null) {
            mAdapt = new Adapt();
        }
        mListView.setAdapter(mAdapt);
        mAdapt.setData(mItemList);
    }
    
    private void openBrowser(String url) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }
    
    private class Adapt extends BaseAdapter{
        private ArrayList<ItemInfo> items = new ArrayList<HomeFragment.ItemInfo>();
        
        @Override
        public int getCount() {
            return mItemList.size();
        }

        @Override
        public Object getItem(int position) {
            return mItemList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public void setData(ArrayList<ItemInfo> list) {
            if (!this.items.isEmpty()) {
                this.items.clear();
            }
            if (list != null) {
                this.items = (ArrayList<ItemInfo>) list.clone();
                notifyDataSetChanged();
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final int pos = position;
            ViewHolder holder = null;;
            final ItemInfo item = items.get(pos);//mItemList.get(pos);

            if (convertView == null) {
                LayoutInflater mInflater = LayoutInflater.from(getActivity());
                convertView = mInflater.inflate(R.layout.home_fragment_item, parent, false);
                holder = new ViewHolder();
                holder.title = (TextView) convertView.findViewById(R.id.home_item_title);
                holder.title.setText(item.title);
                holder.summary = (TextView) convertView.findViewById(R.id.home_item_summary);
                holder.summary.setText(item.summary);;
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.summary.setText(UtilTools.getUrlString(item.summary, item.summary));
            holder.summary.setMovementMethod(LinkMovementMethod.getInstance());
            /*holder.summary.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    openBrowser(item.summary);
                }
            });*/
            return convertView;
        }
        
        private final class ViewHolder {
            TextView title;
            TextView summary;
        }
        
    }

}
