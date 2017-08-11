/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT
 */
package nox.platform.gradlize


import nox.core.Version
import nox.core.Versioned

class Dependency @JvmOverloads constructor(name: String, version: Version = Version.DEFAULT, val optional: Boolean = false) : Versioned(name, version)
