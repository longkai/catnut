/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.plugin.fantasy;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.BaseColumns;
import android.util.Log;
import org.catnut.R;
import org.catnut.core.CatnutApp;
import org.catnut.core.CatnutMetadata;
import org.catnut.core.CatnutProcessor;
import org.catnut.core.CatnutProvider;
import org.catnut.util.Constants;
import org.catnut.util.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * fantasy
 *
 * @author longkai
 */
public class Photo implements CatnutMetadata<JSONObject, ContentValues> {

	public static final String TAG = "Photo";
	public static final String TABLE = "photos";
	public static final String SINGLE = "photo";
	public static final String MULTIPLE = "photos";

	public static final String LAST_FANTASY_MILLIS = "last_fantasy_millis";

	public static final String FEATURE_POPULAR = "popular";
	public static final String FEATURE_EDITORS = "editors";
	public static final String FEATURE_UPCOMING = "upcoming";
	public static final String FEATURE_FRESH_TODAY = "fresh_today";
	public static final String FEATURE_FRESH_YESTERDAY = "fresh_yesterday";
	public static final String FEATURE_FRESH_WEEK = "fresh_week";

	public static final String feature = "feature";
	public static final String name = "name";
	public static final String width = "width";
	public static final String height = "height";
	public static final String description = "description";
	public static final String image_url = "image_url";

	public static final Photo METADATA = new Photo();

	private Photo(){}

	@Override
	public String ddl() {
		Log.i(TAG, "create table [photos]...");
		StringBuilder ddl = new StringBuilder("CREATE TABLE ");
		ddl.append(TABLE).append("(")
				.append(BaseColumns._ID).append(" int primary key,")
				.append(name).append(" text,")
				.append(feature).append(" text,")
				.append(description).append(" text,")
				.append(image_url).append(" int,")
				.append(width).append(" int, ")
				.append(height).append(" int")
				.append(")");
		return ddl.toString();
	}

	public static boolean shouldRefresh() {
		CatnutApp app = CatnutApp.getTingtingApp();
		SharedPreferences pref = app.getPreferences();

		return pref.getBoolean(
				app.getString(R.string.pref_enable_fantasy),
				app.getResources().getBoolean(R.bool.default_plugin_status)
		) && System.currentTimeMillis() - pref.getLong(LAST_FANTASY_MILLIS, 0) > DateTime.DAY_MILLIS;
	}

	@Override
	public ContentValues convert(JSONObject data) {
		ContentValues photo = new ContentValues();
		photo.put(BaseColumns._ID, data.optLong(Constants.ID));
		photo.put(name, data.optString(name));
		photo.put(description, data.optString(description));
		photo.put(image_url, data.optString(image_url));
		photo.put(width, data.optInt(width));
		photo.put(height, data.optInt(height));
		return photo;
	}

	/**
	 * 缓存500px照片元数据
	 *
	 * @author longkai
	 */
	public static class _500pxProcessor implements CatnutProcessor<JSONObject> {

		private String feature;

		public _500pxProcessor(String feature) {
			this.feature = feature;
		}

		public _500pxProcessor() {
			this.feature = FEATURE_POPULAR;
		}

		@Override
		public void asyncProcess(Context context, JSONObject data) throws Exception {
			Log.d(TAG, "load 500px done...");
			JSONArray array = data.optJSONArray(Photo.MULTIPLE);
			ContentValues[] photos = new ContentValues[array.length()];
			for (int i = 0; i < array.length(); i++) {
				photos[i] = Photo.METADATA.convert(array.optJSONObject(i));
				photos[i].put(Photo.feature, feature);
			}
			context.getContentResolver().bulkInsert(CatnutProvider.parse(Photo.MULTIPLE), photos);
			// 记录上次更新的日期
			CatnutApp.getTingtingApp().getPreferences()
					.edit()
					.putLong(LAST_FANTASY_MILLIS, System.currentTimeMillis())
					.commit();
		}
	}
}
