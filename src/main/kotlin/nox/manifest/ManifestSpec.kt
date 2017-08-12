/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT 
 */
package nox.manifest

import aQute.bnd.osgi.Analyzer
import com.google.common.collect.Multimap
import com.google.common.collect.MultimapBuilder
import nox.core.Version
import org.gradle.api.GradleException


/**
 * ManifestSpec defines a specification for generating a manifest for an OSGi
 * module by analyzing its code and classpath. Directly assignable in build.gradle
 * to jar.manifest.spec after the nox.OSGi plugin has been applied.
 */
class ManifestSpec internal constructor(val symbolicName: String, val version: Version) {

	var singleton = false

	var uses = true

	var activator: String? = null

	private val instructions: Multimap<String, String> = MultimapBuilder.hashKeys()
		.linkedHashSetValues()
		.build()

	fun instructions(): Multimap<String, String> {
		return instructions
	}

	fun instruction(instruction: String, vararg values: String): ManifestSpec {
		if (Analyzer.BUNDLE_SYMBOLICNAME == instruction || Analyzer.BUNDLE_VERSION == instruction) {
			throw GradleException("Must use symbolicName and version attributes instead of corresponding instructions")
		}
		this.instructions.putAll(instruction, listOf(*values))
		return this
	}

	fun exports(vararg packs: String): ManifestSpec {
		return instruction(Analyzer.EXPORT_PACKAGE, *packs)
	}

	fun privates(vararg packs: String): ManifestSpec {
		val privates = packs.map { "!" + it }.toTypedArray()
		return instruction(Analyzer.EXPORT_PACKAGE, *privates)
	}

	fun optionals(vararg packs: String): ManifestSpec {
		val optionals = packs.map { it + ";resolution:=optional" }.toTypedArray()
		return instruction(Analyzer.IMPORT_PACKAGE, *optionals)
	}

	fun imports(vararg packs: String): ManifestSpec {
		return instruction(Analyzer.IMPORT_PACKAGE, *packs)
	}
}
