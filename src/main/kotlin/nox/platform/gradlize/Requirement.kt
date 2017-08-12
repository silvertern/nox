/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT
 */
package nox.platform.gradlize


import nox.core.Version

class Requirement(val name: String, val from: Version, val to: Version, val optional: Boolean) {

	constructor(name: String, from: Version) : this(name, from, from.nextMajor(), false)

	override fun toString(): String {
		return "%s:[%s,%s)".format(name, from, to)
	}
}
