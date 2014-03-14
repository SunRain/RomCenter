package com.magicmod.romcenter;

import android.content.Context;

import com.magicmod.cloudserver.netdisk.BaseDownloadService;
import com.magicmod.cloudserver.netdisk.OTAUtils.ItemInfo;

public class OtaDownloadService  extends BaseDownloadService{

    public static void start(Context context, ItemInfo item, String targetPath) {
        BaseDownloadService.start(context, item, targetPath);
    }
}
