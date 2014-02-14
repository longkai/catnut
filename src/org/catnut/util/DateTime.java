/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.util;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 处理微博时间转换
 *
 * @author longkai
 */
public class DateTime {

	private static final String TAG = "DateTime";

	/**
	 * 将微博文本转换为时间
	 *
	 * @return 若失败，返回null
	 */
	public static Date parse(String string) {
		try {
			return sSafeDateFormat.get().parse(string);
		} catch (ParseException e) {
			Log.e(TAG, "parsing string [" + string + "] to date error!", e);
			return null;
		}
	}

	/**
	 * 将文本转换为时间
	 *
	 * @return 若失败，返回0
	 */
	public static long getTimeMills(String string) {
		try {
			return sSafeDateFormat.get().parse(string).getTime();
		} catch (ParseException e) {
			Log.e(TAG, "parsing string [" + string + "] to millis error!", e);
			return 0L;
		}
	}

	private static ThreadLocal<SimpleDateFormat> sSafeDateFormat = new ThreadSafeDateFormat();

	public static class ThreadSafeDateFormat extends ThreadLocal<SimpleDateFormat> {

		@Override
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("EEE MMM d HH:mm:ss Z yyyy", Locale.ENGLISH);
		}

		@Override
		public SimpleDateFormat get() {
			return super.get();
		}

		@Override
		public void set(SimpleDateFormat value) {
			super.set(value);
		}

		@Override
		public void remove() {
			super.remove();
		}
	}
}
