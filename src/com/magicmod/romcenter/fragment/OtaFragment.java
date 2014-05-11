package com.magicmod.romcenter.fragment;

import android.R.integer;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.os.Bundle;
import android.os.AsyncTask;
import android.os.Handler;
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
import android.widget.ProgressBar;
import android.widget.TextView;

import com.magicmod.cloudserver.netdisk.BaseItem;
import com.magicmod.cloudserver.netdisk.BaseItem.ItemType;
import com.magicmod.cloudserver.netdisk.GetServerListAsyncTask;
import com.magicmod.cloudserver.netdisk.NetDisk;
import com.magicmod.cloudserver.netdisk.NetDisk.DownloadListener;
import com.magicmod.cloudserver.netdisk.NetDisk.ListDirCallBack;
import com.magicmod.cloudserver.netdisk.NetDiskConstants;
import com.magicmod.cloudserver.utils.Utils;
import com.magicmod.cloudserver.utils.Constants.AccessType;
import com.magicmod.cloudserver.utils.Constants.OtaExceptions;
import com.magicmod.romcenter.adapter.OtaAdapter;
import com.magicmod.romcenter.adapter.OtaAdapter.onOtaItemClickListener;
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
    private static final int DIALOG_TYPE_STOP_DOWNLOAD_CONFIRM = 0x04;

    private BaseItem mClickedItem;

    private TextView mCurVersionView;
    private TextView mOtaSiteLinkView;
    private ListView mListView;
    private ProgressBar mProgressBar;
    
    protected MenuItem mRefreshItem;

    private Date mCurrentRomDate;
    private NetDisk mNetDisk;
    private OtaAdapter mAdapter;
    
    private Handler mUpdateHandler = new Handler();
    //private DownloadManager mDownloadManager;
    
    private static boolean mInitOK = false;
    
    //全局变量,所以需要确定每次只有一个download在运行
    private long mDownloadID = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setHasOptionsMenu(true);
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            mCurrentRomDate = sdf.parse(Utils.getMmBuildDate());
        } catch (ParseException e) {
            e.printStackTrace();
            mCurrentRomDate = new Date();
        }

        mNetDisk = new NetDisk(getActivity());
        mNetDisk.Init(new NetDisk.InitCallBack() {
            @Override
            public void OnInitSucceed() {
                mInitOK = true;
            }
            @Override
            public void OnInitFailed(OtaExceptions exception, String extraInfo) {
                mInitOK = false;
                ToastUtil.showShort(getActivity(), "init token error");
             }
        });
        mNetDisk.setDownloadListener(mDownloadListener);
        if (DBG) Log.d(TAG, "Current rom build date is =>" + mCurrentRomDate.toString());
        
        showDialogInner(DIALOG_TYPE_DEV_WIP_WARNING);
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
        mProgressBar = (ProgressBar) this.getActivity().findViewById(R.id.ota_item_dlprogress);
        mClickedItem = null;
    }

    @Override
    public void onDestroy() {
        mNetDisk.DeInit();
        super.onDestroy();
    }

    
    @Override
    public void onStart() {
        super.onStart();
        mDownloadID = mNetDisk.getCurrentDownloadId();
        ArrayList<BaseItem> items = UtilTools.getCachedUpdate(getActivity());
        setAdapterData(items);
        if (mDownloadID >= 0) {
            if (DBG) {
                Log.d(TAG, "onStart, download id is" + mDownloadID);
            }
            if (mNetDisk.getRunningDownloadFileName(mDownloadID) != null) {
                BaseItem item = UtilTools.getRunningDownloadItem(getActivity());
                mAdapter.setDownloadingItem(item);
                mUpdateHandler.post(mUpdateProgress);
            } else {
                mDownloadID = -1;
                UtilTools.cacheRunningDownloadItem(getActivity(), null);
            }
        } else {
            mDownloadID = -1;
            UtilTools.cacheRunningDownloadItem(getActivity(), null);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mUpdateHandler.removeCallbacks(mUpdateProgress);
        hideRefreshAnimation();
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
        mNetDisk.listDevRom(mListDirCallBack);
    }

    private void cancelServerListTask () {
        mNetDisk.stopListDir();
        hideRefreshAnimation();
    }

    private void showDialogInner(int dialogType) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(dialogType);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), String.valueOf(dialogType));
    }

    private void downloadCheckedFile() {
        if (mDownloadID >= 0) {
            ToastUtil.showShort(getActivity(), R.string.ota_update_warning_one_download_running);
            return;
        }
        mNetDisk.downloadFile(mClickedItem);
        mAdapter.setDownloadingItem(mClickedItem);
        UtilTools.cacheRunningDownloadItem(getActivity(), mClickedItem);
    }
    
    private void installCheckedFile() {
        if (DBG) {
            Log.d(TAG, String.format("== triggerUpdate for file => %s", mClickedItem.getRemoteName()));
        }
        try {
            UtilTools.triggerUpdate(getActivity(), mClickedItem.getRemoteName());
        } catch (IOException e) {
            e.printStackTrace();
            ToastUtil.showLong(this.getActivity(), R.string.ota_install_failed);
        }
    }

    private void stopDownloading() {
        if (mAdapter != null) {
            mAdapter.stopProgress();
            mAdapter.notifyDataSetChanged();
        }
        mNetDisk.stopDownloadFile(mDownloadID);
        mUpdateHandler.removeCallbacks(mUpdateProgress);
        mDownloadID = -1;
        UtilTools.cacheRunningDownloadItem(getActivity(), null);
    }
    
    private void setAdapterData(ArrayList<BaseItem> items) {
        if (mAdapter == null) {
            mAdapter = new OtaAdapter(getActivity());
        }
        mAdapter.setOtaItemClickListener(mOnOtaItemClickListener);
        mListView.setAdapter(mAdapter);
        mAdapter.setData(items);
        mAdapter.setListView(mListView);
    }

    private DownloadListener mDownloadListener = new DownloadListener() {
        
        @Override
        public void onDownloadSucceed(String completedFileFullPath) {
            if (DBG) {
                Log.d(TAG, "*** onDownloadSucceed "+ completedFileFullPath);
            }
            //succeed, reset local cached value
            stopDownloading();
        }
        
        @Override
        public void onDownloadStarted(long downloadID) {
            mDownloadID = downloadID;
            mUpdateHandler.post(mUpdateProgress);
        }
        
        @Override
        public void onDownloadFailed(long downloadID, OtaExceptions exceptions) {
            if (DBG) {
                Log.d(TAG, "*** onDownloadFailed ");
            }
            stopDownloading();

        }
    };
    
    private Runnable mUpdateProgress = new Runnable() {
        public void run() {
            if (DBG) {
                Log.d(TAG, "********* Runnable Loop ***********");
            }
            if (mDownloadID < 0) {
                return;
            }
            Log.d(TAG, "******** current download is is "+ mDownloadID);
            
            int status = mNetDisk.getDownloadStatus(mDownloadID);
            switch (status) {
                case DownloadManager.STATUS_PENDING:
                    if (DBG) {
                        Log.d(TAG, "=== status is pengind");
                    }
                    mAdapter.updateProgress(-1, -1);
                    break;
                case DownloadManager.STATUS_PAUSED:
                case DownloadManager.STATUS_RUNNING:
                    if (DBG) {
                        Log.d(TAG, "=== status is STATUS_RUNNING");
                    }
                    int downloadedBytes = mNetDisk.getDownloadedBytes(mDownloadID);//cursor.getInt(
                        //cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    int totalBytes = mNetDisk.getTotalBytes(mDownloadID);//cursor.getInt(
                        //cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                    if (DBG) {
                        Log.d(TAG, "=== status is running, download bytes is "+downloadedBytes+" totalbytes is "+totalBytes);
                    }
                    if (totalBytes < 0) {
                        mAdapter.updateProgress(-1, -1);
                    } else {
                        mAdapter.updateProgress(totalBytes, downloadedBytes);
                    }
                    break;
                case DownloadManager.STATUS_FAILED:
                    if (DBG) {
                        Log.d(TAG, "status is faild");
                    }
                    mDownloadID = -1;
                    break;
            }
            if (status != DownloadManager.STATUS_FAILED) {
                mUpdateHandler.postDelayed(this, 1000);
            }
        }
    };

    private onOtaItemClickListener mOnOtaItemClickListener = new onOtaItemClickListener() {
        
        @Override
        public void onItemClicked(BaseItem item) {
            mClickedItem = item;
            String targetPath = String.format("%s/%s", UtilTools.getStorageDir(), Constants.UPDATES_FOLDER);
            mClickedItem.setLocalPath(targetPath);
            
            if (DBG) {
                Log.d(TAG, String.format("Click item => %s", mClickedItem.getRemoteName()));
            }

            String downloadedList[] = UtilTools.getDownloadedOtaFiles();
            if (UtilTools.contain(downloadedList, mClickedItem.getRemoteName())) {
                showDialogInner(DIALOG_TYPE_INSTALL_CONFIRM);
            } else if (mClickedItem.getRemoteName().contains(Utils.getMmBuildDate())) {
                ToastUtil.showShort(getActivity(), R.string.ota_file_has_installed);
            } else {
                if (!Utils.isNetworkAvailable(getActivity())) {
                    ToastUtil.showShort(getActivity(), R.string.network_unavailable);
                    return;
                }
                if (!Utils.isWifi(getActivity())) {
                    showDialogInner(DIALOG_TYPE_NO_WIFI_DL_WARNING);
                } else {
                    showDialogInner(DIALOG_TYPE_DOWNLOAD_CONFIRM);
                }
            }
        }

        @Override
        public void onStopDownload() {
            showDialogInner(DIALOG_TYPE_STOP_DOWNLOAD_CONFIRM);            
        }
    };

    private NetDisk.ListDirCallBack mListDirCallBack = new  ListDirCallBack() {

        @Override
        public void onResult(ArrayList<BaseItem> items) {
            if (items == null) {
                ToastUtil.showShort(getActivity(), R.string.refresh_fail);
                hideRefreshAnimation();
                return;
            }
            //if it is an error handle item
            if (items.size() == 1 && items.get(0).getItemType().equals(ItemType.TYPE_ERROR_INFO_ITEM)) {
                ToastUtil.showShort(getActivity(), items.get(0).getRemoteMd5());
                hideRefreshAnimation();
                return;
            }
            ArrayList<BaseItem> list = new ArrayList<BaseItem>();
            for (BaseItem item : items) {
                //if it is an error handle item
                if (item.getItemType().equals(ItemType.TYPE_ERROR_INFO_ITEM)) {
                    ToastUtil.showShort(getActivity(), item.getRemoteMd5());
                }
                if (!item.getRemoteName().contains(Utils.getMagicModVerison())) {
                    continue;
                }
                if (item.getRemoteName().contains(".zip.md5sum")) {
                    continue;
                }
                
                boolean add = true;
                try {
                    //MagicMod-DE-4.4-20140107-LOCAL_BUILD-linux-SunRain-mako
                    String s[] = item.getRemoteName().split("-");
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
                                item.getRemoteName(), Utils.getMagicModVerison()));
                    }
                    continue;
                }
                if(DBG) {
                    Log.d(TAG, String.format("ADD !!! item name is => %s, magicmod verison is => %s", item.getRemoteName(), Utils.getMagicModVerison()));
                }
                list.add(item);                
            }
            if (!list.isEmpty()) {
                UtilTools.cacheAvailableUpdate(getActivity(), list);
                setAdapterData(list);
            }
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
                    cancelServerListTask();
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

        @Override
        public void onExecption(OtaExceptions e, String errorMsg) {
            switch (e) {
                case GET_TOKEN_ERROR:
                    ToastUtil.showLong(getActivity(), errorMsg);
                    break;
                default:
                    break;
            }
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
            builder.setTitle(R.string.ota_update_dialog_title);
            builder.setNegativeButton(R.string.cancel, null);
            switch (type) {
                case DIALOG_TYPE_DOWNLOAD_CONFIRM:
                    builder.setMessage(R.string.ota_update_dialog_download_content);
                    builder.setPositiveButton(R.string.ok, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            getOwner().downloadCheckedFile();                            
                        }
                    });
                    break;
                case DIALOG_TYPE_INSTALL_CONFIRM:
                    builder.setMessage(R.string.ota_update_dialog_install_content);
                    builder.setPositiveButton(R.string.ok, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            getOwner().installCheckedFile();
                        }
                    });
                    break;
                case DIALOG_TYPE_NO_WIFI_DL_WARNING:
                    builder.setMessage(R.string.no_wifi_warning);
                    builder.setPositiveButton(R.string.ok, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            getOwner().downloadCheckedFile();
                        }
                    });
                    break;
                case DIALOG_TYPE_STOP_DOWNLOAD_CONFIRM:
                    builder.setMessage(R.string.ota_update_dialog_stop_dl_content);
                    builder.setPositiveButton(R.string.ok, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            getOwner().stopDownloading();
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
