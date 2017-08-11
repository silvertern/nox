/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT
 */
package nox.platform.gradlize

import nox.platform.gradlize.impl.DependencyResolverImpl


interface DependencyResolver {

	fun resolveFor(bundle: Bundle): Collection<Dependency>

	companion object {

		fun instance(universe: BundleUniverse): DependencyResolver {
			return DependencyResolverImpl(universe)
		}
	}
}
