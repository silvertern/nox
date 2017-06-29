/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT 
 */
package nox;

import nox.compilation.BuildPropertiesCreationAction;
import nox.compilation.ManifestUnpackingAction;
import nox.compilation.OSGiExt;
import nox.core.PlatformInfoHolder;
import nox.manifest.OsgiManifest;
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


public class OSGi implements Plugin<Project> {

	private static final String sourceSetName = "main";

	private static final String configName = "runtime";

	@Override
	public void apply(Project target) {
		ProjectInternal project = (ProjectInternal) target;

		project.getPluginManager().apply(JavaPlugin.class);

		TaskContainerInternal tasks = project.getTasks();
		ExtensionContainerInternal ext = project.getExtensions();

		Jar jarTask = (Jar) tasks.getByName("jar");
		OsgiManifest manifest = new OsgiManifest(
			() -> project.getConfigurations().getByName(configName).getFiles(),
			() -> {
				JavaPluginConvention javaConv = project.getConvention().getPlugin(JavaPluginConvention.class);
				return javaConv.getSourceSets().getByName(sourceSetName).getOutput().getClassesDirs().getSingleFile();
			},
			project.getFileResolver());
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
}
