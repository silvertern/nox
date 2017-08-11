/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT
 */
package nox.platform.gradlize

import nox.platform.gradlize.impl.MetadataExporterIvyImpl
import java.io.File
import java.io.IOException


interface MetadataExporter {

	@Throws(IOException::class)
	fun exportTo(targetDir: File)

	companion object {

		fun instance(bundle: Bundle, org: String, dependencies: Collection<Dependency>): MetadataExporter {
			return MetadataExporterIvyImpl(bundle, org, dependencies)
		}
	}
}
