/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.volley.toolbox.ImageLoader;
import org.catnut.R;
import org.catnut.core.CatnutApp;
import org.catnut.metadata.User;

/**
 * 用户信息的封面界面
 *
 * @author longkai
 */
public class CoverImageFragment extends Fragment {

	private CatnutApp mApp;

	private ImageView mAvatar;
	private TextView mScreenName;
	private TextView mRemark;

	public static CoverImageFragment getFragment(String avatarUrl, String screenName,
				String remark, boolean verified) {
		Bundle args = new Bundle();
		args.putString(User.avatar_large, avatarUrl);
		args.putString(User.screen_name, screenName);
		args.putString(User.remark, remark);
		args.putBoolean(User.verified, verified);
		CoverImageFragment fragment = new CoverImageFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mApp = CatnutApp.getTingtingApp();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Bundle args = getArguments();
		View view = inflater.inflate(R.layout.profile_cover, container, false);
		mAvatar = (ImageView) view.findViewById(R.id.avatar);
		mApp.getImageLoader().get(args.getString(User.avatar_large),
				ImageLoader.getImageListener(mAvatar, R.drawable.error, R.drawable.error));
		String screenName = args.getString(User.screen_name);
		mScreenName = (TextView) view.findViewById(R.id.screen_name);
		mScreenName.setText("@" + screenName);
		mRemark = (TextView) view.findViewById(R.id.remark);
		// 如果说没有备注的话那就和微博id一样
		String remark = args.getString(User.remark, screenName);
		mRemark.setText(TextUtils.isEmpty(remark) ? screenName : remark);
		if (args.getBoolean(User.verified, false)) {
			view.findViewById(R.id.verified).setVisibility(View.VISIBLE);
		}
		return view;
	}
}
