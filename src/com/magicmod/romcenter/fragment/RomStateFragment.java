package com.magicmod.romcenter.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import com.magicmod.romcenter.R;
import com.magicmod.romcenter.receiver.StateReportingServiceReceiver;
import com.magicmod.romcenter.utils.Constants;
import com.magicmod.romcenter.utils.UtilTools;

public class RomStateFragment extends PreferenceFragment implements 
DialogInterface.OnClickListener, DialogInterface.OnDismissListener, Preference.OnPreferenceChangeListener {
    private static final String TAG = "RomStateFragment";
    private static final boolean DBG = Constants.DEBUG;
    
    
    private static final String VIEW_STATS = "pref_view_stats";
    private static final String PREF_FILE_NAME = "MMStats";
    
    private static final String UNIQUE_ID = "preview_id";
    private static final String DEVICE = "preview_device";
    private static final String VERSION = "preview_version";
    private static final String COUNTRY = "preview_country";
    private static final String CARRIER = "preview_carrier";

    private CheckBoxPreference mEnableReporting;
    private Preference mViewStats;

    private Dialog mOkDialog;

    private SharedPreferences mPrefs;
    private boolean mOkClicked;

    public static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREF_FILE_NAME, 0);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.rom_state);
        

        mPrefs = this.getActivity().getSharedPreferences(PREF_FILE_NAME, 0);

        PreferenceScreen prefSet = getPreferenceScreen();
        mEnableReporting = (CheckBoxPreference) prefSet.findPreference(Constants.ANONYMOUS_OPT_IN);
        mViewStats = (Preference) prefSet.findPreference(VIEW_STATS);
        
        Context context = this.getActivity();
        prefSet.findPreference(UNIQUE_ID).setSummary(UtilTools.getUniqueID(context));
        prefSet.findPreference(DEVICE).setSummary(UtilTools.getDevice());
        prefSet.findPreference(VERSION).setSummary(UtilTools.getModVersion());
        prefSet.findPreference(COUNTRY).setSummary(UtilTools.getCountryCode(context));
        prefSet.findPreference(CARRIER).setSummary(UtilTools.getCarrier(context));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (!mOkClicked) {
            mEnableReporting.setChecked(false);
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            mOkClicked = true;
            mPrefs.edit().putBoolean(Constants.ANONYMOUS_OPT_IN, true).apply();
            StateReportingServiceReceiver.launchService(getActivity());
        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
            mEnableReporting.setChecked(false);
        } else {
            Uri uri = Uri.parse("http://www.cyanogenmod.org/blog/cmstats-what-it-is-and-why-you-should-opt-in");
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        }
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mEnableReporting) {
            if (mEnableReporting.isChecked()) {
                // Display the confirmation dialog
                mOkClicked = false;
                if (mOkDialog != null) {
                    mOkDialog.dismiss();
                }
                mOkDialog = new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.anonymous_statistics_warning)
                        .setTitle(R.string.anonymous_statistics_warning_title)
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setPositiveButton(android.R.string.yes, this)
                        .setNeutralButton(R.string.anonymous_learn_more, this)
                        .setNegativeButton(android.R.string.no, this)
                        .show();
                mOkDialog.setOnDismissListener(this);
            } else {
                // Disable reporting
                mOkClicked = true;
                if (mOkDialog != null) {
                    mOkDialog.dismiss();
                }
                mOkDialog = new AlertDialog.Builder(getActivity())
                        .setMessage(R.string.anonymous_statistics_disable_warning)
                        .setTitle(R.string.anonymous_statistics_warning_title)
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setPositiveButton(android.R.string.yes, this)
                        .show();
                mPrefs.edit().putBoolean(Constants.ANONYMOUS_OPT_IN, false).apply();
            }
        } else if (preference == mViewStats) {
            // Display the stats page
            Uri uri = Uri.parse(Constants.SITE_STATE_URL);
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return true;
    }

}
