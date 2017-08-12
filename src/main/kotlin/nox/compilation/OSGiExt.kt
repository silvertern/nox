/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT 
 */
package nox.compilation

import groovy.lang.Closure
import nox.core.PlatformInfoHolder
import org.gradle.api.artifacts.ClientModule
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.internal.artifacts.BaseRepositoryFactory
import org.gradle.api.internal.artifacts.dependencies.DefaultClientModule
import org.gradle.api.internal.artifacts.repositories.layout.DefaultIvyPatternRepositoryLayout
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.util.ConfigureUtil
import java.io.File
import java.nio.file.Path


open class OSGiExt(project: ProjectInternal, private val holder: PlatformInfoHolder) {

	private val repoFactory: BaseRepositoryFactory = project.services.get(BaseRepositoryFactory::class.java)

	class BuildProperties {

		val sources = mutableListOf<String>()

		val outputs = mutableListOf<String>()

		val binincludes = mutableSetOf("META-INF/")

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
	fun bundle(symbolicName: String, version: String = "+"): ClientModule {
		return DefaultClientModule(group, holder.bundleMappings.getOrDefault(symbolicName, symbolicName), version)
	}

	fun repo(name: String, repoRoot: Any): ArtifactRepository {
		val root = when (repoRoot) {
			is File -> repoRoot
			is Path -> repoRoot.toFile()
			else -> File(repoRoot.toString())
		}
		val repo = repoFactory.createIvyRepository()
		repo.name = name
		repo.setUrl(root)
		repo.layout("pattern") { layout ->
			val ivyLayout = layout as DefaultIvyPatternRepositoryLayout
			// eclipse jar format
			ivyLayout.artifact("%s/[module](.[classifier])_[revision].[ext]".format(pluginsDir))
			// eclipse dir format
			ivyLayout.artifact("%s/[module](.[classifier])_[revision]".format(pluginsDir))
			// maven format
			ivyLayout.artifact("%s/[module]-[revision](-[classifier]).[ext]".format(pluginsDir))
			// ivy metadata format
			ivyLayout.ivy("%s/[module]-[revision](-[classifier]).[ext]".format(ivyDir))
		}
		return repo
	}

	fun buildProperties(config: Closure<*>) {
		ConfigureUtil.configure(config, buildProperties)
	}

	companion object {

		val registrationName = "osgi"

		val pluginsDir = "plugins"

		val ivyDir = "ivy-metadata"

		val group = "plugins" // not really used for repo layout, can be anything
	}
}
