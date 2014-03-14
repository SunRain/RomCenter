package com.magicmod.romcenter.utils;

import android.content.Context;
import android.os.Environment;
import android.os.PowerManager;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;

import com.magicmod.cloudserver.utils.Constants;
import com.magicmod.romcenter.R;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class UtilTools {

    public static final String getStorageDir() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }
    
    public static void triggerUpdate(Context context, String updateFileName) throws IOException {
        /*
         * Should perform the following steps.
         * 1.- mkdir -p /cache/recovery
         * 2.- echo 'boot-recovery' > /cache/recovery/command
         * 3.- if(mBackup) echo '--nandroid'  >> /cache/recovery/command
         * 4.- echo '--update_package=SDCARD:update.zip' >> /cache/recovery/command
         * 5.- reboot recovery
         */

        // Set the 'boot recovery' command
        Process p = Runtime.getRuntime().exec("sh");
        OutputStream os = p.getOutputStream();
        os.write("mkdir -p /cache/recovery/\n".getBytes());
        os.write("echo 'boot-recovery' >/cache/recovery/command\n".getBytes());

        // See if backups are enabled and add the nandroid flag
        /* TODO: add this back once we have a way of doing backups that is not recovery specific
           if (mPrefs.getBoolean(Constants.BACKUP_PREF, true)) {
           os.write("echo '--nandroid'  >> /cache/recovery/command\n".getBytes());
           }
           */

        // Add the update folder/file name
        // Emulated external storage moved to user-specific paths in 4.2
        String userPath = Environment.isExternalStorageEmulated() ? ("/" + UserHandle.myUserId()) : "";

        String cmd = "echo '--update_package=" + getStorageMountpoint(context) + userPath
            + "/" + Constants.UPDATES_FOLDER + "/" + updateFileName
            + "' >> /cache/recovery/command\n";
        os.write(cmd.getBytes());
        os.flush();

        // Trigger the reboot
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        powerManager.reboot("recovery");
    }

    private static String getStorageMountpoint(Context context) {
        StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        StorageVolume[] volumes = sm.getVolumeList();
        String primaryStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        boolean alternateIsInternal = context.getResources().getBoolean(R.bool.alternateIsInternal);

        if (volumes.length <= 1) {
            // single storage, assume only /sdcard exists
            return "/sdcard";
        }

        for (int i = 0; i < volumes.length; i++) {
            StorageVolume v = volumes[i];
            if (v.getPath().equals(primaryStoragePath)) {
                /* This is the primary storage, where we stored the update file
                 *
                 * For CM10, a non-removable storage (partition or FUSE)
                 * will always be primary. But we have older recoveries out there
                 * in which /sdcard is the microSD, and the internal partition is
                 * mounted at /emmc.
                 *
                 * At buildtime, we try to automagically guess from recovery.fstab
                 * what's the recovery configuration for this device. If "/emmc"
                 * exists, and the primary isn't removable, we assume it will be
                 * mounted there.
                 */
                if (!v.isRemovable() && alternateIsInternal) {
                    return "/emmc";
                }
            };
        }
        // Not found, assume non-alternate
        return "/sdcard";
    }
    
    public static String[] getDownloadedOtaFiles() {
        return new File(UtilTools.getStorageDir() + "/" + Constants.UPDATES_FOLDER).list();
    }

    public static boolean contain(final String collection[], final String key) {
        if (collection == null || collection.length == 0)
            return false;
        for (int i=0; i<collection.length; i++) {
            if (collection[i].equals(key))
                return true;
        }
        return false;
    }

    public static void cacheVaule(Context context, final String key, final String value) {
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .edit()
            .putString(key, value)
            .apply();
    }
    
    public static String getCachedValue(Context context, final String key, final String defaultValue) {
        return PreferenceManager
                .getDefaultSharedPreferences(context)
                .getString(key, defaultValue);
    }

    public static void removeCachedValeu(Context context, final String key) {
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .edit()
            .remove(key)
            .apply();
    }
    
    public static Spanned getUrlString(String urlLink, String displayText) {
        String link = String.format("<a href=\"%s\">%s</a>", urlLink, displayText);
        return Html.fromHtml(link);
    }
}
