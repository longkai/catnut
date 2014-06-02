/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.util;

import android.provider.BaseColumns;

/**
 * 常量池
 *
 * @author longkai
 * @date 2014-01-18
 */
public class Constants {

	public static final String WEIBO_DOMAIN = "weibo.com/";

	public static final String ACTION = "action";
	public static final String ID = "id";
	public static final String PIC = "pic";
	public static final String THUMBNAIL = "thumbnail";
	public static final String IMAGE_DIR = "images/"; // 保持在用户外部存储上的，并非cache的
	public static final String FANTASY_DIR = "fantasy";
	public static final String NULL = "null";
	public static final String JPG = ".jpg";
	public static final String JSON = "json";
	public static final String KEYWORDS = "keywords";

	public static final float GOLDEN_RATIO = 0.618f;

	public static final String SHARE_IMAGE = "share.png";

	// default sort order, must have `_id` field!
	public static final String DEFAULT_ORDER = BaseColumns._ID + " desc";
	public static final String RANDOM_ORDER = "RANDOM()";

	public static final String[] COUNT_PROJECTION = new String[]{
			"count(0)"
	};
}
