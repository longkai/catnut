/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;
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
	private ProgressBar mProgressBar;

	private Target mTarget = new Target() {
		@Override
		public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
			mFantasy.setImageBitmap(bitmap);
			mProgressBar.setVisibility(View.GONE);
		}

		@Override
		public void onBitmapFailed(Drawable errorDrawable) {
			mProgressBar.setVisibility(View.GONE);
			mFantasy.setImageDrawable(errorDrawable);
		}

		@Override
		public void onPrepareLoad(Drawable placeHolderDrawable) {
			if (getArguments().getBoolean(FIT_XY)) {
				mFantasy.setImageDrawable(placeHolderDrawable);
				mProgressBar.setVisibility(View.GONE);
			} else {
				mProgressBar.setVisibility(View.VISIBLE);
			}
		}
	};

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
		View view = inflater.inflate(R.layout.photo, container, false);
		mProgressBar = (ProgressBar) view.findViewById(android.R.id.progress);
		mFantasy = (TouchImageView) view.findViewById(R.id.image);
		mFantasy.setMaxZoom(4f);
		if (getArguments().getBoolean(FIT_XY)) {
			mFantasy.setScaleType(ImageView.ScaleType.FIT_XY);
		}
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		Bundle args = getArguments();
		RequestCreator creator = Picasso.with(getActivity())
				.load(args.getString(TAG));
		if (getArguments().getBoolean(FIT_XY)) {
			creator.placeholder(R.drawable.default_fantasy);
		}
		creator.error(R.drawable.error)
				.into(mTarget);
	}
}
