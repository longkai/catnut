/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */

package org.catnut.support.annotation;

/**
 * a indicator that indicates a param in a method cannot be null, you should check on your own.
 *
 * @author longkai
 */
public @interface NotNull {
	/**
	 * tell the client why it cannot be null.
	 * @return the not null reason, optional
	 */
	String value() default "";
}
