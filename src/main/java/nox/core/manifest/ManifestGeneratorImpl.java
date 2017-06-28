/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT 
 */
package nox.core.manifest;

import java.io.IOException;
import java.util.jar.Manifest;
import javax.annotation.Nonnull;


class ManifestGeneratorImpl implements ManifestGenerator {

	@Override
	public Manifest generate(@Nonnull Spec spec, @Nonnull Classpath classpath) throws IOException {
		// FIXME
		return new Manifest();
	}
}
