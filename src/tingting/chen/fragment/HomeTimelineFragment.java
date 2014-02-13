/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.fragment;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import tingting.chen.R;
import tingting.chen.adapter.TweetAdapter;
import tingting.chen.api.TweetAPI;
import tingting.chen.metadata.Status;
import tingting.chen.metadata.User;
import tingting.chen.processor.StatusProcessor;
import tingting.chen.tingting.TingtingAPI;
import tingting.chen.tingting.TingtingProvider;
import tingting.chen.tingting.TingtingRequest;
import tingting.chen.util.TingtingUtils;

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
		Status.columnText,
		Status.thumbnail_pic,
		Status.comments_count,
		Status.reposts_count,
		Status.attitudes_count,
		Status.source,
		"s." + Status.created_at,
		User.screen_name,
		User.profile_image_url
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAdapter = new TweetAdapter(mActivity, null);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		// 先尝试去新浪抓一把，避免那啥，empty*_*
		boolean autoFetch = mPref.getBoolean(PrefFragment.AUTO_FETCH_ON_START, true);
		boolean firstRun = mPref.getBoolean(PrefFragment.IS_FIRST_RUN, true);
		if (autoFetch || firstRun) {
			mPullToRefreshLayout.setRefreshing(true);
			fetchTweetsFromCloud(true, 0);
			if (firstRun) {
				mPref.edit().putBoolean(PrefFragment.IS_FIRST_RUN, false).commit();
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
		String where = null;
		int size = TingtingUtils.resolveListPrefInt(mPref,
			PrefFragment.DEFAULT_FETCH_SIZE, mActivity.getResources().getInteger(R.integer.default_fetch_size));
		String limit = String.valueOf(size * (mCurPage + 1));
		// 搜索只能是本地搜索
		if (!TextUtils.isEmpty(mCurFilter) && mCurFilter.trim().length() != 0) {
			where = Status.columnText + " like " + TingtingUtils.like(mCurFilter) +
				" or " +
				User.screen_name + " like " + TingtingUtils.like(mCurFilter);
			limit = null;
			if (!isSearching) {
				getListView().removeFooterView(mLoadMore);
			}
			isSearching = true;
		} else if (isSearching) {
			isSearching = false;
			getListView().addFooterView(mLoadMore);
		}

		CursorLoader cursorLoader = TingtingUtils.getCursorLoader(
			mActivity,
			TingtingProvider.parse(Status.MULTIPLE),
			COLUMNS,
			where,
			null,
			Status.TABLE + " as s",
			"inner join " + User.TABLE + " as u on s.uid=u._id",
			"s._id desc",
			limit
		);
		// 清除加载更多标志位
		if (mCurPage != 0) {
			mLoading = false;
		}
		return cursorLoader;
	}

	@Override
	protected void fetchTweetsFromCloud(boolean isRefresh, long offset) {
		int count = super.getDefaultFetchSize();
		TingtingAPI api = isRefresh ? TweetAPI.homeTimeline(offset, 0, count, 0, 0, 0, 0)
			: TweetAPI.homeTimeline(0, offset, count, 0, 0, 0, 0);
		mRequestQueue.add(new TingtingRequest(
			mActivity,
			api,
			new StatusProcessor.MyTweetsProcessor(),
			success,
			error
		)).setTag(TAG);
	}
}
