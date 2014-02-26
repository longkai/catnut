/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.support;

import android.net.Uri;
import org.catnut.core.CatnutAPI;

import java.util.List;
import java.util.Map;

/**
 * 文件上传API，使用Uri标识文件
 *
 * @author longkai
 */
public class MultipartAPI extends CatnutAPI {

	/** 需上传的文件，键值为文件参数名 */
	public final Map<String, List<Uri>> files;

	public MultipartAPI(int method, String uri, boolean authRequired, Map<String, String> params, Map<String, List<Uri>> files) {
		super(method, uri, authRequired, params);
		this.files = files;
	}
}
