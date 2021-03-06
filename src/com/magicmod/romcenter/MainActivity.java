
package com.magicmod.romcenter;

import android.os.Bundle;
import android.os.Environment;
import android.os.UserHandle;
import android.preference.PreferenceFragment;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import cn.jpush.android.api.JPushInterface;

import com.magicmod.romcenter.fragment.HomeFragment;
import com.magicmod.romcenter.fragment.OtaFragment;
import com.magicmod.romcenter.fragment.RomStateFragment;
import com.magicmod.romcenter.utils.Constants;
import com.magicmod.romcenter.utils.ReflectionTools;
import com.special.ResideMenu.ResideMenu;
import com.special.ResideMenu.ResideMenuItem;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final boolean DBG = Constants.DEBUG;
    
    private boolean isMenuOpened = false;
    private ResideMenu mResideMenu;
    private ResideMenuItem mHomeMenuItem;
    private ResideMenuItem mOtaMenuItem;
    private ResideMenuItem mRomStateItem;

    HomeFragment mHomeFragment;
    OtaFragment mOtaFragment;
    RomStateFragment mRomStateFragment;
    ActionBar mActionBar;
    Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        //String userPath = Environment.isExternalStorageEmulated() ? ("/" + UserHandle.myUserId()) : "";
        
        String getStorageMountpoint = ReflectionTools.getStorageMountpoint(this.getApplicationContext(), false);
        
        //Log.d(TAG, "==========  getStorageMountpoint " + getStorageMountpoint);
        
        mContext = this.getApplicationContext();
        mActionBar = this.getActionBar();
        mActionBar.setHomeButtonEnabled(true);

        setResideMenu();

        mActionBar.setTitle(R.string.title_home);
        
        mHomeFragment = new HomeFragment();
        mOtaFragment = new OtaFragment();
        mRomStateFragment = new RomStateFragment();
        changeFragment(mHomeFragment);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        JPushInterface.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        JPushInterface.onPause(this);
    }

    //动态的改变actionbar menu
    //使用 mActivity.getWindow().invalidatePanelMenu(Window.FEATURE_OPTIONS_PANEL);改变布局
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // TODO Auto-generated method stub
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (DBG) Log.d(TAG, "menu selected = > " + id);
        switch (id) {
            case android.R.id.home:
                mResideMenu.openMenu();
                break;

            default:
                break;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return mResideMenu.onInterceptTouchEvent(ev) || super.dispatchTouchEvent(ev);
    }

    @Override
    public void onBackPressed() {
        if (!isMenuOpened) {
            isMenuOpened = !isMenuOpened;
            mResideMenu.openMenu();
        } else {
            finish();
        }
    }

    private void setResideMenu() {
        mResideMenu = new ResideMenu(this);
        mResideMenu.setBackground(R.drawable.menu_background);
        mResideMenu.attachToActivity(this);
        mResideMenu.setMenuListener(mOnMenuListener);
        
        mHomeMenuItem = new ResideMenuItem(this, R.drawable.ic_home, R.string.menu_home);
        mHomeMenuItem.setOnClickListener(mOnResideMenuItemClickListener);
        mOtaMenuItem = new ResideMenuItem(this, R.drawable.ic_ota, R.string.menu_ota);
        mOtaMenuItem.setOnClickListener(mOnResideMenuItemClickListener);
        mRomStateItem = new ResideMenuItem(this, R.drawable.ic_state, R.string.menu_state);
        mRomStateItem.setOnClickListener(mOnResideMenuItemClickListener);
        
        mResideMenu.addMenuItem(mHomeMenuItem);
        mResideMenu.addMenuItem(mOtaMenuItem);
        mResideMenu.addMenuItem(mRomStateItem);
    }
    
    private ResideMenu.OnMenuListener mOnMenuListener = new ResideMenu.OnMenuListener() {
        
        @Override
        public void openMenu() {
            if (DBG) Log.d(TAG, "menu opened");
            isMenuOpened = true;            
        }
        
        @Override
        public void closeMenu() {
            if (DBG) Log.d(TAG, "menu closed");
            isMenuOpened = false;
            
        }
    };
    
    private OnClickListener mOnResideMenuItemClickListener = new OnClickListener() {
        
        @Override
        public void onClick(View v) {
            if (v == mOtaMenuItem) {
                mActionBar.setTitle(R.string.title_ota);
                isMenuOpened = true;
                changeFragment(mOtaFragment);
            } else if (v == mHomeMenuItem) {
                mActionBar.setTitle(R.string.title_home);
                isMenuOpened = true;
                changeFragment(mHomeFragment);
            } else if (v == mRomStateItem) {
                mActionBar.setTitle(R.string.title_state);
                isMenuOpened = true;
                changeFragment(mRomStateFragment);
            }
            mResideMenu.closeMenu();
            isMenuOpened = false;
        }
    };
    
    private void changeFragment(Fragment targetfFragment) {
        this.getFragmentManager()
                .beginTransaction()
                .replace(R.id.main_fragment, targetfFragment, "targetFragment")
                .setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
        
    }
    
    private void changeFragment(PreferenceFragment targerFragment) {
        this.getFragmentManager()
        .beginTransaction()
        .replace(R.id.main_fragment, targerFragment, "targetFragment")
        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
        .commit();
    }

}
