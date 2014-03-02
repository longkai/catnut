/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Toast;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import org.catnut.R;
import org.catnut.adapter.UsersAdapter;
import org.catnut.api.FriendshipsAPI;
import org.catnut.core.CatnutAPI;
import org.catnut.core.CatnutProvider;
import org.catnut.core.CatnutRequest;
import org.catnut.metadata.User;
import org.catnut.processor.UserProcessor;
import org.catnut.ui.ProfileActivity;
import org.catnut.util.CatnutUtils;
import org.catnut.util.Constants;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 当前登录用户的好友或者关注列表
 *
 * @author longkai
 */
public class MyRelationshipFragment extends TimelineFragment {

	private static final String TAG = "MyRelationshipFragment";

	private static final String NEXT_CURSOR = "next_cursor";

	private static final String[] PROJECTION = new String[]{
			BaseColumns._ID,
			User.screen_name,
			User.remark,
			User.profile_image_url,
			User.verified,
			User.location,
			User.description,
			User.following,
			User.follow_me
	};

	private RequestQueue mRequestQueue;

	private UsersAdapter mAdapter;

	// 是否是我关注的用户
	private boolean mIsFollowing = false;

	private String mSelection;
	private int mTotal;
	private int mNextCursor;
	private long mUid;
	// 上一次的列表总数，两次一样就表示load完了
	private int mLastTotalNumber;

	public static MyRelationshipFragment getFragment(boolean following) {
		Bundle args = new Bundle();
		args.putBoolean(TAG, following);
		MyRelationshipFragment fragment = new MyRelationshipFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mIsFollowing = getArguments().getBoolean(TAG);
		mRequestQueue = mApp.getRequestQueue();
		mSelection = (mIsFollowing ? User.following : User.follow_me) + "=1";
		mUid = mApp.getAccessToken().uid;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAdapter = new UsersAdapter(getActivity());
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		// refresh it!
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
		getActivity().getActionBar().setTitle(
				getString(mIsFollowing ? R.string.my_followings_title : R.string.follow_me_title)
		);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Cursor cursor = (Cursor) mAdapter.getItem(position);
		Intent intent = new Intent(getActivity(), ProfileActivity.class);
		intent.putExtra(User.screen_name, cursor.getString(cursor.getColumnIndex(User.screen_name)));
		intent.putExtra(Constants.ID, id);
		getActivity().startActivity(intent);
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
		CatnutAPI api = mIsFollowing ? FriendshipsAPI.friends(mUid, getFetchSize(), 0, 1)
				: FriendshipsAPI.followers(mUid, getFetchSize(), 0, 1);
		mRequestQueue.add(new CatnutRequest(
				getActivity(),
				api,
				new UserProcessor.UsersProcessor(),
				new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						Log.d(TAG, "refresh done...");
						mTotal = response.optInt(TOTAL_NUMBER);
						mNextCursor = response.optInt(NEXT_CURSOR);
						// 重新置换数据
						mLastTotalNumber = 0;
						JSONArray jsonArray = response.optJSONArray(User.MULTIPLE);
						int newSize = jsonArray.length(); // 刷新，一切从新开始...
						Bundle args = new Bundle();
						args.putInt(TAG, newSize);
						getLoaderManager().restartLoader(0, args, MyRelationshipFragment.this);
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

	private Runnable updateLocalCount = new Runnable() {
		@Override
		public void run() {
			String query = CatnutUtils.buildQuery(
					new String[]{"count(0)"},
					mSelection,
					User.TABLE,
					null, null, null
			);
			Cursor cursor = getActivity().getContentResolver().query(
					CatnutProvider.parse(User.MULTIPLE),
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
	protected void loadMore(long max_id) {
		// 加载更多，判断一下是从本地加载还是从远程加载
		// 根据(偏好||是否有网络连接)
		boolean fromCloud = mPreferences.getBoolean(
				getString(R.string.pref_keep_latest),
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

	private void loadFromLocal() {
		Bundle args = new Bundle();
		args.putInt(TAG, mAdapter.getCount() + getFetchSize());
		getLoaderManager().restartLoader(0, args, this);
		mPullToRefreshLayout.setRefreshing(true);
	}

	// max_id 无用
	private void loadFromCloud(long max_id) {
		mPullToRefreshLayout.setRefreshing(true);
		CatnutAPI api = mIsFollowing
				? FriendshipsAPI.friends(mUid, getFetchSize(), mNextCursor, 1)
				: FriendshipsAPI.followers(mUid, getFetchSize(), mNextCursor, 1);
		mRequestQueue.add(new CatnutRequest(
				getActivity(),
				api,
				new UserProcessor.UsersProcessor(),
				new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						Log.d(TAG, "load more from cloud done...");
						mTotal = response.optInt(TOTAL_NUMBER);
						mNextCursor = response.optInt(NEXT_CURSOR);
						mLastTotalNumber = mAdapter.getCount();
						int newSize = response.optJSONArray(User.MULTIPLE).length() + mAdapter.getCount();
						Bundle args = new Bundle();
						args.putInt(TAG, newSize);
						getLoaderManager().restartLoader(0, args, MyRelationshipFragment.this);
					}
				},
				errorListener
		)).setTag(TAG);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String selection = mSelection;
		boolean search = args.getBoolean(SEARCH_TWEET);
		if (search) {
			if (!TextUtils.isEmpty(mCurFilter)) {
				selection = new StringBuilder(mSelection)
						.append(" and (")
						.append(User.remark).append(" like ").append(CatnutUtils.like(mCurFilter))
						.append(" or ")
						.append(User.screen_name).append(" like ").append(CatnutUtils.like(mCurFilter))
						.append(")")
						.toString();
			} else {
				search = false;
			}
		}
		int limit = args.getInt(TAG, getFetchSize());
		return CatnutUtils.getCursorLoader(
				getActivity(),
				CatnutProvider.parse(User.MULTIPLE),
				PROJECTION,
				selection,
				null,
				User.TABLE,
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
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		super.onScrollStateChanged(view, scrollState);
		boolean canLoading = SCROLL_STATE_IDLE == scrollState // 停住了，不滑动了
				&& mListView.getLastVisiblePosition() == mAdapter.getCount() - 1 // 到底了
				&& (mSearchView == null || !mSearchView.isSearching()) // 用户没有打开搜索框
				&& !mPullToRefreshLayout.isRefreshing() // 当前没有处在刷新状态
				&& mAdapter.getCount() > 0; // 不是一开始
		if (canLoading) {
			// 可以加载更多，但是我们需要判断一下是否加载完了，没有更多了
			// 返回的数据可能会比实际少，因为新浪会过滤掉一些用户...
			if (mAdapter.getCount() >= mTotal || mLastTotalNumber == mAdapter.getCount()) {
				Log.d(TAG, "load all done..." + mAdapter.getCount());
				super.loadAllDone();
			} else {
				Log.d(TAG, "load...");
				loadMore(mAdapter.getItemId(mAdapter.getCount() - 1));
			}
		} else {
			Log.d(TAG, "cannot load more!");
		}
	}
}
