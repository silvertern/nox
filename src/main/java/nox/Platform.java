/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.internal.plugins.ExtensionContainerInternal;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.plugins.ExtraPropertiesExtension;

import nox.core.sys.Arch;
import nox.core.sys.OS;
import nox.core.sys.Win;
import nox.platform.BundleMappingsExt;
import nox.platform.PlatformRepoMethod;


public class Platform implements Plugin<Project> {

	public static final String GROUP = "platform";

	public static final String PLUGINS = "plugins";

	public static final String IVY_METADATA = "ivy-metadata";

	@Override
	public void apply(Project target) {
		ProjectInternal project = (ProjectInternal) target;

		ExtensionContainerInternal ext = project.getExtensions();
		ExtraPropertiesExtension extProps = ext.getExtraProperties();

		ext.create(BundleMappingsExt.name, BundleMappingsExt.class);

		// apply to every sub-project
		extProps.set(PlatformRepoMethod.methodName, new PlatformRepoMethod(project));
		extProps.set("p2os", OS.current().toString());
		extProps.set("p2ws", Win.current().toString());
		extProps.set("p2arch", Arch.current().toString());
	}
}
