/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT 
 */
package nox.platform;

import java.io.File;
import java.nio.file.Path;

import com.google.common.base.Preconditions;

import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.IvyArtifactRepository;
import org.gradle.api.internal.artifacts.BaseRepositoryFactory;
import org.gradle.api.internal.artifacts.repositories.layout.DefaultIvyPatternRepositoryLayout;
import org.gradle.api.internal.project.ProjectInternal;

import groovy.lang.Closure;
import nox.Platform;


public class PlatformRepoMethod extends Closure<ArtifactRepository> {

	public static final String methodName = "platformRepo";

	private final BaseRepositoryFactory repoFactory;

	public PlatformRepoMethod(ProjectInternal project) {
		super(project);
		repoFactory = project.getServices().get(BaseRepositoryFactory.class);
	}

	@Override
	public ArtifactRepository call(Object... args) {
		Preconditions.checkArgument(args.length == 2, "Expected target platform name and root");
		String name = String.valueOf(args[0]);
		File root;
		if (args[1] instanceof File) {
			root = (File) args[1];
		} else if (args[1] instanceof Path) {
			root = ((Path) args[1]).toFile();
		} else {
			root = new File(String.valueOf(args[1]));
		}
		IvyArtifactRepository repo = repoFactory.createIvyRepository();
		repo.setName(name);
		repo.setUrl(root);
		repo.layout("pattern", layout -> {
			DefaultIvyPatternRepositoryLayout ivyLayout = (DefaultIvyPatternRepositoryLayout) layout;
			ivyLayout.artifact(
				String.format("%s/[module](.[classifier])_[revision].[ext]", Platform.PLUGINS));
			ivyLayout.artifact(
				String.format("%s/[module](.[classifier])_[revision]", Platform.PLUGINS));
			ivyLayout.artifact(
				String.format("%s/[module](.[classifier])-[revision].[ext]", Platform.PLUGINS));
			ivyLayout.artifact(
				String.format("%s/[module](.[classifier])-[revision]", Platform.PLUGINS));
			ivyLayout.ivy(
				String.format("%s/[module](.[classifier])_[revision].[ext]", Platform.IVY_METADATA));
		});
		return repo;
	}
}
