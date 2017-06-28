/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT 
 */
package nox.core.manifest;

import java.util.Arrays;
import java.util.Collection;

import com.google.common.collect.Sets;

import org.apache.commons.lang3.StringUtils;


public final class ManifestUtil {

	private ManifestUtil() {}

	public static String toSymbolicName(String groupId, String artifactId) {
		if (artifactId.startsWith(groupId)) {
			return artifactId;
		}
		String[] parts = (groupId + "." + artifactId).split("[\\.-]");
		Collection<String> elements = Sets.newLinkedHashSet(Arrays.asList(parts));
		return StringUtils.join(elements, ".");
	}
}
