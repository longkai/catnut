/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import com.google.analytics.tracking.android.EasyTracker;
import org.catnut.R;
import org.catnut.core.CatnutApp;
import org.catnut.fragment.TweetFragment;
import org.catnut.support.OnFragmentBackPressedListener;
import org.catnut.util.Constants;

/**
 * 微博界面
 *
 * @author longkai
 */
public class TweetActivity extends Activity {

	private EasyTracker mTracker;

	private OnFragmentBackPressedListener mKeyDownListener;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (CatnutApp.getTingtingApp().getPreferences()
				.getBoolean(getString(R.string.pref_enable_analytics), true)) {
			mTracker = EasyTracker.getInstance(this);
		}
		ActionBar bar = getActionBar();
		bar.setIcon(R.drawable.ic_title_view_tweet);
		bar.setDisplayHomeAsUpEnabled(true);

		long id = getIntent().getLongExtra(Constants.ID, 0L);
		String json = getIntent().getStringExtra(Constants.JSON);
		if (savedInstanceState == null) {
			TweetFragment fragment = id != 0L
					? TweetFragment.getFragment(id)
					: TweetFragment.getFragment(json);
			// 添加back回调
			mKeyDownListener = fragment;
			getFragmentManager().beginTransaction()
					.replace(android.R.id.content, fragment)
					.commit();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK:
				if (mKeyDownListener != null) {
					mKeyDownListener.onBackPressed();
				}
				break;
			default:
				break;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (mTracker != null) {
			mTracker.activityStart(this);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (mTracker != null) {
			mTracker.activityStop(this);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				if (mKeyDownListener != null) {
					mKeyDownListener.onBackPressed();
				} else {
					this.navigateUpTo(getIntent());
				}
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}
}