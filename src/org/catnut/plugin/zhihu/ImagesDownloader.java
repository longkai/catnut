/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.plugin.zhihu;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import org.catnut.R;
import org.catnut.util.CatnutUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

/**
 * 文件下载器
 *
 * @author longkai
 */
public class ImagesDownloader extends IntentService {

	public static final String TAG = ImagesDownloader.class.getSimpleName();

	public static final String URLS = "urls";
	public static final String LOCATION = "location";

	private Notification.Builder mBuilder;
	private NotificationManager mNotificationManager;
	private static final int ID = 1;

	public ImagesDownloader() {
		super(TAG);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mBuilder = new Notification.Builder(this)
				.setSmallIcon(R.drawable.ic_launcher);
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		ArrayList<String> urls = intent.getStringArrayListExtra(URLS);
		String location = intent.getStringExtra(LOCATION);
		if (urls != null) {
			File file;
			InputStream in = null;
			byte[] buffer = new byte[1024];
			int tmp;
			String url;
			FileOutputStream outputStream = null;
			mBuilder.setTicker(getString(R.string.downloading_img));
			mBuilder.setContentTitle(getString(R.string.downloading_img));
			mNotificationManager.notify(ID, mBuilder.build());
			for (int i = 0; i < urls.size(); i++) {
				url = urls.get(i);
				file = new File(location + File.separator + Uri.parse(url).getLastPathSegment());
				if (!file.exists()) {
					try {
						URL u = new URL(url);
						in = u.openStream();
						outputStream = new FileOutputStream(file);
						while ((tmp = in.read(buffer)) != -1) {
							outputStream.write(buffer, 0, tmp);
						}
					} catch (Exception e) {
					} finally {
						CatnutUtils.closeIO(outputStream, in);
					}
				}
				mBuilder.setProgress(100, (int) ((i + 1) * 1.f / urls.size() * 100), false);
				mNotificationManager.notify(ID, mBuilder.build());
			}
			mBuilder.setProgress(0, 0, false);
			mNotificationManager.notify(ID, mBuilder.build());

			mBuilder.setContentText(getString(R.string.img_download_done))
					.setContentText(getString(R.string.total_cache_img, urls.size()))
					.setAutoCancel(true);
			mNotificationManager.notify(ID, mBuilder.build());
		}
	}
}
