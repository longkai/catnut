/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.analytics.tracking.android.EasyTracker;
import org.catnut.R;
import org.catnut.api.UserAPI;
import org.catnut.core.CatnutApp;
import org.catnut.core.CatnutRequest;
import org.catnut.fragment.ProfileFragment;
import org.catnut.metadata.User;
import org.catnut.metadata.WeiboAPIError;
import org.catnut.processor.UserProcessor;
import org.catnut.util.Constants;
import org.json.JSONObject;

/**
 * 用户信息界面
 *
 * @author longkai
 */
public class ProfileActivity extends Activity implements FragmentManager.OnBackStackChangedListener {

	private CatnutApp mApp;
	private EasyTracker mTracker;

	private Handler mHandler = new Handler();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mApp = CatnutApp.getTingtingApp();
		if (savedInstanceState == null) {
			final FragmentManager fragmentManager = getFragmentManager();
			fragmentManager.addOnBackStackChangedListener(this);
			Intent intent = getIntent();
			final long uid = intent.getLongExtra(Constants.ID, 0L);
			Uri uri = intent.getData();
			if (uri != null) {
				final String screenName = uri.getLastPathSegment();
				final ProgressDialog dialog = ProgressDialog.show(this, null, getString(R.string.loading));
				dialog.show();
				mApp.getRequestQueue().add(new CatnutRequest(
						this,
						UserAPI.profile(screenName),
						new UserProcessor.UserProfileProcessor(),
						new Response.Listener<JSONObject>() {
							@Override
							public void onResponse(JSONObject response) {
								dialog.dismiss();
								fragmentManager.beginTransaction()
										.replace(android.R.id.content, ProfileFragment.getFragment(uid, screenName))
										.commit();
							}
						},
						new Response.ErrorListener() {
							@Override
							public void onErrorResponse(VolleyError error) {
								dialog.dismiss();
								WeiboAPIError weiboAPIError = WeiboAPIError.fromVolleyError(error);
								Toast.makeText(ProfileActivity.this, weiboAPIError.error, Toast.LENGTH_SHORT).show();
							}
						}
				));
			} else {
				String screenName = intent.getStringExtra(User.screen_name);
				fragmentManager
						.beginTransaction()
						.replace(android.R.id.content, ProfileFragment.getFragment(uid, screenName))
						.commit();
			}
			if (mApp.getPreferences().getBoolean(getString(R.string.pref_enable_analytics), true)) {
				mTracker = EasyTracker.getInstance(this);
			}
			ActionBar bar = getActionBar();
			bar.setIcon(R.drawable.ic_title_profile_default);
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
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, R.id.pref, Menu.NONE, R.string.pref)
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				this.navigateUpTo(getIntent());
				break;
			case R.id.pref:
				startActivity(SingleFragmentActivity.getIntent(this, SingleFragmentActivity.PREF));
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackStackChanged() {
		invalidateOptionsMenu();
	}

	public void flipCard(Fragment fragment, String tag, boolean backStack) {
		FragmentTransaction fragmentTransaction = getFragmentManager()
				.beginTransaction();
		fragmentTransaction
				.setCustomAnimations(
						R.animator.card_flip_right_in, R.animator.card_flip_right_out,
						R.animator.card_flip_left_in, R.animator.card_flip_left_out)
				.replace(android.R.id.content, fragment, tag);
		if (backStack) {
			fragmentTransaction.addToBackStack(null);
		}
		fragmentTransaction.commit();
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				invalidateOptionsMenu();
			}
		});
	}
}