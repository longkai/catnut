/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.app.ActionBar;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import org.catnut.R;

/**
 * 插件设置
 *
 * @author longkai
 */
public class PluginsPrefFragment extends PreferenceFragment {

	public static final int ZHIHU = 1;

	public static PluginsPrefFragment getFragment() {
		return new PluginsPrefFragment();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.plugins);
	}

	@Override
	public void onStart() {
		super.onStart();
		ActionBar bar = getActivity().getActionBar();
		bar.setIcon(R.drawable.ic_title_pref);
		bar.setTitle(getString(R.string.plugins_pref));
	}
}
