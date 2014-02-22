/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.fragment;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ProgressBar;
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
import org.catnut.core.CatnutAPI;
import org.catnut.core.CatnutApp;
import org.catnut.metadata.User;
import org.catnut.support.TransientRequest;
import org.catnut.support.TransientUser;
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
public class FollowersFragment extends ListFragment implements AbsListView.OnScrollListener {

	private int next_cursor = 0;
	private int total_number = 0;

	private RequestQueue mRequestQueue;

	private String mScreenName;

	private ProgressBar mLoadMore;
	private boolean mLoading = false;

	private ArrayList<TransientUser> mUsers;
	private TransientUsersAdapter mAdapter;

	private Response.Listener<List<TransientUser>> listener = new Response.Listener<List<TransientUser>>() {
		@Override
		public void onResponse(List<TransientUser> response) {
			mUsers.addAll(response);
			mAdapter.notifyDataSetChanged();
			if (total_number <= mUsers.size()) {
				Toast.makeText(getActivity(), R.string.no_more, Toast.LENGTH_SHORT).show();
				getListView().removeFooterView(mLoadMore);
			}
			mLoading = false;
		}
	};

	private Response.ErrorListener errorListener = new Response.ErrorListener() {
		@Override
		public void onErrorResponse(VolleyError error) {
			Toast.makeText(getActivity(), error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
			mLoading = false;
		}
	};

	public static FollowersFragment getFragment(String screenName) {
		Bundle args = new Bundle();
		args.putString(User.screen_name, screenName);
		FollowersFragment followersFragment = new FollowersFragment();
		followersFragment.setArguments(args);
		return followersFragment;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mRequestQueue = CatnutApp.getTingtingApp().getRequestQueue();
		Bundle args = getArguments();
		mScreenName = args.getString(User.screen_name);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mUsers = new ArrayList<TransientUser>();
		mAdapter = new TransientUsersAdapter(getActivity(), mUsers);
		mLoadMore = new ProgressBar(getActivity());
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		loadFromCloud();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getListView().setOnScrollListener(this);
		setListAdapter(mAdapter);
		getListView().addFooterView(mLoadMore);
	}

	@Override
	public void onStart() {
		super.onStart();
		getActivity().getActionBar().setTitle(getString(R.string.his_followers, mScreenName));
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		if (!mLoading && mLoadMore.isShown() && mUsers.size() != 0) {
			loadFromCloud();
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
	}

	private void loadFromCloud() {
		mLoading = true;
		CatnutAPI api = FriendshipsAPI.followers(mScreenName, 0, next_cursor, 1);
		mRequestQueue.add(new TransientRequest<List<TransientUser>>(api, listener, errorListener) {
			@Override
			protected Response<List<TransientUser>> parseNetworkResponse(NetworkResponse response) {
				return FollowersFragment.this.parseNetworkResponse(response);
			}
		});
	}

	// inner use...
	private Response<List<TransientUser>> parseNetworkResponse(NetworkResponse response) {
		try {
			String jsonString =
					new String(response.data, HttpHeaderParser.parseCharset(response.headers));
			JSONObject result = new JSONObject(jsonString);
			// set next_cursor
			next_cursor = result.optInt(User.next_cursor);
			total_number = result.optInt(User.total_number);
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
