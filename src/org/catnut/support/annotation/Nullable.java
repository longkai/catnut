/*
 * The MIT License (MIT)
 * Copyright (c) 2014 longkai
 * The software shall be used for good, not evil.
 */

package org.catnut.support.annotation;

/**
 * an indicator that indicates a param in a method could be null or notnull, that depends you.
 *
 * @author longkai
 */
public @interface Nullable {
	/**
	 * tell the client why it could be null.
	 * @return null reason, optional
	 */
	String value() default "";
}
