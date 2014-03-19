/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.plugin.zhihu;

import android.app.Activity;
import android.app.Fragment;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.catnut.R;
import org.catnut.core.CatnutProvider;
import org.catnut.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * 以pager的形式查看条目
 *
 * @author longkai
 */
public class PagerItemFragment extends Fragment {

	public static final String TAG = PagerItemFragment.class.getSimpleName();
	public static final String ORDER_ID = "order_id";

	private static final String[] ID_PROJECTION = new String[]{
			Zhihu.ANSWER_ID,
			BaseColumns._ID
	};

	private static final int LIMIT = 10;
	private static final int MAX_OFFSET = 3;

	private long mCurrentItemId;
	private long mCurrentItemOrderId;

	private Handler mHandler = new Handler();
	private List<ItemHolder> mItems = new ArrayList<ItemHolder>();
	private ViewPager mViewPager;
	private FragmentStatePagerAdapter mAdapter;

	public static PagerItemFragment getFragment(long id,long orderId) {
		Bundle args = new Bundle();
		args.putLong(Constants.ID, id);
		args.putLong(ORDER_ID, orderId);
		PagerItemFragment fragment = new PagerItemFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mCurrentItemId = getArguments().getLong(Constants.ID);
		mCurrentItemOrderId = getArguments().getLong(ORDER_ID);
		ItemHolder holder = new ItemHolder();
		holder.answerId = mCurrentItemId;
		holder.orderId = mCurrentItemOrderId;
		mItems.add(holder);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.pager, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		mViewPager = (ViewPager) view;
		mAdapter = new FragmentStatePagerAdapter(getFragmentManager()) {
			@Override
			public Fragment getItem(int position) {
				return ZhihuItemFragment.getFragment(mItems.get(position).answerId);
			}

			@Override
			public int getCount() {
				return mItems.size();
			}
		};

		mViewPager.setAdapter(mAdapter);
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
				if (mItems.size() - position < MAX_OFFSET) {
					new Thread(mLoadMore).start();
				}
			}
		});
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	private Runnable mLoadMore = new Runnable() {
		@Override
		public void run() {
			final Cursor cursor = getActivity().getContentResolver().query(
					CatnutProvider.parse(Zhihu.MULTIPLE),
					ID_PROJECTION,
					BaseColumns._ID + "<" + mCurrentItemOrderId,
					null,
					BaseColumns._ID + " desc limit " + (mItems.size() + LIMIT)
			);
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					while (cursor.moveToNext()) {
						ItemHolder holder = new ItemHolder();
						holder.answerId = cursor.getLong(0);
						holder.orderId = cursor.getLong(1);
						mItems.add(holder);
					}
					cursor.close();
					mAdapter.notifyDataSetChanged();
				}
			});
		}
	};

	private static class ItemHolder {
		long answerId;
		long orderId;
	}
}
