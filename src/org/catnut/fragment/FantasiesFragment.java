/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.app.Fragment;
import android.app.FragmentManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.catnut.R;
import org.catnut.core.CatnutProvider;
import org.catnut.metadata.Photo;
import org.catnut.util.CatnutUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 以pager的形式查看fantasy
 *
 * @author longkai
 */
public class FantasiesFragment extends Fragment {

	private static final String TAG = "FantasiesFragment";
	public static final String PICS = "pics";
	public static final String DESCS = "descs";
	private static final int OFFSET = 3; // 更新pager的阈值
	private static final int NEW_SIZE = 10; // 每次取多少条

	private Handler mHandler = new Handler();

	private ViewPager mViewPager;
	private PagerAdapter mAdapter;
	private List<Fragment> mFragments;

	public static FantasiesFragment getFragment(String[] urls, String[] descs) {
		FantasiesFragment fantasiesFragment = new FantasiesFragment();
		Bundle args = new Bundle();
		args.putStringArray(PICS, urls);
		args.putStringArray(DESCS, descs);
		fantasiesFragment.setArguments(args);
		return fantasiesFragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.pager, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		mViewPager = (ViewPager) view;
		mAdapter = new EndlessPagerAdapter(getFragmentManager());
		mViewPager.setAdapter(mAdapter);
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
				if (mFragments.size() - position <= OFFSET) { // 动态添加
					new Thread(expand).start();
				}
			}
		});
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mFragments = new ArrayList<Fragment>();
		String[] initialPagers = getArguments().getStringArray(PICS);
		String[] descs = getArguments().getStringArray(DESCS);
		for (int i = 0; i < initialPagers.length; i++) {
			mFragments.add(FantasyFragment.getFragment(initialPagers[i], descs[i]));
		}
		getActivity().getActionBar().setTitle(R.string.fantasy);
	}

	private class EndlessPagerAdapter extends FragmentStatePagerAdapter {

		public EndlessPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			return mFragments.get(position);
		}

		@Override
		public int getCount() {
			return mFragments.size();
		}
	}

	private static final String[] PROJECTION = new String[] {
			Photo.image_url,
			Photo.description,
	};

	private Runnable expand = new Runnable() {
		@Override
		public void run() {
			String query = CatnutUtils.buildQuery(PROJECTION, null, Photo.TABLE, null, null, String.valueOf(mFragments.size() + NEW_SIZE));
			final Cursor cursor = getActivity().getContentResolver()
					.query(CatnutProvider.parse(Photo.MULTIPLE), null, query, null, null);
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					while (cursor.moveToNext()) {
						mFragments.add(FantasyFragment.getFragment(cursor.getString(0), cursor.getString(1)));
					}
					mAdapter.notifyDataSetChanged();
					cursor.close();
				}
			});
		}
	};
}
