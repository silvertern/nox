/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT 
 */
package nox.platform;

import com.google.common.base.Preconditions;
import nox.Platform;
import org.gradle.api.artifacts.ClientModule;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.IvyArtifactRepository;
import org.gradle.api.internal.artifacts.BaseRepositoryFactory;
import org.gradle.api.internal.artifacts.dependencies.DefaultClientModule;
import org.gradle.api.internal.artifacts.repositories.layout.DefaultIvyPatternRepositoryLayout;
import org.gradle.api.internal.project.ProjectInternal;

import java.io.File;
import java.nio.file.Path;

public class PlatformExt {

	public static final String name = "platform";

	public static final String group = "plugins";

	private final PlatformInfoHolder holder;
	private final BaseRepositoryFactory repoFactory;

	public PlatformExt(ProjectInternal project, PlatformInfoHolder holder) {
		Preconditions.checkNotNull(holder,
			"Developer error: PlatformInfoHolder must have been initialized by nox.Platform");
		this.holder = holder;
		this.repoFactory = project.getServices().get(BaseRepositoryFactory.class);
	}

	public PlatformExt map(String fromSymbolicName, String toSymbolicName) {
		holder.bundleMappings.put(fromSymbolicName, toSymbolicName);
		return this;
	}

	public ClientModule bundle(String symbolicName, String version) {
		return new DefaultClientModule(group, holder.bundleMappings.getOrDefault(symbolicName, symbolicName), version);
	}

	public ArtifactRepository repo(String name, Object file) {
		File root;
		if (file instanceof File) {
			root = (File) file;
		} else if (file instanceof Path) {
			root = ((Path) file).toFile();
		} else {
			root = new File(String.valueOf(file));
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
