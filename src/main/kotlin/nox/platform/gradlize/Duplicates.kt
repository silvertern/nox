/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT
 */
package nox.platform.gradlize

enum class Duplicates(private val value: Boolean) {
	Overwrite(true),
	Forbid(false);

	fun permitted(): Boolean {
		return value
	}
}
