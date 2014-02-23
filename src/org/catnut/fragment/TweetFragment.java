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
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import org.catnut.R;
import org.catnut.adapter.CommentsAdapter;
import org.catnut.api.CommentsAPI;
import org.catnut.core.CatnutApp;
import org.catnut.core.CatnutProvider;
import org.catnut.core.CatnutRequest;
import org.catnut.metadata.Status;
import org.catnut.metadata.User;
import org.catnut.processor.StatusProcessor;
import org.catnut.support.TweetImageSpan;
import org.catnut.support.TweetTextView;
import org.catnut.util.CatnutUtils;
import org.catnut.util.Constants;
import org.catnut.util.DateTime;
import org.json.JSONObject;

/**
 * 微博界面
 *
 * @author longkai
 */
public class TweetFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

	private static final String TAG = "TweetFragment";

	private static final int BATCH_SIZE = 50;

	/** 待检索的列 */
	private static final String[] PROJECTION = new String[]{
			"s._id",
			Status.uid,
			Status.columnText,
//			Status.source,
			"s." + Status.created_at,
			User.screen_name,
			User.profile_image_url,
			User.remark
	};

	private RequestQueue mRequestQueue;
	private ImageLoader mImageLoader;
	private TweetImageSpan mImageSpan;

	private CommentsAdapter mAdapter;

	// tweet id
	private long mId;

	// widgets
	private View mTweetLayout;
	private ImageView mAvatar;
	private TextView mRemark;
	private TextView mScreenName;
	private TweetTextView mText;
	private TextView mReplayCount;
	private TextView mReteetCount;
	private TextView mFavoriteCount;
	private TextView mSource;
	private TextView mCreateAt;

	private Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
		@Override
		public void onResponse(JSONObject response) {
			// todo
		}
	};

	private Response.ErrorListener errorListener = new Response.ErrorListener() {
		@Override
		public void onErrorResponse(VolleyError error) {
			Toast.makeText(getActivity(), error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
		}
	};

	public static TweetFragment getFragment(long id) {
		Bundle args = new Bundle();
		args.putLong(Constants.ID, id);
		TweetFragment fragment = new TweetFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		CatnutApp app = CatnutApp.getTingtingApp();
		mImageLoader = app.getImageLoader();
		mRequestQueue = app.getRequestQueue();
		mImageSpan = new TweetImageSpan(activity);
		mId = getArguments().getLong(Constants.ID);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mAdapter = new CommentsAdapter(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mTweetLayout = inflater.inflate(R.layout.tweet, null, false);
		mAvatar = (ImageView) mTweetLayout.findViewById(R.id.avatar);
		mRemark = (TextView) mTweetLayout.findViewById(R.id.remark);
		mScreenName = (TextView) mTweetLayout.findViewById(R.id.screen_name);
		mText = (TweetTextView) mTweetLayout.findViewById(R.id.text);
		mReplayCount = (TextView) mTweetLayout.findViewById(R.id.reply_count);
		mReteetCount = (TextView) mTweetLayout.findViewById(R.id.reply_count);
		mFavoriteCount = (TextView) mTweetLayout.findViewById(R.id.favorite_count);
		mSource = (TextView) mTweetLayout.findViewById(R.id.source);
		mCreateAt = (TextView) mTweetLayout.findViewById(R.id.create_at);
		// just return the list
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onViewCreated(final View view, Bundle savedInstanceState) {
		// load comment from cloud
		loadComments();
		// load from local...
		String query = CatnutUtils.buildQuery(
				new String[]{
						Status.uid,
						Status.columnText,
						Status.thumbnail_pic,
						Status.comments_count,
						Status.reposts_count,
						Status.attitudes_count,
						Status.source,
						"s." + Status.created_at,
						User.screen_name,
						User.avatar_large,
						User.remark,
						User.verified
				},
				"s._id=" + mId,
				Status.TABLE + " as s",
				"inner join " + User.TABLE + " as u on s.uid=u._id",
				null,
				null
		);
		new AsyncQueryHandler(getActivity().getContentResolver()) {
			@Override
			protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
				if (cursor.moveToNext()) {
					mImageLoader.get(cursor.getString(cursor.getColumnIndex(User.avatar_large)),
							ImageLoader.getImageListener(mAvatar, R.drawable.error, R.drawable.error));
					String remark = cursor.getString(cursor.getColumnIndex(User.remark));
					String screenName = cursor.getString(cursor.getColumnIndex(User.screen_name));
					mRemark.setText(TextUtils.isEmpty(remark) ? screenName : remark);
					mScreenName.setText("@" + screenName);
					mText.setText(cursor.getString(cursor.getColumnIndex(Status.columnText)));
					CatnutUtils.vividTweet(mText, mImageSpan);
					int replyCount = cursor.getInt(cursor.getColumnIndex(Status.comments_count));
					mReplayCount.setText(CatnutUtils.approximate(replyCount));
					int retweetCount = cursor.getInt(cursor.getColumnIndex(Status.reposts_count));
					mReteetCount.setText(CatnutUtils.approximate(retweetCount));
					int favoriteCount = cursor.getInt(cursor.getColumnIndex(Status.attitudes_count));
					mFavoriteCount.setText(CatnutUtils.approximate(favoriteCount));
					String source = cursor.getString(cursor.getColumnIndex(Status.source));
					mSource.setText(Html.fromHtml(source).toString());
					mCreateAt.setText(DateUtils.getRelativeTimeSpanString(
							DateTime.getTimeMills(cursor.getString(cursor.getColumnIndex(Status.created_at)))));
					if (CatnutUtils.getBoolean(cursor, User.verified)) {
						view.findViewById(R.id.verified).setVisibility(View.VISIBLE);
					}
				}
				cursor.close();
			}
		}.startQuery(0, null, CatnutProvider.parse(Status.MULTIPLE), null, query, null, null);
	}

	@Override
	public void onStart() {
		super.onStart();
		getActivity().getActionBar().setTitle(getString(R.string.tweet));
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getListView().addHeaderView(mTweetLayout);
		setListAdapter(mAdapter);
		getLoaderManager().initLoader(0, null, this);
	}

	private void loadComments() {
		mRequestQueue.add(new CatnutRequest(
				getActivity(),
				CommentsAPI.show(mId, 0, 0, BATCH_SIZE, 0, 0),
				new StatusProcessor.CommentTweetsProcessor(mId),
				listener,
				errorListener
		));
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String limit = String.valueOf(BATCH_SIZE);
		CursorLoader cursorLoader = CatnutUtils.getCursorLoader(
				getActivity(),
				CatnutProvider.parse(Status.MULTIPLE),
				PROJECTION,
				Status.TYPE + "=" + Status.COMMENT + " and " + Status.TO_WHICH_TWEET + "=" + mId,
				null,
				Status.TABLE + " as s",
				"inner join " + User.TABLE + " as u on s.uid=u._id",
				"s._id desc",
				limit
		);
		return cursorLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}
}
