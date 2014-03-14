
package com.magicmod.romcenter;

import android.os.Bundle;
import android.app.ActionBar;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.magicmod.romcenter.fragment.HomeFragment;
import com.magicmod.romcenter.fragment.OtaFragment;
import com.magicmod.romcenter.utils.Constants;
import com.special.ResideMenu.ResideMenu;
import com.special.ResideMenu.ResideMenuItem;

public class MainActivity extends FragmentActivity {
    private static final String TAG = "MainActivity";
    private static final boolean DBG = Constants.DEBUG;
    
    private boolean isMenuOpened = false;
    private ResideMenu mResideMenu;
    private ResideMenuItem mHomeMenuItem;
    private ResideMenuItem mOtaMenuItem;

    HomeFragment mHomeFragment;
    OtaFragment mOtaFragment;
    ActionBar mActionBar;
    Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mContext = this.getApplicationContext();
        mActionBar = this.getActionBar();
        mActionBar.setHomeButtonEnabled(true);

        setResideMenu();

        mActionBar.setTitle(R.string.title_home);
        
        mHomeFragment = new HomeFragment();
        mOtaFragment = new OtaFragment();
        changeFragment(mHomeFragment);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
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
        
        mResideMenu.addMenuItem(mHomeMenuItem);
        mResideMenu.addMenuItem(mOtaMenuItem);
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
            }
            mResideMenu.closeMenu();
            isMenuOpened = false;
        }
    };
    
    private void changeFragment(Fragment targetfFragment) {
        this.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_fragment, targetfFragment, "targetFragment")
                .setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
        
    }

}
