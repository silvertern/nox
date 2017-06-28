/**
 * Copyright (c) Profidata AG 2017
 */
package nox.compile;

import com.google.common.collect.Maps;
import org.gradle.api.AntBuilder;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class Ajc extends DefaultTask {

	private static final Logger logger = LoggerFactory.getLogger(Ajc.class);

	public SourceSet sourceSet;

	public FileCollection aspectpath;

	public FileCollection ajInpath;

	// ignore or warning
	public String xlint = "ignore";

	public String maxmem;

	public Map<String, String> additionalAjcArgs = Maps.newHashMap();

	public Ajc() {
		getLogging().captureStandardOutput(LogLevel.INFO);
	}

	@TaskAction
	public void compile() {
		logger.info("Compining AspectJ. Classpath: {}, srcDirs: {}",
				sourceSet.getCompileClasspath().getAsPath(),
				sourceSet.getJava().getSrcDirs());

		AntBuilder ant = getAnt();

		Map<String, Object> taskdefArgs = Maps.newHashMap();
		taskdefArgs.put("resource", "org/aspectj/tools/ant/taskdefs/aspectjTaskdefs.properties");
		taskdefArgs.put("classpath", getProject().getConfigurations().getByName("ajtools").getAsPath());

		ant.invokeMethod("taskdef", taskdefArgs);

		JavaPluginConvention javaConv = (JavaPluginConvention) getProject().getConvention().getPlugins().get("java");

		Map<String, Object> ajcArgs = Maps.newHashMap();
		ajcArgs.put("classpath", sourceSet.getCompileClasspath().getAsPath());
		ajcArgs.put("destDir", sourceSet.getOutput().getClassesDir().getAbsolutePath());
		ajcArgs.put("s", sourceSet.getOutput().getClassesDir().getAbsolutePath());
		ajcArgs.put("source", javaConv.getSourceCompatibility());
		ajcArgs.put("target", javaConv.getTargetCompatibility());
		ajcArgs.put("inpath", ajInpath.getAsPath());
		ajcArgs.put("xlint", xlint);
		ajcArgs.put("fork", "true");
		ajcArgs.put("aspectPath", aspectpath.getAsPath());
		ajcArgs.put("sourceRootCopyFilter", "**/*.java,**/*.aj");
		ajcArgs.put("showWeaveInfo", "true");
		ajcArgs.put("sourceRoots", sourceSet.getJava().getSourceDirectories().getAsPath());
		if (maxmem != null) {
			ajcArgs.put("maxmem", maxmem);
		}
		ajcArgs.putAll(additionalAjcArgs);

		ant.invokeMethod("iajc", ajcArgs);
	}
}
