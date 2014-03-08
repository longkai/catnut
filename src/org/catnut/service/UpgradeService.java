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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import org.catnut.R;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

/**
 * 升级服务
 *
 * @author longkai
 */
public class UpgradeService extends IntentService {

	public static final String TAG = "UpgradeService";
	public static final String ACTION_DOWNLOAD = "download";
	public static final String ACTION_DISMISS = "dismiss";

	private static final String METADATA_URL = "https://dl.dropboxusercontent.com/u/96034496/apps/catnut.json";

	private static final String FIELD_VERSION_CODE = "version_code";
	private static final String DOWNLOAD_LINK = "link";

	private static final int ID = 7;

	private Notification.Builder mBuilder;
	private NotificationManager mNotificationManager;

	public UpgradeService() {
		super(TAG);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mBuilder = new Notification.Builder(this)
				.setSmallIcon(R.drawable.ic_launcher)
				.setPriority(Notification.PRIORITY_HIGH)
				.setAutoCancel(true);
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		String action = intent.getAction();
		try {
			if (TextUtils.isEmpty(action)) {
				checkout();
			} else if (action.equals(ACTION_DOWNLOAD)) {
				download(intent);
			} else if (action.equals(ACTION_DISMISS)) {
				mNotificationManager.cancelAll();
			}
		} catch (final Exception e) {
			Log.e(TAG, "fail upgrade", e);
			new Handler(Looper.getMainLooper()).post(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(UpgradeService.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
				}
			});
		}
	}

	// 下载apk并提示用户更新
	private void download(Intent intent) throws IOException {
		String link = intent.getExtras().getString(DOWNLOAD_LINK);
		URL url = new URL(link);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		File apk = new File(getExternalCacheDir().getPath() + "/"
				+ Uri.parse(link).getLastPathSegment());
		FileOutputStream outputStream = new FileOutputStream(apk);
		InputStream inputStream = new BufferedInputStream(connection.getInputStream());

		connection.connect();
		int length = connection.getContentLength();

		byte[] buffer = new byte[1024];
		int tmp;
		int count = 0;
		mBuilder.setContentTitle(getString(R.string.download_apk));
		mBuilder.setContentText(getString(R.string.downloading));
		while ((tmp = inputStream.read(buffer)) != -1) {
			count += tmp;
			outputStream.write(buffer, 0, tmp);
			mBuilder.setProgress(100, (int) ((count * 1.f / length) * 100), true);
			mNotificationManager.notify(ID, mBuilder.build());
		}
		inputStream.close();
		outputStream.close();
		connection.disconnect();

		Intent install = new Intent(Intent.ACTION_VIEW);
		install.setDataAndType(Uri.fromFile(apk), "application/vnd.android.package-archive");
		PendingIntent piInstall = PendingIntent.getActivity(this, 0, install, 0);
		mBuilder.setProgress(0, 0, false);
		mBuilder.setContentIntent(piInstall);

		mBuilder.setTicker(getString(R.string.done_download))
				.setContentTitle(getString(R.string.done_download))
				.setContentText(getString(R.string.click_to_upgrade));
		mNotificationManager.notify(ID, mBuilder.setDefaults(Notification.DEFAULT_ALL).build());
	}

	// 检查新版本
	private void checkout() throws Exception {
		URL url = new URL(METADATA_URL);
		InputStream inputStream = url.openStream();
		Scanner in = new Scanner(inputStream).useDelimiter("\\A");
		if (in.hasNext()) {
			JSONObject metadata = new JSONObject(in.next());
			PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
			if (info.versionCode < metadata.optInt(FIELD_VERSION_CODE)) {
				Notification.InboxStyle style = new Notification.InboxStyle(mBuilder);
				String size = metadata.optString("size");
				style.setBigContentTitle(getString(R.string.find_new_version, size));
				JSONArray messages = metadata.optJSONArray("messages");
				for (int i = 0; i < messages.length(); i++) {
					style.addLine(messages.optString(i));
				}
				// download&upgrade intent
				Intent download = new Intent(this, UpgradeService.class);
				download.setAction(ACTION_DOWNLOAD);
				download.putExtra(DOWNLOAD_LINK, metadata.optString(DOWNLOAD_LINK));
				PendingIntent piDownload = PendingIntent.getService(this, 0, download, 0);
				mBuilder.addAction(R.drawable.ic_stat_download_dark, getString(R.string.down_load_and_upgrade), piDownload);
				// dismiss notification
				Intent dismiss = new Intent(this, UpgradeService.class);
				dismiss.setAction(ACTION_DISMISS);
				PendingIntent piDismiss = PendingIntent.getService(this, 0, dismiss, 0);
				mBuilder.addAction(R.drawable.ic_stat_content_remove_dark, getString(R.string.not_upgrade_now), piDismiss);
				// show it.
				mBuilder.setTicker(getString(R.string.find_new_version));
				mNotificationManager.notify(ID, mBuilder.setDefaults(Notification.DEFAULT_ALL).build());
			} else {
				new Handler(Looper.getMainLooper()).post(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(UpgradeService.this, getString(R.string.already_updated), Toast.LENGTH_SHORT).show();
					}
				});
			}
		}
		in.close();
	}
}
