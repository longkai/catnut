/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.api;

import com.android.volley.Request;
import org.catnut.core.CatnutAPI;
import org.catnut.util.CatnutUtils;

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
}
