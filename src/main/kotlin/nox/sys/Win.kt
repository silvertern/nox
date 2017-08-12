/*
  Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.sys

import org.gradle.internal.os.OperatingSystem


enum class Win private constructor(private val value: String) {
	gtk("gtk"),
	win32("win32"),
	cocoa("cocoa");

	override fun toString(): String {
		return value
	}

	companion object {

		fun current(): Win {
			val os = OperatingSystem.current()
			return if (os.isWindows) win32 else if (os.isMacOsX) cocoa else gtk
		}
	}
}
