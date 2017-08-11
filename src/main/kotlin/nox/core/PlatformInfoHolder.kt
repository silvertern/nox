/**
 * Copyright (c) Profidata AG 2017
 */
package nox.core


/**
 * PlatformInfoHolder as a root project extension holds platform relevant information
 * for the overall build in a singleton manner.
 */
open class PlatformInfoHolder {

	val bundleMappings = mutableMapOf<String, String>()

	companion object {

		val name = "platformInfoHolder"
	}
}
