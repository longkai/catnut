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
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import org.catnut.R;
import org.catnut.adapter.TweetAdapter;
import org.catnut.api.TweetAPI;
import org.catnut.core.CatnutAPI;
import org.catnut.core.CatnutProvider;
import org.catnut.core.CatnutRequest;
import org.catnut.metadata.Status;
import org.catnut.metadata.User;
import org.catnut.processor.StatusProcessor;
import org.catnut.ui.TweetActivity;
import org.catnut.util.CatnutUtils;
import org.catnut.util.Constants;

/**
 * 当前登录用户及其所关注用户的最新微博时间线
 *
 * @author longkai
 */
public class HomeTimelineFragment extends TimelineFragment {

	private static final String TAG = "HomeTimelineFragment";

	/** 待检索的列 */
	private static final String[] COLUMNS = new String[]{
		"s._id",
		Status.uid,
		Status.columnText,
		Status.thumbnail_pic,
		Status.comments_count,
		Status.reposts_count,
		Status.attitudes_count,
		Status.source,
		"s." + Status.created_at,
		User.screen_name,
		User.profile_image_url,
		User.remark
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAdapter = new TweetAdapter(mActivity, null);
	}

	@Override
	public void onStart() {
		super.onStart();
		mActivity.getActionBar().setTitle(mPref.getString(User.screen_name, null));
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		// 先尝试去新浪抓一把，避免那啥，empty*_*
		boolean autoFetch = mPref.getBoolean(getString(R.string.pref_auto_fetch_on_start), true);
		boolean firstRun = mPref.getBoolean(getString(R.string.pref_first_run), true);
		if (autoFetch || firstRun) {
			mPullToRefreshLayout.setRefreshing(true);
			fetchTweetsFromCloud(true, 0);
			if (firstRun) {
				mPref.edit().putBoolean(getString(R.string.pref_first_run), false).commit();
			}
		}
		// 接下来办正事
		getListView().addFooterView(mLoadMore);
		setEmptyText(mActivity.getString(R.string.no_tweets));
		setListAdapter(mAdapter);
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onStop() {
		mRequestQueue.cancelAll(TAG);
		super.onStop();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		StringBuilder where = new StringBuilder(Status.TYPE).append("=").append(Status.HOME);
		int size = super.getDefaultFetchSize();
		String limit = String.valueOf(size * (mCurPage + 1));
		// 搜索只能是本地搜索
		if (!TextUtils.isEmpty(mCurFilter) && mCurFilter.trim().length() != 0) {
			where.append(" and (")
					.append(Status.columnText).append(" like ").append(CatnutUtils.like(mCurFilter))
					.append(" or ")
					.append(User.screen_name).append(" like ").append(CatnutUtils.like(mCurFilter))
					.append(")");
			limit = null;
			if (!isSearching) {
				getListView().removeFooterView(mLoadMore);
			}
			isSearching = true;
		} else if (isSearching) {
			isSearching = false;
			getListView().addFooterView(mLoadMore);
		}

		CursorLoader cursorLoader = CatnutUtils.getCursorLoader(
			mActivity,
			CatnutProvider.parse(Status.MULTIPLE),
			COLUMNS,
			where.toString(),
			null,
			Status.TABLE + " as s",
			"inner join " + User.TABLE + " as u on s.uid=u._id",
			"s._id desc",
			limit
		);
		return cursorLoader;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Intent intent = new Intent(getActivity(), TweetActivity.class);
		intent.putExtra(Constants.ID, id);
		startActivity(intent);
	}

	@Override
	protected void fetchTweetsFromCloud(boolean isRefresh, long offset) {
		int count = super.getDefaultFetchSize();
		CatnutAPI api = isRefresh ? TweetAPI.homeTimeline(offset, 0, count, 0, 0, 0, 0)
			: TweetAPI.homeTimeline(0, offset, count, 0, 0, 0, 0);
		mRequestQueue.add(new CatnutRequest(
			mActivity,
			api,
			new StatusProcessor.HomeTweetsProcessor(),
			isRefresh ? refreshSuccessListener : loadMoreSuccessListener,
			isRefresh ? refreshFailListener : loadMoreFailListener
		)).setTag(TAG);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (isAdded()) {
			if (key.equals(getString(R.string.pref_tweet_font_size))
					|| key.equals(getString(R.string.pref_customize_tweet_font))
					|| key.equals(getString(R.string.pref_show_tweet_thumbs))) {
				Log.d(TAG, "pref change, the home timeline fragment needs update!");
				// 应用新的偏好
				mAdapter.swapCursor(null);
				mAdapter = new TweetAdapter(mActivity, null);
				setListAdapter(mAdapter);
				getLoaderManager().restartLoader(0, null, this);
			}
		}
	}
}
