/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import tingting.chen.R;

/**
 * 应用偏好设置
 *
 * @author longkai
 */
public class PrefFragment extends PreferenceFragment {

	public static final String IS_FIRST_RUN = "first_run";

	public static final String AUTO_FETCH_ON_START = "auto_fetch_on_start";
	public static final String DEFAULT_FETCH_SIZE = "default_fetch_size";
	public static final String AUTO_LOAD_MORE_FROM_CLOUD = "auto_load_more_from_cloud";
	public static final String SHOW_TWEET_THUMBS = "show_tweet_thumbs";
	public static final String ABOUT = "about";
	public static final String AUTHOR = "author";
	public static final String SOURCE_CODE = "source_code";
	public static final String OPEN_SOURCE_LICENSE = "open_source_license";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref);
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		if (preference.hasKey()) {
			String key = preference.getKey();
			Intent intent = null;
			if (key.equals(SOURCE_CODE)) {
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_link)));
			} else if (key.equals(AUTHOR)) {
				intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "im.longkai@gmail.com", null));
				intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
			} else if (key.equals(ABOUT)) {
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.commit_link)));
			} else if (key.equals(OPEN_SOURCE_LICENSE)) {
				// todo: set open source license!
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://opensource.org/licenses/mit-license.php"));
			}
			if (intent != null) {
				startActivity(intent);
				return true;
			}
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}
}
