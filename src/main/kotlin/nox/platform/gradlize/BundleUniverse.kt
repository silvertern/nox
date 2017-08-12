/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT
 */
package nox.platform.gradlize

import nox.core.Version
import nox.platform.gradlize.impl.BundleUniverseImpl
import java.util.SortedMap
import java.util.SortedSet


interface BundleUniverse {

	companion object {

		fun instance(duplicates: Duplicates): BundleUniverse {
			return BundleUniverseImpl(duplicates)
		}
	}

	fun bundleNames(): Set<String>

	fun bundleVersions(bundleName: String): SortedSet<Version>

	fun packageNames(): Set<String>

	fun packageVersionsWithExportingBundles(pkgName: String): SortedMap<Version, Bundle>

	fun with(bundle: Bundle): BundleUniverse

}
