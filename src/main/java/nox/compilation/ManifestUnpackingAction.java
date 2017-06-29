/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT
 */
package nox.compilation;

import com.google.common.collect.Maps;
import nox.manifest.OsgiManifest;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.internal.plugins.ExtensionContainerInternal;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.internal.tasks.TaskContainerInternal;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.jvm.tasks.Jar;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class ManifestUnpackingAction {

	/**
	 * Add osgiUnpackManifest=false to gradle.properties to prevent copying
	 */
	private static final String unpackOSGiManifest = "unpackOSGiManifest";

	private final Path manifestFile;

	public ManifestUnpackingAction(Project target) {
		ProjectInternal project = (ProjectInternal) target;
		this.manifestFile = Paths.get(project.getProjectDir().getAbsolutePath(), "META-INF", "MANIFEST.MF");

		TaskContainerInternal tasks = project.getTasks();
		ExtensionContainerInternal ext = project.getExtensions();

		OSGiExt platform = ext.findByType(OSGiExt.class);
		Jar jarTask = (Jar) tasks.getByName("jar");

		// extension value has precedency over ext; default=unpack
		if (((OsgiManifest) jarTask.getManifest()).from != null) {
			return;
		} else if (platform.unpackOSGiManifest != null) {
			if (!platform.unpackOSGiManifest.booleanValue()) {
				return;
			}
		} else {
			ExtraPropertiesExtension extProps = ext.getExtraProperties();
			if (extProps.has(unpackOSGiManifest)) {
				if (!Boolean.valueOf(String.valueOf(extProps.get(unpackOSGiManifest))).booleanValue()) {
					return;
				}
			}
		}

		Task buildTask = tasks.getByName("build");
		Task cleanTask = tasks.getByName("clean");

		buildTask.doLast(task -> unpack(jarTask));
		cleanTask.doLast(task -> clean());
	}

	private void unpack(Jar jarTask) {
		// ignore failure here, will throw below
		manifestFile.getParent().toFile().mkdirs();
		URI jarUri = URI.create("jar:" + jarTask.getArchivePath().toURI());
		try (FileSystem jarfs = FileSystems.newFileSystem(
			jarUri, Maps.newHashMap())) {
			Path source = jarfs.getPath("META-INF", "MANIFEST.MF");
			Files.copy(source, manifestFile, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new GradleException("Failed to copy MANIFEST.MF out of the jar");
		}
	}

	private void clean() {
		manifestFile.toFile().delete();
	}
}
