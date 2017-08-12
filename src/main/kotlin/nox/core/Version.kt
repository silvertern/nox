/**
 * Created by skol on 27.02.17.
 */
package nox.core


class Version : Comparable<Version> {

	enum class Component {
		Major, Minor, Build, Suffix
	}

	val major: Long

	val minor: Long

	val build: Long

	val suffix: String?

	var shortVersion = false

	@JvmOverloads constructor(major: Long, minor: Long, build: Long, suffix: String? = null) {
		this.major = major
		this.minor = minor
		this.build = build
		this.suffix = suffix
	}

	@JvmOverloads constructor(versionString: String, withSuffix: Boolean = true) {
		val parts = versionString.trim { it <= ' ' }.split("(\\.|-|_)".toRegex())
		if (parts.isEmpty()) {
			throw IllegalArgumentException("Major version is required")
		}
		major = java.lang.Long.valueOf(parts[0])!!.toLong()
		var minorno: Long = 0
		if (parts.size > 1) {
			try {
				minorno = java.lang.Long.valueOf(parts[1])!!.toLong()
			} catch (ex: NumberFormatException) {
				// ignore
			}
		}
		minor = minorno
		var buildno: Long = 0
		if (parts.size > 2) {
			try {
				buildno = java.lang.Long.valueOf(parts[2])!!.toLong()
			} catch (ex: NumberFormatException) {
				// ignore
			}

		} else {
			shortVersion = true
		}
		build = buildno
		if (parts.size == 4 && withSuffix) {
			suffix = parts[3]
		} else if (parts.size > 4 && withSuffix) {
			val suffixParts = ArrayList(parts).subList(3, parts.size)
			suffix = suffixParts.joinToString("-")
		} else {
			suffix = null
		}
	}

	fun nextMajor(): Version {
		return Version(major + 1, 0, 0)
	}

	fun nextMinor(): Version {
		return Version(major, minor + 1, 0)
	}

	override fun toString(): String {
		if (shortVersion) {
			return "%d.%d".format(major, minor)
		}
		var res = "%d.%d.%d".format(major, minor, build)
		if (!suffix.isNullOrBlank()) {
			res += "." + suffix!!
		}
		return res
	}

	fun toString(component: Component): String {
		when (component) {
			Version.Component.Major -> return "%d".format(major)
			Version.Component.Build -> {
				if (!shortVersion) {
					return "%d.%d.%d".format(major, minor, build)
				}
				return "%d.%d".format(major, minor)
			}
			Version.Component.Minor -> return "%d.%d".format(major, minor)
			else -> return toString()
		}
	}

	override fun compareTo(other: Version): Int {
		var diff = java.lang.Long.compare(major, other.major)
		if (diff != 0) {
			return diff
		}
		diff = java.lang.Long.compare(minor, other.minor)
		if (diff != 0) {
			return diff
		}
		return java.lang.Long.compare(build, other.build)
	}

	companion object {

		val MIN = Version(0, 0, 0)

		val DEFAULT = Version(0, 1, 0)

		val MAX = Version(java.lang.Long.MAX_VALUE, 0, 0)
	}
}
