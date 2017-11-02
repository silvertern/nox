/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT 
 */
package nox.compilation

import groovy.lang.Closure
import nox.core.PlatformInfoHolder
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.internal.artifacts.BaseRepositoryFactory
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.util.ConfigureUtil
import java.io.File
import java.nio.file.Path


open class OSGiExt(project: ProjectInternal, private val holder: PlatformInfoHolder) {

	private val repoFactory: BaseRepositoryFactory = project.services.get(BaseRepositoryFactory::class.java)

	class BuildProperties {

		val sources = mutableListOf<String>()

		val outputs = mutableListOf<String>()

		val binincludes = mutableSetOf("META-INF/", ".")

		val instructions: MutableMap<String, String> = LinkedHashMap()

		fun sources(vararg sources: String) {
			this.sources.addAll(sources)
		}

		fun binincludes(vararg binincludes: String) {
			this.binincludes.addAll(binincludes)
		}

		fun outputs(vararg outputs: String) {
			this.outputs.addAll(outputs)
		}

		fun instruction(key: String, value: String) {
			this.instructions.put(key, value)
		}
	}

	var unpackOSGiManifest: Boolean? = null

	val buildProperties = BuildProperties()

	fun map(fromSymbolicName: String, toSymbolicName: String): OSGiExt {
		holder.bundleMappings.put(fromSymbolicName, toSymbolicName)
		return this
	}

	@JvmOverloads
	fun bundle(symbolicName: String, version: String = "+"): String {
		return "%s:%s:%s".format(group, holder.bundleMappings.getOrDefault(symbolicName, symbolicName), version)
	}

	fun repo(name: String, repoRoot: Any): ArtifactRepository {
		var root = when (repoRoot) {
			is String -> repoRoot
			is File -> repoRoot.absolutePath
			is Path -> repoRoot.toFile().absolutePath
			else -> repoRoot.toString()
		}
		root = root.replace("\\", "/")
		val repo = repoFactory.createIvyRepository()
		repo.name = name
		// eclipse jar format
		repo.artifactPattern("%s/[organisation]/[module](.[classifier])_[revision].[ext]".format(root))
		// eclipse dir format
		repo.artifactPattern("%s/[organisation]/[module](.[classifier])_[revision]".format(root))
		// maven format
		repo.artifactPattern("%s/[organisation]/[module]-[revision](-[classifier]).[ext]".format(root))
		// ivy metadata format
		repo.ivyPattern("%s/%s/[organisation]/[module]-[revision](-[classifier]).[ext]".format(root, ivyDir))
		return repo
	}

	fun buildProperties(config: Closure<*>) {
		ConfigureUtil.configure(config, buildProperties)
	}

	companion object {

		val registrationName = "osgi"

		val pluginsDir = "plugins"

		val ivyDir = "ivy-metadata"

		val group = pluginsDir
	}
}
