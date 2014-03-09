/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.squareup.picasso.Picasso;
import org.catnut.R;
import org.catnut.support.TouchImageView;

/**
 * 查看fantasy
 *
 * @author longkai
 */
public class FantasyFragment extends Fragment {


	private static final String TAG = "FantasyFragment";

	private static final String DESC = "desc";
	private static final String FIT_XY = "fit_xy";

	private TouchImageView mFantasy;

	public static FantasyFragment getFragment(String url, String desc, boolean fitXY) {
		Bundle args = new Bundle();
		args.putString(TAG, url);
		args.putString(DESC, desc);
		args.putBoolean(FIT_XY, fitXY);
		FantasyFragment fantasyFragment = new FantasyFragment();
		fantasyFragment.setArguments(args);
		return fantasyFragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mFantasy = new TouchImageView(getActivity());
		if (getArguments().getBoolean(FIT_XY)) {
			mFantasy.setScaleType(ImageView.ScaleType.FIT_XY);
		}
		return mFantasy;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		Bundle args = getArguments();
		Picasso.with(getActivity())
				.load(args.getString(TAG))
				.placeholder(R.drawable.default_fantasy)
				.error(R.drawable.default_fantasy)
				.into(mFantasy);
	}
}
