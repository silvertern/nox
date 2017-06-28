/*
  Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.core.sys;

import org.gradle.internal.os.OperatingSystem;


public enum Win {
	gtk("gtk"),
	win32("win32"),
	cocoa("cocoa");

	private final String value;

	Win(String value) {
		this.value = value;
	}

	public static Win current() {
		OperatingSystem os = OperatingSystem.current();
		return os.isWindows() ? win32 : (os.isMacOsX() ? cocoa : gtk);
	}

	@Override
	public String toString() {
		return value;
	}
}
