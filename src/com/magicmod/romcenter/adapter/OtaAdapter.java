package com.magicmod.romcenter.adapter;

import android.R.raw;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.magicmod.cloudserver.netdisk.BaseItem;
import com.magicmod.cloudserver.utils.Utils;
import com.magicmod.romcenter.R;
import com.magicmod.romcenter.R.drawable;
import com.magicmod.romcenter.R.id;
import com.magicmod.romcenter.R.layout;
import com.magicmod.romcenter.R.string;
import com.magicmod.romcenter.utils.Constants;
import com.magicmod.romcenter.utils.UtilTools;

import java.util.ArrayList;

public class OtaAdapter extends BaseAdapter{
    private static final String TAG = "OtaAdapter";
    private static final boolean DBG = Constants.DEBUG;

    public interface onOtaItemClickListener {
        public void onItemClicked(BaseItem item);
        public void onStopDownload();
    }

    private ArrayList<BaseItem> items = new ArrayList<BaseItem>();
    
    private onOtaItemClickListener mOnOtaItemClickListener;
    private Context mContext;
    private ListView mListView;
    private String mDownloadedList[];
    private BaseItem mDownloadingItem;
    private int mDownloadingItemPos = -1;
    
    
    public OtaAdapter(Context context/*, CallBack  callBack*/) {
        this.mContext = context;
        this.mDownloadedList = UtilTools.getDownloadedOtaFiles();
    }

    @Override
    public int getCount() {
        return this.items.size();
    }

