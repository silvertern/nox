/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT
 */
package nox.platform

import nox.compilation.OSGiExt
import nox.core.Versioned
import nox.platform.gradlize.Bundle
import nox.platform.gradlize.BundleUniverse
import nox.platform.gradlize.Dependency
import nox.platform.gradlize.Duplicates
import nox.platform.gradlize.MetadataExporter
import nox.platform.gradlize.UniverseAnalyzer
import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import java.io.File
import java.io.IOException


open class IvynizeTask : DefaultTask() {

	companion object {

		val registrationName = "ivynize"
	}

	@Internal
	var targetPlatform : File? = null

	val ivyDir: File
		@OutputDirectory
		get() = File(targetPlatform, OSGiExt.ivyDir)

	val pluginsDir: File
		@InputDirectory
		get() = File(targetPlatform, OSGiExt.pluginsDir)

	init {
		group = "nox.Platform"
		description = "Resolves target platform bundle dependencies into an Ivy repository under {targetPlatform}/" + OSGiExt.ivyDir
	}

	@TaskAction
	fun action(inputs: IncrementalTaskInputs) {
		if (!inputs.isIncremental) {
			action()
		} else {
			var counter = 0
			inputs.outOfDate { _ -> counter++ }
			inputs.removed { _ -> counter++ }
			if (counter > 0) {
				action()
			}
		}
	}

	private fun action() {
		pluginsDir.mkdirs()
		try {
			FileUtils.deleteDirectory(ivyDir)

			val nameMap = remapSymbolicNamesToFilePrefixes()

			UniverseAnalyzer(pluginsDir, { bundle, deps ->
				val bundleFilePrefix = evalFilePrefix(nameMap, bundle)
				val remappedBundle = bundle.rename(bundleFilePrefix)
				val remappedDeps = deps.map { Dependency(evalFilePrefix(nameMap, it), it.version) }

				MetadataExporter.instance(remappedBundle, OSGiExt.group, remappedDeps).exportTo(ivyDir)
			}).analyze(BundleUniverse.instance(Duplicates.Overwrite))
		} catch (ex: IOException) {
			throw IllegalStateException(ex)
		}
	}

	@Throws(IOException::class)
	private fun remapSymbolicNamesToFilePrefixes(): Map<String, Map<String, String>> {
		val res = mutableMapOf<String, MutableMap<String, String>>()
		UniverseAnalyzer.listBundles(pluginsDir).forEach { file ->
			val bundle = Bundle.parse(UniverseAnalyzer.loadManifest(file))

			val symbolicName = bundle.name
			if (!res.containsKey(symbolicName)) {
				res.put(symbolicName, mutableMapOf())
			}

			val version = bundle.version.toString()
			var filePrefix = file.name.split(version.toRegex())[0]
			if (filePrefix.endsWith("_") || filePrefix.endsWith("-")) {
				filePrefix = filePrefix.substring(0, filePrefix.length - 1)
			}

			res[symbolicName]!![version] = filePrefix
		}
		return res
	}

	private fun evalFilePrefix(nameMap: Map<String, Map<String, String>>, versioned: Versioned): String {
		val bySymbolicName = nameMap[versioned.name]
		if (bySymbolicName != null) {
			val filePrefix = bySymbolicName[versioned.version.toString()]
			if (!filePrefix.isNullOrBlank()) {
				return filePrefix!!
			}
		}
		return versioned.name
	}
}
