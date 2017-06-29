/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT 
 */
package nox;

import com.google.common.annotations.VisibleForTesting;
import nox.compile.OSGiJarManifest;
import nox.core.manifest.Classpath;
import nox.compile.BuildPropertiesCreationAction;
import nox.compile.ManifestUnpackingAction;
import nox.compile.OSGiExt;
import nox.platform.PlatformInfoHolder;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.internal.plugins.ExtensionContainerInternal;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.internal.tasks.TaskContainerInternal;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.Copy;
import org.gradle.jvm.tasks.Jar;
import org.gradle.language.jvm.tasks.ProcessResources;

import javax.inject.Inject;
import java.io.File;
import java.util.Set;


public class OSGi implements Plugin<Project> {

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
		ExtensionContainerInternal ext = project.getExtensions();

		Jar jarTask = (Jar) tasks.getByName("jar");
		OSGiJarManifest manifest = new OSGiJarManifest(provider.provide(project), project.getFileResolver());
		jarTask.setManifest(manifest);

		PlatformInfoHolder infoHolder = project.getRootProject().getExtensions().findByType(PlatformInfoHolder.class);
		ext.create(OSGiExt.name, OSGiExt.class, project, infoHolder);

		project.afterEvaluate(ManifestUnpackingAction::new);
		project.afterEvaluate(BuildPropertiesCreationAction::new);

		Task procBinincludes = tasks.create("processBinincludes", Copy.class);
		Task procRes = tasks.getByName("processResources");
		procRes.dependsOn(procBinincludes);

		project.afterEvaluate(this::configureProcessBinincludes);
	}

	private void configureProcessBinincludes(Project target) {
		ProjectInternal project = (ProjectInternal) target;

		ExtensionContainerInternal ext = project.getExtensions();
		OSGiExt.BuildProperties buildProps = ext.findByType(OSGiExt.class).buildProperties;

		TaskContainerInternal tasks = project.getTasks();
		ProcessResources procRes = (ProcessResources) tasks.getByName("processResources");
		Copy procBinincludes = (Copy) tasks.getByName("processBinincludes");

		procBinincludes.from(project.getProjectDir())
			.into(procRes.getDestinationDir())
			.include(buildProps.binincludes.toArray(new String[]{}))
			.exclude("**/MANIFEST.MF", "**/.gitkeep");
	}

	static class ProjectClasspath implements Classpath {

		private static final String sourceSetName = "main";

		private static final String configName = "runtime";

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

}
