/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.catnut.support;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.TextView;
import org.catnut.R;

/**
 * modify to support a confirm utility by longkai
 */
public class ConfirmBarController {
	private View mBarView;
	private TextView mMessageView;
	private ViewPropertyAnimator mBarAnimator;
	private Handler mHideHandler = new Handler();

	private ConfirmListener mConfirmListener;

	// State objects
	private Bundle mConfirmArgs;
	private CharSequence mConfirmMessage;

	public interface ConfirmListener {
		void onUndo(Bundle args);
	}

	public ConfirmBarController(View confirmBarView, ConfirmListener confirmListener) {
		mBarView = confirmBarView;
		mBarAnimator = mBarView.animate();
		mConfirmListener = confirmListener;

		mMessageView = (TextView) mBarView.findViewById(R.id.confirmbar_message);
		mBarView.findViewById(R.id.confirm_button)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						hideConfirmBar(false);
						mConfirmListener.onUndo(mConfirmArgs);
					}
				});

		hideConfirmBar(true);
	}

	public void showUndoBar(boolean immediate, CharSequence message, Bundle confirmArgs) {
		mConfirmArgs = confirmArgs;
		mConfirmMessage = message;
		mMessageView.setText(mConfirmMessage);

		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable,
				mBarView.getResources().getInteger(R.integer.undobar_hide_delay));

		mBarView.setVisibility(View.VISIBLE);
		if (immediate) {
			mBarView.setAlpha(1);
		} else {
			mBarAnimator.cancel();
			mBarAnimator
					.alpha(1)
					.setDuration(
							mBarView.getResources()
									.getInteger(android.R.integer.config_shortAnimTime))
					.setListener(null);
		}
	}

	public void hideConfirmBar(boolean immediate) {
		mHideHandler.removeCallbacks(mHideRunnable);
		if (immediate) {
			mBarView.setVisibility(View.GONE);
			mBarView.setAlpha(0);
			mConfirmMessage = null;
			mConfirmArgs = null;
		} else {
			mBarAnimator.cancel();
			mBarAnimator
					.alpha(0)
					.setDuration(mBarView.getResources()
							.getInteger(android.R.integer.config_shortAnimTime))
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mBarView.setVisibility(View.GONE);
							mConfirmMessage = null;
							mConfirmArgs = null;
						}
					});
		}
	}

	public void onSaveInstanceState(Bundle outState) {
		outState.putCharSequence("confirm_message", mConfirmMessage);
		outState.putBundle("confirm_args", mConfirmArgs);
	}

	public void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			mConfirmMessage = savedInstanceState.getCharSequence("confirm_message");
			mConfirmArgs = savedInstanceState.getBundle("confirm_args");

			if (mConfirmArgs != null || !TextUtils.isEmpty(mConfirmMessage)) {
				showUndoBar(true, mConfirmMessage, mConfirmArgs);
			}
		}
	}

	private Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			hideConfirmBar(false);
		}
	};
}

