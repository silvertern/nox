/**
 * Copyright (c): Java port of https://github.com/eveoh/gradle-aspectj
 */
package nox

import nox.compilation.AjcTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet


class AspectJ : Plugin<Project> {

	override fun apply(project: Project) {
		project.pluginManager.apply(JavaBasePlugin::class.java)

		if (!project.hasProperty("aspectjVersion")) {
			throw GradleException("You must set the property 'aspectjVersion' before applying the aspectj plugin")
		}

		val confs = project.configurations
		val tasks = project.tasks

		if (confs.findByName("ajtools") == null) {
			val aspectjVersion = project.properties["aspectjVersion"]
			confs.create("ajtools")
			project.dependencies.add("ajtools", "org.aspectj:aspectjtools:" + aspectjVersion)
			project.dependencies.add("compile", "org.aspectj:aspectjrt:" + aspectjVersion)
		}

		val javaConv = project.convention.plugins["java"] as JavaPluginConvention


		for (projectSourceSet in javaConv.sourceSets) {
			val namingConventions = if (projectSourceSet.name == "main") MainNamingConventions() else DefaultNamingConventions()
			val aspectPathConfName = namingConventions.getAspectPathConfigurationName(projectSourceSet)
			val aspectInpathConfName = namingConventions.getAspectInpathConfigurationName(projectSourceSet)
			for (configuration in arrayOf(aspectPathConfName, aspectInpathConfName)) {
				if (confs.findByName(configuration) == null) {
					confs.create(configuration)
				}
			}

			if (!projectSourceSet.allJava.isEmpty) {
				val aspectTaskName = namingConventions.getAspectCompileTaskName(projectSourceSet)
				val javaTask = tasks.getByName(namingConventions.getJavaCompileTaskName(projectSourceSet))
				val classesTask = tasks.getByName("classes")

				val ajc = tasks.create(aspectTaskName, AjcTask::class.java)
				// aspectTaskArgs.put("overwrite", true);
				ajc.description = "Compiles AspectJ Source for " + projectSourceSet.name + " source set"
				ajc.sourceSet = projectSourceSet
				ajc.inputs.files(projectSourceSet.allJava)
				ajc.outputs.dir(projectSourceSet.output.classesDirs.singleFile)
				ajc.aspectpath = confs.findByName(aspectPathConfName)
				ajc.ajInpath = confs.findByName(aspectInpathConfName)

				ajc.dependsOn(javaTask.taskDependencies)
				ajc.dependsOn(ajc.aspectpath)
				ajc.dependsOn(ajc.ajInpath)
				javaTask.setActions(emptyList())
				classesTask.dependsOn(ajc)
				javaTask.mustRunAfter(ajc)
			}
		}
	}

	private interface NamingConventions {

		fun getJavaCompileTaskName(sourceSet: SourceSet): String

		fun getAspectCompileTaskName(sourceSet: SourceSet): String

		fun getAspectPathConfigurationName(sourceSet: SourceSet): String

		fun getAspectInpathConfigurationName(sourceSet: SourceSet): String
	}

	private class MainNamingConventions : NamingConventions {

		override fun getJavaCompileTaskName(sourceSet: SourceSet): String {
			return "compileJava"
		}

		override fun getAspectCompileTaskName(sourceSet: SourceSet): String {
			return "compileAspect"
		}

		override fun getAspectPathConfigurationName(sourceSet: SourceSet): String {
			return "aspectpath"
		}

		override fun getAspectInpathConfigurationName(sourceSet: SourceSet): String {
			return "ajInpath"
		}
	}

	private class DefaultNamingConventions : NamingConventions {

		override fun getJavaCompileTaskName(sourceSet: SourceSet): String {
			return "compile" + capitalize(sourceSet.name) + "Java"
		}

		override fun getAspectCompileTaskName(sourceSet: SourceSet): String {
			return "compile" + capitalize(sourceSet.name) + "Aspect"
		}

		override fun getAspectPathConfigurationName(sourceSet: SourceSet): String {
			return sourceSet.name + "Aspectpath"
		}

		override fun getAspectInpathConfigurationName(sourceSet: SourceSet): String {
			return sourceSet.name + "AjInpath"
		}

		private fun capitalize(name: String): String {
			return name.substring(0, 1).toUpperCase() + name.substring(1)
		}
	}


}
