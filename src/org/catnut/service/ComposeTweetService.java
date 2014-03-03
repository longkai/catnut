/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.provider.BaseColumns;
import android.util.Log;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import org.catnut.R;
import org.catnut.api.TweetAPI;
import org.catnut.core.CatnutAPI;
import org.catnut.core.CatnutApp;
import org.catnut.core.CatnutProcessor;
import org.catnut.core.CatnutProvider;
import org.catnut.core.CatnutRequest;
import org.catnut.metadata.Draft;
import org.catnut.metadata.Status;
import org.catnut.metadata.User;
import org.catnut.metadata.WeiboAPIError;
import org.catnut.support.HttpClient;
import org.catnut.ui.SingleFragmentActivity;
import org.catnut.ui.TweetActivity;
import org.catnut.util.CatnutUtils;
import org.catnut.util.Constants;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 上传照片&微博
 *
 * @author longkai
 */
public class ComposeTweetService extends IntentService {

	public static final String UPLOAD = CatnutAPI.API_DOMAIN + "/2/statuses/upload.json";

	public static final String TAG = "ComposeTweetService";

	private CatnutApp mApp;
	private NotificationManager mNotifyManager;
	private Notification.Builder mBuilder;

	private SendWeiboProcessor mSendWeiboProcessor;

	private final int ID = (int) (System.currentTimeMillis() / 10000);

	public ComposeTweetService() {
		super(TAG);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mApp = CatnutApp.getTingtingApp();
		mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mBuilder = new Notification.Builder(this)
				.setContentTitle(getString(R.string.notify_send_tweet))
				.setContentText(getString(R.string.notify_send_tweet_text))
				.setSmallIcon(R.drawable.ic_launcher);
	}

