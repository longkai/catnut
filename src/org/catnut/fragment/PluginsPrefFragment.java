/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.widget.Toast;
import org.catnut.R;
import org.catnut.core.CatnutApp;

import java.util.regex.Pattern;

/**
 * 插件设置
 *
 * @author longkai
 */
public class PluginsPrefFragment extends PreferenceFragment implements DialogInterface.OnClickListener {

	public static final int FANTASY = 1;
	public static final int ZHIHU = 2;

	private SharedPreferences mPref;

	public static PluginsPrefFragment getFragment() {
		return new PluginsPrefFragment();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.plugins);
		mPref = CatnutApp.getTingtingApp().getPreferences();
	}

	@Override
	public void onStart() {
		super.onStart();
		ActionBar bar = getActivity().getActionBar();
		bar.setIcon(R.drawable.ic_title_pref);
		bar.setTitle(getString(R.string.plugins_pref));
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
		if (preference.hasKey()) {
			String key = preference.getKey();
			if (key.equals(getString(R.string.pref_zhihu_typeface))) {
				new AlertDialog.Builder(getActivity())
						.setMessage(getString(R.string.customized_font_message))
						.setNegativeButton(getString(R.string.keep_current_font), this)
						.setNeutralButton(getString(R.string.use_default_font), this)
						.setPositiveButton(getString(R.string.customize_font), this)
						.show();
			}
		}
		return true;
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		Intent intent;
		switch (which) {
			// 自定义字体
			case DialogInterface.BUTTON_POSITIVE:
				intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType("font/opentype");
				startActivityForResult(Intent.createChooser(intent, getString(R.string.select_font_title)), 1);
				break;
			// 使用默认字体
			case DialogInterface.BUTTON_NEUTRAL:
				mPref.edit()
						.remove(getString(R.string.pref_zhihu_typeface))
						.commit();
				break;
			// 保持现有字体
			case DialogInterface.BUTTON_NEGATIVE:
			default:
				break;
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1 && data != null) {
			// 简单的通过文件名后缀判断一下
			String path = data.getData().getPath();
			Pattern pattern = Pattern.compile("(?i).+(ttf|otf|fon|ttc)$");
			if (!pattern.matcher(path).matches()) {
				Toast.makeText(getActivity(),
						getString(R.string.supported_font_types), Toast.LENGTH_SHORT).show();
			} else {
				mPref.edit()
						.putString(getString(R.string.pref_zhihu_typeface), path)
						.commit();
			}
		}
	}
}
