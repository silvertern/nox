/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT
 */
package nox.compilation

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSetContainer
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths


class BuildPropertiesCreationAction(target: Project) {

	private val projectDir: File

	private val javaConv: JavaPluginConvention

	private val buildProps: OSGiExt.BuildProperties

	init {
		val project = target as ProjectInternal

		val tasks = project.tasks
		val ext = project.extensions

		projectDir = project.projectDir
		javaConv = project.convention.getPlugin(JavaPluginConvention::class.java)
		buildProps = ext.findByType(OSGiExt::class.java).buildProperties

		tasks.getByName("jar").doFirst { _ -> create() }
		tasks.getByName("clean").doFirst { _ -> clean() }
	}

	internal interface SourceExtractor {
		fun extract(sourceSets: SourceSetContainer): SourceDirectorySet
	}

	private val javaExtractor = object : SourceExtractor {
		override fun extract(sourceSets: SourceSetContainer): SourceDirectorySet {
			return sourceSets.getByName("main").java
		}
	}

	private val resourceExtractor = object : SourceExtractor {
		override fun extract(sourceSets: SourceSetContainer): SourceDirectorySet {
			return sourceSets.getByName("main").resources
		}
	}
	private fun create() {
		val lines = mutableListOf<String>()
		var javaSources = getSources(javaExtractor)
		javaSources.addAll(buildProps.sources)
		javaSources = javaSources.filter { File(projectDir, it).exists() }.toMutableList()
		if (javaSources.isNotEmpty()) {
			lines.add("source.. = " + javaSources.joinToString(","))
			lines.add("output.. = " + if (buildProps.outputs.isEmpty()) "bin/" else buildProps.outputs.joinToString(","))
		}
		var bins = getSources(resourceExtractor)
		bins.addAll(buildProps.binincludes)
		bins = bins.filter { it == "META-INF/" || it == "." || File(projectDir, it).exists() }.toMutableList()
		if (bins.isNotEmpty()) {
			lines.add("bin.includes = " + bins.joinToString(","))
		}
		for ((key, value) in buildProps.instructions) {
			lines.add("$key=$value")
		}

		try {
			Files.write(Paths.get(projectDir.absolutePath, BUILD_PROPERTIES), lines, Charset.forName("UTF-8"))
		} catch (ex: IOException) {
			throw GradleException("Failed to create build.properties for an OSGi bundle", ex)
		}

	}

	private fun getSources(sourceExtractor: SourceExtractor): MutableList<String> {
		val sourceEntries = sourceExtractor.extract(javaConv.sourceSets).srcDirs
		return sourceEntries
			.filter { it.exists() }
			.map {
				var element = it.absolutePath.replace(projectDir.absolutePath + "/", "").replace(projectDir.absolutePath + "\\", "")
				element += if (!element.endsWith("/")) "/" else ""
				element.replace("\\", "/")
			}.toMutableList()
	}

	private fun clean() {
		File(projectDir, BUILD_PROPERTIES).delete()
	}

	companion object {

		private val BUILD_PROPERTIES = "build.properties"
	}
}
