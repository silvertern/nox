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

	fun bundle(symbolicName: String, version: String): ClientModule {
		return DefaultClientModule(group, holder.bundleMappings.getOrDefault(symbolicName, symbolicName), version)
	}

	fun repo(name: String, file: Any): ArtifactRepository {
		val root: File
		if (file is File) {
			root = file
		} else if (file is Path) {
			root = file.toFile()
		} else {
			root = File(file.toString())
		}
		val repo = repoFactory.createIvyRepository()
		repo.name = name
		repo.setUrl(root)
		repo.layout("pattern") { layout ->
			val ivyLayout = layout as DefaultIvyPatternRepositoryLayout
			ivyLayout.artifact(
				"%s/[module](.[classifier])_[revision].[ext]".format(PLUGINS))
			ivyLayout.artifact(
				"%s/[module](.[classifier])_[revision]".format(PLUGINS))
			ivyLayout.artifact(
				"%s/[module](.[classifier])-[revision].[ext]".format(PLUGINS))
			ivyLayout.artifact(
				"%s/[module](.[classifier])-[revision]".format(PLUGINS))
			ivyLayout.ivy(
				"%s/[module](.[classifier])_[revision].[ext]".format(IVY_METADATA))
		}
		return repo
	}

	fun buildProperties(config: Closure<*>) {
		ConfigureUtil.configure(config, buildProperties)
	}

	companion object {

		val PLUGINS = "plugins"

		val IVY_METADATA = "ivy-metadata"

		val name = "osgi"

		val group = "plugins"
	}
}
