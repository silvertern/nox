/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT 
 */
package nox;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import javax.inject.Inject;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.internal.tasks.TaskContainerInternal;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.Copy;
import org.gradle.jvm.tasks.Jar;
import org.gradle.language.jvm.tasks.ProcessResources;

import nox.compile.BuildProperties;
import nox.compile.osgi.OSGiJarManifest;
import nox.core.manifest.Classpath;


public class OSGi implements Plugin<Project> {

	/**
	 * Add osgiUnpackManifest=false to gradle.properties to prevent copying
	 */
	private static final String unpackOSGiManifest = "unpackOSGiManifest";

	private static final String sourceSetName = "main";

	private static final String configName = "runtime";

	@VisibleForTesting
	interface ClasspathProvider {
		Classpath provide(ProjectInternal project);
	}

	private final ClasspathProvider provider;

	@Inject
	public OSGi() {
		provider = ProjectClasspath::new;
	}

	@VisibleForTesting
	OSGi(ClasspathProvider provider) {
		this.provider = provider;
	}

	@Override
	public void apply(Project target) {
		ProjectInternal project = (ProjectInternal) target;

		project.getPluginManager().apply(JavaPlugin.class);

		TaskContainerInternal tasks = project.getTasks();
		ProcessResources procRes = (ProcessResources) tasks.getByName("processResources");
		Jar jarTask = (Jar) tasks.getByName("jar");
		Task buildTask = tasks.getByName("build");
		Task cleanTask = tasks.getByName("clean");

		jarTask.setManifest(new OSGiJarManifest(provider.provide(project), project.getFileResolver()));

		File projectDir = project.getProjectDir().getAbsoluteFile();

		if (boolProperty(project, unpackOSGiManifest, true)) {
			buildTask.doLast(task -> unpackOSGiManifest(projectDir, jarTask));
			cleanTask.doLast(task -> Paths.get(projectDir.getAbsolutePath(), "META-INF","MANIFEST.MF").toFile().delete());
		}

		BuildProperties buildpropsTask = tasks.create(BuildProperties.name, BuildProperties.class);
		buildpropsTask.onlyIf(task -> !jarTask.getState().getUpToDate());

		Copy copyBinIncludes = tasks.create("copyBinIncludes", Copy.class);
		procRes.dependsOn(copyBinIncludes);
		copyBinIncludes.dependsOn(buildpropsTask);

		project.afterEvaluate(p -> copyBinIncludes.from(p.getProjectDir())
			.into(procRes.getDestinationDir())
			.include(buildpropsTask.binincludes.toArray(new String[]{}))
			.exclude("**/MANIFEST.MF", "**/.gitkeep"));
	}

	static class ProjectClasspath implements Classpath {

		private final ProjectInternal project;

		ProjectClasspath(ProjectInternal project) {
			this.project = project;
		}

		@Override
		public Set<File> classPath() {
			return project.getConfigurations().getByName(configName).getFiles();
		}

		@Override
		public File jarFile() {
			JavaPluginConvention javaConv = project.getConvention().getPlugin(JavaPluginConvention.class);
			return javaConv.getSourceSets().getByName(sourceSetName).getOutput().getClassesDirs().getSingleFile();
		}
	}




	private void unpackOSGiManifest(File projectDir, Jar jarTask) {
		// ignore failure here, will throw below
		new File(projectDir, "META-INF").mkdirs();
		URI jarUri = URI.create("jar:" + jarTask.getArchivePath().toURI());
		try (FileSystem jarfs = FileSystems.newFileSystem(
			jarUri, Maps.newHashMap())) {
			Path source = jarfs.getPath("META-INF", "MANIFEST.MF");
			Path target = Paths.get(projectDir.getAbsolutePath(), "META-INF", "MANIFEST.MF");
			Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new GradleException("Failed to copy MANIFEST.MF out of the jar");
		}
	}

	private boolean boolProperty(Project project, String name, boolean def) {
		if (!project.getProperties().containsKey(name)) {
			return def;
		}
		return Boolean.valueOf(String.valueOf(project.getProperties().get(name))).booleanValue();
	}


}
