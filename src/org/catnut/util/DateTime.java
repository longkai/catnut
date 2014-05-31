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

	public static final int SECOND_MILLIS = 1000;
	public static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
	public static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
	public static final long DAY_MILLIS = 24 * HOUR_MILLIS;
	public static final long MONTH_MILLIS = 30 * DAY_MILLIS; // 30 days as a month
	public static final long YEAR_MILLIS = 12 * MONTH_MILLIS;

	private static final int[] CONVERSIONS = {
			1,		// ms, now
			1000,	// ms, sec
			60,		// sec, min
			60,		// min, hour
			24,		// hour, day
			30,		// day, month
			12,		// month, year
	};

	private static final String[] SPANS = {
			"now",
			"s",
			"m",
			"h",
			"d",
			"mon",
			"year",
	};

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
		long now = System.currentTimeMillis();
		long delta = now - getTimeMills(string);
		delta = Math.abs(delta);
		int unitKey = 0; // ms
		for (int i = 0; i < CONVERSIONS.length; i++) {
			if (delta < CONVERSIONS[i]) {
				break;
			}
			unitKey = i;
			delta /= CONVERSIONS[i];
		}
		return delta + SPANS[unitKey];
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
