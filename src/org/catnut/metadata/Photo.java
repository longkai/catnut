/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.metadata;

import android.content.ContentValues;
import android.provider.BaseColumns;
import android.util.Log;
import org.catnut.core.CatnutMetadata;
import org.catnut.util.Constants;
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

	public static final String name = "name";
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
				.append(description).append(" text,")
				.append(image_url).append(" int")
				.append(")");
		return ddl.toString();
	}

	@Override
	public ContentValues convert(JSONObject data) {
		ContentValues photo = new ContentValues();
		photo.put(BaseColumns._ID, data.optLong(Constants.ID));
		photo.put(name, data.optString(name));
		photo.put(description, data.optString(description));
		photo.put(image_url, data.optString(image_url));
		return photo;
	}
}