    @Override
    public Object getItem(int position) {
        return this.items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final int pos = position;
        ViewHolder holder = null;;
        final BaseItem item = items.get(pos);

        if (convertView == null) {
            LayoutInflater mInflater = LayoutInflater.from(mContext);
            convertView = mInflater.inflate(R.layout.ota_items, parent, false);
            holder = new ViewHolder();
            holder.icon = (ImageButton) convertView.findViewById(R.id.ota_item_romstate_button);
            holder.romInfo = (TextView) convertView.findViewById(R.id.ota_item_rominfo);
            holder.romSummary = (TextView) convertView.findViewById(R.id.ota_item_romsummary);
            holder.progressBar = (ProgressBar) convertView.findViewById(R.id.ota_item_dlprogress);
            holder.progressBar.setVisibility(View.GONE);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (mDownloadingItem != null) {
            if (item.getRemoteName().equals(mDownloadingItem.getRemoteName())) {
                mDownloadingItemPos = pos;
            }
        }

        if (item.getRemoteName().equals(Utils.getMagicModRomName()+".zip")) {
            holder.icon.setImageResource(R.drawable.ic_ota_installed);
            holder.romSummary.setText(R.string.ota_item_summary_installed);
        } else if (UtilTools.contain(mDownloadedList, item.getRemoteName())){
            holder.icon.setImageResource(R.drawable.ic_ota_install);
            holder.romSummary.setText(R.string.ota_item_summary_install);
        } else {
            holder.icon.setImageResource(R.drawable.ic_ota_download);
            holder.romSummary.setText(R.string.ota_item_summary_new);
        }
        holder.icon.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                if (mOnOtaItemClickListener != null) {
                    mOnOtaItemClickListener.onItemClicked(item);
                }
            }
        });
        holder.romInfo.setText(item.getRemoteName());
        
        return convertView;
    }
    
    public void setData(ArrayList<BaseItem> list) {
        if (!this.items.isEmpty()) {
            this.items.clear();
        }
        if (list != null) {
            this.items = (ArrayList<BaseItem>) list.clone();
            this.mDownloadedList = UtilTools.getDownloadedOtaFiles();
            notifyDataSetChanged();
        }
    }

    public void setDownloadingItem(BaseItem item) {
        this.mDownloadedList = UtilTools.getDownloadedOtaFiles();
        mDownloadingItem = item;
        notifyDataSetChanged();
    }

    public void setOtaItemClickListener(onOtaItemClickListener listener) {
        if (listener != null) {
            mOnOtaItemClickListener = listener;
        }
    }
    
    public void setListView(ListView listView) {
        this.mListView = listView;
    }

    public void updateProgress(int max, int progress) {
        if (mListView == null) {
            return;
        }
        ///int itemIndex = mAdapter.getDownloadItemPosInArray();
        //得到第1个可显示控件的位置,记住是第1个可显示控件。而不是第1个控件
        int visiblePosition = mListView.getFirstVisiblePosition(); 
        //得到你需要更新item的View
        View view = mListView.getChildAt(mDownloadingItemPos - visiblePosition);
        
        if (DBG) {
            Log.d(TAG, "== visible pos is "+visiblePosition + " download item pos is "+mDownloadingItemPos);
        }
        ViewHolder holder = new ViewHolder();
        holder = new ViewHolder();
        holder.icon = (ImageButton) view.findViewById(R.id.ota_item_romstate_button);
        holder.romInfo = (TextView) view.findViewById(R.id.ota_item_rominfo);
        holder.romSummary = (TextView) view.findViewById(R.id.ota_item_romsummary);
        holder.romSummary.setVisibility(View.VISIBLE);
        holder.progressBar = (ProgressBar) view.findViewById(R.id.ota_item_dlprogress);
        holder.progressBar.setVisibility(View.VISIBLE);
        holder.icon.setImageResource(R.drawable.ic_ota_cancel_dl);
        holder.icon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnOtaItemClickListener != null) {
                    mOnOtaItemClickListener.onStopDownload();
                }
            }
        });
        
        if (max < 0) {
            holder.progressBar.setIndeterminate(true);
        } else {
            holder.progressBar.setIndeterminate(false);
            holder.progressBar.setMax(max);
            holder.progressBar.setProgress(progress>=0 ? progress : 0);
            
            String summary = String.format("%s download / %s Toatl", progress, max);
            holder.romSummary.setText(summary);
        }
    }
    
    public void stopProgress() {
        if (mListView == null) {
            return;
        }
        this.mDownloadedList = UtilTools.getDownloadedOtaFiles();
        
        ///int itemIndex = mAdapter.getDownloadItemPosInArray();
        //得到第1个可显示控件的位置,记住是第1个可显示控件。而不是第1个控件
        int visiblePosition = mListView.getFirstVisiblePosition(); 
        //得到你需要更新item的View
        View view = mListView.getChildAt(mDownloadingItemPos - visiblePosition);
        
        if (DBG) {
            Log.d(TAG, "== visible pos is "+visiblePosition + " download item pos is "+mDownloadingItemPos);
        }
        final BaseItem item = items.get(mDownloadingItemPos);
        ViewHolder holder = new ViewHolder();
        holder = new ViewHolder();
        holder.icon = (ImageButton) view.findViewById(R.id.ota_item_romstate_button);
        holder.romInfo = (TextView) view.findViewById(R.id.ota_item_rominfo);
        holder.romSummary = (TextView) view.findViewById(R.id.ota_item_romsummary);
        holder.romSummary.setVisibility(View.VISIBLE);
        holder.progressBar = (ProgressBar) view.findViewById(R.id.ota_item_dlprogress);
        holder.progressBar.setVisibility(View.GONE);
        //holder.icon.setImageResource(R.drawable.ic_ota_download);
        //holder.romSummary.setText(R.string.ota_item_summary_new);
        
        if (item.getRemoteName().equals(Utils.getMagicModRomName()+".zip")) {
            holder.icon.setImageResource(R.drawable.ic_ota_installed);
            holder.romSummary.setText(R.string.ota_item_summary_installed);
        } else if (UtilTools.contain(UtilTools.getDownloadedOtaFiles(), item.getRemoteName())){
            holder.icon.setImageResource(R.drawable.ic_ota_install);
            holder.romSummary.setText(R.string.ota_item_summary_install);
        } else {
            holder.icon.setImageResource(R.drawable.ic_ota_download);
            holder.romSummary.setText(R.string.ota_item_summary_new);
        }
        
        holder.icon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnOtaItemClickListener != null) {
                    mOnOtaItemClickListener.onItemClicked(item);
                }
            }
        });
        //rest the item flag when stop show progress
        mDownloadingItemPos = -1;
    }

    public final class ViewHolder {
        ImageButton icon;
        TextView romInfo;
        TextView romSummary;
        ProgressBar progressBar;
    }
}
