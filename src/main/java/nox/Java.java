/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox;

import com.google.common.base.Preconditions;
import groovy.lang.Closure;
import nox.core.platform.PlatformExt;
import nox.core.sys.Arch;
import nox.core.sys.OS;
import nox.core.sys.Win;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ClientModule;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.IvyArtifactRepository;
import org.gradle.api.internal.artifacts.dependencies.DefaultClientModule;
import org.gradle.api.internal.artifacts.repositories.layout.DefaultIvyPatternRepositoryLayout;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.plugins.JavaPlugin;


public class Java implements Plugin<Project> {

	private static final String METHOD_NAME = "bundle";

	@Override
	public void apply(Project target) {

		ProjectInternal project = (ProjectInternal) target;
		project.getPluginManager().apply(JavaPlugin.class);

		ExtensionContainer extensions = project.getExtensions();
		RepositoryHandler repositories = project.getRepositories();

		PlatformExt platform = PlatformExt.get(project);

		ExtraPropertiesExtension extProps = extensions.getExtraProperties();
		extProps.set(METHOD_NAME, new BundleMethod(platform, project));
		extProps.set("p2os", OS.current().toString());
		extProps.set("p2ws", Win.current().toString());
		extProps.set("p2arch", Arch.current().toString());

		IvyArtifactRepository repository = repositories.ivy(repo -> {
			repo.setName("plugins");
			repo.setUrl(platform.root());
			repo.layout("pattern", layout -> {
				DefaultIvyPatternRepositoryLayout ivyLayout = (DefaultIvyPatternRepositoryLayout) layout;
				ivyLayout.artifact(
					String.format("%s/[module](.[classifier])_[revision].[ext]", PlatformExt.pluginsDir));
				ivyLayout.artifact(
					String.format("%s/[module](.[classifier])_[revision]", PlatformExt.pluginsDir));
				ivyLayout.artifact(
					String.format("%s/[module](.[classifier])-[revision].[ext]", PlatformExt.pluginsDir));
				ivyLayout.artifact(
					String.format("%s/[module](.[classifier])-[revision]", PlatformExt.pluginsDir));
				ivyLayout.ivy(
					String.format("%s/[module](.[classifier])_[revision].[ext]", PlatformExt.ivyMetadataDir));
			});
		});
		repositories.remove(repository);
		repositories.addFirst(repository);
	}

	private static class BundleMethod extends Closure<ClientModule> {

		private final PlatformExt platform;

		BundleMethod(PlatformExt platform, Project project) {
			super(project);
			this.platform = platform;
		}

		@Override
		public ClientModule call(Object... args) {
			Preconditions.checkArgument(args.length == 2, "Expected module name and version");
			String name = String.valueOf(args[0]);
			String version = String.valueOf(args[1]);

			return new DefaultClientModule(PlatformExt.platformGroup, platform.map(name), version);
		}
	}
}
