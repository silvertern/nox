/**
 * Created by skol on 27.02.17.
 */
package nox.core;

import java.util.List;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.apache.commons.lang3.StringUtils;


public class Version implements Comparable<Version> {

	public enum Component {
		Major, Minor, Build, Suffix
	}

	public static final Version MIN = new Version(0, 0, 0);

	public static final Version DEFAULT = new Version(0, 1, 0);

	public static final Version MAX = new Version(Long.MAX_VALUE, 0, 0);

	public final long major;

	public final long minor;

	public final long build;

	public final String suffix;

	public boolean shortVersion = false;

	public Version(long major, long minor, long build, String suffix) {
		this.major = major;
		this.minor = minor;
		this.build = build;
		this.suffix = suffix;
	}

	public Version(long major, long minor, long build) {
		this(major, minor, build, null);
	}

	public Version(String versionString, boolean withSuffix) {
		Preconditions.checkNotNull(versionString, "Version string required");
		String[] parts = versionString.trim().split("(\\.|-|_)");
		if (parts.length < 1) {
			throw new IllegalArgumentException("Major version is required");
		}
		major = Long.valueOf(parts[0]).longValue();
		long minorno = 0;
		if (parts.length > 1) {
			try {
				minorno = Long.valueOf(parts[1]).longValue();
			} catch (NumberFormatException ex) {
				// ignore
			}
		}
		minor = minorno;
		long buildno = 0;
		if (parts.length > 2) {
			try {
				buildno = Long.valueOf(parts[2]).longValue();
			} catch (NumberFormatException ex) {
				// ignore
			}
		} else {
			shortVersion = true;
		}
		build = buildno;
		if (parts.length == 4 && withSuffix) {
			suffix = parts[3];
		} else if (parts.length > 4 && withSuffix) {
			List<String> suffixParts = Lists.newArrayList(parts).subList(3, parts.length);
			suffix = StringUtils.join(suffixParts, "-");
		} else {
			suffix = null;
		}
	}

	public Version(String versionString) {
		this(versionString, true);
	}

	public Version nextMajor() {
		return new Version(major + 1, 0, 0);
	}

	public Version nextMinor() {
		return new Version(major, minor + 1, 0);
	}

	@Override
	public String toString() {
		if (shortVersion) {
			return String.format("%d.%d", Long.valueOf(major), Long.valueOf(minor));
		}
		String res = String.format("%d.%d.%d", Long.valueOf(major), Long.valueOf(minor), Long.valueOf(build));
		if (StringUtils.isNotBlank(suffix)) {
			res += "." + suffix;
		}
		return res;
	}

	public String toString(Component component) {
		switch (component) {
			case Major:
				return String.format("%d", Long.valueOf(major));
			case Build:
				if (!shortVersion) {
					return String.format("%d.%d.%d", Long.valueOf(major), Long.valueOf(minor),
						Long.valueOf(build));
				}
				// fall through to minor
			case Minor:
				return String.format("%d.%d", Long.valueOf(major), Long.valueOf(minor));
		}
		return toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Version version = (Version) o;
		return major == version.major && minor == version.minor && build == version.build;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(Long.valueOf(major), Long.valueOf(minor), Long.valueOf(build));
	}

	@Override
	public int compareTo(Version o) {
		int diff = Long.compare(major, o.major);
		if (diff != 0) {
			return diff;
		}
		diff = Long.compare(minor, o.minor);
		if (diff != 0) {
			return diff;
		}
		return Long.compare(build, o.build);
	}

	// expects at least 1 value, otherwise AIOBE or NPE
	public static Version min(Version... versions) {
		Version min = versions[0];
		for (Version version : versions) {
			if (version.compareTo(min) < 0) {
				min = version;
			}
		}
		return min;
	}

	// expects at least 1 value, otherwise AIOBE or NPE
	public static Version max(Version... versions) {
		Version max = versions[0];
		for (Version version : versions) {
			if (version.compareTo(max) > 0) {
				max = version;
			}
		}
		return max;
	}
}
