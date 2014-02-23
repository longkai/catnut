/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import com.google.analytics.tracking.android.EasyTracker;
import org.catnut.R;
import org.catnut.core.CatnutApp;
import org.catnut.fragment.TweetFragment;
import org.catnut.util.Constants;

/**
 * 微博界面
 *
 * @author longkai
 */
public class TweetActivity extends Activity {

	private EasyTracker mTracker;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			if (CatnutApp.getTingtingApp().getPreferences()
					.getBoolean(getString(R.string.pref_enable_analytics), true)) {
				mTracker = EasyTracker.getInstance(this);
			}
			long id = getIntent().getLongExtra(Constants.ID, 0L);
			getFragmentManager().beginTransaction()
					.replace(android.R.id.content, TweetFragment.getFragment(id))
					.commit();
			ActionBar bar = getActionBar();
			bar.setDisplayShowHomeEnabled(false);
			bar.setDisplayHomeAsUpEnabled(true);
		}
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
				this.navigateUpTo(getIntent());
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}
}