	// 发送文字
	private void update(final Draft draft) {
		mApp.getRequestQueue().add(new CatnutRequest(
				this,
				TweetAPI.update(draft.status, draft.visible, draft.list_id, draft.lat, draft._long, null, draft.rip),
				mSendWeiboProcessor,
				null,
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						WeiboAPIError weiboAPIError = WeiboAPIError.fromVolleyError(error);
						fallback(draft, weiboAPIError.error);
					}
				}
		)).setTag(TAG);
	}

	// 发送图片&文字
	private void upload(Draft draft) {
		HttpClient client = new HttpClient(UPLOAD);
		InputStream inputStream = null;
		try {
			client.connectForMultipart(mApp.getAuthHeaders());
			client.addFormPart(Draft.STATUS, draft.status);
			client.addFormPart(Draft.LAT, String.valueOf(CatnutUtils.scaleNumber(draft.lat, 1)));
			client.addFormPart(Draft._LONG, String.valueOf(CatnutUtils.scaleNumber(draft._long, 1)));
			// todo: 其它的参数，有需要的时候再加了
			inputStream = getContentResolver().openInputStream(draft.pic);

			// 显示发送进度条
			int total = inputStream.available();
			int current = 0;

			ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
			int bufferSize = 2048; // 2k
			byte[] buffer = new byte[bufferSize];
			int len;
			while ((len = inputStream.read(buffer)) != -1) {
				byteBuffer.write(buffer, 0, len);
				current += len;
				float progress = (current * 1.f / total) * 100;
				mBuilder.setProgress(100, (int) progress, false);
				mNotifyManager.notify(ID, mBuilder.build());
			}
			client.addFilePart(Draft.PIC, draft.pic.getLastPathSegment(), byteBuffer.toByteArray());
			client.finishMultipart();

			mBuilder.setContentText(getString(R.string.notify_waiting_result));
			mNotifyManager.notify(ID, mBuilder.build());

			// 获取结果
			HttpClient.UploadResponse response = client.getResponse();
			Log.d(TAG, response.statusCode + " " + response.response);
			// 解析结果
			parseNetworkResponse(response, draft);
		} catch (Exception e) {
			Log.e(TAG, "upload error!", e);
			fallback(draft, e.getLocalizedMessage());
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					Log.e(TAG, "i/o error!", e);
				}
			}
		}
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Draft weibo = intent.getExtras().getParcelable(Draft.DRAFT);
		mSendWeiboProcessor = new SendWeiboProcessor(weibo);
		if (weibo.pic == null) {
			update(weibo);
		} else {
			upload(weibo);
		}
	}

	/**
	 * 解析结果
	 *
	 * @param response
	 * @throws JSONException
	 */
	private void parseNetworkResponse(HttpClient.UploadResponse response, Draft draft) throws Exception {
		if (response.statusCode == 200) {
			// 更新本地数据
			JSONObject json = new JSONObject(response.response);
			mSendWeiboProcessor.asyncProcess(this, json);
		} else {
			JSONObject error = new JSONObject(response.response);
			fallback(draft, error.optString(WeiboAPIError.ERROR));
		}
	}

	// 上传失败，那么就把草稿保存在本地
	private void fallback(Draft draft, String error) {
		draft.createAt = System.currentTimeMillis();
		getContentResolver().insert(CatnutProvider.parse(Draft.MULTIPLE), Draft.METADATA.convert(draft));
		mBuilder.setContentTitle(getString(R.string.post_fail))
				.setContentText(error)
				.setTicker(getText(R.string.post_fail))
				.setProgress(0, 0, false);

		// 添加fallback跳转
		Intent resultIntent = SingleFragmentActivity.getIntent(this, SingleFragmentActivity.DRAFT);

		TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(this);
		taskStackBuilder.addParentStack(SingleFragmentActivity.class);
		taskStackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent =
				taskStackBuilder.getPendingIntent(
						ID,
						PendingIntent.FLAG_UPDATE_CURRENT
				);
		mBuilder.setContentIntent(resultPendingIntent)
				.setAutoCancel(true);
		mNotifyManager.notify(ID, mBuilder.build());
	}

	// 解析数据并持久化
	private class SendWeiboProcessor implements CatnutProcessor<JSONObject> {

		private Draft draft;

		private SendWeiboProcessor(Draft draft) {
			this.draft = draft;
		}

		@Override
		public void asyncProcess(Context context, JSONObject json) throws Exception {
			// 本地持久化
			ContentValues tweet = Status.METADATA.convert(json);
			tweet.put(Status.TYPE, Status.HOME); // 标记一下
			getContentResolver().insert(CatnutProvider.parse(Status.MULTIPLE), tweet);
			String update = CatnutUtils.increment(true, User.TABLE, User.statuses_count, mApp.getAccessToken().uid);
			getContentResolver().update(CatnutProvider.parse(User.MULTIPLE), null, update, null);
			// 更新status bar
			mBuilder.setContentText(getText(R.string.post_success))
					.setTicker(getText(R.string.post_success))
					.setProgress(0, 0, false);

			// 设置点击后的跳转
			Intent resultIntent = new Intent(ComposeTweetService.this, TweetActivity.class);
			resultIntent.putExtra(Constants.ID, json.optLong(Constants.ID));

			TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(ComposeTweetService.this);
			taskStackBuilder.addParentStack(TweetActivity.class);
			taskStackBuilder.addNextIntent(resultIntent);
			PendingIntent resultPendingIntent =
					taskStackBuilder.getPendingIntent(
							ID,
							PendingIntent.FLAG_UPDATE_CURRENT
					);
			// 判断一下是否是已经保存的草稿
			if (draft.id != Integer.MIN_VALUE) {
				getContentResolver().delete(CatnutProvider.parse(Draft.MULTIPLE), BaseColumns._ID + "=" + draft.id, null);
			}
			mBuilder.setContentIntent(resultPendingIntent);
			mBuilder.setAutoCancel(true);
			mNotifyManager.notify(ID, mBuilder.build());
		}
	};
}
