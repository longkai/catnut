/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.Toast;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import org.catnut.R;
import org.catnut.adapter.TweetAdapter;
import org.catnut.api.TweetAPI;
import org.catnut.core.CatnutAPI;
import org.catnut.core.CatnutProcessor;
import org.catnut.core.CatnutProvider;
import org.catnut.core.CatnutRequest;
import org.catnut.metadata.Status;
import org.catnut.metadata.User;
import org.catnut.metadata.WeiboAPIError;
import org.catnut.processor.StatusProcessor;
import org.catnut.util.CatnutUtils;
import org.catnut.util.Constants;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 用户时间线
 *
 * @author longkai
 */
public class UserTimelineFragment extends TimelineFragment {

	public static final String TAG = "UserTimelineFragment";

	private static final String[] PROJECTION = {
			BaseColumns._ID,
			Status.columnText,
			Status.thumbnail_pic,
			Status.bmiddle_pic,
			Status.comments_count,
			Status.reposts_count,
			Status.attitudes_count,
			Status.source,
			Status.created_at,
			Status.favorited,
			Status.retweeted_status
	};

	private Handler mHandler = new Handler();

	private RequestQueue mRequestQueue;

	private long mUid;
	private String mScreenName;
	private int mTotal;
	private String mSelection;

	// it' s the host user?
	private boolean mIsMe = false;

	public static UserTimelineFragment getFragment(long uid, String screenName) {
		Bundle args = new Bundle();
		args.putLong(Constants.ID, uid);
		args.putString(User.screen_name, screenName);
		UserTimelineFragment fragment = new UserTimelineFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mUid = getArguments().getLong(Constants.ID);
		mScreenName = getArguments().getString(User.screen_name);
		StringBuilder where = new StringBuilder();
		// 不要评论！
		where.append(Status.uid).append("=").append(mUid)
				.append(" and ").append(Status.TYPE).append(" in (")
				.append(Status.HOME).append(",")
				.append(Status.RETWEET).append(")");
		mSelection = where.toString();
		mIsMe = mUid == mApp.getAccessToken().uid;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mRequestQueue = mApp.getRequestQueue();
		mAdapter = new TweetAdapter(getActivity(), mScreenName);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		// refresh it!
		mPullToRefreshLayout.setRefreshing(true);
		refresh();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mListView.setAdapter(mAdapter);
	}

	@Override
	public void onStart() {
		super.onStart();
		ActionBar bar = getActivity().getActionBar();
		bar.setDisplayShowHomeEnabled(false);
		bar.setTitle(mIsMe
				? getString(R.string.my_timeline)
				: getString(R.string.his_timeline, mScreenName));
	}

	@Override
	public void onStop() {
		super.onStop();
		mRequestQueue.cancelAll(TAG);
	}

