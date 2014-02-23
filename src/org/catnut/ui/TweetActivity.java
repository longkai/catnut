/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.ui;

import android.app.Activity;
import android.os.Bundle;
import org.catnut.fragment.TweetFragment;
import org.catnut.util.Constants;

/**
 * 微博界面
 *
 * @author longkai
 */
public class TweetActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			long id = getIntent().getLongExtra(Constants.ID, 0L);
			getFragmentManager().beginTransaction()
					.replace(android.R.id.content, TweetFragment.getFragment(id))
					.commit();
		}
	}
}