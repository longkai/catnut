/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package tingting.chen.beans;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 微博
 *
 * @author longkai
 */
public class Status implements Parcelable {

	public long id;
	/**
	 * 创建时间
	 */
	public String created_at;
	/**
	 * 内容
	 */
	public String text;
	/**
	 * 来源
	 */
	public String source;
	/**
	 * 是否已收藏
	 */
	public boolean favorited;
	/**
	 * 是否被截断
	 */
	public boolean truncated;
	/**
	 * （暂未支持）回复ID
	 */
	public String in_reply_to_status_id;
	/**
	 * （暂未支持）回复人UID
	 */
	public String in_reply_to_user_id;
	/**
	 * （暂未支持）回复人昵称
	 */
	public String in_reply_to_screen_name;
	// todo: pic_urls
	/**
	 * 缩略图片地址，没有时不返回此字段
	 */
	public String thumbnail_pic;
	/**
	 * 中等尺寸图片地址，没有时不返回此字段
	 */
	public String bmiddle_pic;
	/**
	 * 原始图片地址，没有时不返回此字段
	 */
	public String original_pic;
	/**
	 * 被转发的原微博信息字段，当该微博为转发微博时返回
	 */
	public Status retweeted_status;
	// todo: geo
	/**
	 * 作者
	 */
	public User user;
	/**
	 * 转发数
	 */
	public int reposts_count;
	/**
	 * 评论数
	 */
	public int comments_count;
	/**
	 * 表态数
	 */
	public int attitudes_count;
	/**
	 * 暂未支持
	 */
	public int mlevel;
	// todo: visible

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {

	}
}
