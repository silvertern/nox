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
import org.gradle.api.GradleException
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import java.io.File
import java.io.IOException
import java.util.jar.JarFile
import java.util.jar.Manifest


open class IvynizeTask : DefaultTask() {

	@InputDirectory
	val pluginsDir: File

	@OutputDirectory
	val ivyDir: File

	init {
		group = "nox.Platform"
		description = "Resolves target platform bundle dependencies into an Ivy repo, " + "the repo is auto-added with nox.Java [implies bundle, create]."

		val ext = project.extensions.extraProperties
		if (!ext.has(targetPlatformRoot)) {
			throw GradleException("targetPlatformRoot undefined")
		}

		val targetPlatformFile = ext.get(targetPlatformRoot) as File
		pluginsDir = File(targetPlatformFile, OSGiExt.PLUGINS)
		ivyDir = File(targetPlatformFile, OSGiExt.IVY_METADATA)
	}

	@TaskAction
	fun action(inputs: IncrementalTaskInputs) {
		if (!inputs.isIncremental) {
			action()
			return
		}
		inputs.outOfDate { details -> action() }
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

				MetadataExporter.instance(remappedBundle, groupName, remappedDeps).exportTo(ivyDir)
			}).analyze(BundleUniverse.instance(Duplicates.Overwrite))
		} catch (ex: IOException) {
			throw IllegalStateException(ex)
		}

	}

	@Throws(IOException::class)
	private fun remapSymbolicNamesToFilePrefixes(): Map<String, Map<String, String>> {
		val res = mutableMapOf<String, MutableMap<String, String>>()
		val files = pluginsDir.listFiles { file -> !file.name.contains(".source_") }
		checkNotNull(files) { "No permissions to list target plugins directory" }
		for (file in files!!) {
			var manifest: Manifest? = null
			if (file.isDirectory) {
				FileUtils.openInputStream(File(file, "META-INF/MANIFEST.MF")).use { s -> manifest = Manifest(s) }
			} else {
				manifest = JarFile(file).manifest
			}
			val bundle = Bundle.parse(manifest!!)

			val symbolicName = bundle.name
			val version = bundle.version.toString()
			var filePrefix = file.name.split(version.toRegex())[0]
			if (filePrefix.endsWith("_") || filePrefix.endsWith("-")) {
				filePrefix = filePrefix.substring(0, filePrefix.length - 1)
			}

			if (!res.containsKey(symbolicName)) {
				res.put(symbolicName, mutableMapOf<String, String>())
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

	companion object {

		val targetPlatformRoot = "targetPlatformRoot"

		val groupName = "eclipse"
	}

}
