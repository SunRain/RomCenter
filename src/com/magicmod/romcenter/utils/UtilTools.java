package com.magicmod.romcenter.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;

import com.magicmod.cloudserver.netdisk.BaseItem;
import com.magicmod.cloudserver.netdisk.BaseItem.ItemType;
import com.magicmod.romcenter.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;

public class UtilTools {

    public static final String getStorageDir() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }
    
    public static void triggerUpdate(Context context, String updateFileName) throws IOException {
        boolean alternateIsInternal = context.getResources().getBoolean(R.bool.alternateIsInternal);
        String folder = Constants.UPDATES_FOLDER;
        
        ReflectionTools.triggerUpdate(context, folder, updateFileName, alternateIsInternal);
    }

    private static String getStorageMountpoint(Context context) {
        boolean alternateIsInternal = context.getResources().getBoolean(R.bool.alternateIsInternal);
        return ReflectionTools.getStorageMountpoint(context, alternateIsInternal);
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
    
    public static SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
    
    public static boolean cacheRunningDownloadItem(Context context, BaseItem item) {
        if (item == null) {
            getSharedPreferences(context)
                .edit()
                .putString(Constants.OTA_RUNNING_DL_ITEM, "")
                .apply();
            return true;
        }
        JSONObject object = new JSONObject();
        try {
            object.put("key_remoteName", item.getRemoteName());
            object.put("key_remotePath", item.getRemotePath());
            object.put("key_localPath", item.getLocalPath());
            object.put("key_remoteMD5", item.getRemoteMd5());
            object.put("key_remoteSize", item.getRemoteSize());
            object.put("key_itemType", item.getItemType().toString());
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        getSharedPreferences(context)
            .edit()
            .putString(Constants.OTA_RUNNING_DL_ITEM, object.toString())
            .apply();

        return true;
    }
    
    public static BaseItem getRunningDownloadItem(Context context) {
        String jString = getSharedPreferences(context)
                .getString(Constants.OTA_RUNNING_DL_ITEM, "");
        BaseItem item = null;
        try {
            JSONObject object = new JSONObject(jString);
            String remoteName = object.getString("key_remoteName");
            String remotePath = object.getString("key_remotePath");
            String localPath = object.getString("key_localPath");
            String remoteMd5 = object.getString("key_remoteMD5");
            int remoteSize = object.getInt("key_remoteSize");
            ItemType type = Enum.valueOf(ItemType.class, object.getString("key_itemType"));
            item = new BaseItem(remoteName, remotePath, localPath, remoteMd5, remoteSize,
                    type);
        } catch (Exception e) {
            e.printStackTrace();
            item = null;
        }
        return item;
    }

    public static boolean cacheAvailableUpdate(Context context, ArrayList<BaseItem> items) {
        if (items == null || items.isEmpty()) {
            getSharedPreferences(context)
                .edit()
                .putString(Constants.OTA_CACHE_LIST, "")
                .apply();
            return true;
        }
        JSONArray array = new JSONArray();
        for (BaseItem item : items) {
            JSONObject object = new JSONObject();
            try {
                object.put("key_remoteName", item.getRemoteName());
                object.put("key_remotePath", item.getRemotePath());
                object.put("key_localPath", item.getLocalPath());
                object.put("key_remoteMD5", item.getRemoteMd5());
                object.put("key_remoteSize", item.getRemoteSize());
                object.put("key_itemType", item.getItemType().toString());
            } catch (JSONException e) {
                e.printStackTrace();
                return false;
            }
            array.put(object);      
        }
        getSharedPreferences(context)
            .edit()
            .putString(Constants.OTA_CACHE_LIST, array.toString())
            .apply();

        return true;
    }
    
    public static ArrayList<BaseItem> getCachedUpdate(Context context) {
        String jsArray = getSharedPreferences(context).getString(Constants.OTA_CACHE_LIST, "");
        JSONArray array = null;
        try {
            array = new JSONArray(jsArray);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
        
        ArrayList<BaseItem> items = null;
        
        try {
            items = new ArrayList<BaseItem>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                String remoteName = object.getString("key_remoteName");
                String remotePath = object.getString("key_remotePath");
                String localPath = object.getString("key_localPath");
                String remoteMd5 = object.getString("key_remoteMD5");
                int remoteSize = object.getInt("key_remoteSize");
                ItemType type = Enum.valueOf(ItemType.class, object.getString("key_itemType"));
                BaseItem item = new BaseItem(remoteName, remotePath, localPath, remoteMd5,
                        remoteSize, type);
                items.add(item);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return items;
    }
    
    public static String getUniqueID(Context context) {
        final String id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        return digest(context.getPackageName() + id);
    }

    public static String getCarrier(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String carrier = tm.getNetworkOperatorName();
        if (TextUtils.isEmpty(carrier)) {
            carrier = "Unknown";
        }
        return carrier;
    }

    public static String getCarrierId(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String carrierId = tm.getNetworkOperator();
        if (TextUtils.isEmpty(carrierId)) {
            carrierId = "0";
        }
        return carrierId;
    }

    public static String getCountryCode(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String countryCode = tm.getNetworkCountryIso();
        if (TextUtils.isEmpty(countryCode)) {
            countryCode = "Unknown";
        }
        return countryCode;
    }

    public static String getDevice() {
        String s = ReflectionTools.getProperty("ro.mm.device");
        if (TextUtils.isEmpty(s)) {
            s = "Unknown";
        }
        return s;
    }

    public static String getModVersion() {
        String s = ReflectionTools.getProperty("ro.mm.version");
        if (TextUtils.isEmpty(s)) {
            s = "Unknown";
        }
        return s;
    }
    
    public static String getRomName() {
        String s = ReflectionTools.getProperty("ro.mm.mmname");
        if (TextUtils.isEmpty(s)) {
            s = "Unknown";
        }
        return s;
    }

    public static String getRomVersion(){
        String s = ReflectionTools.getProperty("ro.mm.mmversion");
        if (TextUtils.isEmpty(s)) {
            s = "Unknown";
        }
        return s;
    }
    public static String digest(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return new BigInteger(1, md.digest(input.getBytes())).toString(16).toUpperCase();
        } catch (Exception e) {
            return null;
        }
    }
}
