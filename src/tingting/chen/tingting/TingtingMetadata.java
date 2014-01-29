/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.tingting;

/**
 * 元数据的接口（作为一个待持久化到sqlite的元数据类型必须实现此接口）。
 *
 * @author longkai
 */
public interface TingtingMetadata<From, To> {

	/**
	 * 生成元数据的数据库模式（数据表语句）
	 *
	 * @return sqlite数据建表语句
	 */
	String ddl();

	/**
	 * 将元数据从一种类型（如{@link org.json.JSONObject}）转化为另一种类型（如{@link android.content.ContentValues}）
	 * <p/>
	 * 本方法应当在后台线程中调用！
	 *
	 * @param data 原来的数据
	 * @return 新的数据类型
	 */
	To convert(From data);
}
