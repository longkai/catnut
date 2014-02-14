/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.fragment;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;
import tingting.chen.R;
import tingting.chen.tingting.TingtingApp;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * 应用偏好设置
 *
 * @author longkai
 */
public class PrefFragment extends PreferenceFragment implements DialogInterface.OnClickListener {

	public static final int CUSTOMIZE_FONT_REQUEST_CODE = 1;

	public static final String IS_FIRST_RUN = "first_run";

	public static final String AUTO_FETCH_ON_START = "auto_fetch_on_start";
	public static final String DEFAULT_FETCH_SIZE = "default_fetch_size";
	public static final String AUTO_LOAD_MORE_FROM_CLOUD = "auto_load_more_from_cloud";
	public static final String TWEET_FONT_SIZE = "tweet_font_size";
	public static final String SHOW_TWEET_THUMBS = "show_tweet_thumbs";
	public static final String CUSTOMIZE_TWEET_FONT = "customize_tweet_font";
	public static final String ABOUT = "about";
	public static final String AUTHOR = "author";
	public static final String SOURCE_CODE = "source_code";
	public static final String OPEN_SOURCE_LICENSE = "open_source_license";

	private SharedPreferences mPref;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref);
		mPref = TingtingApp.getTingtingApp().getPreferences();
	}

	@Override
	public void onStart() {
		super.onStart();
		getActivity().getActionBar().setTitle(getText(R.string.pref));
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
				InputStream inputStream = null;
				try {
					inputStream = getActivity().getAssets().open("license.html");
					Scanner in = new Scanner(inputStream).useDelimiter("\\A");
					WebView html = new WebView(getActivity());
					html.loadDataWithBaseURL(null, in.next(), "text/html", "utf-8", null);
					new AlertDialog.Builder(getActivity())
						.setTitle("Open Source Licenses")
						.setView(html)
						.setNeutralButton(android.R.string.ok, null)
						.show();
				} catch (IOException e) {
					Log.e("preference fragment", "error open license file from assets!", e);
				} finally {
					if (inputStream != null) {
						try {
							inputStream.close();
						} catch (IOException e) {
							Log.wtf("preference fragment", "error closing input stream!", e);
						}
					}
				}
			} else if (key.equals(CUSTOMIZE_TWEET_FONT)) {
				new AlertDialog.Builder(getActivity())
					.setMessage(getString(R.string.customized_font_message))
					.setNegativeButton(getString(R.string.keep_current_font), this)
					.setNeutralButton(getString(R.string.use_default_font), this)
					.setPositiveButton(getString(R.string.customize_font), this)
					.show();
				return true;
			}
			if (intent != null) {
				startActivity(intent);
				return true;
			}
		}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == CUSTOMIZE_FONT_REQUEST_CODE && data != null) {
			// 简单的通过文件名后缀判断一下
			String path = data.getData().getPath();
			Pattern pattern = Pattern.compile("(?i)[^\\s]+(ttf|otf|fon|ttc)$");
			if (!pattern.matcher(path).matches()) {
				Toast.makeText(getActivity(),
					getString(R.string.supported_font_types), Toast.LENGTH_SHORT).show();
			} else {
				mPref.edit()
					.putString(CUSTOMIZE_TWEET_FONT, path)
					.commit();
			}
		}
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		Intent intent;
		switch (which) {
			// 自定义字体
			case DialogInterface.BUTTON_POSITIVE:
				intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType("font/opentype");
				startActivityForResult(Intent.createChooser(intent, getString(R.string.select_font_title)), CUSTOMIZE_FONT_REQUEST_CODE);
				break;
			// 使用默认字体
			case DialogInterface.BUTTON_NEUTRAL:
				mPref.edit()
					.remove(CUSTOMIZE_TWEET_FONT)
					.commit();
				break;
			// 保持现有字体
			case DialogInterface.BUTTON_NEGATIVE:
			default:
				break;
		}
	}
}
