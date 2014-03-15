/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.Toast;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import org.catnut.R;
import org.catnut.adapter.TweetAdapter;
import org.catnut.api.FavoritesAPI;
import org.catnut.core.CatnutProvider;
import org.catnut.core.CatnutRequest;
import org.catnut.metadata.Status;
import org.catnut.metadata.User;
import org.catnut.processor.StatusProcessor;
import org.catnut.ui.TweetActivity;
import org.catnut.util.CatnutUtils;
import org.catnut.util.Constants;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 当前登录用户的收藏列表界面
 *
 * @author longkai
 */
public class FavoriteFragment extends TimelineFragment {

	public static final String TAG = "FavoriteFragment"; // 计算有多少条删除的微博用

	/** 待检索的列 */
	private static final String[] PROJECTION = new String[]{
			"s._id",
			Status.uid,
			Status.columnText,
			Status.thumbnail_pic,
			Status.bmiddle_pic,
			Status.original_pic,
			Status.comments_count,
			Status.reposts_count,
			Status.attitudes_count,
			Status.source,
			Status.retweeted_status,
			Status.favorited,
			"s." + Status.created_at,
			User.screen_name,
			User.profile_image_url,
			User.remark
	};

	private RequestQueue mRequestQueue;

	private int mCurrentPage = 1; // 默认为1，见api
	private int mTotal = 0;
	private int mDeleteCount = 0;
	private String mSelection;

	private CursorAdapter mAdapter;

	public static FavoriteFragment getFragment() {
		FavoriteFragment fragment = new FavoriteFragment();
		return fragment;
	}

