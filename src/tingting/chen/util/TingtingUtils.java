/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.util;

import android.database.Cursor;

/**
 * 工具类
 *
 * @author longkai
 */
public class TingtingUtils {

	/**
	 * cursor默认没有得到boolean类型的值，所以才有了这个方法
	 * @param cursor
	 * @param columnName
	 * @return x == 1 ? true : false
	 */
	public static boolean getBoolean(Cursor cursor, String columnName) {
		return cursor.getInt(cursor.getColumnIndex(columnName)) == 1 ? true : false;
	}
}
