/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox.compile;

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import nox.core.BuildAPI;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class BuildProperties extends DefaultTask {

	public static final String name = BuildProperties.class.getSimpleName().toLowerCase();

	@BuildAPI	public final List<String> sources = Lists.newArrayList();

	@BuildAPI	public final List<String> outputs = Lists.newArrayList();

	@BuildAPI	public final Set<String> binincludes = Sets.newLinkedHashSet();

	@BuildAPI	public final Map<String, String> instructions = Maps.newLinkedHashMap();

	@BuildAPI	public void sources(String... sources) {
		this.sources.addAll(Arrays.asList(sources));
	}

	@BuildAPI	public void binincludes(String... binincludes) {
		this.binincludes.addAll(Arrays.asList(binincludes));
	}

	@BuildAPI	public void outputs(String... outputs) {
		this.outputs.addAll(Arrays.asList(outputs));
	}

	public void instruction(String key, String value) {
		this.instructions.put(key, value);
	}

	public BuildProperties() {
		binincludes.add("META-INF/");
	}

	@TaskAction
	public void action() {
		List<String> lines = Lists.newArrayList();
		List<String> javaSources = getSources(sources, ss -> ss.getByName("main").getJava());
		javaSources.addAll(getSources(Lists.newArrayList(), ss -> ss.getByName("main").getResources()));
		if (!javaSources.isEmpty()) {
			lines.add("source.. = " + StringUtils.join(javaSources, ","));
		}
		Collection<String> bins = Collections2.filter(Sets.newLinkedHashSet(binincludes), path ->
			"META-INF/".equals(path) || new File(getProject().getProjectDir(), path).exists());
		if (!bins.isEmpty()) {
			lines.add("bin.includes = " + StringUtils.join(bins, ","));
		}
		if (!javaSources.isEmpty()) {
			lines.add("output.. = " + (outputs.isEmpty() ? "bin/" : StringUtils.join(outputs, ",")));
		}
		for (Map.Entry<String, String> entry: instructions.entrySet()) {
			lines.add(String.format("%s=%s", entry.getKey(), entry.getValue()));
		}

		try {
			Files.write(Paths.get(getProject().getProjectDir().getAbsolutePath(), "build.properties"), lines, Charset.forName("UTF-8"));
		} catch (IOException ex) {
			throw new GradleException("Failed to create build.properties for an OSGi bundle", ex);
		}
	}

	interface SourceExtractor {
		SourceDirectorySet extract(SourceSetContainer sourceSets);
	}

	private List<String> getSources(Collection<String> preconfigured,  SourceExtractor sourceExtractor) {
		List<String> src = Lists.newArrayList(preconfigured);
		if (src.isEmpty()) {
			File projectDir = getProject().getProjectDir();
			SourceSetContainer sourceSets = getProject().getConvention()
				.getPlugin(JavaPluginConvention.class)
				.getSourceSets();
			for (File sourceEntry: sourceExtractor.extract(sourceSets).getSrcDirs()) {
				if (sourceEntry.exists()) {
					String element = sourceEntry.getAbsolutePath().replace(projectDir.getAbsolutePath() + "/", "");
					if (!element.endsWith("/")) {
						element += "/";
					}
					src.add(element);
				}
			}
		}
		return src;
	}

	@BuildAPI
	public void clean() {
		new File(getProject().getProjectDir(), "build.properties").delete();
	}
}
