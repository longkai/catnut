/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.v4.widget.SlidingPaneLayout;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.analytics.tracking.android.EasyTracker;
import com.squareup.picasso.Picasso;
import org.catnut.R;
import org.catnut.adapter.EmotionsAdapter;
import org.catnut.adapter.MentionSearchAdapter;
import org.catnut.api.StuffAPI;
import org.catnut.api.TweetAPI;
import org.catnut.core.CatnutAPI;
import org.catnut.core.CatnutApp;
import org.catnut.core.CatnutProvider;
import org.catnut.core.CatnutRequest;
import org.catnut.metadata.Status;
import org.catnut.metadata.User;
import org.catnut.metadata.WeiboAPIError;
import org.catnut.processor.StatusProcessor;
import org.catnut.support.LocationSupport;
import org.catnut.support.MultiPartRequest;
import org.catnut.support.TweetImageSpan;
import org.catnut.support.TweetTextView;
import org.catnut.util.CatnutUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * 发微博
 *
 * @author longkai
 */
public class ComposeTweetActivity extends Activity implements TextWatcher,
		AdapterView.OnItemClickListener, View.OnClickListener, LoaderManager.LoaderCallbacks<Cursor>, MenuItem.OnActionExpandListener {

	public static final String TAG = "ComposeTweetActivity";
	private static final int GALLERY = 1;
	private static final int CAMERA = 2;

	private Handler mHandler = new Handler();
	// app specifics
	private CatnutApp mApp;
	private EasyTracker mTracker;

//	private boolean mKeyboardShown = false; // 软键盘是否显示

	// customized actionbar widgets
	private View mCustomizedBar;
	private TextView mTextCounter;
	private View mSender; // 发送触发按钮
	private View mProgressor; // 发送进度条

	// widgets
	private SlidingPaneLayout mSlidingPaneLayout;
	private GridView mEmotions;
	private ActionBar mActionBar;
	private GridView mPhotos; // 待上传的图片
	private List<Uri> mUris;
	private ArrayAdapter<Uri> mAdapter;

	private Uri mTmpUri; // 暂存刚才拍照的图片

	// str
	private String mTitle;
	private String mEmotionTitle;
	private int mImageThumbSize;

	private ImageView mAvatar;
	private TextView mScreenName;
	private EditText mText;

	// @ search
	private String mCurKeywords;
	private MenuItem mMentionItem;
	private AutoCompleteTextView mAutoCompleteTextView;
	private InputMethodManager mInputMethodManager;
	private MentionSearchAdapter mMentionSearchAdapter;

	// listeners
	private Response.Listener<JSONObject> listener;
	private Response.ErrorListener errorListener;

	// location
	private LocationSupport.LocationResult mLocationResult; // lazy, if user require.
	private View mLocationMarker;
	private double mLongitude;
	private double mLatitude;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.compose);
		mApp = CatnutApp.getTingtingApp();
		mActionBar = getActionBar();

		mTitle = getString(R.string.compose);
		mEmotionTitle = getString(R.string.add_emotions);
		mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);

		injectLayout();
		injectActionBar();
		injectListener();

		mActionBar.setIcon(R.drawable.ic_title_compose);
		mActionBar.setTitle(mTitle);
		mActionBar.setDisplayHomeAsUpEnabled(true);
		mActionBar.setHomeButtonEnabled(true);

		mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

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
		mMentionItem = menu.findItem(R.id.action_mention);
		mMentionItem.setOnActionExpandListener(this);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (!mSlidingPaneLayout.isOpen()) {
			mSlidingPaneLayout.openPane();
		}
		switch (item.getItemId()) {
			case R.id.pref:
				startActivity(SingleFragmentActivity.getIntent(this, SingleFragmentActivity.PREF));
				break;
			case R.id.action_gallery:
				// todo: 暂时只支持上传一张图片，因为没有高级权限Orz
				if (mUris != null && mUris.size() > 0) {
					Toast.makeText(this, getString(R.string.only_one_pic_hint), Toast.LENGTH_LONG).show();
				} else {
					startActivityForResult(new Intent(Intent.ACTION_PICK).setType("image/*"), 1);
				}
				break;
			case R.id.action_shorten:
				shorten();
				break;
			case R.id.action_camera:
				// same as above
				if (mUris != null && mUris.size() > 0) {
					break;
				}
				Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				if (intent.resolveActivity(getPackageManager()) != null) {
					mTmpUri = CatnutUtils.createImageFile();
					if (mTmpUri != null) {
						intent.putExtra(MediaStore.EXTRA_OUTPUT, mTmpUri);
					}
					startActivityForResult(intent, CAMERA);
				} else {
					Toast.makeText(this, getString(R.string.device_not_support), Toast.LENGTH_SHORT).show();
				}
				break;
			default:
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.action_send:
				sendTweet();
				break;
			case R.id.action_discovery:
				int cursor = mText.getSelectionStart();
				mText.getText().append("##");
				mText.setSelection(cursor + 1);
				mText.requestFocus();
				break;
			case R.id.action_geo:
				if (mLocationMarker.getVisibility() == View.VISIBLE) {
					invalidateLocation();
				} else {
					requireLocation().getLocation(this, mLocationResult);
				}
				break;
			case R.id.location_marker:
				invalidateLocation();
				break;
			case R.id.clear:
				mAutoCompleteTextView.setText(null);
				break;
			default:
				break;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK:
				if (CatnutUtils.hasLength(mText) || (mUris != null && mUris.size() > 0)) {
					abort();
					return true; // deal it
				}
				break;
		}
		return super.onKeyDown(keyCode, event);
	}

	// 确定是否放弃已在编辑的内容
	private void abort() {
		new AlertDialog.Builder(this)
				.setMessage(getString(R.string.abort_existing_tweet_alert))
				.setNegativeButton(android.R.string.no, null)
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						navigateUpTo(getIntent());
					}
				})
				.show();
	}

	private void injectLayout() {
		// for panel
		mSlidingPaneLayout = (SlidingPaneLayout) findViewById(R.id.sliding_pane_layout);
		mEmotions = (GridView) findViewById(R.id.emotions);
		mEmotions.setAdapter(new EmotionsAdapter(this));
		mEmotions.setOnItemClickListener(this);
		mSlidingPaneLayout.setPanelSlideListener(new SliderListener());
		mSlidingPaneLayout.openPane();
		mSlidingPaneLayout.getViewTreeObserver().addOnGlobalLayoutListener(new FirstLayoutListener());
		// for tweet
		mAvatar = (ImageView) findViewById(R.id.avatar);
		mScreenName = (TextView) findViewById(R.id.screen_name);
		mText = (EditText) findViewById(R.id.text);
		mLocationMarker = findViewById(R.id.location_marker);
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

	private void injectActionBar() {
		mActionBar.setDisplayShowCustomEnabled(true);
		mCustomizedBar = LayoutInflater.from(this).inflate(R.layout.customized_actionbar, null);
		mTextCounter = (TextView) mCustomizedBar.findViewById(R.id.text_counter);
		mSender = mCustomizedBar.findViewById(R.id.action_send);
		mProgressor = mCustomizedBar.findViewById(android.R.id.progress);
		mActionBar.setCustomView(mCustomizedBar, new ActionBar.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.MATCH_PARENT, Gravity.END));
	}

	private void injectListener() {
		listener = new Response.Listener<JSONObject>() {
			@Override
			public void onResponse(JSONObject response) {
				mSender.setVisibility(View.VISIBLE);
				mProgressor.setVisibility(View.GONE);
				// delete posted text and thumbs
				mText.setText(null);
				if (mUris != null) {
					mUris.clear();
					mAdapter.notifyDataSetChanged();
				}
				Toast.makeText(ComposeTweetActivity.this, R.string.post_success, Toast.LENGTH_SHORT).show();
			}
		};
		errorListener = new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {
				mSender.setVisibility(View.VISIBLE);
				mProgressor.setVisibility(View.GONE);
				Log.e(TAG, "post tweet error!", error);
				WeiboAPIError weiboAPIError = WeiboAPIError.fromVolleyError(error);
				Toast.makeText(ComposeTweetActivity.this, weiboAPIError.error, Toast.LENGTH_LONG).show();
			}
		};
		mCustomizedBar.findViewById(R.id.action_discovery).setOnClickListener(this);
		mCustomizedBar.findViewById(R.id.action_geo).setOnClickListener(this);
		mCustomizedBar.findViewById(R.id.action_send).setOnClickListener(this);
		mLocationMarker.setOnClickListener(this);
	}

	private LocationSupport requireLocation() {
		Toast.makeText(this, getString(R.string.locating), Toast.LENGTH_SHORT).show();
		if (mLocationResult == null) {
			mLocationResult = new LocationSupport.LocationResult() {
				@Override
				public void gotLocation(Location location) {
					final boolean ok;
					if (location == null) {
						ok = false;
					} else {
						mLatitude = location.getLatitude();
						mLongitude = location.getLongitude();
						ok = true;
					}
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							findViewById(R.id.location_marker).setVisibility(ok ? View.VISIBLE : View.GONE);
							Toast.makeText(ComposeTweetActivity.this,
									ok ? R.string.locate_success : R.string.sorry_cannot_locate,
									Toast.LENGTH_SHORT).show();
						}
					});
				}
			};
		}
		return new LocationSupport();
	}

	// 取消定位
	private void invalidateLocation() {
		mLongitude = 0;
		mLatitude = 0;
		mLocationMarker.setVisibility(View.GONE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) { // for the moment, just check ok
			if (mPhotos == null) {
				initGallery();
			}
			switch (requestCode) {
				case GALLERY:
					if (data != null) {
						mUris.add(data.getData());
						mAdapter.notifyDataSetChanged();
					}
					break;
				case CAMERA:
					mUris.add(mTmpUri);
					mAdapter.notifyDataSetChanged();
					// reset
					mTmpUri = null;
					break;
				default:
					break;
			}
		}
	}

	private void initGallery() {
		ViewStub viewStub = (ViewStub) findViewById(R.id.view_stub);
		mPhotos = (GridView) viewStub.inflate();
		mUris = new LinkedList<Uri>();
		mAdapter = new ThumbsAdapter(this, mUris);
		mPhotos.setAdapter(mAdapter);
		// 长按删除之
		mPhotos.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				mUris.remove(position);
				mAdapter.notifyDataSetChanged();
				return true;
			}
		});
		// 单击直接查看
		mPhotos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				startActivity(new Intent(Intent.ACTION_VIEW, mUris.get(position)));
			}
		});
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
			mTextCounter.setTextColor(getResources().getColor(android.R.color.white));
		} else if (count <= 0) { // in fact, never lt 0
			mTextCounter.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
		} else {
			mTextCounter.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
		}
	}

	private void sendTweet() {
		if (!CatnutUtils.hasLength(mText)) {
			Toast.makeText(this, R.string.require_not_empty, Toast.LENGTH_SHORT).show();
			return; // stop here
		}
		// 防止多次提交
		mSender.setVisibility(View.GONE);
		mProgressor.setVisibility(View.VISIBLE);
		if (mUris != null && mUris.size() > 0) { // 有图片的
			mApp.getRequestQueue().add(new MultiPartRequest(
					this,
					TweetAPI.upload(mText.getText().toString(), 0, null, mUris, (float) mLatitude, (float) mLongitude, null, null),
					new StatusProcessor.SingleTweetProcessor(Status.HOME),
					listener,
					errorListener
			)).setTag(TAG);
		} else {
			mApp.getRequestQueue().add(new CatnutRequest(
					this,
					TweetAPI.update(mText.getText().toString(), 0, null, (float) mLatitude, (float) mLongitude, null, null),
					new StatusProcessor.SingleTweetProcessor(Status.HOME),
					listener,
					errorListener
			)).setTag(TAG);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		// 插入到编辑框里
		int cursor = mText.getSelectionStart();
		mText.getText().insert(cursor, CatnutUtils.text2Emotion(this, TweetImageSpan.EMOTION_KEYS[position]));
		// focus
		mText.requestFocus();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Log.d(TAG, mCurKeywords);
		StringBuilder where = new StringBuilder();
		where.append(User.screen_name).append(" like ").append(CatnutUtils.like(mCurKeywords))
				.append(" or ").append(User.remark).append(" like ").append(CatnutUtils.like(mCurKeywords));
		return CatnutUtils.getCursorLoader(
				this,
				CatnutProvider.parse(User.MULTIPLE),
				new String[]{
						User.screen_name,
						User.remark,
						BaseColumns._ID,
				},
				where.toString(),
				null,
				User.TABLE,
				null,
				null,
				null
		);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mMentionSearchAdapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mMentionSearchAdapter.swapCursor(null);
	}

	@Override
	public boolean onMenuItemActionExpand(MenuItem item) {
		if (mAutoCompleteTextView == null) {
			mAutoCompleteTextView = (AutoCompleteTextView) item.getActionView().findViewById(R.id.mention_search);
			mAutoCompleteTextView.setThreshold(1); // 每输入一个字就可以查询了
			mMentionSearchAdapter = new MentionSearchAdapter(ComposeTweetActivity.this);
			mAutoCompleteTextView.setAdapter(mMentionSearchAdapter);
		}
//		Toast.makeText(ComposeTweetActivity.this, getString(R.string.mention_helper_toast), Toast.LENGTH_SHORT).show();
		final View clear = item.getActionView().findViewById(R.id.clear);
		clear.setOnClickListener(ComposeTweetActivity.this);
		mAutoCompleteTextView.addTextChangedListener(new TextWatcher() {
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
				String text = s.toString();
				if (text.length() > 0) {
					clear.setVisibility(View.VISIBLE);
				} else {
					clear.setVisibility(View.GONE);
				}
				String key = text.trim();
				if (!TextUtils.isEmpty(key) && !key.equals(mCurKeywords)) {
					mCurKeywords = key;
					getLoaderManager().restartLoader(0, null, ComposeTweetActivity.this);
				}
			}
		});
		mAutoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Cursor cursor = (Cursor) mMentionSearchAdapter.getItem(position);
				mText.getText().append(getString(R.string.mention_text,
						cursor.getString(cursor.getColumnIndex(User.screen_name))));
				mText.setSelection(mText.length());
			}
		});
		mText.clearFocus();
		mAutoCompleteTextView.requestFocus();
		mAutoCompleteTextView.post(new Runnable() {
			@Override
			public void run() {
				mInputMethodManager.showSoftInput(mAutoCompleteTextView, InputMethodManager.SHOW_FORCED);
			}
		});
		return true;
	}

	// 将编辑框里的长链接统统转换为短链接
	private void shorten() {
		String text = mText.getText().toString();
		final Matcher matcher = TweetTextView.WEB_URL.matcher(text);
		String urls = "";
		while (matcher.find()) {
			urls += matcher.group() + "婷婷"; // 无所谓最后一个了
		}
		// http request
		if (!TextUtils.isEmpty(urls)) {
			final ProgressDialog dialog = ProgressDialog.show(this, null, getString(R.string.converting), true, false);
			CatnutAPI api = StuffAPI.shorten(urls.split("婷婷"));
			mApp.getRequestQueue().add(new JsonObjectRequest(
					api.method,
					api.uri,
					null,
					new Response.Listener<JSONObject>() {
						@Override
						public void onResponse(JSONObject response) {
							matcher.reset(); // 重置正则
							JSONArray _urls = response.optJSONArray("urls");
							StringBuffer sb = new StringBuffer();
							int i = 0;
							try {
								while (matcher.find()) {
									matcher.appendReplacement(sb, _urls.optJSONObject(i).optString("url_short"));
									i++;
								}
								matcher.appendTail(sb);
								mText.setText(sb);
								mText.setSelection(mText.length());
							} catch (Exception ex) {
								Log.e(TAG, "replace shorten url error!", ex);
							}
							dialog.dismiss();
						}
					},
					new Response.ErrorListener() {
						@Override
						public void onErrorResponse(VolleyError error) {
							dialog.dismiss();
							WeiboAPIError weiboAPIError = WeiboAPIError.fromVolleyError(error);
							Toast.makeText(ComposeTweetActivity.this, weiboAPIError.error, Toast.LENGTH_SHORT).show();
						}
					}
			));
		} else {
			Toast.makeText(this, getString(R.string.no_links_hint), Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public boolean onMenuItemActionCollapse(MenuItem item) {
		mInputMethodManager.hideSoftInputFromInputMethod(mAutoCompleteTextView.getWindowToken(), 0);
		mAutoCompleteTextView.setText(null);
		mText.requestFocus();
		return true;
	}

	private class SliderListener extends SlidingPaneLayout.SimplePanelSlideListener {
		@Override
		public void onPanelOpened(View panel) {
			ComposeTweetActivity.this.onPanelOpened();
		}

		@Override
		public void onPanelClosed(View panel) {
			ComposeTweetActivity.this.onPanelClosed();
		}
	}

	private class FirstLayoutListener implements ViewTreeObserver.OnGlobalLayoutListener {
		@Override
		public void onGlobalLayout() {
			onFirstLayout();
			mSlidingPaneLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
		}
	}

	private void onPanelClosed() {
		mActionBar.setDisplayHomeAsUpEnabled(true);
		mActionBar.setHomeButtonEnabled(true);
		mActionBar.setTitle(mEmotionTitle);
	}

	private void onPanelOpened() {
		mActionBar.setHomeButtonEnabled(false);
		mActionBar.setDisplayHomeAsUpEnabled(false);
		mActionBar.setTitle(mTitle);
		mText.requestFocus();
	}

	private void onFirstLayout() {
		if (mSlidingPaneLayout.isSlideable() && !mSlidingPaneLayout.isOpen()) {
			onPanelClosed();
		} else {
			onPanelOpened();
		}
	}

	private class ThumbsAdapter extends ArrayAdapter<Uri> {

		public ThumbsAdapter(Context context, List<Uri> uris) {
			super(context, 0, uris);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final ImageView photo;
			if (convertView == null) {
				photo = new ImageView(ComposeTweetActivity.this);
				photo.setBackgroundResource(R.drawable.overflow_dropdown_light);
				photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
			} else {
				photo = (ImageView) convertView;
			}
			// load image efficiently
			Picasso.with(getContext())
					.load(getItem(position))
					.resize(mImageThumbSize, mImageThumbSize)
					.centerCrop()
					.into(photo);
			return photo;
		}
	}
}
