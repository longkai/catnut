/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.plugin.zhihu;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import com.squareup.picasso.Picasso;
import org.catnut.R;
import org.catnut.core.CatnutApp;
import org.catnut.core.CatnutProvider;
import org.catnut.fragment.GalleryPagerFragment;
import org.catnut.support.QuickReturnScrollView;
import org.catnut.ui.SingleFragmentActivity;
import org.catnut.util.CatnutUtils;
import org.catnut.util.Constants;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 知乎条目
 *
 * @author longkai
 */
public class ZhihuItemFragment extends Fragment implements
		QuickReturnScrollView.Callbacks, OnRefreshListener, View.OnClickListener {
	public static final String TAG = ZhihuItemFragment.class.getSimpleName();

	private static final String[] PROJECTION = new String[]{
			Zhihu.QUESTION_ID,
			Zhihu.ANSWER,
			Zhihu.DESCRIPTION,
			Zhihu.TITLE,
			Zhihu.LAST_ALTER_DATE,
			Zhihu.NICK,
	};

	public static final Pattern HTML_IMG = Pattern.compile("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>");

	private static final int ACTION_VIEW_ON_WEB = 1;
	private static final int ACTION_VIEW_ALL_ON_WEB = 2;

	private ScrollSettleHandler mScrollSettleHandler = new ScrollSettleHandler();

	private View mPlaceholderView;
	private View mQuickReturnView;
	private QuickReturnScrollView mQuickReturnLayout;

	private int mMinRawY = 0;
	private int mState = STATE_ON_SCREEN;
	private int mQuickReturnHeight;
	private int mMaxScrollY;

	private PullToRefreshLayout mPullToRefreshLayout;

	private long mAnswerId;
	private long mQuestionId;

	private ArrayList<Uri> mImageUrls;

	public static ZhihuItemFragment getFragment(long id) {
		Bundle args = new Bundle();
		args.putLong(Constants.ID, id);
		ZhihuItemFragment fragment = new ZhihuItemFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mAnswerId = getArguments().getLong(Constants.ID);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.zhihu_item, container, false);

		mPullToRefreshLayout = (PullToRefreshLayout) view;
		ActionBarPullToRefresh.from(getActivity())
				.allChildrenArePullable()
				.listener(this)
				.setup(mPullToRefreshLayout);
		mPullToRefreshLayout.setRefreshing(true);

		mQuickReturnLayout = (QuickReturnScrollView) view.findViewById(R.id.quick_return);
		mPlaceholderView = mQuickReturnLayout.findViewById(R.id.place_holder);
		mQuickReturnView = mQuickReturnLayout.findViewById(android.R.id.title);
		mQuickReturnLayout.setCallbacks(this);
		mQuickReturnLayout.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						onScrollChanged(mQuickReturnLayout.getScrollY());
						mMaxScrollY = mQuickReturnLayout.computeVerticalScrollRange()
								- mQuickReturnLayout.getHeight();
						mQuickReturnHeight = mQuickReturnView.getHeight();
					}
				}
		);
		return view;
	}

	@Override
	public void onViewCreated(final View view, final Bundle savedInstanceState) {
		final TextView title = (TextView) view.findViewById(android.R.id.title);
		final TextView author = (TextView) view.findViewById(R.id.author);
		final TextView lastAlterDate = (TextView) view.findViewById(R.id.last_alter_date);

		registerForContextMenu(title);
		title.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				getActivity().openContextMenu(title);
			}
		});

		(new Thread(new Runnable() {
			@Override
			public void run() {
				Cursor cursor = getActivity().getContentResolver().query(
						CatnutProvider.parse(Zhihu.MULTIPLE),
						PROJECTION,
						Zhihu.ANSWER_ID + "=" + mAnswerId,
						null,
						null
				);
				if (cursor.moveToNext()) {
					mQuestionId = cursor.getLong(cursor.getColumnIndex(Zhihu.QUESTION_ID));
					final String _title = cursor.getString(cursor.getColumnIndex(Zhihu.TITLE));
					final String _question = cursor.getString(cursor.getColumnIndex(Zhihu.DESCRIPTION));
					final String _nick = cursor.getString(cursor.getColumnIndex(Zhihu.NICK));
					final String _content = cursor.getString(cursor.getColumnIndex(Zhihu.ANSWER));
					final long _lastAlterDate = cursor.getLong(cursor.getColumnIndex(Zhihu.LAST_ALTER_DATE));
					cursor.close();

					// answer
					Matcher matcher = HTML_IMG.matcher(_content);
					final List<String> contentSegment = new ArrayList<String>();
					processText(_content, matcher, contentSegment);

					// question
					matcher = HTML_IMG.matcher(_question);
					final List<String> questionSegment = new ArrayList<String>();
					processText(_question, matcher, questionSegment);
					new Handler(Looper.getMainLooper()).post(new Runnable() {
						@Override
						public void run() {
							title.setText(_title);
							if (_title.length() > 30) {
								title.setTextSize(18);
							}

							// 假设第一个是文本，即偶数文本，奇数图片
							int l = contentSegment.size() > 1 ? contentSegment.size() >> 1 : 0;
							l += questionSegment.size() > 1 ? questionSegment.size() >> 1 : 0;

							if (l > 0) {
								mImageUrls = new ArrayList<Uri>(l);
							}

							// 是否使用缓存的图片
							boolean useCachedImg = CatnutApp.getBoolean(R.string.pref_enable_cache_zhihu_images, R.bool.default_plugin_status);

							l = 0; // reset for reuse
							String text;
							int screenWidth = CatnutUtils.getScreenWidth(getActivity());
							int max = getActivity().getResources().getDimensionPixelSize(R.dimen.max_thumb_width);
							if (screenWidth > max) {
								screenWidth = max;
							}

							LayoutInflater inflater = LayoutInflater.from(getActivity());
							if (!TextUtils.isEmpty(_question)) {
								ViewGroup questionHolder = (ViewGroup) view.findViewById(R.id.question);
								for (int i = 0; i < questionSegment.size(); i++) {
									text = questionSegment.get(i);
									if (!TextUtils.isEmpty(text)) {
										if ((i & 1) == 0) {
											TextView section = (TextView) inflater.inflate(R.layout.zhihu_text, null);
											section.setTextSize(16);
											section.setTextColor(getResources().getColor(R.color.black50PercentColor));
											section.setText(Html.fromHtml(text));
											section.setMovementMethod(LinkMovementMethod.getInstance());
											CatnutUtils.removeLinkUnderline(section);
											questionHolder.addView(section);
										} else {
											ImageView imageView = getImageView();
											Uri uri = useCachedImg ? Zhihu.getCacheImageLocation(getActivity(), Uri.parse(text)) : Uri.parse(text);
											Picasso.with(getActivity())
													.load(uri)
													.resize(screenWidth, (int) (Constants.GOLDEN_RATIO * screenWidth))
													.error(R.drawable.error)
													.into(imageView);
											imageView.setTag(l++); // for click
											imageView.setOnClickListener(ZhihuItemFragment.this);
											mImageUrls.add(uri);
										}
									}
								}
							}

							Typeface typeface = CatnutUtils.getTypeface(
									CatnutApp.getTingtingApp().getPreferences(),
									getString(R.string.pref_customize_tweet_font),
									getString(R.string.default_typeface)
							);
							ViewGroup answerHolder = (ViewGroup) view.findViewById(R.id.answer);
							for (int i = 0; i < contentSegment.size(); i++) {
								text = contentSegment.get(i);
								if (!TextUtils.isEmpty(text)) {
									if ((i & 1) == 0) {
										TextView section = (TextView) inflater.inflate(R.layout.zhihu_text, null);
										section.setText(Html.fromHtml(text));
										CatnutUtils.setTypeface(section, typeface);
										CatnutUtils.removeLinkUnderline(section);
										section.setMovementMethod(LinkMovementMethod.getInstance());
										answerHolder.addView(section);
									} else {
										ImageView image = getImageView();
										Uri uri = useCachedImg ? Zhihu.getCacheImageLocation(getActivity(), Uri.parse(text)) : Uri.parse(text);
										Picasso.with(getActivity())
												.load(uri)
												.resize(screenWidth, (int) (Constants.GOLDEN_RATIO * screenWidth))
												.error(R.drawable.error)
												.into(image);
										image.setTag(l++); // 方便点击事件
										image.setOnClickListener(ZhihuItemFragment.this);
										mImageUrls.add(uri);
										answerHolder.addView(image);
									}
								}
							}
							author.setText(_nick);
							lastAlterDate.setText(DateUtils.getRelativeTimeSpanString(_lastAlterDate));
							if (mPullToRefreshLayout != null) {
								mPullToRefreshLayout.setRefreshComplete();
							}
							if (savedInstanceState != null) {
								final int scrollY = savedInstanceState.getInt(TAG);
								mQuickReturnLayout.scrollTo(0, scrollY);
								mQuickReturnLayout.postDelayed(new Runnable() {
									@Override
									public void run() {
										mQuickReturnLayout.scrollTo(0, scrollY);
									}
								}, 500);
							}
						}
					});
				} else {
					cursor.close();
				}
			}
		})).start();
	}

	private ImageView getImageView() {
		ImageView image = new ImageView(getActivity());
		image.setAdjustViewBounds(true);
		image.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return CatnutUtils.imageOverlay(v, event);
			}
		});
		image.setOnClickListener(this);
		return image;
	}

	private void processText(String _content, Matcher matcher, List<String> contentSegment) {
		int start;
		int lastStart = 0;
		while (matcher.find()) {
			start = matcher.start();
			contentSegment.add(_content.substring(lastStart, start));
			lastStart = matcher.end();
			contentSegment.add(matcher.group(1));
		}
		// no image, fallback
		if (contentSegment.size() == 0) {
			contentSegment.add(_content);
		} else {
			// append tail
			if ((contentSegment.size() - 1 & 1) == 0) {
				contentSegment.add(null); // place holder...
			}
			contentSegment.add(_content.substring(lastStart));
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		getActivity().getActionBar().setTitle(getString(R.string.read_zhihu));
		// 标记已读
		new Thread(new MarkHasReadRunable(mAnswerId, getActivity(), true)).start();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		menu.add(Menu.NONE, ACTION_VIEW_ALL_ON_WEB, Menu.NONE, getString(R.string.view_all_answer));
		menu.add(Menu.NONE, ACTION_VIEW_ON_WEB, Menu.NONE, getString(R.string.zhihu_view_on_web));
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		viewOutside(item.getItemId());
		return super.onContextItemSelected(item);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.add(Menu.NONE, ACTION_VIEW_ALL_ON_WEB, Menu.NONE, getString(R.string.view_all_answer))
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		menu.add(Menu.NONE, ACTION_VIEW_ON_WEB, Menu.NONE, getString(R.string.zhihu_view_on_web))
				.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		viewOutside(item.getItemId());
		return super.onOptionsItemSelected(item);
	}

	private void viewOutside(int which) {
		switch (which) {
			case ACTION_VIEW_ON_WEB:
				startActivity(new Intent(Intent.ACTION_VIEW,
						Uri.parse("http://www.zhihu.com/question/" + mQuestionId + "/answer/" + mAnswerId)));
				break;
			case ACTION_VIEW_ALL_ON_WEB:
				startActivity(new Intent(Intent.ACTION_VIEW,
						Uri.parse("http://www.zhihu.com/question/" + mQuestionId)));
				break;
			default:
				break;
		}
	}

	@Override
	public void onScrollChanged(int scrollY) {
		scrollY = Math.min(mMaxScrollY, scrollY);

		mScrollSettleHandler.onScroll(scrollY);

		int rawY = mPlaceholderView.getTop() - scrollY;
		int translationY = 0;

		switch (mState) {
			case STATE_OFF_SCREEN:
				if (rawY <= mMinRawY) {
					mMinRawY = rawY;
				} else {
					mState = STATE_RETURNING;
				}
				translationY = rawY;
				break;

			case STATE_ON_SCREEN:
				if (rawY < -mQuickReturnHeight) {
					mState = STATE_OFF_SCREEN;
					mMinRawY = rawY;
				}
				translationY = rawY;
				break;

			case STATE_RETURNING:
				translationY = (rawY - mMinRawY) - mQuickReturnHeight;
				if (translationY > 0) {
					translationY = 0;
					mMinRawY = rawY - mQuickReturnHeight;
				}

				if (rawY > 0) {
					mState = STATE_ON_SCREEN;
					translationY = rawY;
				}

				if (translationY < -mQuickReturnHeight) {
					mState = STATE_OFF_SCREEN;
					mMinRawY = rawY;
				}
				break;
		}
		mQuickReturnView.animate().cancel();
		mQuickReturnView.setTranslationY(translationY + scrollY);
	}

	@Override
	public void onDownMotionEvent() {
		mScrollSettleHandler.setSettleEnabled(false);
	}

	@Override
	public void onUpOrCancelMotionEvent() {
		mScrollSettleHandler.setSettleEnabled(true);
		mScrollSettleHandler.onScroll(mQuickReturnLayout.getScrollY());
	}

	@Override
	public void onRefreshStarted(View view) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				// just for fun~
				try {
					TimeUnit.MILLISECONDS.sleep(1500);
				} catch (InterruptedException e) {
				}
				mScrollSettleHandler.post(new Runnable() {
					@Override
					public void run() {
						if (mPullToRefreshLayout != null) {
							mPullToRefreshLayout.setRefreshComplete();
						}
					}
				});
			}
		}).start();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt(TAG, mQuickReturnLayout.getScrollY());
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onClick(View v) {
		ImageView image = (ImageView) v;
		((ImageView) v).getDrawable().clearColorFilter();
		image.invalidate();
		Integer index = (Integer) v.getTag();
		Intent intent = SingleFragmentActivity.getIntent(getActivity(), SingleFragmentActivity.GALLERY);
		intent.putExtra(GalleryPagerFragment.CUR_INDEX, index);
		intent.putExtra(GalleryPagerFragment.URLS, mImageUrls);
		intent.putExtra(GalleryPagerFragment.TITLE, getString(R.string.view_photos));
		startActivity(intent);
	}

	// quick return animation
	private class ScrollSettleHandler extends Handler {
		private static final int SETTLE_DELAY_MILLIS = 100;

		private int mSettledScrollY = Integer.MIN_VALUE;
		private boolean mSettleEnabled;

		public void onScroll(int scrollY) {
			if (mSettledScrollY != scrollY) {
				// Clear any pending messages and post delayed
				removeMessages(0);
				sendEmptyMessageDelayed(0, SETTLE_DELAY_MILLIS);
				mSettledScrollY = scrollY;
			}
		}

		public void setSettleEnabled(boolean settleEnabled) {
			mSettleEnabled = settleEnabled;
		}

		@Override
		public void handleMessage(Message msg) {
			// Handle the scroll settling.
			if (STATE_RETURNING == mState && mSettleEnabled) {
				int mDestTranslationY;
				if (mSettledScrollY - mQuickReturnView.getTranslationY() > mQuickReturnHeight / 2) {
					mState = STATE_OFF_SCREEN;
					mDestTranslationY = Math.max(
							mSettledScrollY - mQuickReturnHeight,
							mPlaceholderView.getTop());
				} else {
					mDestTranslationY = mSettledScrollY;
				}

				mMinRawY = mPlaceholderView.getTop() - mQuickReturnHeight - mDestTranslationY;
				mQuickReturnView.animate().translationY(mDestTranslationY);
			}
			mSettledScrollY = Integer.MIN_VALUE; // reset
		}
	}

	/**
	 * 标记已读线程
	 */
	public static class MarkHasReadRunable implements Runnable {

		private long answerId;
		private Context context;
		private boolean hasRead;

		public MarkHasReadRunable(long answerId, Context context, boolean hasRead) {
			this.answerId = answerId;
			this.context = context;
			this.hasRead = hasRead;
		}

		@Override
		public void run() {
			ContentValues values = new ContentValues();
			values.put(Zhihu.HAS_READ, hasRead);
			context.getContentResolver().update(
					CatnutProvider.parse(Zhihu.MULTIPLE, answerId),
					values,
					null,
					null
			);
		}
	}
}
