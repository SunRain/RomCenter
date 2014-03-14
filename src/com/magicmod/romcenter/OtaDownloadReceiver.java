package com.magicmod.romcenter;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.magicmod.cloudserver.netdisk.BaseDownloadReceiver;
import com.magicmod.cloudserver.utils.Constants;
import com.magicmod.cloudserver.utils.Constants.OtaExceptions;

public class OtaDownloadReceiver extends BaseDownloadReceiver{

    private static final String TAG = "OtaDownloadReceiver";
    private static final boolean DBG = Constants.DEBUG;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (DBG) {
            Log.d(TAG, String.format("get action ==> %s", action));
        }
        super.onReceive(context, intent);
    }

    @Override
    public void onDownloadSucceed(String completedFileFullPath) {
        if (DBG) {
            Log.d(TAG, "== onDownloadSucceed == completedFileFullPath ==> " + completedFileFullPath);
        }
        
    }

    @Override
    public void onDownloadStarted(long downloadID) {
        if (DBG) {
            Log.d(TAG, "== onDownloadStarted == downloadID ==> " + downloadID);
        }
        
    }

    @Override
    public void onDownloadFail(long downloadID, OtaExceptions exceptions) {
        if (DBG) {
            Log.d(TAG, "== onDownloadFail == downloadID ==> " + downloadID);
        }
        
    }

}
