
package com.magicmod.romcenter.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

public class CustomWebView extends WebView {
    public interface OnInitialContentReadyListener {
        void onInitialContentReady(WebView view);
    }

    private OnInitialContentReadyListener mListener;
    private boolean mContentReady = false;

    public CustomWebView(Context context) {
        super(context);
    }

    public CustomWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setOnInitialContentReadyListener(OnInitialContentReadyListener listener) {
        mListener = listener;
    }

    @Override
    public void invalidate() {
        super.invalidate();

        if (getContentHeight() > 0 && !mContentReady) {
            if (mListener != null) {
                mListener.onInitialContentReady(this);
            }
            mContentReady = true;
        }
    }
}
