/*************************************************************************

Copyright 2014 MagicMod Project

This file is part of MagicMod Weather.

MagicMod Weather is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

MagicMod Weather is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with MagicMod Weather. If not, see <http://www.gnu.org/licenses/>.

*************************************************************************/

package com.magicmod.romcenter.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.magicmod.romcenter.R;

public class RotateImageView extends ImageView {
	private Animation mRotateAnimation;
	private boolean isAnim;

	public RotateImageView(Context context) {
		this(context, null);
		mRotateAnimation = AnimationUtils.loadAnimation(context,
				R.anim.refresh_rotate);
		isAnim = false;
	}

	public RotateImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
		mRotateAnimation = AnimationUtils.loadAnimation(context,
				R.anim.refresh_rotate);
		isAnim = false;
	}

	public RotateImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mRotateAnimation = AnimationUtils.loadAnimation(context,
				R.anim.refresh_rotate);
		isAnim = false;
	}
	public boolean isStartAnim(){
		return isAnim;
	}
	public synchronized void startAnim() {
		stopAnim();
		this.startAnimation(mRotateAnimation);
		isAnim = true;

	}

	public synchronized void stopAnim() {
		if (isAnim){
			this.clearAnimation();
			isAnim = false;
		}
	}
}
