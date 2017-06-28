/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT 
 */
package nox.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.SOURCE)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
public @interface BuildAPI {
}
