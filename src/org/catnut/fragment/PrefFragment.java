/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

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
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;
import org.catnut.R;
import org.catnut.core.CatnutApp;

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

	private static final String TAG = "PrefFragment";

	private static final int CUSTOMIZE_FONT_REQUEST_CODE = 1;

	private SharedPreferences mPref;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.pref);
		mPref = CatnutApp.getTingtingApp().getPreferences();
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
			if (key.equals(getString(R.string.pref_source_code))) {
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_link)));
			} else if (key.equals(getString(R.string.pref_author))) {
				intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "im.longkai@gmail.com", null));
				intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
			} else if (key.equals(getString(R.string.pref_about))) {
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.commit_link)));
			} else if (key.equals(getString(R.string.pref_open_source_license))) {
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
					Log.e(TAG, "error open license file from assets!", e);
				} finally {
					if (inputStream != null) {
						try {
							inputStream.close();
						} catch (IOException e) {
						}
					}
				}
				return true;
			} else if (key.equals(getString(R.string.pref_customize_tweet_font))) {
				new AlertDialog.Builder(getActivity())
					.setMessage(getString(R.string.customized_font_message))
					.setNegativeButton(getString(R.string.keep_current_font), this)
					.setNeutralButton(getString(R.string.use_default_font), this)
					.setPositiveButton(getString(R.string.customize_font), this)
					.show();
				return true;
			} else if (key.equals(getString(R.string.pref_notes))) {
				InputStream inputStream = null;
				try {
					inputStream = getActivity().getAssets().open("notes.txt");
					View v = LayoutInflater.from(getActivity()).inflate(R.layout.scroll_text, null);
					TextView tv = (TextView) v.findViewById(android.R.id.text1);
					tv.setText(new Scanner(inputStream).useDelimiter("\\A").next());
					new AlertDialog.Builder(getActivity())
						.setTitle("Notes")
						.setView(v)
						.setNeutralButton(android.R.string.ok, null)
						.show();
				} catch (IOException e) {
					Log.e(TAG, "error open notes from assets!", e);
				} finally {
					if (inputStream != null) {
						try {
							inputStream.close();
						} catch (IOException e) {
						}
					}
				}
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
					.putString(getString(R.string.pref_customize_tweet_font), path)
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
					.remove(getString(R.string.pref_customize_tweet_font))
					.commit();
				break;
			// 保持现有字体
			case DialogInterface.BUTTON_NEGATIVE:
			default:
				break;
		}
	}
}
