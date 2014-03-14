package com.magicmod.romcenter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import com.magicmod.cloudserver.netdisk.OTAUtils.ItemInfo;
import com.magicmod.cloudserver.utils.Utils;
import com.magicmod.romcenter.utils.Constants;
import com.magicmod.romcenter.utils.UtilTools;

import java.util.ArrayList;

public class OtaAdapter extends BaseAdapter{
    private static final String TAG = "OtaAdapter";
    private static final boolean DBG = Constants.DEBUG;

    public interface onOtaItemClickListener {
        public void onItemClicked(ItemInfo item);
    }

    private ArrayList<ItemInfo> items = new ArrayList<ItemInfo>();
    
    private onOtaItemClickListener mOnOtaItemClickListener;
    private Context mContext;
    
    private String mDownloadedList[];
    
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
        final ItemInfo item = items.get(pos);

        if (convertView == null) {
            LayoutInflater mInflater = LayoutInflater.from(mContext);
            convertView = mInflater.inflate(R.layout.ota_items, parent, false);
            holder = new ViewHolder();
            holder.icon = (ImageButton) convertView.findViewById(R.id.ota_item_romstate);
            holder.name = (TextView) convertView.findViewById(R.id.ota_item_rominfo);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        
        if (item.name.equals(Utils.getMagicModRomName())) {
            holder.icon.setImageResource(R.drawable.ic_ota_installed);
        } else if (UtilTools.contain(mDownloadedList, item.name)){
            holder.icon.setImageResource(R.drawable.ic_ota_install);
        } else {
            holder.icon.setImageResource(R.drawable.ic_ota_download);
        }
        holder.icon.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
                if (mOnOtaItemClickListener != null) {
                    mOnOtaItemClickListener.onItemClicked(item);
                }
            }
        });
        holder.name.setText(item.name);
        
        return convertView;
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

    public void setOtaItemClickListener(onOtaItemClickListener listener) {
        if (listener != null) {
            mOnOtaItemClickListener = listener;
        }
    }
    public final class ViewHolder {
        ImageButton icon;
        TextView name;
    }
}
