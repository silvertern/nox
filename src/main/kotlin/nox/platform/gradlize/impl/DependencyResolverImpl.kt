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
import java.util.*


internal class DependencyResolverImpl(private val universe: BundleUniverse) : DependencyResolver {

	private enum class Status {
		Ok,
		Ignored,
		VersionMismatch,
		Missing
	}

	override fun resolveFor(bundle: Bundle): Collection<Dependency> {
		val depRange = mutableMapOf<String, SortedSet<Version>>()
		for (req in bundle.requiredBundles) {
			if (!req.optional && bundle.name != req.name && !req.name.startsWith("system.bundle") && !req.name.startsWith("org.eclipse")) {
				val allBundleVersions = universe.bundleVersions(req.name)
				val okBundleVersions = allBundleVersions.subSet(req.from, req.to)
				if (okBundleVersions.isNotEmpty()) {
					depRange.put(req.name, TreeSet(okBundleVersions))
				} else if (allBundleVersions.isNotEmpty()) {
					logger.warn(":bundle -> {} has bundle version mismatch\n\tusing {} for {}", bundle, allBundleVersions, req)
					depRange.put(req.name, allBundleVersions)
				} else {
					logger.error(":bundle -> {} is missing required bundle\n\t{}", bundle, req.name)
				}
			}
		}

		var missing : SortedSet<String> = TreeSet()
		val misversioned = mutableListOf<String>()

		for (pkgReq in bundle.importedPackages) {
			val dep = resolveForPackage(bundle, pkgReq)
			if (dep.first == Status.Ok || dep.first == Status.VersionMismatch) {
				depRange.put(dep.second, dep.third) // possibly overwrite with more stringent
			}
			when (dep.first) {
				Status.Missing -> missing.add(pkgReq.name)
				Status.VersionMismatch -> misversioned.add("using %s for %s".format(dep.third, pkgReq))
			}
		}

		missing = missing.filter {
			var res = true
			for (m in missing) {
				if (it != m && it.startsWith(m)) {
					res = false
					break
				}
			}
			res
		}.toSortedSet()

		if (missing.isNotEmpty()) {
			logger.error(":bundle -> {} is missing required packages\n\t{}", bundle, missing.joinToString("\n\t"))
		}
		if (misversioned.isNotEmpty()) {
			logger.warn(":bundle -> {} has package version mismatch\n\t{}", bundle, misversioned.joinToString("\n\t"))
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
	private fun resolveForPackage(bundle: Bundle, pkgReq: Requirement): Triple<Status, String, SortedSet<Version>> {
		if (pkgReq.optional ||
			pkgReq.name.startsWith("org.eclipse") ||
			pkgReq.name.startsWith("javax") ||
			// pkgReq.name.startsWith("org.osgi") ||
			pkgReq.name.startsWith("org.w3c.dom")) {
			return Triple(Status.Ignored, "", TreeSet())
		}
		bundle.exportedPackages
			.filter { pkgReq.name == it.name }
			.forEach { return Triple(Status.Ignored, "", TreeSet()) }

		val allPkgVersions = universe.packageVersionsWithExportingBundles(pkgReq.name)
			.filterValues { implBundle -> implBundle.name != bundle.name }.toSortedMap()
		if (allPkgVersions.isEmpty()) {
			return Triple(Status.Missing, "", TreeSet())
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
		return Triple(if (imperfectMatch) Status.VersionMismatch else Status.Ok, bundleName, bundleVersions)
	}

	companion object {

		private val logger = LoggerFactory.getLogger(DependencyResolverImpl::class.java)
	}
}
