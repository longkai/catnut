/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.google.analytics.tracking.android.EasyTracker;
import org.catnut.R;
import org.catnut.api.TweetAPI;
import org.catnut.core.CatnutApp;
import org.catnut.core.CatnutProvider;
import org.catnut.core.CatnutRequest;
import org.catnut.metadata.Status;
import org.catnut.metadata.User;
import org.catnut.metadata.WeiboAPIError;
import org.catnut.processor.StatusProcessor;
import org.catnut.util.CatnutUtils;
import org.json.JSONObject;

/**
 * 发微博
 *
 * @author longkai
 */
public class ComposeTweetActivity extends Activity implements TextWatcher {

	public static final String TAG = "ComposeTweetActivity";

	// app specifics
	private CatnutApp mApp;
	private EasyTracker mTracker;

	// widgets
	private ImageView mAvatar;
	private TextView mScreenName;
	private TextView mTextCounter;
	private EditText mText;
	private ImageView mGallery;
	private ImageView mGeo;
	private ImageView mCamera;

	// listeners
	private Response.Listener<JSONObject> listener = new Response.Listener<JSONObject>() {
		@Override
		public void onResponse(JSONObject response) {
			// delete posted text
			mText.setText(null);
			Toast.makeText(ComposeTweetActivity.this, R.string.post_success, Toast.LENGTH_SHORT).show();
		}
	};

	private Response.ErrorListener  errorListener = new Response.ErrorListener() {
		@Override
		public void onErrorResponse(VolleyError error) {
			Log.e(TAG, "post tweet error!", error);
			WeiboAPIError weiboAPIError = WeiboAPIError.fromVolleyError(error);
			Toast.makeText(ComposeTweetActivity.this, weiboAPIError.error, Toast.LENGTH_SHORT).show();
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.compose);
		mApp = CatnutApp.getTingtingApp();

		injectLayout();

		ActionBar bar = getActionBar();
		bar.setIcon(R.drawable.ic_title_compose);
		bar.setDisplayHomeAsUpEnabled(true);
		bar.setTitle(R.string.compose);

		if (mApp.getPreferences().getBoolean(getString(R.string.pref_enable_analytics), true)) {
			mTracker = EasyTracker.getInstance(this);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (mTracker != null) {
			mTracker.activityStart(this);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (mTracker != null) {
			mTracker.activityStop(this);
		}
		mApp.getRequestQueue().cancelAll(TAG);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.compose, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				navigateUpTo(getIntent());
				break;
			case R.id.action_send:
				sendTweet(false);
				break;
			case R.id.pref:
				startActivity(SingleFragmentActivity.getIntent(this, SingleFragmentActivity.PREF));
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void injectLayout() {
		mAvatar = (ImageView) findViewById(R.id.avatar);
		mScreenName = (TextView) findViewById(R.id.screen_name);
		mTextCounter = (TextView) findViewById(R.id.text_counter);
		mText = (EditText) findViewById(R.id.text);
		mGallery = (ImageView) findViewById(R.id.action_gallery);
		mCamera = (ImageView) findViewById(R.id.action_camera);
		mGeo = (ImageView) findViewById(R.id.action_geo);
		// set data to layout...
		new AsyncQueryHandler(getContentResolver()) {
			@Override
			protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
				if (cursor.moveToNext()) {
					mApp.getImageLoader().get(cursor.getString(cursor.getColumnIndex(User.avatar_large)),
							ImageLoader.getImageListener(mAvatar, R.drawable.error, R.drawable.error));
					mScreenName.setText("@" + cursor.getString(cursor.getColumnIndex(User.screen_name)));
				}
				cursor.close();
			}
		}.startQuery(0, null,
				CatnutProvider.parse(User.MULTIPLE, mApp.getAccessToken().uid),
				new String[]{User.avatar_large, User.screen_name}, null, null, null);
		// other stuffs...
		mText.addTextChangedListener(this);
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		// no-op
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// no-op
	}

	@Override
	public void afterTextChanged(Editable s) {
		int count = 140 - mText.length();
		mTextCounter.setText(String.valueOf(count));
		if (count >= 10) {
			// def color
			mTextCounter.setTextColor(getResources().getColor(android.R.color.holo_green_light));
		} else if (count <= 0) { // in fact, never lt 0
			mTextCounter.setTextColor(getResources().getColor(android.R.color.holo_red_light));
		} else {
			mTextCounter.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
		}
	}

	private void sendTweet(boolean withImage) {
		if (!CatnutUtils.hasLength(mText)) {
			Toast.makeText(this, R.string.require_not_empty, Toast.LENGTH_SHORT).show();
			return; // stop here
		}
		mApp.getRequestQueue().add(new CatnutRequest(
				this,
				TweetAPI.update(mText.getText().toString(), 0, null, 0f, 0f, null, null),
				new StatusProcessor.SingleTweetProcessor(Status.HOME),
				listener,
				errorListener
		)).setTag(TAG);
	}

}