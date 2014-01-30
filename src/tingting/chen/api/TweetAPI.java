/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.api;

import com.android.volley.Request;
import tingting.chen.tingting.TingtingAPI;
import tingting.chen.util.TingtingUtils;

/**
 * 微博API
 */
public class TweetAPI {

	/**
	 * 获取某个用户最新发表的微博列表
	 *
	 * @param uid       需要查询的用户ID
	 * @param since_id  若指定此参数，则返回ID比since_id大的微博（即比since_id时间晚的微博），默认为0
	 * @param max_id    若指定此参数，则返回ID小于或等于max_id的微博，默认为0
	 * @param count     单页返回的记录条数，最大不超过100，超过100以100处理，默认为20
	 * @param page      返回结果的页码，默认为1
	 * @param base_app  是否只获取当前应用的数据。0为否（所有数据），1为是（仅当前应用），默认为0
	 * @param feature   过滤类型ID，0：全部、1：原创、2：图片、3：视频、4：音乐，默认为0
	 * @param trim_user 返回值中user字段开关，0：返回完整user字段、1：user字段仅返回user_id，默认为0
	 * @return 某个用户最新发表的微博列表
	 */
	public static TingtingAPI myTimeline(long uid, long since_id, long max_id, int count, int page, int base_app, int feature, int trim_user) {
		StringBuilder uri = new StringBuilder(TingtingAPI.API_DOMAIN);
		uri.append("/2/statuses/user_timeline.json")
			.append("?uid=").append(uid)
			.append("&since_id=").append(TingtingUtils.optValue(since_id, 0))
			.append("&max_id=").append(TingtingUtils.optValue(max_id, 0))
			.append("&count=").append(TingtingUtils.optValue(count, 20))
			.append("&page=").append(TingtingUtils.optValue(page, 1))
			.append("&base_app=").append(TingtingUtils.optValue(base_app, 0))
			.append("&feature=").append(TingtingUtils.optValue(feature, 0))
			.append("&trim_user=").append(TingtingUtils.optValue(trim_user, 0));
		return new TingtingAPI(Request.Method.GET, uri.toString(), true, null);
	}
}
