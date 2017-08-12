/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT 
 */
package nox

import nox.compilation.BuildPropertiesCreationAction
import nox.compilation.ManifestUnpackingAction
import nox.compilation.OSGiExt
import nox.core.PlatformInfoHolder
import nox.manifest.OsgiManifest
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.Copy
import org.gradle.jvm.tasks.Jar
import org.gradle.language.jvm.tasks.ProcessResources
import java.util.function.Supplier


class OSGi : Plugin<Project> {

	override fun apply(target: Project) {
		val project = target as ProjectInternal

		project.pluginManager.apply(JavaPlugin::class.java)

		val tasks = project.tasks
		val ext = project.extensions

		val jarTask = tasks.getByName("jar") as Jar
		val manifest = OsgiManifest(Supplier { project.configurations.getByName(configName).files },
			Supplier {
				val javaConv = project.convention.getPlugin(JavaPluginConvention::class.java)
				javaConv.sourceSets.getByName(sourceSetName).output.classesDirs.singleFile
			},
			project.fileResolver)
		jarTask.manifest = manifest

		val infoHolder = project.rootProject.extensions.findByType(PlatformInfoHolder::class.java)
		ext.create(OSGiExt.registrationName, OSGiExt::class.java, project, infoHolder)

		project.afterEvaluate { ManifestUnpackingAction(it) }
		project.afterEvaluate { BuildPropertiesCreationAction(it) }

		val procBinincludes = tasks.create("processBinincludes", Copy::class.java)
		val procRes = tasks.getByName("processResources")
		procRes.dependsOn(procBinincludes)

		project.afterEvaluate { this.configureProcessBinincludes(it) }
	}

	private fun configureProcessBinincludes(target: Project) {
		val project = target as ProjectInternal

		val ext = project.extensions
		val buildProps = ext.findByType(OSGiExt::class.java).buildProperties

		val tasks = project.tasks
		val procRes = tasks.getByName("processResources") as ProcessResources
		val procBinincludes = tasks.getByName("processBinincludes") as Copy

		procBinincludes.from(project.projectDir)
			.into(procRes.destinationDir)
			.include(*buildProps.binincludes.toTypedArray())
			.exclude("**/MANIFEST.MF", "**/.gitkeep")
	}

	companion object {

		private val sourceSetName = "main"

		private val configName = "runtime"
	}
}
