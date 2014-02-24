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
import org.catnut.R;

/**
 * 发微博
 *
 * @author longkai
 */
public class ComposeTweetActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			setContentView(R.layout.compose);
		}
		ActionBar bar = getActionBar();
		bar.setIcon(R.drawable.ic_title_compose);
		bar.setDisplayHomeAsUpEnabled(true);
		bar.setTitle(R.string.compose);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				navigateUpTo(getIntent());
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}
}