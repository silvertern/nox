/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.sys;

import org.gradle.internal.os.OperatingSystem;


public enum OS {
	linux("linux"),
	win32("win32"),
	macosx("macosx");

	private final String value;

	OS(String value) {
		this.value = value;
	}

	public static OS current() {
		OperatingSystem os = OperatingSystem.current();
		return os.isWindows() ? win32 : (os.isMacOsX() ? macosx : linux);
	}

	public static boolean current(OS check) {
		return current() == check;
	}

	@Override
	public String toString() {
		return value;
	}
}
