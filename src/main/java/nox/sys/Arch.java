/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.sys;

public enum Arch {
	x86_64("x86_64"),
	x86("x86");

	private final String value;

	Arch(String value) {
		this.value = value;
	}

	public static Arch current() {
		if (OS.current(OS.macosx)) {
			return x86_64;
		}
		return System.getProperty("os.arch").contains("64") ? x86_64 : x86;
	}

	public static boolean current(Arch check) {
		return current() == check;
	}

	@Override
	public String toString() {
		return value;
	}
}
