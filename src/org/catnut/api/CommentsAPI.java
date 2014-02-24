/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.api;

import com.android.volley.Request;
import org.catnut.core.CatnutAPI;
import org.catnut.util.CatnutUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 评论接口
 *
 * @author longkai
 */
public class CommentsAPI {

	private static final String BASE_URI = CatnutAPI.API_DOMAIN + "/2/comments/";

	/**
	 * 根据微博ID返回某条微博的评论列表
	 *
	 * @param id               需要查询的微博ID
	 * @param since_id         若指定此参数，则返回ID比since_id大的评论（即比since_id时间晚的评论），默认为0
	 * @param max_id           若指定此参数，则返回ID小于或等于max_id的评论，默认为0
	 * @param count            单页返回的记录条数，默认为50
	 * @param page             返回结果的页码，默认为1
	 * @param filter_by_author 作者筛选类型，0：全部、1：我关注的人、2：陌生人，默认为0
	 * @return api
	 */
	public static CatnutAPI show(long id, long since_id, long max_id, int count, int page, int filter_by_author) {
		StringBuilder uri = new StringBuilder(BASE_URI);
		uri.append("show.json")
				.append("?id=").append(id)
				.append("&since_id=").append(CatnutUtils.optValue(since_id, 0))
				.append("&max_id=").append(CatnutUtils.optValue(max_id, 0))
				.append("&count=").append(CatnutUtils.optValue(count, 50))
				.append("&page=").append(CatnutUtils.optValue(page, 1))
				.append("&filter_by_author=").append(CatnutUtils.optValue(filter_by_author, 0));
		return new CatnutAPI(Request.Method.GET, uri.toString(), true, null);
	}

	/**
	 * 对一条微博进行评论
	 *
	 * @param comment     评论内容，必须做URLencode，内容不超过140个汉字
	 * @param id          需要评论的微博ID
	 * @param comment_ori 当评论转发微博时，是否评论给原微博，0：否、1：是，默认为0
	 * @param rip         开发者上报的操作用户真实IP，形如：211.156.0.1
	 * @return api
	 */
	public static CatnutAPI create(String comment, long id, int comment_ori, String rip) {
		StringBuilder uri = new StringBuilder(BASE_URI).append("create.json");
		Map<String, String> params = new HashMap<String, String>();
		params.put("comment", comment);
		params.put("id", String.valueOf(id));
		params.put("comment_ori", String.valueOf(CatnutUtils.optValue(comment_ori, 0)));
		params.put("rip", String.valueOf(rip));
		return new CatnutAPI(Request.Method.POST, uri.toString(), true, params);
	}

	/**
	 * 回复一条评论
	 *
	 * @param cid             需要回复的评论ID
	 * @param id              需要评论的微博ID
	 * @param comment         回复评论内容，必须做URLencode，内容不超过140个汉字
	 * @param without_mention 回复中是否自动加入“回复@用户名”，0：是、1：否，默认为0
	 * @param comment_ori     当评论转发微博时，是否评论给原微博，0：否、1：是，默认为0
	 * @param rip             开发者上报的操作用户真实IP，形如：211.156.0.1
	 * @return api
	 */
	public static CatnutAPI reply(long cid, long id, String comment, int without_mention, int comment_ori, String rip) {
		StringBuilder uri = new StringBuilder(BASE_URI).append("reply.json");
		Map<String, String> params = new HashMap<String, String>();
		params.put("cid", String.valueOf(cid));
		params.put("id", String.valueOf(id));
		params.put("comment", comment);
		params.put("without_mention", String.valueOf(CatnutUtils.optValue(without_mention, 0)));
		params.put("comment_ori", String.valueOf(CatnutUtils.optValue(comment_ori, 0)));
		params.put("rip", String.valueOf(rip));
		return new CatnutAPI(Request.Method.POST, uri.toString(), true, params);
	}
}
