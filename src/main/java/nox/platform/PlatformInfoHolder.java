/**
 * Copyright (c) Profidata AG 2017
 */
package nox.platform;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * PlatformInfoHolder as a root project extension holds platform relevant information
 * for the overall build in a singleton manner.
 */
public class PlatformInfoHolder {

	public static final String name = "platformInfoHolder";

	public final Map<String, String> bundleMappings = Maps.newConcurrentMap();
}
