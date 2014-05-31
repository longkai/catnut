/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.api;

import android.net.Uri;
import com.android.volley.Request;
import org.catnut.core.CatnutAPI;
import org.catnut.support.MultipartAPI;
import org.catnut.util.CatnutUtils;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 微博API
 */
public class TweetAPI {

	private static final String BASE_URI = CatnutAPI.API_DOMAIN + "/2/statuses/";

	/**
	 * 返回最新的200条公共微博，返回结果非完全实时
	 *
	 * @param count 单页返回的记录条数，最大不超过200，默认为20
	 * @return api
	 */
	public static CatnutAPI public_timeline(int count) {
		StringBuilder uri = new StringBuilder(BASE_URI);
		uri.append("public_timeline.json?count=").append(CatnutUtils.optValue(count, 20));
		return new CatnutAPI(Request.Method.GET, uri, true, null);
	}

	/**
	 * 获取当前登录用户及其所关注用户的最新微博
	 *
	 * @param since_id  若指定此参数，则返回ID比since_id大的微博（即比since_id时间晚的微博），默认为0
	 * @param max_id    若指定此参数，则返回ID小于或等于max_id的微博，默认为0
	 * @param count     单页返回的记录条数，最大不超过100，默认为20
	 * @param page      返回结果的页码，默认为1
	 * @param base_app  是否只获取当前应用的数据。0为否（所有数据），1为是（仅当前应用），默认为0
	 * @param feature   过滤类型ID，0：全部、1：原创、2：图片、3：视频、4：音乐，默认为0
	 * @param trim_user 返回值中user字段开关，0：返回完整user字段、1：user字段仅返回user_id，默认为0
	 * @return api
	 */
	public static CatnutAPI homeTimeline(long since_id, long max_id, int count, int page, int base_app, int feature, int trim_user) {
		StringBuilder uri = new StringBuilder(BASE_URI);
		uri.append("home_timeline.json")
				.append("?since_id=").append(CatnutUtils.optValue(since_id, 0))
				.append("&max_id=").append(CatnutUtils.optValue(max_id, 0))
				.append("&count=").append(CatnutUtils.optValue(count, 20))
				.append("&page=").append(CatnutUtils.optValue(page, 1))
				.append("&base_app=").append(CatnutUtils.optValue(base_app, 0))
				.append("&feature=").append(CatnutUtils.optValue(feature, 0))
				.append("&trim_user=").append(CatnutUtils.optValue(trim_user, 0));
		return new CatnutAPI(Request.Method.GET, uri, true, null);
	}

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
	 * @return api
	 */
	public static CatnutAPI userTimeline(long uid, long since_id, long max_id, int count, int page, int base_app, int feature, int trim_user) {
		StringBuilder uri = new StringBuilder(BASE_URI);
		uri.append("user_timeline.json")
				.append("?uid=").append(uid)
				.append("&since_id=").append(CatnutUtils.optValue(since_id, 0))
				.append("&max_id=").append(CatnutUtils.optValue(max_id, 0))
				.append("&count=").append(CatnutUtils.optValue(count, 20))
				.append("&page=").append(CatnutUtils.optValue(page, 1))
				.append("&base_app=").append(CatnutUtils.optValue(base_app, 0))
				.append("&feature=").append(CatnutUtils.optValue(feature, 0))
				.append("&trim_user=").append(CatnutUtils.optValue(trim_user, 0));
		return new CatnutAPI(
				Request.Method.GET,
				uri,
				true,
				null
		);
	}