	@Override
	protected void refresh() {
		// 检测一下是否网络已经连接，否则从本地加载
		if (!isNetworkAvailable()) {
			Toast.makeText(getActivity(), getString(R.string.network_unavailable), Toast.LENGTH_SHORT).show();
			initFromLocal();
			return;
		}
		// 重置数据
		mCurrentPage = 0;
		mDeleteCount = 0;
		mTotal = 0;

		// go go go
		mRequestQueue.add(new CatnutRequest(
				getActivity(),
				FavoritesAPI.favorites(getFetchSize(), mCurrentPage),
				new StatusProcessor.FavoriteTweetsProcessor(),
				new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						Log.d(TAG, "refresh done...");
						mDeleteCount += response.optInt(TAG);
						mTotal = response.optInt(TOTAL_NUMBER);
						// 重新置换数据
						JSONArray jsonArray = response.optJSONArray(Status.FAVORITES);
						int newSize = jsonArray.length(); // 刷新，一切从新开始...
						Bundle args = new Bundle();
						args.putInt(TAG, newSize);
						getLoaderManager().restartLoader(0, args, FavoriteFragment.this);
					}
				},
				errorListener
		)).setTag(TAG);
	}

	@Override
	protected void loadMore(long max_id) {
		// 加载更多，判断一下是从本地加载还是从远程加载
		// 根据(偏好||是否有网络连接)
		boolean fromCloud = mPreferences.getBoolean(
				getString(R.string.pref_keep_latest),
				getResources().getBoolean(R.bool.pref_load_more_from_cloud)
		);
		if (fromCloud && isNetworkAvailable()) {
			// 如果用户要求最新的数据并且网络连接ok，那么从网络上加载数据
			loadFromCloud();
		} else {
			// 从本地拿
			loadFromLocal();
			// 顺便更新一下本地的数据总数
			new Thread(updateLocalCount).start();
		}
	}

	private void loadFromCloud() {
		mPullToRefreshLayout.setRefreshing(true);
		mRequestQueue.add(new CatnutRequest(
				getActivity(),
				FavoritesAPI.favorites(getFetchSize(), mCurrentPage),
				new StatusProcessor.FavoriteTweetsProcessor(),
				new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						Log.d(TAG, "load more from cloud done...");
						mDeleteCount += response.optInt(TAG);
						mTotal = response.optInt(TOTAL_NUMBER);
						int newSize = response.optJSONArray(Status.FAVORITES).length() + mAdapter.getCount();
						Bundle args = new Bundle();
						args.putInt(TAG, newSize);
						getLoaderManager().restartLoader(0, args, FavoriteFragment.this);
					}
				},
				errorListener
		)).setTag(TAG);
	}

	private void loadFromLocal() {
		Bundle args = new Bundle();
		args.putInt(TAG, mAdapter.getCount() + getFetchSize());
		getLoaderManager().restartLoader(0, args, this);
		mPullToRefreshLayout.setRefreshing(true);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mRequestQueue = mApp.getRequestQueue();
		mSelection = Status.favorited + "=1";
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAdapter = new TweetAdapter(getActivity(), null); // 并不是某个用户的时间线，所以null
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mPullToRefreshLayout.setRefreshing(true);
		if (mPreferences.getBoolean(getString(R.string.pref_keep_latest), true)) {
			refresh();
		} else {
			initFromLocal();
		}
	}

	private void initFromLocal() {
		Bundle args = new Bundle();
		args.putInt(TAG, getFetchSize());
		getLoaderManager().initLoader(0, args, this);
		new Thread(updateLocalCount).start();
	}

	private Runnable updateLocalCount = new Runnable() {
		@Override
		public void run() {
			String query = CatnutUtils.buildQuery(
					new String[]{"count(0)"},
					mSelection,
					Status.TABLE,
					null, null, null
			);
			Cursor cursor = getActivity().getContentResolver().query(
					CatnutProvider.parse(Status.MULTIPLE),
					null,
					query,
					null, null
			);
			if (cursor.moveToNext()) {
				mTotal = cursor.getInt(0);
			}
			cursor.close();
		}
	};

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mListView.setAdapter(mAdapter);
	}

	@Override
	public void onStart() {
		super.onStart();
		ActionBar actionBar = getActivity().getActionBar();
		actionBar.setTitle(getString(R.string.my_favorites));
		actionBar.setDisplayShowHomeEnabled(false);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String selection = mSelection;
		boolean search = args.getBoolean(SEARCH_TWEET);
		if (search) {
			if (!TextUtils.isEmpty(mCurFilter)) {
				selection = new StringBuilder(mSelection)
						.append(" and ").append(Status.columnText)
						.append(" like ").append(CatnutUtils.like(mCurFilter))
						.toString();
			} else {
				search = false;
			}
		}
		int limit = args.getInt(TAG, getFetchSize());
		return CatnutUtils.getCursorLoader(
				getActivity(),
				CatnutProvider.parse(Status.MULTIPLE),
				PROJECTION,
				selection,
				null,
				Status.TABLE + " as s",
				"inner join " + User.TABLE + " as u on s.uid=u._id",
				"s." + BaseColumns._ID + " desc",
				search ? null : String.valueOf(limit)
		);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if (mPullToRefreshLayout.isRefreshing()) {
			mPullToRefreshLayout.setRefreshComplete();
		}
		mAdapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		super.onScrollStateChanged(view, scrollState);
		boolean canLoading = SCROLL_STATE_IDLE == scrollState // 停住了，不滑动了
				&& mListView.getLastVisiblePosition() == mAdapter.getCount() - 1 // 到底了
				&& (mSearchView == null || !mSearchView.isSearching()) // 用户没有打开搜索框
				&& !mPullToRefreshLayout.isRefreshing(); // 当前没有处在刷新状态
//				&& mAdapter.getCount() > 0; // 不是一开始
		if (canLoading) {
			// 可以加载更多，但是我们需要判断一下是否加载完了，没有更多了
			if (mAdapter.getCount() >= (mTotal - mDeleteCount)) {
				Log.d(TAG, "load all done...");
				super.loadAllDone();
			} else {
				Log.d(TAG, "load...");
				loadMore(0); // 参数无用
			}
		} else {
			Log.d(TAG, "cannot load more!");
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Cursor cursor = (Cursor) mAdapter.getItem(position);
		String screenName = cursor.getString(cursor.getColumnIndex(User.screen_name));
		Intent intent = new Intent(getActivity(), TweetActivity.class);
		intent.putExtra(Constants.ID, id);
		intent.putExtra(User.screen_name, screenName);
		startActivity(intent);
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		String newFilter = !TextUtils.isEmpty(newText) ? newText : null;
		// Don't do anything if the filter hasn't actually changed.
		// Prevents restarting the loader when restoring state.
		if (mCurFilter == null && newFilter == null) {
			return true;
		}
		if (mCurFilter != null && mCurFilter.equals(newFilter)) {
			return true;
		}
		Bundle args = new Bundle();
		args.putBoolean(SEARCH_TWEET, true);
		mCurFilter = newFilter;
		getLoaderManager().restartLoader(0, args, this);
		return true;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (isAdded()) {
			if (key.equals(getString(R.string.pref_tweet_font_size))
					|| key.equals(getString(R.string.pref_line_spacing))
					|| key.equals(getString(R.string.pref_customize_tweet_font))
					|| key.equals(getString(R.string.pref_thumbs_options))) {
				Log.d(TAG, "pref change, the home timeline fragment needs update!");
				// 应用新的偏好
				int size = mAdapter.getCount();
				int firstVisiblePosition = mListView.getFirstVisiblePosition();
				mAdapter.changeCursor(null);
				mAdapter = new TweetAdapter(getActivity(), null);
				mListView.setAdapter(mAdapter);
				Bundle args = new Bundle();
				args.putInt(TAG, size);
				getLoaderManager().restartLoader(0, args, this);
				mListView.setSelection(firstVisiblePosition); // 保持现场~
			}
		}
	}
}
