/**
 * Copyright (c) Profidata AG 2017
 */
package nox.compilation

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.slf4j.LoggerFactory


open class AjcTask : DefaultTask() {

	@Internal var sourceSet: SourceSet? = null

	@Internal var aspectpath: FileCollection? = null

	@Internal var ajInpath: FileCollection? = null

	// ignore or warning
	@Internal var xlint = "ignore"

	@Internal var maxmem: String? = null

	@Internal var additionalAjcArgs = mutableMapOf<String, String>()

	init {
		logging.captureStandardOutput(LogLevel.INFO)
	}

	@TaskAction
	fun compile() {
		logger.info("Compining AspectJ. Classpath: {}, srcDirs: {}",
			sourceSet!!.compileClasspath.asPath,
			sourceSet!!.java.srcDirs)

		val ant = ant

		val taskdefArgs = mutableMapOf<String, Any>()
		taskdefArgs.put("resource", "org/aspectj/tools/ant/taskdefs/aspectjTaskdefs.properties")
		taskdefArgs.put("classpath", project.configurations.getByName("ajtools").asPath)

		ant.invokeMethod("taskdef", taskdefArgs)

		val javaConv = project.convention.plugins["java"] as JavaPluginConvention

		val ajcArgs = mutableMapOf<String, Any>()
		ajcArgs.put("classpath", sourceSet!!.compileClasspath.asPath)
		ajcArgs.put("destDir", sourceSet!!.output.classesDirs.singleFile.absolutePath)
		ajcArgs.put("s", sourceSet!!.output.classesDirs.singleFile.absolutePath)
		ajcArgs.put("source", javaConv.sourceCompatibility)
		ajcArgs.put("target", javaConv.targetCompatibility)
		ajcArgs.put("inpath", ajInpath!!.asPath)
		ajcArgs.put("xlint", xlint)
		ajcArgs.put("fork", "true")
		ajcArgs.put("aspectPath", aspectpath!!.asPath)
		ajcArgs.put("sourceRootCopyFilter", "**/*.java,**/*.aj")
		ajcArgs.put("showWeaveInfo", "true")
		ajcArgs.put("sourceRoots", sourceSet!!.java.sourceDirectories.asPath)
		if (maxmem != null) {
			ajcArgs.put("maxmem", maxmem!!)
		}
		ajcArgs.putAll(additionalAjcArgs)

		ant.invokeMethod("iajc", ajcArgs)
	}

	companion object {

		private val logger = LoggerFactory.getLogger(AjcTask::class.java)
	}
}
