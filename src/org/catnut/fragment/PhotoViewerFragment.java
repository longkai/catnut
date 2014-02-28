/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.squareup.picasso.Picasso;
import org.catnut.R;
import org.catnut.support.TouchImageView;

/**
 * 照片查看器
 *
 * @author longkai
 */
public class PhotoViewerFragment extends Fragment {

	private static final String TAG = "PhotoViewerFragment";

	private String mUri;

	public static PhotoViewerFragment getFragment(String url) {
		Bundle args = new Bundle();
		args.putString(TAG, url);
		PhotoViewerFragment fragment = new PhotoViewerFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		ActionBar bar = activity.getActionBar();
		bar.setTitle(activity.getString(R.string.view_photos));
		bar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.actionbar_overlay)));
		bar.setIcon(R.drawable.ic_title_content_picture_dark);
		mUri = getArguments().getString(TAG);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		TouchImageView view = new TouchImageView(getActivity());
		view.setMaxZoom(3.5f); // 放大一些，有些长微博畸形啊。。。
		view.setBackground(new ColorDrawable(getResources().getColor(R.color.black_background)));
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		Picasso.with(getActivity())
				.load(mUri)
				.error(R.drawable.error)
				.placeholder(R.drawable.error)
				.into((ImageView) view);
	}
}
