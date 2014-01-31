/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.tingting;

import android.content.Context;

/**
 * 对从http获得的数据做进一步处理（比如持久化到本地），此方法应实现在后台线程中！
 *
 * @author longkai
 */
public interface TingtingProcessor<X> {

	/**
	 * 对从http获得的数据做进一步处理（比如持久化到本地），此方法应实现在后台线程中！
	 *
	 * @param context {@link android.content.Context}
	 * @param data    http请求成功后得到的数据
	 * @throws java.lang.Exception 处理过程中出现了问题并且本处理器无法处理，则抛出交给{@link com.android.volley.Response.ErrorListener}进行异常处理
	 */
	void asyncProcess(Context context, X data) throws Exception;
}
