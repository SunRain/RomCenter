
package com.magicmod.romcenter.utils;

import android.R.integer;
import android.content.Context;
import android.os.Environment;
import android.os.PowerManager;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
//import android.os.storage.StorageVolume;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectionTools {

    public static String getProperty(String property) {
        Class clazz;
        try {
            clazz = Class.forName("android.os.SystemProperties");
            Method m = clazz.getMethod("get", String.class);
            return (String) m.invoke(clazz, property);
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Log.d("aaaaaaa", "=============================== Cant find value");
        return "";
    }
    
    public static String getStorageMountpoint(Context context, boolean alternateIsInternal ) {
        StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);

        StorageVolume[] volumes = null;//=sm.getVolumeList();
        try {
            volumes = (StorageVolume[]) sm.getClass().getMethod("getVolumeList", new Class[0]).invoke(sm, new Object[0]);
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String primaryStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        //boolean alternateIsInternal = context.getResources().getBoolean(R.bool.alternateIsInternal);

        if (volumes == null || volumes.length <= 1) {
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
    
    public static void triggerUpdate(Context context, String updateFolder,String updateFileName, boolean alternateIsInternal) throws IOException {
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

        int userId = 0;
        try {
            // Add the update folder/file name
            // Emulated external storage moved to user-specific paths in 4.2
            Class cls = Class.forName("android.os.UserHandle");
            Method method = cls.getMethod("myUserId", new Class[0]);
            userId = ((Integer) method.invoke(cls, new Object[0])).intValue();
        }  catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String userPath = Environment.isExternalStorageEmulated() ? ("/" + String.valueOf(userId)) : "";
        
        Log.d("ReflectionTools", "==============="+ userPath);

        String cmd = "echo '--update_package=" + getStorageMountpoint(context, alternateIsInternal) + userPath
            + "/" + updateFolder + "/" + updateFileName
            + "' >> /cache/recovery/command\n";
        os.write(cmd.getBytes());
        os.flush();

        // Trigger the reboot
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        powerManager.reboot("recovery");
    }
}
