/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT
 */
package nox.platform.gradlize.impl

import nox.core.Version
import nox.platform.gradlize.ExportedPackage
import nox.platform.gradlize.Requirement
import java.util.regex.Pattern

internal fun parseExportedPackages(exportString: String?, bundleVersion: Version): Set<ExportedPackage> {
	if (exportString == null || exportString.isBlank()) {
		return emptySet()
	}
	val res = mutableSetOf<ExportedPackage>()
	parseMfLine(exportString).forEach { key, value ->
		val version = value["version"]
		if (version != null) {
			res.add(ExportedPackage(key, Version(version, false)))
		} else {
			res.add(ExportedPackage(key, bundleVersion))
		}
	}
	return res
}

private val MF_KV_PATTERN = Pattern.compile("^(.+?):?=(.+)$")


internal fun parseRequirements(requireString: String?): List<Requirement> {
	if (requireString == null || requireString.isBlank()) {
		return emptyList()
	}
	val res = mutableListOf<Requirement>()
	parseMfLine(requireString).forEach { name, value ->
		val optional = "optional".equals(value["resolution"], ignoreCase = true)
		var from = Version.MIN
		var to = Version.MAX
		val versionString = value["version"] ?: value["bundle-version"]
		if (!versionString.isNullOrBlank()) {
			val matcher = MF_VERS_PATTERN.matcher(versionString)
			if (matcher.find()) {
				from = Version(matcher.group(1), false)
				to = Version(matcher.group(2), false)
				if (to == from) {
					to = from.nextMajor()
				}
			} else {
				from = Version(versionString!!, false)
				to = from.nextMajor()
			}
		}
		res.add(Requirement(name, from, to, optional))
	}
	return res
}

private val MF_VERS_PATTERN = Pattern.compile("^\\[(.+?),(.+?)(\\)|])$")

internal fun parseMfLine(line: String): Map<String, Map<String, String>> {
	val res = LinkedHashMap<String, Map<String, String>>()
	var inQuotation = false
	var start = 0
	var end = 0
	while (end < line.length) {
		val c = line[end]
		if (c == '"') {
			inQuotation = !inQuotation
		}
		if (!inQuotation && end > start + 1 && (c == ',' || end == line.length - 1)) {
			val parts = line.substring(start, if (c == ',') end else end + 1).split(";".toRegex()).toMutableList()
			start = end + 1
			val elements = mutableMapOf<String, String>()
			res.put(parts.removeAt(0).trim { it <= ' ' }, elements)
			for (part in parts) {
				val matcher = MF_KV_PATTERN.matcher(part)
				if (matcher.find()) {
					val key = matcher.group(1).trim { it <= ' ' }
					var value = matcher.group(2).trim { it <= ' ' }
					if (value.startsWith("\"") && value.endsWith("\"")) {
						value = value.substring(1, value.length - 1)
					}
					elements.put(key, value)
				}
			}
		}
		end++
	}
	return res
}
