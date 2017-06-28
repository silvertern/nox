/**
 * Copyright (c) Profidata AG 2017
 */
package nox.core.platform;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import nox.core.BuildAPI;
import org.gradle.api.Project;

import java.io.File;
import java.util.Map;

public class PlatformExt {

	public static final String name = "platform";

	public static final String platformGroup = "platform";

	public static final String pluginsDir = "plugins";

	public static final String ivyMetadataDir = "ivy-metadata";

	private static final String targetPlatfromRootPropKey = "targetPlatformRoot";

	private static final PlatformExt platform = new PlatformExt();

	public static PlatformExt init(Project project) {
		Project rootProject = project.getRootProject();
		Preconditions.checkArgument(rootProject.getProperties().containsKey(targetPlatfromRootPropKey),
			"Root project must contain a 'targetPlatformRoot' property");
		platform.root = new File(String.valueOf(rootProject.getProperties().get(targetPlatfromRootPropKey)));
		platform.bundleMappings.clear();
		return platform;
	}

	public static PlatformExt get(Project project) {
		synchronized (platform) {
			if (platform.root == null) {
				init(project);
			}
		}
		return platform;
	}

	private File root = null;

	private final Map<String, String> bundleMappings = Maps.newConcurrentMap();

	private PlatformExt() {}

	@BuildAPI	public File root() {
		return platform.root;
	}

	@BuildAPI public PlatformExt mapping(String fromSymbolicName, String toSymbolicName) {
		bundleMappings.put(fromSymbolicName, toSymbolicName);
		return this;
	}

	public String map(String symbolicName) {
		return bundleMappings.getOrDefault(symbolicName, symbolicName);
	}

}
