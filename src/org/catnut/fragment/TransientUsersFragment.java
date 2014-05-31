/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ListFragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.Toast;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import org.catnut.R;
import org.catnut.adapter.TransientUsersAdapter;
import org.catnut.api.FriendshipsAPI;
import org.catnut.api.UserAPI;
import org.catnut.core.CatnutAPI;
import org.catnut.core.CatnutApp;
import org.catnut.core.CatnutRequest;
import org.catnut.metadata.User;
import org.catnut.metadata.WeiboAPIError;
import org.catnut.processor.UserProcessor;
import org.catnut.support.TransientRequest;
import org.catnut.support.TransientUser;
import org.catnut.support.app.SwipeRefreshListFragment;
import org.catnut.ui.ProfileActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * 关注者列表界面
 *
 * @author longkai
 */
public class TransientUsersFragment extends SwipeRefreshListFragment implements AbsListView.OnScrollListener, SwipeRefreshLayout.OnRefreshListener {

	private static final String TAG = "TransientUsersFragment";

	private int mNext_cursor = 0;
	private int mTotal_number = 0;

	private RequestQueue mRequestQueue;

	private String mScreenName;
	private boolean mFollowing;

	private ArrayList<TransientUser> mUsers;
	private TransientUsersAdapter mAdapter;

	private int mLastTotalCount;

	private Response.Listener<List<TransientUser>> listener = new Response.Listener<List<TransientUser>>() {
		@Override
		public void onResponse(List<TransientUser> response) {
			mLastTotalCount = mAdapter.getCount();
			mUsers.addAll(response);
			mAdapter.notifyDataSetChanged();
			setRefreshing(false);
		}
	};

	private Response.ErrorListener errorListener = new Response.ErrorListener() {
		@Override
		public void onErrorResponse(VolleyError error) {
			Log.e(TAG, "load friends error!", error);
			WeiboAPIError weiboAPIError = WeiboAPIError.fromVolleyError(error);
			Toast.makeText(getActivity(), weiboAPIError.error, Toast.LENGTH_LONG).show();
			setRefreshing(false);
		}
	};

	public static TransientUsersFragment getFragment(String screenName, boolean following) {
		Bundle args = new Bundle();
		args.putString(User.screen_name, screenName);
		args.putBoolean(TAG, following);
		TransientUsersFragment transientUsersFragment = new TransientUsersFragment();
		transientUsersFragment.setArguments(args);
		return transientUsersFragment;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mRequestQueue = CatnutApp.getTingtingApp().getRequestQueue();
		Bundle args = getArguments();
		mScreenName = args.getString(User.screen_name);
		mFollowing = args.getBoolean(TAG);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mUsers = new ArrayList<TransientUser>();
		mAdapter = new TransientUsersAdapter(getActivity(), mUsers);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		ViewGroup viewGroup = (ViewGroup) view;
		setOnRefreshListener(this);
		// go！开始加载数据吧！
		load(true);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getListView().setOnScrollListener(this);
		setEmptyText(getString(R.string.no_more));
		setListAdapter(mAdapter);
	}

	@Override
	public void onStart() {
		super.onStart();
		ActionBar bar = getActivity().getActionBar();
		bar.setIcon(R.drawable.ic_title_people);
		bar.setTitle(getString(
				mFollowing ? R.string.his_followings : R.string.his_followers, mScreenName)
		);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		final TransientUser user = mUsers.get(position);
		CatnutAPI api = UserAPI.profile(user.id);
		final ProgressDialog loading = ProgressDialog.show(getActivity(), null, getString(R.string.loading));
		loading.show();
		mRequestQueue.add(new CatnutRequest(
				getActivity(),
				api,
				new UserProcessor.UserProfileProcessor(),
				new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						loading.dismiss();
						ProfileActivity activity = (ProfileActivity) getActivity();
						activity.flipCard(ProfileFragment.getFragment(user.id, user.screenName), null, true);
					}
				},
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						loading.dismiss();
						Log.d(TAG, "load user profile error!", error);
						WeiboAPIError weiboAPIError = WeiboAPIError.fromVolleyError(error);
						Toast.makeText(getActivity(), weiboAPIError.error, Toast.LENGTH_LONG).show();
					}
				}
		));
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (SCROLL_STATE_IDLE == scrollState
				&& getListView().getLastVisiblePosition() == mAdapter.getCount() - 1
				&& !isRefreshing()) {
			if (mAdapter.getCount() >= mTotal_number || mLastTotalCount == mAdapter.getCount()) {
				Toast.makeText(getActivity(), R.string.no_more, Toast.LENGTH_SHORT).show();
			} else {
				load(false);
			}
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
	}

	@Override
	public void onRefresh() {
		load(true);
	}

	// 加载数据
	private void load(boolean refresh) {
		setRefreshing(true);
		if (refresh) {
			mUsers.clear(); // 清空现有的列表
			mNext_cursor = 0;
		}
		CatnutAPI api = mFollowing
				? FriendshipsAPI.friends(mScreenName, 0, mNext_cursor, 1)
				: FriendshipsAPI.followers(mScreenName, 0, mNext_cursor, 1);
		mRequestQueue.add(new TransientRequest<List<TransientUser>>(api, listener, errorListener) {
			@Override
			protected Response<List<TransientUser>> parseNetworkResponse(NetworkResponse response) {
				return TransientUsersFragment.this.parseNetworkResponse(response);
			}
		});
	}

	// inner use
	private Response<List<TransientUser>> parseNetworkResponse(NetworkResponse response) {
		try {
			String jsonString =
					new String(response.data, HttpHeaderParser.parseCharset(response.headers));
			JSONObject result = new JSONObject(jsonString);
			// set next_cursor
			mNext_cursor = result.optInt(User.next_cursor);
			mTotal_number = result.optInt(User.total_number);
			JSONArray array = result.optJSONArray(User.MULTIPLE);
			if (array != null) {
				List<TransientUser> users = new ArrayList<TransientUser>(array.length());
				for (int i = 0; i < array.length(); i++) {
					users.add(TransientUser.convert(array.optJSONObject(i)));
				}
				return Response.success(users,
						HttpHeaderParser.parseCacheHeaders(response));
			} else {
				throw new RuntimeException("no users found!");
			}
		} catch (UnsupportedEncodingException e) {
			return Response.error(new ParseError(e));
		} catch (JSONException je) {
			return Response.error(new ParseError(je));
		} catch (Exception ex) {
			return Response.error(new ParseError(ex));
		}
	}
}
