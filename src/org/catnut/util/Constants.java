/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.util;

/**
 * 常量池
 *
 * @author longkai
 * @date 2014-01-18
 */
public class Constants {

	public static final String WEIBO_DOMAIN = "weibo.com/";
	
	public static final String ID = "id";
	public static final String PIC = "pic";
	public static final String IMAGE_DIR = "images/"; // 保持在用户外部存储上的，并非cache的
	public static final String FANTASY_DIR = "fantasy";
	public static final String NULL = "null";
	public static final String JPG = ".jpg";
	public static final String JSON = "json";

	public static final String SHARE_IMAGE = "share.png";

	// for error dialog box
	public static final String TITLE = "title";
	public static final String MESSAGE = "message";

	public static final String[] COUNT_PROJECTION = new String[]{
			"count(0)"
	};
}
