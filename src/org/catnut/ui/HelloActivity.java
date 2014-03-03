/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.MapBuilder;
import com.squareup.picasso.Picasso;
import org.catnut.R;
import org.catnut.api._500pxAPI;
import org.catnut.core.CatnutApp;
import org.catnut.core.CatnutProcessor;
import org.catnut.core.CatnutProvider;
import org.catnut.core.CatnutRequest;
import org.catnut.metadata.Photo;
import org.catnut.util.CatnutUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 欢迎界面，可以在这里放一些更新说明，pager，或者进行一些初始化（检测网络状态，是否通过了新浪的授权）等等，
 * 设置好播放的时间后自动跳转到main-ui，或者用户自己触发某个控件跳转
 * <p/>
 * no history in android-manifest!
 *
 * @author longkai
 */
public class HelloActivity extends Activity {

	private static final String TAG = "HelloActivity";

	/** 欢迎界面默认的播放时间 */
	private static final long DEFAULT_SPLASH_TIME_MILLS = 3000L;

	// only use for auth!
	private EasyTracker mTracker;

	private CatnutApp mApp;
	private SharedPreferences mPreferences;

	private Handler mHandler = new Handler();

	private View mAbout;
	private ImageView mFantasy;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mApp = CatnutApp.getTingtingApp();
		mPreferences = mApp.getPreferences();
		// 根据是否已经授权，切换不同的界面
		if (mApp.getAccessToken() == null) {
			startActivity(SingleFragmentActivity.getIntent(this, SingleFragmentActivity.AUTH));
		} else {
			if (mPreferences.getBoolean(getString(R.string.pref_enter_home_directly), getResources().getBoolean(R.bool.pref_enter_home_directly))) {
				startActivity(new Intent(HelloActivity.this, MainActivity.class));
			} else {
				setContentView(R.layout.about);
				fetch500px();
				mAbout = findViewById(R.id.about);
				mFantasy = (ImageView) findViewById(R.id.fantasy);
				ActionBar bar = getActionBar();
				bar.setTitle("fantasy");
				TextView about = (TextView) findViewById(R.id.about_body);
				about.setText(Html.fromHtml(getString(R.string.about_body)));
				loadImage();

				if (mApp.getPreferences().getBoolean(getString(R.string.enable_analytics), true)) {
					mTracker = EasyTracker.getInstance(this);
				}
			}
		}
	}

	// save image urls locally
	private void fetch500px() {
		if (mPreferences.getBoolean(getString(R.string.pref_enable_fantasy),
				getResources().getBoolean(R.bool.pref_enable_fantasy))) {
			mApp.getRequestQueue().add(new CatnutRequest(
					this,
					_500pxAPI.photos("popular"),
					new CatnutProcessor<JSONObject>() {
						@Override
						public void asyncProcess(Context context, JSONObject data) throws Exception {
							Log.d(TAG, String.valueOf(data));
							JSONArray array = data.optJSONArray(Photo.MULTIPLE);
							ContentValues[] photos = new ContentValues[array.length()];
							for (int i = 0; i < array.length(); i++) {
								photos[i] = Photo.METADATA.convert(array.optJSONObject(i));
							}
							context.getContentResolver().bulkInsert(CatnutProvider.parse(Photo.MULTIPLE), photos);
						}
					},
					null,
					new Response.ErrorListener() {
						@Override
						public void onErrorResponse(VolleyError error) {
							Log.e(TAG, "fetch 500px error", error);
						}
					}
			));
		}
	}

	private void loadImage() {
		(new Thread(mLoadImage)).start();
	}

	private Runnable mLoadImage = new Runnable() {
		@Override
		public void run() {
			String query = CatnutUtils.buildQuery(null, null, Photo.TABLE, null, "RANDOM()", "1");
			final Cursor cursor = getContentResolver().query(CatnutProvider.parse(Photo.MULTIPLE), null, query, null, null);
			if (cursor.moveToNext()) {
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						Picasso.with(HelloActivity.this)
								.load(cursor.getString(cursor.getColumnIndex(Photo.image_url)))
								.into(mFantasy);
						cursor.close();
					}
				});
			}
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.fantasy, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.toggle_fantasy:
				if (mAbout.getVisibility() == View.VISIBLE) {
					mAbout.setVisibility(View.GONE);
				} else {
					mAbout.setVisibility(View.VISIBLE);
				}
				break;
			case R.id.change_fantasy:
				loadImage();
				break;
			case R.id.pref:
				startActivity(SingleFragmentActivity.getIntent(this, SingleFragmentActivity.PREF));
				break;
			case android.R.id.home:
				startActivity(new Intent(this, MainActivity.class));
				break;
			case R.id.check_default:
				mFantasy.setImageResource(R.drawable.defaul_fantasy);
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (mTracker != null) {
			mTracker.send(MapBuilder.createAppView().set(Fields.SCREEN_NAME, "auth").build());
			mTracker.activityStart(this);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (mTracker != null) {
			mTracker.activityStop(this);
		}
	}
}