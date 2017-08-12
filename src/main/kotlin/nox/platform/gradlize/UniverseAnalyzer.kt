/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT
 */
package nox.platform.gradlize

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

	companion object {

		fun listBundles(pluginsDir: File): Collection<File> {
			val files = pluginsDir.listFiles { file ->
				!file.name.contains(".source_") && !file.name.contains("-sources")
			}
			checkNotNull(files) { "No permissions to list target plugins directory" }
			return files!!.toList()
		}

		fun loadManifest(bundleFile: File): Manifest {
			if (bundleFile.isDirectory) {
				FileUtils.openInputStream(File(bundleFile, "META-INF/MANIFEST.MF"))
					.use { s -> return Manifest(s) }
			} else {
				return JarFile(bundleFile).manifest
			}
		}

		@Throws(IOException::class)
		fun loadBundles(pluginsDir: File): Collection<Bundle> =
			listBundles(pluginsDir).map { file -> Bundle.parse(loadManifest(file)) }
	}
}
