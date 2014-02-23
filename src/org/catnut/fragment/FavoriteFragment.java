/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.AsyncQueryHandler;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import org.catnut.R;
import org.catnut.adapter.TweetAdapter;
import org.catnut.api.FavoritesAPI;
import org.catnut.core.CatnutApp;
import org.catnut.core.CatnutProvider;
import org.catnut.core.CatnutRequest;
import org.catnut.metadata.Status;
import org.catnut.metadata.User;
import org.catnut.metadata.WeiboAPIError;
import org.catnut.processor.StatusProcessor;
import org.catnut.ui.TweetActivity;
import org.catnut.util.CatnutUtils;
import org.catnut.util.Constants;
import org.json.JSONObject;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

/**
 * 当前登录用户的收藏列表界面
 *
 * @author longkai
 */
public class FavoriteFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>,
		OnRefreshListener, AbsListView.OnScrollListener, SharedPreferences.OnSharedPreferenceChangeListener {

	public static final String TAG = "FavoriteFragment"; // 计算有多少条删除的微博用

	/** 待检索的列 */
	private static final String[] PROJECTION = new String[]{
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

	private CatnutApp mApp;
	private SharedPreferences mPref;
	private RequestQueue mRequestQueue;

	private PullToRefreshLayout mPullToRefreshLayout;
	private int mCurrentPage = 1; // 默认为1，见api
	private int mTotalNumber = 0;
	private int mDeleteCount = 0;
	private int mPageSize = 0;
	private ProgressBar mLoadMore;

	private CursorAdapter mAdapter;

	private Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
		@Override
		public void onResponse(JSONObject response) {
			mDeleteCount += response.optInt(TAG); // 计算删除的数目
			mTotalNumber = response.optInt(Status.total_number);
			if (mTotalNumber == 0) {
				getListView().removeFooterView(mLoadMore);
			} else {
				mCurrentPage++;
				getLoaderManager().restartLoader(0, null, FavoriteFragment.this);
			}
			mPullToRefreshLayout.setRefreshComplete();
		}
	};

	private Response.ErrorListener errorListener = new Response.ErrorListener() {
		@Override
		public void onErrorResponse(VolleyError error) {
			Log.e(TAG, "load favorites from cloud error!", error);
			WeiboAPIError weiboAPIError = WeiboAPIError.fromVolleyError(error);
			Toast.makeText(getActivity(), weiboAPIError.error, Toast.LENGTH_SHORT).show();
			mPullToRefreshLayout.setRefreshComplete();
		}
	};

	public static FavoriteFragment getFragment() {
		FavoriteFragment fragment = new FavoriteFragment();
		return fragment;
	}

	private void loadFromCloud(boolean refresh) {
		if (refresh) {
			// reset
			mCurrentPage = 1;
			mTotalNumber = 0;
			mDeleteCount = 0;
			mAdapter.swapCursor(null);
			mAdapter = new TweetAdapter(getActivity(), null);
			setListAdapter(mAdapter);
		}
		mRequestQueue.add(new CatnutRequest(
				getActivity(),
				FavoritesAPI.favorites(mPageSize, mCurrentPage),
				new StatusProcessor.FavoriteTweetsProcessor(),
				listener,
				errorListener
		));
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mApp = CatnutApp.getTingtingApp();
		mRequestQueue = mApp.getRequestQueue();
		mPref = mApp.getPreferences();
		int defaultSize = getResources().getInteger(R.integer.default_fetch_size);
		mPageSize = CatnutUtils.resolveListPrefInt(mPref, getString(R.string.pref_default_fetch_size), defaultSize);
		mPref.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		mLoadMore = new ProgressBar(getActivity());
		mAdapter = new TweetAdapter(getActivity(), null); // 并不是某个用户的时间线，所以null
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		// set actionbar refresh facility
		ViewGroup viewGroup = (ViewGroup) view;
		mPullToRefreshLayout = new PullToRefreshLayout(viewGroup.getContext());
		ActionBarPullToRefresh.from(getActivity())
				.insertLayoutInto(viewGroup)
				.theseChildrenArePullable(android.R.id.list, android.R.id.empty)
				.listener(this)
				.setup(mPullToRefreshLayout);
		// 来一发?
		if (mPref.getBoolean(getString(R.string.pref_auto_fetch_on_start), true)) {
			mPullToRefreshLayout.setRefreshing(true);
			loadFromCloud(false); // first, every thing is init.
		}
		// load total count from sqlite
		String query = CatnutUtils
				.buildQuery(new String[]{"count(0)"}, Status.favorited + "=1", Status.TABLE, null, null, null);
		new AsyncQueryHandler(getActivity().getContentResolver()) {
			@Override
			protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
				if (cursor.moveToNext()) {
					mTotalNumber = cursor.getInt(0);
				}
				cursor.close();
			}
		}.startQuery(0, null, CatnutProvider.parse(Status.MULTIPLE), null, query, null, null);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getListView().addFooterView(mLoadMore);
		setListAdapter(mAdapter);
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onStart() {
		super.onStart();
		getActivity().getActionBar().setTitle(getString(R.string.my_favorites));
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		// no search!
		return CatnutUtils.getCursorLoader(
				getActivity(),
				CatnutProvider.parse(Status.MULTIPLE),
				PROJECTION,
				Status.favorited + "=1",
				null,
				Status.TABLE + " as s",
				"inner join " + User.TABLE + " as u on s.uid=u._id",
				"s._id desc",
				String.valueOf(mCurrentPage * mPageSize)
		);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.swapCursor(data);
		if (mTotalNumber != 0 && (mTotalNumber - mDeleteCount) == mAdapter.getCount()) {
			getListView().removeFooterView(mLoadMore);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	@Override
	public void onRefreshStarted(View view) {
		loadFromCloud(true);
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (mLoadMore.isShown() && !mPullToRefreshLayout.isRefreshing()) {
			if (mPref.getBoolean(getString(R.string.pref_load_more_from_cloud), true)) {
				loadFromCloud(false);
			} else {
				mCurrentPage++;
				getLoaderManager().restartLoader(0, null, this);
			}
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		// no-op
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Cursor cursor = (Cursor) mAdapter.getItem(position);
		String screenName = cursor.getString(cursor.getColumnIndex(User.screen_name));
		Intent intent = new Intent(getActivity(), TweetActivity.class);
		intent.putExtra(Constants.ID, id);
		intent.putExtra(User.screen_name, screenName);
		startActivity(intent);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (isAdded()) {
			if (key.equals(mApp.getString(R.string.pref_tweet_font_size))
					|| key.equals(mApp.getString(R.string.pref_customize_tweet_font))
					|| key.equals(mApp.getString(R.string.pref_show_tweet_thumbs))) {
				Log.d(TAG, "pref change, the home timeline fragment needs update!");
				// 应用新的偏好
				mAdapter.swapCursor(null);
				mAdapter = new TweetAdapter(getActivity(), null);
				setListAdapter(mAdapter);
				getLoaderManager().restartLoader(0, null, this);
			}
		}
	}
}
