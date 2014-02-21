/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.catnut.R;
import org.catnut.metadata.User;
import org.catnut.util.CatnutUtils;
import org.catnut.util.Constants;

/**
 * 用户封面的介绍信息
 *
 * @author longkai
 */
public class CoverIntroFragment extends Fragment {

	public static CoverIntroFragment getFragment(String description, String location, String domain) {
		Bundle args = new Bundle();
		args.putString(User.description, description);
		args.putString(User.location, location);
		args.putString(User.profile_url, domain);
		CoverIntroFragment fragment = new CoverIntroFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.profile_intro, container, false);
		Bundle args = getArguments();
		String desc = args.getString(User.description);
		CatnutUtils.setText(view, R.id.description, TextUtils.isEmpty(desc)
				? getString(R.string.no_description) : desc);
		CatnutUtils.setText(view, R.id.location, args.getString(User.location));
		CatnutUtils.setText(view, R.id.profile_url, Constants.WEIBO_DOMAIN + args.getString(User.profile_url));
		return view;
	}
}
