/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.beans;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 微博用户。
 *
 * @author longkai
 */
public class User implements Parcelable {

	public long id;
	/**
	 * 昵称
	 */
	public String screen_name;
	/**
	 * 友好显示名称
	 */
	public String name;
	/**
	 * 所在省级ID
	 */
	public int province;
	/**
	 * 所在省级ID
	 */
	public int city;
	/**
	 * 所在地
	 */
	public String location;
	/**
	 * 个人描述
	 */
	public String description;
	/**
	 * 博客地址
	 */
	public String url;
	/**
	 * 头像地址（中图），50×50像素
	 */
	public String profile_image_url;
	/**
	 * 主页封面图片（桌面）
	 */
	public String cover_image;
	/**
	 * 主页封面图片（手机）
	 */
	public String cover_image_phone;
	/**
	 * 微博统一URL地址
	 */
	public String profile_url;
	/**
	 * 个性化域名
	 */
	public String domain;
	/**
	 * 微号
	 */
	public String weihao;
	/**
	 * 性别，m：男、f：女、n：未知
	 */
	public String gender;
	/**
	 * 粉丝数
	 */
	public int followers_count;
	/**
	 * 关注数
	 */
	public int friends_count;
	/**
	 * 微博数
	 */
	public int statuses_count;
	/**
	 * 收藏数
	 */
	public int favourites_count;
	/**
	 * 用户创建（注册）时间
	 */
	public String created_at;
	/**
	 * 暂未支持
	 */
	public boolean following;
	/**
	 * 是否允许所有人给我发私信
	 */
	public boolean allow_all_act_msg;
	/**
	 * 是否允许标识用户的地理位置
	 */
	public boolean geo_enabled;
	/**
	 * 是否是微博认证用户，即加V用户
	 */
	public boolean verified;
	/**
	 * 暂未支持
	 */
	public int verified_type;
	/**
	 * 用户备注信息，只有在查询用户关系时才返回此字段
	 */
	public String remark;
	public int ptype;
	/**
	 * 是否允许所有人对我的微博进行评论
	 */
	public boolean allow_all_comment;
	/**
	 * 头像地址（大图），180×180像素
	 */
	public String avatar_large;
	/**
	 * 头像地址（高清），高清头像原图
	 */
	public String avatar_hd;
	/**
	 * 认证原因
	 */
	public String verified_reason;
	/**
	 * 是否关注当前登录用户
	 */
	public boolean follow_me;
	/**
	 * 在线状态，0：不在线、1：在线
	 */
	public int online_status;
	/**
	 * 互粉数
	 */
	public int bi_followers_count;
	/**
	 * 当前的语言版本，zh-cn：简体中文，zh-tw：繁体中文，en：英语
	 */
	public String lang;
	public int star;
	public int mbtype;
	public int mbrank;
	public int block_word;

	// todo: latest weibo

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {

	}
}