	@Override
	public void onConfirm(Bundle args) {
		final long id = args.getLong(TAG);
		mRequestQueue.add(new CatnutRequest(
				getActivity(),
				TweetAPI.destroy(id),
				new CatnutProcessor<JSONObject>() {
					@Override
					public void asyncProcess(Context context, JSONObject data) throws Exception {
						context.getContentResolver().delete(
								CatnutProvider.parse(Status.MULTIPLE),
								BaseColumns._ID + "=" + id, null
						);
					}
				},
				null,
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						Log.e(TAG, "error deleting tweet id[" + id + "]", error);
						WeiboAPIError weiboAPIError = WeiboAPIError.fromVolleyError(error);
						Toast.makeText(getActivity(), weiboAPIError.error, Toast.LENGTH_LONG).show();
					}
				}
		)).setTag(TAG);
	}

	@Override
	public boolean canDismiss(int position) {
		return mIsMe;
	}

	@Override
	public void onDismiss(ListView listView, int[] reverseSortedPositions) {
		for (int position : reverseSortedPositions) {
			Bundle arg = new Bundle();
			arg.putLong(TAG, mAdapter.getItemId(position));
			mConfirmBarController.showUndoBar(true, getText(R.string.confirm_delete), arg);
		}
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
				Status.TABLE,
				null,
				BaseColumns._ID + " desc",
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
	protected void loadMore(long max_id) {
		// 加载更多，判断一下是从本地加载还是从远程加载
		// 根据(偏好||是否有网络连接)
		boolean fromCloud = mPreferences.getBoolean(
				getString(R.string.pref_load_more_from_cloud),
				getResources().getBoolean(R.bool.pref_load_more_from_cloud)
		);
		if (fromCloud && isNetworkAvailable()) {
			// 如果用户要求最新的数据并且网络连接ok，那么从网络上加载数据
			loadFromCloud(max_id);
		} else {
			// 从本地拿
			loadFromLocal();
			// 顺便更新一下本地的数据总数
			new Thread(updateLocalCount).start();
		}
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

	private void loadFromLocal() {
		Bundle args = new Bundle();
		args.putInt(TAG, mAdapter.getCount() + getFetchSize());
		getLoaderManager().restartLoader(0, args, this);
		mPullToRefreshLayout.setRefreshing(true);
	}

	private void loadFromCloud(long max_id) {
		mPullToRefreshLayout.setRefreshing(true);
		CatnutAPI api = TweetAPI.myTimeline(mUid, 0, max_id, getFetchSize(), 0, 0, 0, 1);
		mRequestQueue.add(new CatnutRequest(
				getActivity(),
				api,
				new StatusProcessor.TimelineProcessor(),
				new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						Log.d(TAG, "load more from cloud done...");
						mTotal = response.optInt(TOTAL_NUMBER);
						int newSize = response.optJSONArray(Status.MULTIPLE).length() + mAdapter.getCount();
						Bundle args = new Bundle();
						args.putInt(TAG, newSize);
						getLoaderManager().restartLoader(0, args, UserTimelineFragment.this);
					}
				},
				errorListener
		)).setTag(TAG);
	}

	private void initFromLocal() {
		Bundle args = new Bundle();
		args.putInt(TAG, getFetchSize());
		getLoaderManager().initLoader(0, args, this);
		new Thread(updateLocalCount).start();
	}

	@Override
	protected void refresh() {
		// 检测一下是否网络已经连接，否则从本地加载
		if (!isNetworkAvailable()) {
			Toast.makeText(getActivity(), getString(R.string.network_unavailable), Toast.LENGTH_SHORT).show();
			initFromLocal();
			return;
		}
		// refresh!
		final int size = getFetchSize();
		(new Thread(new Runnable() {
			@Override
			public void run() {
				// 这里需要注意一点，我们不需要最新的那条，而是需要(最新那条-数目)，否则你拿最新那条去刷新，球都没有返回Orz...
				String query = CatnutUtils.buildQuery(
						new String[]{BaseColumns._ID},
						mSelection,
						Status.TABLE,
						null,
						BaseColumns._ID + " desc",
						size + ", 1" // limit x, y
				);
				Cursor cursor = getActivity().getContentResolver().query(
						CatnutProvider.parse(Status.MULTIPLE),
						null, query, null, null
				);
				// the cursor never null?
				final long since_id;
				if (cursor.moveToNext()) {
					since_id = cursor.getLong(0);
				} else {
					since_id = 0;
				}
				cursor.close();
				final CatnutAPI api = TweetAPI.myTimeline(mUid, since_id, 0, getFetchSize(), 0, 0, 0, 1);
				// refresh...
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						mRequestQueue.add(new CatnutRequest(
								getActivity(),
								api,
								new StatusProcessor.TimelineProcessor(Status.HOME),
								new Response.Listener<JSONObject>() {
									@Override
									public void onResponse(JSONObject response) {
										Log.d(TAG, "refresh done...");
										mTotal = response.optInt(TOTAL_NUMBER);
										// 重新置换数据
										JSONArray jsonArray = response.optJSONArray(Status.MULTIPLE);
										int newSize = jsonArray.length(); // 刷新，一切从新开始...
										Bundle args = new Bundle();
										args.putInt(TAG, newSize);
										getLoaderManager().restartLoader(0, args, UserTimelineFragment.this);
									}
								},
								errorListener
						)).setTag(TAG);
					}
				});
			}
		})).start();
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		if (firstVisibleItem + visibleItemCount >= totalItemCount) {
			if (mSearchView != null && mSearchView.isSearching()) {
				Log.d(TAG, "searching... no load need...");
			} else if (mAdapter.getCount() >= mTotal && totalItemCount > visibleItemCount) { // 防止一开始还没加载就toast...
				Log.d(TAG, "load all!");
				super.loadAllDone();
			} else if (!mPullToRefreshLayout.isRefreshing() && totalItemCount > visibleItemCount) {
				Log.d(TAG, "loading....");
				loadMore(mAdapter.getItemId(mAdapter.getCount() - 1)); // 这里需要注意是否有header或者footer!
			} else {
				Log.d(TAG, "already loading...");
			}
		}
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
					|| key.equals(getString(R.string.pref_customize_tweet_font))
					|| key.equals(getString(R.string.pref_thumbs_options))) {
				Log.d(TAG, "pref change, the home timeline fragment needs update!");
				// 应用新的偏好
				int size = mAdapter.getCount();
				int firstVisiblePosition = mListView.getFirstVisiblePosition();
				mAdapter.changeCursor(null);
				mAdapter = new TweetAdapter(getActivity(), mScreenName);
				mListView.setAdapter(mAdapter);
				Bundle args = new Bundle();
				args.putInt(TAG, size);
				getLoaderManager().restartLoader(0, args, this);
				mListView.setSelection(firstVisiblePosition); // 保持现场~
			}
		}
	}
}