	/**
	 * 获取某个用户最新发表的微博列表
	 *
	 * @param screen_name 需要查询的用户昵称
	 * @param since_id    若指定此参数，则返回ID比since_id大的微博（即比since_id时间晚的微博），默认为0
	 * @param max_id      若指定此参数，则返回ID小于或等于max_id的微博，默认为0
	 * @param count       单页返回的记录条数，最大不超过100，超过100以100处理，默认为20
	 * @param page        返回结果的页码，默认为1
	 * @param base_app    是否只获取当前应用的数据。0为否（所有数据），1为是（仅当前应用），默认为0
	 * @param feature     过滤类型ID，0：全部、1：原创、2：图片、3：视频、4：音乐，默认为0
	 * @param trim_user   返回值中user字段开关，0：返回完整user字段、1：user字段仅返回user_id，默认为0
	 * @return api
	 */
	public static CatnutAPI userTimeline(String screen_name, long since_id, long max_id, int count, int page, int base_app, int feature, int trim_user) {
		StringBuilder uri = new StringBuilder(BASE_URI);
		uri.append("user_timeline.json")
				.append("?screen_name=").append(CatnutAPI.encode(screen_name))
				.append("&since_id=").append(CatnutUtils.optValue(since_id, 0))
				.append("&max_id=").append(CatnutUtils.optValue(max_id, 0))
				.append("&count=").append(CatnutUtils.optValue(count, 20))
				.append("&page=").append(CatnutUtils.optValue(page, 1))
				.append("&base_app=").append(CatnutUtils.optValue(base_app, 0))
				.append("&feature=").append(CatnutUtils.optValue(feature, 0))
				.append("&trim_user=").append(CatnutUtils.optValue(trim_user, 0));
		return new CatnutAPI(
				Request.Method.GET,
				uri,
				true,
				null
		);
	}

