/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT
 */
package nox.platform.gradlize

import nox.core.Version
import nox.core.Versioned
import nox.platform.gradlize.impl.parseExportedPackages
import nox.platform.gradlize.impl.parseRequirements
import java.util.jar.Manifest


class Bundle private constructor(name: String, version: Version, val exportedPackages: Set<ExportedPackage>, val importedPackages: List<Requirement>, val requiredBundles: List<Requirement>) : Versioned(name, version) {

	companion object {

		fun parse(manifest: Manifest): Bundle {
			val attrs = manifest.mainAttributes

			val nameString : String? = attrs.getValue("Bundle-SymbolicName")
			checkNotNull(nameString) { "Missing Bundle-SymbolicName in manifest %s".format(manifest) }
			val versionString = attrs.getValue("Bundle-Version")
			checkNotNull(versionString) { "Missing Bundle-Version in manifest %s".format(manifest) }

			val name = nameString!!.split(";".toRegex())[0].split(",".toRegex())[0].trim { it <= ' ' }.replace(";singleton:=true".toRegex(), "")
			val version = Version(versionString, true)

			val exportString : String? = attrs.getValue("Export-Package")
			val importString : String? = attrs.getValue("Import-Package")
			val requireString :String ? = attrs.getValue("Require-Bundle")

			val expPacks = parseExportedPackages(exportString, version)
			val impPacks = parseRequirements(importString)
			val reqBndls = parseRequirements(requireString)

			return Bundle(name, version, expPacks, impPacks, reqBndls)
		}
	}

	fun rename(newName: String): Bundle {
		return Bundle(newName, version, exportedPackages, importedPackages, requiredBundles)
	}
}
