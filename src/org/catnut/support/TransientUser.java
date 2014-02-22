/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.support;

import android.util.Log;
import org.catnut.metadata.User;
import org.catnut.util.Constants;
import org.json.JSONObject;

/**
 * 瞬时用户元数据，即这些数据没有必要存在在本地,etc. 查看别人用户的关注列表，这个只是在新浪那里有联系，放到本地纯属浪费空间Orz
 * <p/>
 * 暂时就是这么多字段吧，需要的时候再添加
 *
 * @author longkai
 */
public class TransientUser {

	public long id;
	public String screenName;
	public String location;
	public String description;
	public boolean verified;
	public String avatarUrl;

	public static TransientUser convert(JSONObject jsonObject) {
		TransientUser user = new TransientUser();
		user.id = jsonObject.optLong(Constants.ID);
		user.screenName = jsonObject.optString(User.screen_name);
		user.location = jsonObject.optString(User.location);
		user.description = jsonObject.optString(User.description);
		user.verified = jsonObject.optBoolean(User.verified);
		user.avatarUrl = jsonObject.optString(User.profile_image_url);
		return user;
	}
}