	/**
	 * 发布一条新微博
	 *
	 * @param status      要发布的微博文本内容，必须做URLencode，内容不超过140个汉字
	 * @param visible     微博的可见性，0：所有人能看，1：仅自己可见，2：密友可见，3：指定分组可见，默认为0
	 * @param list_id     微博的保护投递指定分组ID，只有当visible参数为3时生效且必选
	 * @param lat         纬度，有效范围：-90.0到+90.0，+表示北纬，默认为0.0
	 * @param _long       经度，有效范围：-180.0到+180.0，+表示东经，默认为0.0
	 * @param annotations 元数据，主要是为了方便第三方应用记录一些适合于自己使用的信息，每条微博可以包含一个或者多个元数据，必须以json字串的形式提交，字串长度不超过512个字符，具体内容可以自定
	 * @param rip         开发者上报的操作用户真实IP，形如：211.156.0.1
	 * @return api
	 */
	public static CatnutAPI update(String status, int visible, String list_id, float lat, float _long, JSONObject annotations, String rip) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("status", status.trim());
		params.put("visible", String.valueOf(CatnutUtils.optValue(visible, 0)));
		if (visible == 3) { // 暂时不碰这个先
			params.put("list_id", list_id);
		}
		params.put("lat", String.valueOf(CatnutUtils.scaleNumber(CatnutUtils.optValue(lat, 0.f), 1)));
		params.put("long", String.valueOf(CatnutUtils.scaleNumber(CatnutUtils.optValue(_long, 0.f), 1)));
		if (annotations != null) {
			params.put("annotations", annotations.toString());
		}
		params.put("rip", String.valueOf(rip));
		return new CatnutAPI(Request.Method.POST, BASE_URI + "update.json", true, params);
	}

	/**
	 * 转发一条微博
	 *
	 * @param id         要转发的微博ID
	 * @param status     添加的转发文本，必须做URLencode，内容不超过140个汉字，不填则默认为“转发微博”
	 * @param is_comment 是否在转发的同时发表评论，0：否、1：评论给当前微博、2：评论给原微博、3：都评论，默认为0
	 * @param rip        开发者上报的操作用户真实IP，形如：211.156.0.1
	 * @return api
	 */
	public static CatnutAPI repost(long id, String status, int is_comment, String rip) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("id", String.valueOf(id));
		params.put("status", status.trim());
		params.put("is_comment", String.valueOf(CatnutUtils.optValue(is_comment, 0)));
		params.put("rip", String.valueOf(rip));
		return new CatnutAPI(Request.Method.POST, BASE_URI + "repost.json", true, params);
	}


	/**
	 * 上传图片并发布一条新微博
	 *
	 * @param status      要发布的微博文本内容，必须做URLencode，内容不超过140个汉字
	 * @param visible     微博的可见性，0：所有人能看，1：仅自己可见，2：密友可见，3：指定分组可见，默认为0
	 * @param list_id     微博的保护投递指定分组ID，只有当visible参数为3时生效且必选
	 * @param pic         要上传的图片，仅支持JPEG、GIF、PNG格式，图片大小小于5M
	 * @param lat         纬度，有效范围：-90.0到+90.0，+表示北纬，默认为0.0
	 * @param _long       经度，有效范围：-180.0到+180.0，+表示东经，默认为0.0
	 * @param annotations 元数据，主要是为了方便第三方应用记录一些适合于自己使用的信息，每条微博可以包含一个或者多个元数据，必须以json字串的形式提交，字串长度不超过512个字符，具体内容可以自定
	 * @param rip         开发者上报的操作用户真实IP，形如：211.156.0.1
	 * @return api
	 */
	public static MultipartAPI upload(String status, int visible, String list_id, List<Uri> pic, float lat, float _long, JSONObject annotations, String rip) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("status", status.trim());
		params.put("visible", String.valueOf(CatnutUtils.optValue(visible, 0)));
		if (visible == 3) { // 暂时不碰这个先
			params.put("list_id", list_id);
		}
		params.put("lat", String.valueOf(CatnutUtils.scaleNumber(CatnutUtils.optValue(lat, 0.f), 1)));
		params.put("long", String.valueOf(CatnutUtils.scaleNumber(CatnutUtils.optValue(_long, 0.f), 1)));
		if (annotations != null) {
			params.put("annotations", annotations.toString());
		}
		params.put("rip", String.valueOf(rip));
		Map<String, List<Uri>> files = new HashMap<String, List<Uri>>();
		files.put("pic", pic);
		return new MultipartAPI(Request.Method.POST, BASE_URI + "upload.json", true, params, files);
	}

	/**
	 * 根据微博ID删除指定微博
	 *
	 * @param id 需要删除的微博ID
	 * @return api
	 */
	public static CatnutAPI destroy(long id) {
		StringBuilder uri = new StringBuilder(BASE_URI).append("destroy.json");
		HashMap<String, String> params = new HashMap<String, String>(1);
		params.put("id", String.valueOf(id));
		return new CatnutAPI(Request.Method.POST, uri.toString(), true, params);
	}

	/**
	 * 获取最新的提到登录用户的微博列表，即@我的微博
	 *
	 * @param since_id         若指定此参数，则返回ID比since_id大的微博（即比since_id时间晚的微博），默认为0
	 * @param max_id           若指定此参数，则返回ID小于或等于max_id的微博，默认为0
	 * @param count            单页返回的记录条数，最大不超过200，默认为20
	 * @param page             返回结果的页码，默认为1
	 * @param filter_by_author 作者筛选类型，0：全部、1：我关注的人、2：陌生人，默认为0
	 * @param filter_by_source 来源筛选类型，0：全部、1：来自微博、2：来自微群，默认为0
	 * @param filter_by_type   原创筛选类型，0：全部微博、1：原创的微博，默认为0
	 * @return api
	 */
	public static CatnutAPI mentions(long since_id, long max_id, int count, int page, int filter_by_author, int filter_by_source, int filter_by_type) {
		StringBuilder uri = new StringBuilder(BASE_URI);
		uri.append("mentions.json")
				.append("?since_id=").append(CatnutUtils.optValue(since_id, 0))
				.append("&max_id=").append(CatnutUtils.optValue(max_id, 0))
				.append("&count=").append(CatnutUtils.optValue(count, 20))
				.append("&page=").append(CatnutUtils.optValue(page, 1))
				.append("&filter_by_author=").append(CatnutUtils.optValue(filter_by_author, 0))
				.append("&filter_by_source=").append(CatnutUtils.optValue(filter_by_source, 0))
				.append("&filter_by_type=").append(CatnutUtils.optValue(filter_by_type, 0));
		return new CatnutAPI(Request.Method.GET, uri, true, null);
	}
}
