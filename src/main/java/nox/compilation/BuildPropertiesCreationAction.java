/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT
 */
package nox.compilation;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.plugins.ExtensionContainerInternal;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.internal.tasks.TaskContainerInternal;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSetContainer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


public class BuildPropertiesCreationAction {

	private static final String BUILD_PROPERTIES = "build.properties";

	private final File projectDir;

	private final JavaPluginConvention javaConv;

	private final OSGiExt.BuildProperties buildProps;

	public BuildPropertiesCreationAction(Project target) {
		ProjectInternal project = (ProjectInternal) target;

		TaskContainerInternal tasks = project.getTasks();
		ExtensionContainerInternal ext = project.getExtensions();

		projectDir = project.getProjectDir();
		javaConv = project.getConvention().getPlugin(JavaPluginConvention.class);
		buildProps = ext.findByType(OSGiExt.class).buildProperties;

		tasks.getByName("jar").doFirst(task -> create());
		tasks.getByName("clean").doFirst(task -> clean());
	}

	private void create() {
		List<String> lines = Lists.newArrayList();
		List<String> javaSources = getSources(buildProps.sources, ss -> ss.getByName("main").getJava());
		javaSources.addAll(getSources(Lists.newArrayList(), ss -> ss.getByName("main").getResources()));
		if (!javaSources.isEmpty()) {
			lines.add("source.. = " + StringUtils.join(javaSources, ","));
		}
		List<String> bins = buildProps.binincludes.stream()
			.filter(path -> "META-INF/".equals(path) || new File(projectDir, path).exists())
			.collect(Collectors.toList());
		if (!bins.isEmpty()) {
			lines.add("bin.includes = " + StringUtils.join(bins, ","));
		}
		if (!javaSources.isEmpty()) {
			lines.add("output.. = " + (buildProps.outputs.isEmpty() ? "bin/" : StringUtils.join(buildProps.outputs, ",")));
		}
		for (Map.Entry<String, String> entry: buildProps.instructions.entrySet()) {
			lines.add(String.format("%s=%s", entry.getKey(), entry.getValue()));
		}

		try {
			Files.write(Paths.get(projectDir.getAbsolutePath(), BUILD_PROPERTIES), lines, Charset.forName("UTF-8"));
		} catch (IOException ex) {
			throw new GradleException("Failed to create build.properties for an OSGi bundle", ex);
		}
	}

	private List<String> getSources(List<String> preconfigured,  Function<SourceSetContainer, SourceDirectorySet> sourceExtractor) {
		if (!preconfigured.isEmpty()) {
			return preconfigured;
		}
		Set<File> sourceEntries = sourceExtractor.apply(javaConv.getSourceSets()).getSrcDirs();
		return sourceEntries.stream()
			.filter(se -> se.exists())
			.map(se -> {
				String element = se.getAbsolutePath().replace(projectDir.getAbsolutePath() + "/", "");
				return element + (!element.endsWith("/") ? "/" : "");
			})
			.collect(Collectors.toList());
	}

	private void clean() {
		new File(projectDir, BUILD_PROPERTIES).delete();
	}
}
