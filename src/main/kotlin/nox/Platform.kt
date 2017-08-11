/**
 * Copyright (c): 2017 Oleg Sklyar and contributors. License: MIT
 */
package nox

import nox.compilation.OSGiExt
import nox.core.PlatformInfoHolder
import nox.sys.Arch
import nox.sys.OS
import nox.sys.Win
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal


class Platform : Plugin<Project> {

	override fun apply(target: Project) {
		val project = target as ProjectInternal

		val rootExt = project.rootProject.extensions
		if (rootExt.findByType(PlatformInfoHolder::class.java) == null) {
			rootExt.create(PlatformInfoHolder.name, PlatformInfoHolder::class.java)
		}

		val ext = project.extensions
		ext.create(OSGiExt.name, OSGiExt::class.java, project, rootExt.findByType(PlatformInfoHolder::class.java))

		val extProps = ext.extraProperties
		// apply to every sub-project
		extProps.set("p2os", OS.current().toString())
		extProps.set("p2ws", Win.current().toString())
		extProps.set("p2arch", Arch.current().toString())
	}
}
