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
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Gravity;
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
import com.squareup.picasso.Target;
import org.catnut.R;
import org.catnut.api._500pxAPI;
import org.catnut.core.CatnutApp;
import org.catnut.core.CatnutProcessor;
import org.catnut.core.CatnutProvider;
import org.catnut.core.CatnutRequest;
import org.catnut.fragment.FantasiesFragment;
import org.catnut.metadata.Photo;
import org.catnut.util.CatnutUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

/**
 * 欢迎界面，可以在这里放一些更新说明，pager，或者进行一些初始化（检测网络状态，是否通过了新浪的授权）等等，
 * 设置好播放的时间后自动跳转到main-ui，或者用户自己触发某个控件跳转
 * <p/>
 * no history in android-manifest!
 *
 * @author longkai
 */
public class HelloActivity extends Activity {

	public static final String TAG = "HelloActivity";

	/** 欢迎界面默认的播放时间 */
	private static final long DEFAULT_SPLASH_TIME_MILLS = 3000L;

	private static final int MIN_FANTASY_RUN_TIMES = 3;

	// only use for auth!
	private EasyTracker mTracker;

	private CatnutApp mApp;
	private SharedPreferences mPreferences;

	private Handler mHandler = new Handler();

	private View mAbout;
	private ImageView mFantasy;
	private int mRuntimes;

	private String mCurrentTitle;
	private String mCurrentFantasy;
	private String mCurrentDesc;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mApp = CatnutApp.getTingtingApp();
		mPreferences = mApp.getPreferences();
		// 根据是否已经授权，切换不同的界面
		if (mApp.getAccessToken() == null) {
			startActivity(SingleFragmentActivity.getIntent(this, SingleFragmentActivity.AUTH));
		} else {
			// 检查一次更新，每周一次
			CatnutUtils.checkout(false, this, mPreferences);
			mRuntimes = mPreferences.getInt(getString(R.string.pref_run_times), 0);
			mPreferences.edit().putInt(getString(R.string.pref_run_times), mRuntimes + 1).commit();
			// 根据情况跳转
			if (getIntent().hasExtra(TAG)) {
				init();
			} else {
				/*if (runtimes < MIN_FANTASY_RUN_TIMES) {
					init();
					String key = getString(R.string.pref_run_times);
					int before = mPreferences.getInt(key, 0);
					mPreferences.edit().putInt(key, before++);
				} else*/ if (mPreferences.getBoolean(getString(R.string.pref_enter_home_directly),
						getResources().getBoolean(R.bool.pref_enter_home_directly))) {
					startActivity(new Intent(this, MainActivity.class));
				} else {
					init();
				}
			}
		}
	}

	private void init() {
		setContentView(R.layout.about);
		fetch500px();
		mAbout = findViewById(R.id.about);
		mFantasy = (ImageView) findViewById(R.id.fantasy);
		ActionBar bar = getActionBar();
		bar.setTitle(R.string.fantasy);
		TextView about = (TextView) findViewById(R.id.about_body);
		TextView version = (TextView) findViewById(R.id.app_version);
		version.setText(getString(R.string.about_version_template, getString(R.string.version_name)));
		if (mRuntimes < MIN_FANTASY_RUN_TIMES) {
			about.setText(Html.fromHtml(getString(R.string.about_body)));
			about.setMovementMethod(LinkMovementMethod.getInstance());
		} else {
			// for girl' s day only, in march 7-21
			Calendar now = Calendar.getInstance();
			boolean girl = now.get(Calendar.MONTH) == Calendar.MARCH
					&& now.get(Calendar.DAY_OF_MONTH) >= 7
					&& now.get(Calendar.DAY_OF_MONTH) <= 21;
			if (girl) {
				about.setText(Html.fromHtml(getString(R.string.girls_day)));
				about.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL);
			}
		}
		loadImage();

		if (mApp.getPreferences().getBoolean(getString(R.string.enable_analytics), true)) {
			mTracker = EasyTracker.getInstance(this);
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
							Log.d(TAG, "load 500px done...");
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
						mCurrentFantasy = cursor.getString(cursor.getColumnIndex(Photo.image_url));
						mCurrentDesc = cursor.getString(cursor.getColumnIndex(Photo.description));
						mCurrentTitle = cursor.getString(cursor.getColumnIndex(Photo.name));
						Picasso.with(HelloActivity.this)
								.load(mCurrentFantasy)
								.into(target);
						cursor.close();
					}
				});
			} else {
				cursor.close();
			}
		}
	};

	private Target target = new Target() {
		@Override
		public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
			mFantasy.setImageBitmap(bitmap);
			mFantasy.setScaleType(ImageView.ScaleType.FIT_XY);
		}

		@Override
		public void onBitmapFailed(Drawable errorDrawable) {

		}

		@Override
		public void onPrepareLoad(Drawable placeHolderDrawable) {

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
				mFantasy.setImageResource(R.drawable.default_fantasy);
				break;
			case R.id.gallery:
				Intent intent = SingleFragmentActivity.getIntent(this, SingleFragmentActivity.GALLERY);
				intent.putExtra(FantasiesFragment.PICS, new String[]{mCurrentFantasy});
				intent.putExtra(FantasiesFragment.DESCS, new String[]{mCurrentDesc});
				startActivity(intent);
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