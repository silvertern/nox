/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.sys

import org.gradle.internal.os.OperatingSystem


enum class OS private constructor(private val value: String) {
	linux("linux"),
	win32("win32"),
	macosx("macosx");

	override fun toString(): String {
		return value
	}

	companion object {

		fun current(): OS {
			val os = OperatingSystem.current()
			return if (os.isWindows) win32 else if (os.isMacOsX) macosx else linux
		}

		fun current(check: OS): Boolean {
			return current() == check
		}
	}
}
