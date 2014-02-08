/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.fragment;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import org.json.JSONObject;
import tingting.chen.R;
import tingting.chen.adapter.TweetAdapter;
import tingting.chen.api.TweetAPI;
import tingting.chen.metadata.Status;
import tingting.chen.metadata.User;
import tingting.chen.processor.StatusProcessor;
import tingting.chen.tingting.TingtingApp;
import tingting.chen.tingting.TingtingProvider;
import tingting.chen.tingting.TingtingRequest;
import tingting.chen.util.TingtingUtils;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

import java.util.concurrent.TimeUnit;

/**
 * 当前登录用户及其所关注用户的最新微博时间线
 *
 * @author longkai
 */
public class HomeTimelineFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>,OnRefreshListener {

	private static final String TAG = "HomeTimelineFragment";

	private static final int TWEETS_SIZE = 20;

	private RequestQueue mRequestQueue;
	private Activity mActivity;
	private TweetAdapter mAdapter;

	private PullToRefreshLayout mPullToRefreshLayout;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mActivity = activity;
		mRequestQueue = TingtingApp.getTingtingApp().getRequestQueue();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		mAdapter = new TweetAdapter(mActivity);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		ViewGroup viewGroup = (ViewGroup) view;
		mPullToRefreshLayout = new PullToRefreshLayout(viewGroup.getContext());
		ActionBarPullToRefresh.from(mActivity)
			.insertLayoutInto(viewGroup)
			.theseChildrenArePullable(android.R.id.list, android.R.id.empty)
			.listener(this)
			.setup(mPullToRefreshLayout);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.add(Menu.NONE, R.id.refresh, Menu.NONE, R.string.refresh)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.refresh:
				mPullToRefreshLayout.setRefreshing(true);
				onRefreshStarted(null);
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
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
		return TingtingUtils.getCursorLoader(
			mActivity,
			TingtingProvider.parse(Status.MULTIPLE),
			new String[] {
				"s._id",
				Status.columnText,
				Status.thumbnail_pic,
				Status.comments_count,
				Status.reposts_count,
				"s." + Status.created_at,
				User.screen_name,
				User.profile_image_url
			},
			null,
			null,
			Status.TABLE + " as s",
			User.TABLE + " as u",
			"s.uid=u._id",
			"s._id desc",
			String.valueOf(TWEETS_SIZE)
		);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	@Override
	public void onRefreshStarted(View view) {
		mRequestQueue.add(new TingtingRequest(
			mActivity,
			TweetAPI.homeTimeline(0, 0, 0, 0, 0, 0, 0),
			new StatusProcessor.MyTweetsProcessor(),
			success,
			error
		)).setTag(TAG);
	}

	private Response.Listener success = new Response.Listener() {
		@Override
		public void onResponse(Object response) {
			mPullToRefreshLayout.setRefreshComplete();
		}
	};

	private Response.ErrorListener error = new Response.ErrorListener() {
		@Override
		public void onErrorResponse(VolleyError error) {
			mPullToRefreshLayout.setRefreshComplete();
			Toast.makeText(mActivity, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
		}
	};
}
