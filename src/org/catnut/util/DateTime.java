/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.util;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * 处理微博时间转换
 *
 * @author longkai
 */
public class DateTime {

	private static final String TAG = "DateTime";

	public static final int SECOND_MILLIS = 1000;
	public static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
	public static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
	public static final long DAY_MILLIS = 24 * HOUR_MILLIS;
	public static final long MONTH_MILLIS = 30 * DAY_MILLIS; // 30 days as a month
	public static final long YEAR_MILLIS = 12 * MONTH_MILLIS;

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

	/**
	 * simplicity time
	 */
	public static String getRelativeTimeString(String string) {
		long mills = getTimeMills(string);
		long offset = System.currentTimeMillis() - mills;
		if (offset < MINUTE_MILLIS) { // 1 min
			return (offset / SECOND_MILLIS) + " s";
		} else if (offset < HOUR_MILLIS) { // 1 hour
			return (offset / MINUTE_MILLIS) + " m";
		} else if (offset < DAY_MILLIS) { // 1 day
			return (offset / HOUR_MILLIS) + " h";
		} else if (offset < 7 * DAY_MILLIS) { // within 1 week
			return (offset / DAY_MILLIS) + " d";
		} else {
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(mills);
			if (offset < YEAR_MILLIS) { // within 1 year
				return (c.get(Calendar.MONTH) + 1) + "-" + c.get(Calendar.DAY_OF_MONTH);
			} else {
				return String.valueOf(c.get(Calendar.YEAR));
			}
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
