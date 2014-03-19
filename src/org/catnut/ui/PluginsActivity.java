/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import org.catnut.R;
import org.catnut.plugin.zhihu.ZhihuItemsFragment;

/**
 * 插件界面
 *
 * @author longkai
 */
public class PluginsActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.replace(android.R.id.content, ZhihuItemsFragment.getFragment())
					.commit();
		}
		getActionBar().setTitle(R.string.plugins);
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