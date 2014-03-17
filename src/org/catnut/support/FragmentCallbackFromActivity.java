/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */
package org.catnut.support;

import android.os.Bundle;

/**
 * 提供在activity中回调fragment的机制
 *
 * @author longkai
 */
public interface FragmentCallbackFromActivity {
	/**
	 * 回调fragment
	 *
	 * @param args 可选参数
	 */
	void callback(Bundle args);
}
