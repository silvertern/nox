/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.core;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;


public abstract class Versioned implements Comparable<Versioned> {

	public final String name;

	public final Version version;

	protected Versioned(String name, Version version) {
		Preconditions.checkNotNull(name, "Name is required");
		Preconditions.checkNotNull(version, "Version is required");
		this.name = name;
		this.version = version;
	}

	protected Versioned(String name) {
		this(name, Version.DEFAULT);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Versioned versioned = (Versioned) o;
		return Objects.equal(name, versioned.name) && Objects.equal(version, versioned.version);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(name, version);
	}

	@Override
	public int compareTo(Versioned o) {
		int diff = name.compareTo(o.name);
		if (diff != 0) {
			return diff;
		}
		return version.compareTo(o.version);
	}

	@Override
	public String toString() {
		return String.format("%s:%s", name, version);
	}
}
