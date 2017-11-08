/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT 
 */
package nox.manifest

import aQute.bnd.osgi.Analyzer
import com.google.common.collect.Multimap
import com.google.common.collect.MultimapBuilder
import groovy.lang.Closure
import nox.core.Version
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.java.archives.internal.DefaultManifest
import org.gradle.internal.file.PathToFileResolver
import org.gradle.util.ConfigureUtil
import org.gradle.util.WrapUtil
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Path
import java.util.*
import java.util.function.Supplier
import java.util.jar.Manifest
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashSet


/**
 * OSGiJarManifest provides a drop in replacement for jar.manifest
 */
class OsgiManifest(private val classpathSupplier: Supplier<Set<File>>, private val jarSupplier: Supplier<File>, fileResolver: PathToFileResolver) : DefaultManifest(fileResolver) {

	var spec: ManifestSpec? = null

	var from: File? = null

	var fromVersion: Version? = null

	fun spec(symbolicName: String, version: Any, vararg configs: Closure<*>) {
		this.spec = ManifestSpec(symbolicName, Version(version.toString()))
		for (config in configs) {
			ConfigureUtil.configure<ManifestSpec>(config, this.spec)
		}
	}

	fun spec(from: Path, version: Any) {
		this.from = from.toFile()
		this.fromVersion = Version(version.toString())
	}

	fun spec(from: File, version: Any) {
		this.from = from
		this.fromVersion = Version(version.toString())
	}

	fun spec(project: Project, vararg configs: Closure<*>) {
		spec(toSymbolicName(project.group.toString(), project.name),
			Version(project.version.toString()), *configs)
	}

	fun toSymbolicName(groupId: String, artifactId: String): String {
		if (artifactId.startsWith(groupId)) {
			return artifactId
		}
		val parts = (groupId + "." + artifactId).split("[\\.-]".toRegex())
		val elements = LinkedHashSet(parts)
		return elements.joinToString(".")
	}

	override fun getEffectiveManifest(): DefaultManifest {
		try {
			val baseManifest = DefaultManifest(null)
			baseManifest.attributes(attributes)

			for ((key, value) in generateManifest().mainAttributes) {
				baseManifest.attributes(
					WrapUtil.toMap(key.toString(), value.toString()))
			}

			// this changing value prevented incremental builds...
			baseManifest.attributes.remove("Bnd-LastModified")
			return getEffectiveManifestInternal(baseManifest)
		} catch (ex: IOException) {
			throw GradleException(ex.message, ex)
		}
	}

	@Throws(IOException::class)
	private fun generateManifest(): Manifest {
		if (from != null) {
			FileInputStream(from).use { s ->
				val manifest = Manifest(s)
				if (fromVersion != null) {
					manifest.mainAttributes.putValue(Analyzer.BUNDLE_VERSION, fromVersion.toString())
				}
				return manifest
			}
		}
		if (spec == null) {
			throw GradleException("Please provide manifest 'spec' or 'from' file")
		}

		val mspec = spec!!

		val analyzer = Analyzer()
		analyzer.setBundleSymbolicName(mspec.symbolicName + if (mspec.singleton) ";singleton:=true" else "")
		analyzer.bundleVersion = mspec.version.toString()

		val instructions: Multimap<String, String> = MultimapBuilder.hashKeys().linkedHashSetValues().build()
		instructions.putAll(mspec.instructions())
		instructions.put(Analyzer.IMPORT_PACKAGE, "*")
		instructions.put(Analyzer.EXPORT_PACKAGE, "*;-noimport:=true;version=" + mspec.version.toString(Version.Component.Build))

		for (instruction in instructions.keySet()) {
			val values = ArrayList(instructions.get(instruction))
			val value = values.joinToString(",")
			analyzer.properties.setProperty(instruction, value)
		}
		if (!mspec.activator.isNullOrBlank()) {
			analyzer.properties.setProperty(Analyzer.BUNDLE_ACTIVATOR, mspec.activator!!)
		}
		if (!mspec.uses) {
			analyzer.setProperty(Analyzer.NOUSES, "true")
		}

		var jar: File? = jarSupplier.get()
		if (jar == null) {
			jar = File.createTempFile("osgi", UUID.randomUUID().toString())
			jar.delete()
			jar.mkdir()
			jar.deleteOnExit()
		}
		analyzer.setJar(jar)

		val classpath = classpathSupplier.get()
		if (classpath.isNotEmpty()) {
			analyzer.setClasspath(classpath)
		}

		try {
			val res = analyzer.calcManifest()
			var imports = res.mainAttributes.getValue(Analyzer.IMPORT_PACKAGE)
			if (!imports.isNullOrBlank()) {
				imports = imports.replace(";common=split", "")
				res.mainAttributes.putValue(Analyzer.IMPORT_PACKAGE, imports)
			}
			return res
		} catch (ex: Exception) {
			throw IllegalStateException(ex)
		}
	}

}
