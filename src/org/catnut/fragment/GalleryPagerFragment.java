/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.squareup.picasso.Picasso;
import org.catnut.R;
import org.catnut.support.TouchImageView;
import org.catnut.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * 以pager的形式查看一组（个）图片
 *
 * @author longkai
 */
public class GalleryPagerFragment extends Fragment {
	public static final String TAG = GalleryPagerFragment.class.getSimpleName();

	public static final String CUR_INDEX = "cur_index";
	public static final String URLS = "urls";
	public static final String TITLE = "title";

	private List<Uri> mUrls;
	private int mCurrentIndex;

	public static GalleryPagerFragment getFragment(int currentIndex, ArrayList<Uri> urls, String actionBarTitle) {
		Bundle args = new Bundle();
		args.putInt(CUR_INDEX, currentIndex);
		args.putParcelableArrayList(URLS, urls);
		args.putString(TITLE, actionBarTitle);
		GalleryPagerFragment fragment = new GalleryPagerFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mUrls = getArguments().getParcelableArrayList(URLS);
		mCurrentIndex = getArguments().getInt(CUR_INDEX);
	}

	@Override
	public void onStart() {
		super.onStart();
		getActivity().getActionBar().setTitle(getArguments().getString(TITLE));
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.pager, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		ViewPager viewPager = (ViewPager) view;
		viewPager.setAdapter(new FragmentStatePagerAdapter(getFragmentManager()) {
			@Override
			public Fragment getItem(int position) {
				return SimpleImageFragment.getFragment(mUrls.get(position));
			}

			@Override
			public int getCount() {
				return mUrls.size();
			}
		});
		viewPager.setCurrentItem(mCurrentIndex);
	}

	public static class SimpleImageFragment extends Fragment {

		public static SimpleImageFragment getFragment(Uri url) {
			Bundle args = new Bundle();
			args.putParcelable(Constants.PIC, url);
			SimpleImageFragment fragment = new SimpleImageFragment();
			fragment.setArguments(args);
			return fragment;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View view = inflater.inflate(R.layout.photo, container, false);
			view.setBackgroundColor(getResources().getColor(R.color.black_background));
			TouchImageView image = (TouchImageView) view.findViewById(R.id.image);
			Picasso.with(getActivity())
					.load((Uri) getArguments().getParcelable(Constants.PIC))
					.error(R.drawable.error)
					.into(image);
			return view;
		}
	}
}
