/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT 
 */
package nox;

import java.io.File;
import java.util.Set;

import javax.inject.Inject;

import com.google.common.annotations.VisibleForTesting;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.jvm.tasks.Jar;

import nox.compile.osgi.OSGiJarManifest;
import nox.core.manifest.Classpath;


public class OSGi implements Plugin<Project> {

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

		project.getPluginManager().apply(JavaBasePlugin.class);
		Jar jarTask = (Jar) project.getTasks().getByName("jar");

		OSGiJarManifest ext = new OSGiJarManifest(provider.provide(project), project.getFileResolver());
		jarTask.setManifest(ext);
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
}
