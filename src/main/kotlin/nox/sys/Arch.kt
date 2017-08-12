/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.sys


enum class Arch private constructor(private val value: String) {
	x86_64("x86_64"),
	x86("x86");

	override fun toString(): String {
		return value
	}

	companion object {

		fun current(): Arch {
			if (OS.current(OS.macosx)) {
				return x86_64
			}
			return if (System.getProperty("os.arch").contains("64")) x86_64 else x86
		}

		fun current(check: Arch): Boolean {
			return current() == check
		}
	}
}
