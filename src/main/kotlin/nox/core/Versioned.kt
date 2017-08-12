/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.core


abstract class Versioned @JvmOverloads protected constructor(val name: String, val version: Version = Version.DEFAULT) : Comparable<Versioned> {

    override fun compareTo(other: Versioned): Int {
        val diff = name.compareTo(other.name)
        if (diff != 0) {
            return diff
        }
        return version.compareTo(other.version)
    }
}
