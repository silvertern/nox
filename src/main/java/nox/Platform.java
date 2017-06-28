/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox;

import nox.core.platform.PlatformExt;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.internal.project.ProjectInternal;


public class Platform implements Plugin<Project> {

	@Override
	public void apply(Project target) {
		ProjectInternal project = (ProjectInternal) target;

		project.getExtensions().add(PlatformExt.name, PlatformExt.init(project));
	}
}
