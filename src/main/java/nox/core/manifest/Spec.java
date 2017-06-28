/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT
 */
package nox.core.manifest;

import com.google.common.collect.Multimap;


public interface Spec {

	boolean singleton();

	boolean uses();

	Multimap<String, String> instructions();
}
