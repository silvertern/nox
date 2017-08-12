/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT
 */
package nox.compilation

import nox.manifest.OsgiManifest
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.jvm.tasks.Jar
import java.io.IOException
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption


class ManifestUnpackingAction {

	private val manifestFile: Path

	constructor(target: Project) {
		val project = target as ProjectInternal
		this.manifestFile = Paths.get(project.projectDir.absolutePath, "META-INF", "MANIFEST.MF")

		val tasks = project.tasks
		val ext = project.extensions

		val platform = ext.findByType(OSGiExt::class.java)
		val jarTask = tasks.getByName("jar") as Jar

		// extension value has precedency over ext; default=unpack
		if ((jarTask.manifest as OsgiManifest).from != null) {
			return
		} else if (platform.unpackOSGiManifest != null) {
			if (!platform.unpackOSGiManifest!!) {
				return
			}
		} else {
			val extProps = ext.extraProperties
			if (extProps.has(unpackOSGiManifest)) {
				if (!java.lang.Boolean.valueOf(extProps.get(unpackOSGiManifest).toString())!!) {
					return
				}
			}
		}

		val buildTask = tasks.getByName("build")
		val cleanTask = tasks.getByName("clean")

		buildTask.doLast { _ -> unpack(jarTask) }
		cleanTask.doLast { _ -> clean() }
	}

	private fun unpack(jarTask: Jar) {
		// ignore failure here, will throw below
		manifestFile.parent.toFile().mkdirs()
		val jarUri = URI.create("jar:" + jarTask.archivePath.toURI())
		FileSystems.newFileSystem(jarUri, mapOf<String, Any>()).use { jarfs ->
			val source = jarfs.getPath("META-INF", "MANIFEST.MF")
			Files.copy(source, manifestFile, StandardCopyOption.REPLACE_EXISTING)
		}
	}

	private fun clean() {
		manifestFile.toFile().delete()
	}

	companion object {

		/**
		 * Add osgiUnpackManifest=false to gradle.properties to prevent copying
		 */
		private val unpackOSGiManifest = "unpackOSGiManifest"
	}
}
