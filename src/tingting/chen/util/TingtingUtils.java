/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.util;

import android.database.Cursor;
import android.text.TextUtils;

/**
 * 工具类
 *
 * @author longkai
 */
public class TingtingUtils {

	/**
	 * cursor默认没有得到boolean类型的值，所以才有了这个方法
	 *
	 * @param cursor
	 * @param columnName
	 * @return x == 1 ? true : false
	 */
	public static boolean getBoolean(Cursor cursor, String columnName) {
		return cursor.getInt(cursor.getColumnIndex(columnName)) == 1 ? true : false;
	}

	/**
	 * 判断实际的int是否为0，否则返回默认的int
	 *
	 * @param real
	 * @param defaultValue
	 * @return int
	 */
	public static int optValue(int real, int defaultValue) {
		return real == 0 ? defaultValue : real;
	}

	/**
	 * 判断实际的long是否为0，否则返回默认的long
	 *
	 * @param real
	 * @param defaultValue
	 * @return long
	 */
	public static long optValue(long real, long defaultValue) {
		return real == 0L ? defaultValue : real;
	}

	/**
	 * 判断实际的String是否为null或者空串，否则返回默认的字符串
	 *
	 * @param real
	 * @param defaultValue
	 * @return String
	 */
	public static String optValue(String real, String defaultValue) {
		return TextUtils.isEmpty(real) ? defaultValue : real;
	}

	/**
	 * 判断实际的float是否为0.0f，否则返回默认的float
	 *
	 * @param real
	 * @param defaultValue
	 * @return float
	 */
	public static float optValue(float real, float defaultValue) {
		return real == 0.0F ? defaultValue : real;
	}

	/**
	 * 判断实际的double是否为0.0D，否则返回默认的double
	 *
	 * @param real
	 * @param defaultValue
	 * @return double
	 */
	public static double optValue(double real, double defaultValue) {
		return real == 0.0D ? defaultValue : real;
	}

	/**
	 * 判断实际的boolean是否为false，否则返回默认的boolean
	 *
	 * @param real
	 * @param defaultValue
	 * @return boolean
	 */
	public static boolean optValue(boolean real, boolean defaultValue) {
		return !real ? defaultValue : real;
	}

	/**
	 * 模仿android的凑sql的方法，不同在于支持join和on，用在raw-query
	 * @param projection
	 * @param selection
	 * @param from
	 * @param join
	 * @param on
	 * @param sort
	 * @param limit
	 * @return query sql
	 */
	public static String buildQuery(String[] projection, String selection, String from, String join, String on, String sort, String limit) {
		StringBuilder query = new StringBuilder("SELECT");
		if (projection == null) {
			query.append(" * ");
		} else {
			query.append(" ").append(projection(projection));
		}

		query.append(" FROM ").append(from);

		if (join != null) {
			query.append(" JOIN ").append(join);
			if (on != null) {
				query.append(" ON ").append(on);
			}
		}

		if (selection != null) {
			query.append(" WHERE ").append(selection);
		}

		if (sort != null) {
			query.append(" ORDER BY ").append(sort);
		}

		if (limit != null) {
			query.append(" LIMIT ").append(limit);
		}

		return query.toString();
	}

	/**
	 * 拼接投影sql
	 * @param projection
	 * @return turn a string[] into xx,xxx,xxx,...,xx
	 */
	private static String projection(String[] projection) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < projection.length; i++) {
			sb.append(projection[i]);
			if (i != projection.length - 1) {
				sb.append(",");
			}
		}
		return sb.toString();
	}
}
