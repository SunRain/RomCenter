package com.magicmod.romcenter.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.AsyncTask;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.magicmod.cloudserver.netdisk.GetServerListAsyncTask;
import com.magicmod.cloudserver.netdisk.GetServerListAsyncTask.CallBack;
import com.magicmod.cloudserver.netdisk.OTAUtils.ItemInfo;
import com.magicmod.cloudserver.utils.Utils;
import com.magicmod.cloudserver.utils.Constants.OtaExceptions;
import com.magicmod.romcenter.OtaAdapter;
import com.magicmod.romcenter.OtaAdapter.onOtaItemClickListener;
import com.magicmod.romcenter.OtaDownloadService;
import com.magicmod.romcenter.R;
import com.magicmod.romcenter.utils.Constants;
import com.magicmod.romcenter.utils.ToastUtil;
import com.magicmod.romcenter.utils.UtilTools;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class OtaFragment extends Fragment{
    private static final String TAG = "OtaFragment";
    private static final boolean DBG = Constants.DEBUG;
    
    private static final String KEY_DIALOG_TYPE = "dialog_type";
    private static final int DIALOG_TYPE_DEV_WIP_WARNING = 0x00;
    private static final int DIALOG_TYPE_DOWNLOAD_CONFIRM = 0x01;
    private static final int DIALOG_TYPE_INSTALL_CONFIRM = 0x02;
    private static final int DIALOG_TYPE_NO_WIFI_DL_WARNING = 0x03;
    
    private ArrayList<ItemInfo> mOtaLists;
    private ItemInfo mClickedItem;
    //private String mfullPathInstallFile;

    private TextView mCurVersionView;
    private TextView mOtaSiteLinkView;
    private ListView mListView;
    
    protected MenuItem mRefreshItem;

    private Date mCurrentRomDate;
    
    private GetServerListAsyncTask mGetServerListAsyncTask;
    private OtaAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setHasOptionsMenu(true);
        mOtaLists = new ArrayList<ItemInfo>();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            mCurrentRomDate = sdf.parse(Utils.getMmBuildDate());
        } catch (ParseException e) {
            e.printStackTrace();
            mCurrentRomDate = new Date();
        }
        
        if (DBG) Log.d(TAG, "Current rom build date is =>" + mCurrentRomDate.toString());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.ota_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        mCurVersionView = (TextView) this.getActivity().findViewById(R.id.ota_fragment_cur_rom_info);
        mCurVersionView.setText(String.format(getString(R.string.ota_cur_verison), Utils.getMagicModRomName()));
        mListView = (ListView) this.getActivity().findViewById(R.id.ota_fragment_update_lists);
        mOtaSiteLinkView = (TextView) this.getActivity().findViewById(R.id.ota_fragment_update_site_info);
        String link = String.format("<a href=\"%s\">%s</a>", Constants.OTA_SITE_LINK, Constants.OTA_SITE_LINK);
        mOtaSiteLinkView.setText(Html.fromHtml(String.format(getString(R.string.ota_update_site_info), link)));
        mOtaSiteLinkView.setMovementMethod(LinkMovementMethod.getInstance());
        //mfullPathInstallFile = null;
        mClickedItem = null;
        showDialogInner(DIALOG_TYPE_DEV_WIP_WARNING);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.ota_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_ota_refresh:
                showRefreshAnimation(item);
                refreshAvailableList();
                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showRefreshAnimation(MenuItem item) {
        hideRefreshAnimation();

        mRefreshItem = item;

        // 这里使用一个ImageView设置成MenuItem的ActionView，这样我们就可以使用这个ImageView显示旋转动画了
        ImageView refreshActionView = (ImageView) this.getActivity().getLayoutInflater().inflate(R.layout.action_ota_refresh_view, null);
        refreshActionView.setImageResource(R.drawable.ic_action_refresh);
        mRefreshItem.setActionView(refreshActionView);

        // 显示刷新动画
        Animation animation = AnimationUtils.loadAnimation(this.getActivity(), R.anim.refresh_rotate);
        animation.setRepeatMode(Animation.RESTART);
        animation.setRepeatCount(Animation.INFINITE);
        refreshActionView.startAnimation(animation);
    }

    private void hideRefreshAnimation() {
        if (mRefreshItem != null) {
            View view = mRefreshItem.getActionView();
            if (view != null) {
                view.clearAnimation();
                mRefreshItem.setActionView(null);
            }
        }
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        super.onOptionsMenuClosed(menu);
    }
    
    private void refreshAvailableList() {
        if (DBG) Log.d(TAG, "refresh  available rom list");

        if (!isTaskActive()) {
            mGetServerListAsyncTask = new GetServerListAsyncTask(this.getActivity(), mGetServerListCallBack);
            mGetServerListAsyncTask.execute();
        }

    }

    private boolean isTaskActive() {
        return mGetServerListAsyncTask != null
                && mGetServerListAsyncTask.getStatus() != AsyncTask.Status.FINISHED;
    }

    private void cancelServerListTask () {
        if (mGetServerListAsyncTask != null) {
            mGetServerListAsyncTask.cancel(true);
        }
        hideRefreshAnimation();
    }

    private void showDialogInner(int dialogType) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(dialogType);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), String.valueOf(dialogType));
    }

    private void downloadCheckedFile() {
        String targetPath = String.format("%s/%s", UtilTools.getStorageDir(), Constants.UPDATES_FOLDER);
        OtaDownloadService.start(getActivity(), mClickedItem, targetPath);        
    }
    
    private void installCheckedFile() {
        String targetPath = String.format("%s/%s", UtilTools.getStorageDir(), Constants.UPDATES_FOLDER);
        String file = targetPath + "/" + mClickedItem.name;
        if (DBG) {
            Log.d(TAG, String.format("== triggerUpdate for file => %s", file));
        }
        try {
            UtilTools.triggerUpdate(getActivity(), file);
        } catch (IOException e) {
            e.printStackTrace();
            ToastUtil.showLong(this.getActivity(), R.string.ota_install_failed);
        }
    }

    private onOtaItemClickListener mOnOtaItemClickListener = new onOtaItemClickListener() {
        
        @Override
        public void onItemClicked(ItemInfo item) {
            mClickedItem = item;
            
            if (DBG) {
                Log.d(TAG, String.format("Click item => %s", mClickedItem.name));
            }

            String downloadedList[] = UtilTools.getDownloadedOtaFiles();
            if (UtilTools.contain(downloadedList, mClickedItem.name)) {
                if (!Utils.isNetworkAvailable(getActivity())) {
                    ToastUtil.showShort(getActivity(), R.string.network_unavailable);
                    return;
                }
                if (!Utils.isWifi(getActivity())) {
                    showDialogInner(DIALOG_TYPE_NO_WIFI_DL_WARNING);
                } else {
                    showDialogInner(DIALOG_TYPE_INSTALL_CONFIRM);
                }
            } else if (mClickedItem.name.contains(Utils.getMmBuildDate())){
                ToastUtil.showShort(getActivity(), R.string.ota_file_has_installed);
            } else {
                showDialogInner(DIALOG_TYPE_DOWNLOAD_CONFIRM);
            }
        }
    };

    private GetServerListAsyncTask.CallBack mGetServerListCallBack = new CallBack() {
        
        @Override
        public void onResult(ArrayList<ItemInfo> items) {
            if (items == null) {
                ToastUtil.showShort(getActivity(), R.string.refresh_fail);
                hideRefreshAnimation();
                return;
            }
            ArrayList<ItemInfo> list = new ArrayList<ItemInfo>();
            for (ItemInfo item : items) {
                if (!item.name.contains(Utils.getMagicModVerison())) {
                    continue;
                }
                if (item.name.contains(".zip.md5sum")) {
                    continue;
                }
                
                boolean add = true;
                try {
                    //MagicMod-DE-4.4-20140107-LOCAL_BUILD-linux-SunRain-mako
                    String s[] = item.name.split("-");
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                    Date d = sdf.parse(s[3]);
                    if (mCurrentRomDate.after(d)) {
                        add = false;
                    }
                    if (DBG) {
                        Log.d(TAG, String.format("=== CurrentRomDate is => %s, target is => %s",
                                mCurrentRomDate.toString(), d.toString()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (!add) {
                    if (DBG) {
                        Log.d(TAG, String.format("NOT ADD !!!item name is => %s, magicmod verison is => %s",
                                item.name, Utils.getMagicModVerison()));
                    }
                    continue;
                }
                if(DBG) {
                    Log.d(TAG, String.format("ADD !!! item name is => %s, magicmod verison is => %s", item.name, Utils.getMagicModVerison()));
                }
                list.add(item);                
            }
            if (mAdapter == null) {
                mAdapter = new OtaAdapter(getActivity());
            }
            mAdapter.setOtaItemClickListener(mOnOtaItemClickListener);
            mListView.setAdapter(mAdapter);
            mAdapter.setData(list);
            hideRefreshAnimation();
        }
        
        @Override
        public void onPreExecute() {
        }

        @Override
        public void onException(OtaExceptions e) {
            hideRefreshAnimation();
            switch(e) {
                case CANNOT_CONNECT:
                    ToastUtil.showShort(getActivity(), R.string.network_unavailable);
                    if (isTaskActive()) cancelServerListTask();
                    break;
                case NETWORK_NOT_WIFI:
                    //TODO: if netwrok not wifi, show a dialog
                    break;
                case NO_SDCARD_FONUND:
                    ToastUtil.showShort(getActivity(), R.string.no_sdcard_worning);
                    break;
                case NO_ENOUGH_DISK_SPACE:
                    ToastUtil.showShort(getActivity(), R.string.no_enough_space_worning);
                    break;
                case NO_NEW_VERSION:
                    ToastUtil.showShort(getActivity(), R.string.no_new_version_worning);
                    break;
                default:
                        break;
            }
            
        }
        
        @Override
        public void onCancelled() {
            hideRefreshAnimation();
        }
    };
    
    public static class MyAlertDialogFragment extends DialogFragment {
        public static MyAlertDialogFragment newInstance(int dialogType) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt(KEY_DIALOG_TYPE, dialogType);
            frag.setArguments(args);
            return frag;
        }
        
        OtaFragment getOwner() {
            return (OtaFragment) getTargetFragment();
        }
        
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int type = getArguments().getInt(KEY_DIALOG_TYPE);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            getOwner().hideRefreshAnimation();
            switch (type) {
                case DIALOG_TYPE_DOWNLOAD_CONFIRM:
                    builder.setTitle(R.string.ota_update_dialog_title);
                    builder.setMessage(R.string.ota_update_dialog_download_content);
                    builder.setNegativeButton(R.string.cancel, null);
                    builder.setPositiveButton(R.string.ok, new OnClickListener() {
                        
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            getOwner().downloadCheckedFile();                            
                        }
                    });
                    break;
                case DIALOG_TYPE_INSTALL_CONFIRM:
                    builder.setTitle(R.string.ota_update_dialog_title);
                    builder.setMessage(R.string.ota_update_dialog_install_content);
                    builder.setNegativeButton(R.string.cancel, null);
                    builder.setPositiveButton(R.string.ok, new OnClickListener() {
                        
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            getOwner().installCheckedFile();
                        }
                    });
                    break;
                case DIALOG_TYPE_NO_WIFI_DL_WARNING:
                    builder.setTitle(R.string.ota_update_dialog_title);
                    builder.setMessage(R.string.no_wifi_warning);
                    builder.setNegativeButton(R.string.cancel, null);
                    builder.setPositiveButton(R.string.ok, new OnClickListener() {
                        
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            getOwner().downloadCheckedFile();
                        }
                    });
                    break;
                case DIALOG_TYPE_DEV_WIP_WARNING:
                default:
                    builder.setTitle(R.string.title_ota);
                    builder.setMessage(R.string.ota_fragment_open_waning);
                    builder.setNegativeButton(R.string.ok, null);                    
                    break;
            }
            return builder.create();
        }
    }
}
