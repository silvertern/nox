/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT
 */
package nox.platform.gradlize.impl

import com.google.common.collect.Maps
import com.google.common.collect.Sets
import nox.core.Version
import nox.platform.gradlize.Bundle
import nox.platform.gradlize.BundleUniverse
import nox.platform.gradlize.Duplicates
import java.util.SortedMap
import java.util.SortedSet


internal class BundleUniverseImpl(private val duplicates: Duplicates) : BundleUniverse {

	private val bundleVersions = mutableMapOf<String, SortedSet<Version>>()

	private val pkgImplBundles = mutableMapOf<String, SortedMap<Version, Bundle>>()

	override fun bundleNames(): Set<String> {
		return bundleVersions.keys
	}

	override fun bundleVersions(bundleName: String): SortedSet<Version> {
		val res = bundleVersions[bundleName] ?: return sortedSetOf()
		return Sets.newTreeSet(res) // copy
	}

	override fun packageNames(): Set<String> {
		return pkgImplBundles.keys
	}

	override fun packageVersionsWithExportingBundles(pkgName: String): SortedMap<Version, Bundle> {
		val res = pkgImplBundles[pkgName] ?: return sortedMapOf()
		return Maps.newTreeMap(res) // copy
	}

	override fun with(bundle: Bundle): BundleUniverse {
		if (!bundleVersions.containsKey(bundle.name)) {
			bundleVersions.put(bundle.name, sortedSetOf())
		}
		val versions = bundleVersions[bundle.name]!!
		check(duplicates.permitted() || !versions.contains(bundle.version)) {
			"Bundle %s already exists".format(bundle) }
		versions.add(bundle.version)

		for (expPkg in bundle.exportedPackages) {
			if (!pkgImplBundles.containsKey(expPkg.name)) {
				pkgImplBundles.put(expPkg.name, sortedMapOf())
			}
			val pkgVersions = pkgImplBundles[expPkg.name]!!
			// overwrites already present bundle for the same package version
			pkgVersions.put(expPkg.version, bundle)
		}
		return this
	}
}
