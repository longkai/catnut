/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import org.catnut.R;
import org.catnut.adapter.TweetAdapter;
import org.catnut.api.TweetAPI;
import org.catnut.core.CatnutAPI;
import org.catnut.metadata.Status;
import org.catnut.metadata.User;
import org.catnut.processor.StatusProcessor;
import org.catnut.core.CatnutProvider;
import org.catnut.core.CatnutRequest;
import org.catnut.ui.TweetActivity;
import org.catnut.util.CatnutUtils;
import org.catnut.util.Constants;

/**
 * 用户时间线
 *
 * @author longkai
 */
public class UserTimeLineFragment extends TimelineFragment {

	private static final String TAG = "UserTimeLineFragment";

	private static final String[] PROJECTION = {
		BaseColumns._ID,
		Status.columnText,
		Status.thumbnail_pic,
		Status.comments_count,
		Status.reposts_count,
		Status.attitudes_count,
		Status.source,
		Status.created_at,
		Status.favorited
	};

	private long uid;
	private String nick;
	private boolean mIsCurAuthUser; // 是否当前授权用户

	public static UserTimeLineFragment getFragment(long uid, String nick) {
		Bundle args = new Bundle();
		args.putLong(Constants.ID, uid);
		args.putString(User.screen_name, nick);
		UserTimeLineFragment fragment = new UserTimeLineFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	protected void fetchTweetsFromCloud(boolean isRefresh, long offset) {
		int size = super.getDefaultFetchSize();
		CatnutAPI api = isRefresh
			? TweetAPI.userTimeline(uid, 0, 0, size, 0, 0, 0, 0)
			: TweetAPI.userTimeline(uid, 0, offset, size, 0, 0, 0, 0);
		mRequestQueue.add(new CatnutRequest(
			mActivity,
			api,
			new StatusProcessor.HomeTweetsProcessor(),
			isRefresh ? refreshSuccessListener : loadMoreSuccessListener,
			isRefresh ? refreshFailListener : loadMoreFailListener
		)).setTag(TAG);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		uid = getArguments().getLong(Constants.ID, 0L);
		nick = getArguments().getString(User.screen_name, null);
		// 判断是否是当前授权用户的时间线
		if (nick != null) {
			mAdapter = new TweetAdapter(mActivity, nick);
		} else {
			mAdapter = new TweetAdapter(mActivity, mPref.getString(User.screen_name, null));
		}
		mIsCurAuthUser = nick == null;
	}

	@Override
	public void onStart() {
		super.onStart();
		mActivity.getActionBar().setTitle(mIsCurAuthUser ? getText(R.string.my_timeline) : nick);
	}

	@Override
	public void onStop() {
		mRequestQueue.cancelAll(TAG);
		super.onStop();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (mPref.getBoolean(getString(R.string.pref_auto_fetch_on_start), true)) {
			mPullToRefreshLayout.setRefreshing(true);
			fetchTweetsFromCloud(true, 0);
		}

		getListView().addFooterView(mLoadMore);
		setEmptyText(mActivity.getString(R.string.no_tweets));
		setListAdapter(mAdapter);
		if (!mIsCurAuthUser) {
			fetchTweetsFromCloud(false, 0);
		}
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		int size = super.getDefaultFetchSize();
		StringBuilder where = new StringBuilder(Status.uid).append("=").append(uid)
				.append(" and ").append(Status.TYPE).append(" != ").append(Status.COMMENT);
		String limit = String.valueOf((mCurPage + 1) * size);
		if (!TextUtils.isEmpty(mCurFilter) && mCurFilter.trim().length() != 0) {
			where.append(" and ").append(Status.columnText).append(" like ").append(CatnutUtils.like(mCurFilter));
			limit = null;
			if (!isSearching) {
				getListView().removeFooterView(mLoadMore);
			}
			isSearching = true;
		} else if (isSearching) {
			isSearching = false;
			getListView().addFooterView(mLoadMore);
		}
		CursorLoader loader = CatnutUtils.getCursorLoader(
			mActivity,
			CatnutProvider.parse(Status.MULTIPLE),
			PROJECTION,
			where.toString(),
			null,
			Status.TABLE,
			null,
			"_id desc",
			limit
		);
		return loader;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Intent intent = new Intent(getActivity(), TweetActivity.class);
		intent.putExtra(Constants.ID, id);
		startActivity(intent);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (isAdded()) {
			if (key.equals(getString(R.string.pref_tweet_font_size))
					|| key.equals(getString(R.string.pref_customize_tweet_font))
					|| key.equals(getString(R.string.pref_show_tweet_thumbs))) {
				Log.d(TAG, "pref change, the user timeline fragment needs update!");
				// 应用新的偏好
				mAdapter.swapCursor(null);
				if (nick != null) {
					mAdapter = new TweetAdapter(mActivity, nick);
				} else {
					mAdapter = new TweetAdapter(mActivity, mPref.getString(User.screen_name, null));
				}
				setListAdapter(mAdapter);
				getLoaderManager().restartLoader(0, null, this);
			}
		}
	}
}
