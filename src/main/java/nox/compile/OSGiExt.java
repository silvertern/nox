/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT 
 */
package nox.compile;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import groovy.lang.Closure;
import nox.Platform;
import nox.platform.PlatformInfoHolder;
import org.gradle.api.artifacts.ClientModule;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.IvyArtifactRepository;
import org.gradle.api.internal.artifacts.BaseRepositoryFactory;
import org.gradle.api.internal.artifacts.dependencies.DefaultClientModule;
import org.gradle.api.internal.artifacts.repositories.layout.DefaultIvyPatternRepositoryLayout;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.util.ConfigureUtil;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OSGiExt {

	public static final String name = "osgi";

	public static final String group = "plugins";

	private final PlatformInfoHolder holder;

	private final BaseRepositoryFactory repoFactory;

	public static class BuildProperties {

		public final List<String> sources = Lists.newArrayList();

		public final List<String> outputs = Lists.newArrayList();

		public final Set<String> binincludes = Sets.newLinkedHashSet(Arrays.asList("META-INF/"));

		public final Map<String, String> instructions = Maps.newLinkedHashMap();

		public void sources(String... sources) {
			this.sources.addAll(Arrays.asList(sources));
		}

		public void binincludes(String... binincludes) {
			this.binincludes.addAll(Arrays.asList(binincludes));
		}

		public void outputs(String... outputs) {
			this.outputs.addAll(Arrays.asList(outputs));
		}

		public void instruction(String key, String value) {
			this.instructions.put(key, value);
		}
	}

	public Boolean unpackOSGiManifest = null;

	public final BuildProperties buildProperties = new BuildProperties();

	public OSGiExt(ProjectInternal project, PlatformInfoHolder holder) {
		Preconditions.checkNotNull(holder,
			"Developer error: PlatformInfoHolder must have been initialized by nox.Platform");
		this.holder = holder;
		this.repoFactory = project.getServices().get(BaseRepositoryFactory.class);
	}

	public OSGiExt map(String fromSymbolicName, String toSymbolicName) {
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

	public void buildProperties(Closure<?> config) {
		ConfigureUtil.configure(config, buildProperties);
	}
}
