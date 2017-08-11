/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT
 */
package nox.platform.gradlize

import com.google.common.base.Preconditions
import com.google.common.collect.Lists
import org.apache.commons.io.FileUtils
import org.gradle.api.GradleException
import java.io.File
import java.io.IOException
import java.util.jar.JarFile
import java.util.jar.Manifest


internal class UniverseAnalyzer(private val pluginsDir: File, private val action: (Bundle, Collection<Dependency>) -> Unit) {

	constructor(pluginsDir: File) : this(pluginsDir, { _, _ -> })

	fun analyze(universe: BundleUniverse) {
		try {

			val bundles = loadBundles(pluginsDir)
			for (bundle in bundles) {
				universe.with(bundle)
			}
			val resolver = DependencyResolver.instance(universe)
			for (bundle in bundles) {
				val deps = resolver.resolveFor(bundle)
				action(bundle, deps)
			}
		} catch (ex: IOException) {
			throw GradleException("Failed to analyze dependency tree", ex)
		}

	}

	@Throws(IOException::class)
	private fun loadBundles(pluginsDir: File): Collection<Bundle> {
		val files = pluginsDir.listFiles { file -> !file.name.contains(".source_") }
		Preconditions.checkNotNull(files, "No permissions to list target plugins directory")
		val bundles = Lists.newArrayList<Bundle>()
		for (file in files!!) {
			var manifest: Manifest? = null
			if (file.isDirectory) {
				FileUtils.openInputStream(File(file, "META-INF/MANIFEST.MF"))
					.use { s -> manifest = Manifest(s) }
			} else {
				manifest = JarFile(file).manifest
			}
			bundles.add(Bundle.parse(manifest!!))
		}
		return bundles
	}
}
