/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT
 */
package nox.platform.gradlize.impl


import nox.core.Version
import nox.platform.gradlize.Bundle
import nox.platform.gradlize.BundleUniverse
import nox.platform.gradlize.Dependency
import nox.platform.gradlize.DependencyResolver
import nox.platform.gradlize.Requirement
import org.slf4j.LoggerFactory
import java.util.SortedSet
import java.util.TreeSet


internal class DependencyResolverImpl(private val universe: BundleUniverse) : DependencyResolver {

	override fun resolveFor(bundle: Bundle): Collection<Dependency> {
		val depRange = mutableMapOf<String, SortedSet<Version>>()
		for (req in bundle.requiredBundles) {
			if (!req.optional && bundle.name != req.name) {
				val allBundleVersions = universe.bundleVersions(req.name)
				val okBundleVersions = allBundleVersions.subSet(req.from, req.to)
				if (okBundleVersions.isNotEmpty()) {
					depRange.put(req.name, TreeSet(okBundleVersions))
				} else if (allBundleVersions.isNotEmpty()) {
					logger.info(":bundle -> {} => version mismatch for bundle {}", bundle, req.name)
					depRange.put(req.name, allBundleVersions)
				} else {
					logger.error(":bundle -> {} => missing required bundle {}", bundle, req.name)
				}
			}
		}

		for (pkgReq in bundle.importedPackages) {
			val dep = resolveForPackage(bundle, pkgReq)
			if (dep != null) {
				depRange.put(dep.first, dep.second) // possibly overwrite with more stringent
			}
		}

		val res = mutableListOf<Dependency>()
		for ((key, value) in depRange) {
			res.add(Dependency(key, value.last())) // above code guarantees at least 1 value
		}
		if (res.isNotEmpty()) {
			logger.debug("Dependency set for {}: {}", bundle, res)
		}
		return res.toList()
	}

	// - will prefer current matching over new
	// - may restrict the version range of current
	// - if no exact matching found may return mismatching version range for implementing bundle (will keep current)
	private fun resolveForPackage(bundle: Bundle, pkgReq: Requirement): Pair<String, SortedSet<Version>>? {
		if (pkgReq.optional || pkgReq.name.startsWith("org.eclipse") || pkgReq.name.startsWith(
			"org.osgi") || pkgReq.name.startsWith("javax.") || pkgReq.name.startsWith("org.w3c.dom")) {
			return null
		}
		for (expPack in bundle.exportedPackages) {
			if (pkgReq.name == expPack.name) {
				return null
			}
		}
		val allPkgVersions = universe.packageVersionsWithExportingBundles(pkgReq.name)
			.filterValues { implBundle -> implBundle.name != bundle.name }.toSortedMap()
		if (allPkgVersions.isEmpty()) {
			logger.error(":bundle -> {} => missing required package {}", bundle, pkgReq.name)
			return null
		}

		var pkgVersions = allPkgVersions.subMap(pkgReq.from, pkgReq.to)
		var imperfectMatch = false
		if (pkgVersions.isEmpty()) {
			imperfectMatch = true
			pkgVersions = allPkgVersions
		}

		// prefer bundle providing latest package version
		val bundleName = pkgVersions[pkgVersions.lastKey()]!!.name
		// but return all bundle versions of the same bundle
		val bundleVersions = pkgVersions.values
			.filter { it.name == bundleName }
			.map { it.version }
			.toSortedSet()
		val res = Pair(bundleName, bundleVersions)
		if (imperfectMatch) {
			logger.info(":bundle -> {} => version mismatch for package {}: {}", bundle, pkgReq, res)
		}
		return res
	}

	companion object {

		private val logger = LoggerFactory.getLogger(DependencyResolverImpl::class.java)
	}
}
