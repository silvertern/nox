/**
 * Copyright (c): Java port of https://github.com/eveoh/gradle-aspectj
 */
package nox;

import nox.compile.AjcTask;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskContainer;


public class AspectJ implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		project.getPluginManager().apply(JavaBasePlugin.class);

		if (!project.hasProperty("aspectjVersion")) {
			throw new GradleException("You must set the property 'aspectjVersion' before applying the aspectj plugin");
		}

		ConfigurationContainer confs = project.getConfigurations();
		TaskContainer tasks = project.getTasks();

		if (confs.findByName("ajtools") == null) {
			Object aspectjVersion = project.getProperties().get("aspectjVersion");
			confs.create("ajtools");
			project.getDependencies().add("ajtools", "org.aspectj:aspectjtools:" + aspectjVersion);
			project.getDependencies().add("compile", "org.aspectj:aspectjrt:" + aspectjVersion);
		}

		JavaPluginConvention javaConv = (JavaPluginConvention) project.getConvention().getPlugins().get("java");


		for (SourceSet projectSourceSet: javaConv.getSourceSets()) {
			NamingConventions namingConventions = projectSourceSet.getName().equals("main") ? new MainNamingConventions() : new DefaultNamingConventions();
			String aspectPathConfName = namingConventions.getAspectPathConfigurationName(projectSourceSet);
			String aspectInpathConfName = namingConventions.getAspectInpathConfigurationName(projectSourceSet);
			for (String configuration : new String[]{aspectPathConfName, aspectInpathConfName}) {
				if (confs.findByName(configuration) == null) {
					confs.create(configuration);
				}
			}

			if (!projectSourceSet.getAllJava().isEmpty()) {
				String aspectTaskName = namingConventions.getAspectCompileTaskName(projectSourceSet);
				Task javaTask = tasks.getByName(namingConventions.getJavaCompileTaskName(projectSourceSet));

				AjcTask ajc = tasks.create(aspectTaskName, AjcTask.class);
				// aspectTaskArgs.put("overwrite", true);
				ajc.setDescription("Compiles AspectJ Source for " + projectSourceSet.getName() + " source set");
				ajc.sourceSet = projectSourceSet;
				ajc.getInputs().files(projectSourceSet.getAllJava());
				ajc.getOutputs().dir(projectSourceSet.getOutput().getClassesDir());
				ajc.aspectpath = confs.findByName(aspectPathConfName);
				ajc.ajInpath = confs.findByName(aspectInpathConfName);

				ajc.setDependsOn(javaTask.getDependsOn());
				ajc.dependsOn(ajc.aspectpath);
				ajc.dependsOn(ajc.ajInpath);
				javaTask.deleteAllActions();
				javaTask.dependsOn(ajc);
			}
		}
	}

	private interface NamingConventions {

		String getJavaCompileTaskName(SourceSet sourceSet);

		String getAspectCompileTaskName(SourceSet sourceSet);

		String getAspectPathConfigurationName(SourceSet sourceSet);

		String getAspectInpathConfigurationName(SourceSet sourceSet);
	}

	private static class MainNamingConventions implements NamingConventions {

		@Override
		public String getJavaCompileTaskName(final SourceSet sourceSet) {
			return "compileJava";
		}

		@Override
		public String getAspectCompileTaskName(final SourceSet sourceSet) {
			return "compileAspect";
		}

		@Override
		public String getAspectPathConfigurationName(final SourceSet sourceSet) {
			return "aspectpath";
		}

		@Override
		public String getAspectInpathConfigurationName(final SourceSet sourceSet) {
			return "ajInpath";
		}
	}

	private static class DefaultNamingConventions implements NamingConventions {

		@Override
		public String getJavaCompileTaskName(final SourceSet sourceSet) {
			return "compile" + capitalize(sourceSet.getName()) + "Java";
		}

		@Override
		public String getAspectCompileTaskName(final SourceSet sourceSet) {
			return "compile" + capitalize(sourceSet.getName()) + "Aspect";
		}

		@Override
		public String getAspectPathConfigurationName(final SourceSet sourceSet) {
			return sourceSet.getName() + "Aspectpath";
		}

		@Override
		public String getAspectInpathConfigurationName(final SourceSet sourceSet) {
			return sourceSet.getName() + "AjInpath";
		}

		private String capitalize(String name) {
			return name.substring(0, 1).toUpperCase() + name.substring(1);
		}
	}


}
