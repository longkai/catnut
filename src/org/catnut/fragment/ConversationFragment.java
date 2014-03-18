/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.app.ActionBar;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Toast;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import org.catnut.R;
import org.catnut.adapter.ConversationAdapter;
import org.catnut.api.CommentsAPI;
import org.catnut.core.CatnutAPI;
import org.catnut.core.CatnutProvider;
import org.catnut.core.CatnutRequest;
import org.catnut.metadata.Comment;
import org.catnut.metadata.Status;
import org.catnut.metadata.User;
import org.catnut.processor.StatusProcessor;
import org.catnut.ui.TweetActivity;
import org.catnut.util.CatnutUtils;
import org.catnut.util.Constants;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 会话列表视图
 *
 * @author longkai
 */
public class ConversationFragment extends TimelineFragment {

	public static final String TAG = ConversationFragment.class.getSimpleName();

	private static final String[] PROJECTION = new String[]{
			"c." + BaseColumns._ID,
			"c." + Comment.created_at,
			Comment.columnText,
			Comment.status,
			Comment.reply_comment,
			Comment.uid,
			User.screen_name,
			User.avatar_large,
			User.remark,
	};

	private Handler mHandler = new Handler();

	private ConversationAdapter mAdapter;
	private RequestQueue mRequestQueue;

	private int mTotal;

	public static ConversationFragment getFragment() {
		return new ConversationFragment();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(false);
		mRequestQueue = mApp.getRequestQueue();
		mAdapter = new ConversationAdapter(getActivity());
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		// refresh it!
		mPullToRefreshLayout.setRefreshing(true);
		String key = getString(R.string.pref_first_run);
		boolean firstRun = mPreferences.getBoolean(key, true);
		if (firstRun) {
			refresh();
			mPreferences.edit().putBoolean(key, false).commit();
		} else if (mPreferences.getBoolean(getString(R.string.pref_keep_latest), true)) {
			refresh();
		} else {
			initFromLocal();
		}
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
		bar.setIcon(R.drawable.ic_action_conversation);
		bar.setTitle(getString(R.string.received_reply));
	}

	@Override
	public void onStop() {
		super.onStop();
		mRequestQueue.cancelAll(TAG);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Cursor c = (Cursor) mAdapter.getItem(position);
		String status = c.getString(c.getColumnIndex(Comment.status));
		Intent intent = new Intent(getActivity(), TweetActivity.class);
		intent.putExtra(Constants.JSON, status);
		startActivity(intent);
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
						null,
						Comment.TABLE,
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
				final CatnutAPI api = CommentsAPI.to_me(since_id, 0, getFetchSize(), 0, 0, 0);
				// refresh...
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						mRequestQueue.add(new CatnutRequest(
								getActivity(),
								api,
								new StatusProcessor.Comments2MeProcessor(),
								new Response.Listener<JSONObject>() {
									@Override
									public void onResponse(JSONObject response) {
										Log.d(TAG, "refresh done...");
										mTotal = response.optInt(TOTAL_NUMBER);
										// 重新置换数据
										JSONArray jsonArray = response.optJSONArray(Comment.MULTIPLE);
										int newSize = jsonArray.length(); // 刷新，一切从新开始...
										Bundle args = new Bundle();
										args.putInt(TAG, newSize);
										getLoaderManager().restartLoader(0, args, ConversationFragment.this);
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

	private void initFromLocal() {
		Bundle args = new Bundle();
		args.putInt(TAG, getFetchSize());
		getLoaderManager().initLoader(0, args, this);
		new Thread(updateLocalCount).start();
	}

	private void loadFromLocal() {
		Bundle args = new Bundle();
		args.putInt(TAG, mAdapter.getCount() + getFetchSize());
		getLoaderManager().restartLoader(0, args, this);
		mPullToRefreshLayout.setRefreshing(true);
	}

	private void loadFromCloud(long max_id) {
		mPullToRefreshLayout.setRefreshing(true);
		CatnutAPI api = CommentsAPI.to_me(0, max_id, getFetchSize(), 0, 0, 0);
		mRequestQueue.add(new CatnutRequest(
				getActivity(),
				api,
				new StatusProcessor.Comments2MeProcessor(),
				new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						Log.d(TAG, "load more from cloud done...");
						mTotal = response.optInt(TOTAL_NUMBER);
						int newSize = response.optJSONArray(Comment.MULTIPLE).length() + mAdapter.getCount();
						Bundle args = new Bundle();
						args.putInt(TAG, newSize);
						getLoaderManager().restartLoader(0, args, ConversationFragment.this);
					}
				},
				errorListener
		)).setTag(TAG);
	}

	private Runnable updateLocalCount = new Runnable() {
		@Override
		public void run() {
			String query = CatnutUtils.buildQuery(
					new String[]{"count(0)"},
					null,
					Comment.TABLE,
					null, null, null
			);
			Cursor cursor = getActivity().getContentResolver().query(
					CatnutProvider.parse(Comment.MULTIPLE),
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
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		int limit = args.getInt(TAG, getFetchSize());
		return CatnutUtils.getCursorLoader(
				getActivity(),
				CatnutProvider.parse(Comment.MULTIPLE),
				PROJECTION,
				null,
				null,
				Comment.TABLE + " as c",
				"inner join " + User.TABLE + " as u on c.uid=u._id",
				"c." + BaseColumns._ID + " desc",
				String.valueOf(limit)
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
				&& !mPullToRefreshLayout.isRefreshing(); // 当前没有处在刷新状态
//				&& mAdapter.getCount() > 0; // 不是一开始
		if (canLoading) {
			// 可以加载更多，但是我们需要判断一下是否加载完了，没有更多了
			if (mAdapter.getCount() >= mTotal) {
				Log.d(TAG, "load all done...");
				super.loadAllDone();
			} else {
				Log.d(TAG, "load...");
				loadMore(mAdapter.getItemId(mAdapter.getCount() - 1));
			}
		} else {
			Log.d(TAG, "cannot load more!");
		}
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		return false;
	}
}
