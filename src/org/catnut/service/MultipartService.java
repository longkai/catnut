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
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
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
import org.catnut.metadata.Status;
import org.catnut.metadata.User;
import org.catnut.metadata.WeiboAPIError;
import org.catnut.support.HttpClient;
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
public class MultipartService extends IntentService {

	public static final String UPLOAD = CatnutAPI.API_DOMAIN + "/2/statuses/upload.json";
	public static final String UPDATE = CatnutAPI.API_DOMAIN + "/2/statuses/update.json";

	public static final String TAG = "MultipartService";

	private CatnutApp mApp;
	private NotificationManager mNotifyManager;
	private Notification.Builder mBuilder;

	private static final int ID = 1;

	public MultipartService() {
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
	private void update(Weibo weibo) {
		mApp.getRequestQueue().add(new CatnutRequest(
				this,
				TweetAPI.update(weibo.status, weibo.visible, weibo.list_id, weibo.lat, weibo._long, null, weibo.rip),
				sendWeiboProcessor,
				null,
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						WeiboAPIError weiboAPIError = WeiboAPIError.fromVolleyError(error);
						mBuilder.setContentTitle(getString(R.string.post_fail))
								.setContentText(weiboAPIError.error)
								.setProgress(0, 0, false);
						mNotifyManager.notify(ID, mBuilder.build());
					}
				}
		)).setTag(TAG);
	}

	// 发送图片&文字
	private void upload(Weibo weibo) {
		HttpClient client = new HttpClient(weibo.pic == null ? UPDATE : UPLOAD);
		InputStream inputStream = null;
		try {
			client.connectForMultipart();
			client.addFormPart("status", weibo.status);
			client.addFormPart("lat", String.valueOf(CatnutUtils.scaleNumber(weibo.lat, 1)));
			client.addFormPart("long", String.valueOf(CatnutUtils.scaleNumber(weibo._long, 1)));
			// todo: 其它的参数，有需要的时候再加了
			inputStream = getContentResolver().openInputStream(weibo.pic);

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
			mBuilder.setContentText(getString(R.string.notify_waiting_result));
			mNotifyManager.notify(ID, mBuilder.build());

			client.addFilePart("pic", weibo.pic.getLastPathSegment(), byteBuffer.toByteArray());
			client.finishMultipart();
			// 获取结果
			HttpClient.UploadResponse response = client.getResponse();
			// 解析结果
			parseNetworkResponse(response);
		} catch (Exception e) {
			Log.e(TAG, "upload error!", e);
			mBuilder.setContentTitle(getString(R.string.post_fail))
					.setContentText(e.getLocalizedMessage())
					.setProgress(0, 0, false);
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
		Weibo weibo = intent.getExtras().getParcelable(Weibo.WEIBO);
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
	private void parseNetworkResponse(HttpClient.UploadResponse response) throws Exception {
		if (response.statusCode == 200) {
			// 更新本地数据
			JSONObject json = new JSONObject(response.response);
			sendWeiboProcessor.asyncProcess(this, json);
		} else {
			mBuilder.setContentTitle(getString(R.string.post_fail))
					.setContentText(response.response)
					.setProgress(0, 0, false);
			mNotifyManager.notify(ID, mBuilder.build());
		}
	}

	// 解析数据并持久化
	private CatnutProcessor<JSONObject> sendWeiboProcessor = new CatnutProcessor<JSONObject>() {
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
					.setProgress(0, 0, false);
			Intent resultIntent = new Intent(context, TweetActivity.class);
			resultIntent.putExtra(Constants.ID, json.optLong(Constants.ID));
			TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(context);
			taskStackBuilder.addParentStack(TweetActivity.class);
			taskStackBuilder.addNextIntent(resultIntent);
			PendingIntent resultPendingIntent =
					taskStackBuilder.getPendingIntent(
							0,
							PendingIntent.FLAG_UPDATE_CURRENT
					);
			mBuilder.setContentIntent(resultPendingIntent);
			mBuilder.setAutoCancel(true);
			mNotifyManager.notify(ID, mBuilder.build());
		}
	};


	/**
	 * 待发送的微博
	 *
	 * @author longkai
	 */
	public static class Weibo implements Parcelable {

		public static final String WEIBO = "weibo";

		public String status;
		public int visible = 0;
		public String list_id;
		public float lat = 0.f;
		public float _long = 0.f;
		public String annotations;
		public String rip;
		public Uri pic;

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(status);
			dest.writeInt(visible);
			dest.writeString(list_id);
			dest.writeFloat(lat);
			dest.writeFloat(_long);
			dest.writeString(annotations);
			dest.writeString(rip);
			dest.writeParcelable(pic, flags);
		}

		public static final Creator<Weibo> CREATOR = new Creator<Weibo>() {
			@Override
			public Weibo createFromParcel(Parcel source) {
				Weibo weibo = new Weibo();
				weibo.status = source.readString();
				weibo.visible = source.readInt();
				weibo.list_id = source.readString();
				weibo.lat = source.readFloat();
				weibo._long = source.readFloat();
				weibo.annotations = source.readString();
				weibo.rip = source.readString();
				weibo.pic = source.readParcelable(Thread.currentThread().getContextClassLoader());
				return weibo;
			}

			@Override
			public Weibo[] newArray(int size) {
				return new Weibo[size];
			}
		};
	}

}
