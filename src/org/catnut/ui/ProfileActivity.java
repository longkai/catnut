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
import org.catnut.fragment.ProfileFragment;
import org.catnut.metadata.User;
import org.catnut.util.Constants;

/**
 * 用户信息界面
 *
 * @author longkai
 */
public class ProfileActivity extends Activity {

	private CatnutApp mApp;
	private EasyTracker mTracker;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			mApp = CatnutApp.getTingtingApp();
			if (mApp.getPreferences().getBoolean(getString(R.string.pref_enable_analytics), true)) {
				mTracker = EasyTracker.getInstance(this);
			}
			String screenName = getIntent().getStringExtra(User.screen_name);
			long uid = getIntent().getLongExtra(Constants.ID, 0L);
			getFragmentManager()
					.beginTransaction()
					.replace(android.R.id.content, ProfileFragment.getInstance(screenName, uid))
					.commit();
			ActionBar bar = getActionBar();
			bar.setDisplayShowHomeEnabled(false);
			bar.setDisplayHomeAsUpEnabled(true);
			bar.setTitle(screenName);
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