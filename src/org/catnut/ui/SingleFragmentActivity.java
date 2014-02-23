/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.ui;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import org.catnut.fragment.PrefFragment;

/**
 * helper activity, just show a single fragment
 *
 * @author longkai
 */
public class SingleFragmentActivity extends Activity {

	public static final String TAG = "SingleFragmentActivity";
	public static final int PREF = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			int which = getIntent().getIntExtra(TAG, PREF);
			switch (which) {
				case PREF:
					ft.replace(android.R.id.content, PrefFragment.getFragment());
					break;
				default:
					break;
			}
			ft.commit();
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				navigateUpTo(getIntent());
				break;
			default:
				break;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	/**
	 * helper method for obtaining a intent which will go to this activity
	 */
	public static Intent getIntent(Context context, int which) {
		Intent intent = new Intent(context, SingleFragmentActivity.class);
		intent.putExtra(TAG, which);
		return intent;
	}
}