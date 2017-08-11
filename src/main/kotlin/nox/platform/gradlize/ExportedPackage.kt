/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT
 */
package nox.platform.gradlize


import nox.core.Version
import nox.core.Versioned

class ExportedPackage(name: String, version: Version) : Versioned(name, version) {

	constructor(name: String) : this(name, Version.DEFAULT)
}
