/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT 
 */
package nox.compile;

import com.google.common.base.Preconditions;
import groovy.lang.Closure;
import nox.core.Version;
import nox.core.manifest.Classpath;
import nox.core.manifest.ManifestGenerator;
import nox.core.manifest.ManifestUtil;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.java.archives.internal.DefaultManifest;
import org.gradle.internal.file.PathToFileResolver;
import org.gradle.util.ConfigureUtil;
import org.gradle.util.WrapUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.jar.Manifest;


/**
 * OSGiJarManifest provides a drop in replacement for jar.manifest
 */
public class OSGiJarManifest extends DefaultManifest {

	@Nullable public ManifestSpec spec = null;

	@Nullable public File from = null;

	@Nonnull private final Classpath classpath;

	@Nonnull private final ManifestGenerator mfGenerator;

	public OSGiJarManifest(@Nonnull Classpath classpath, @Nonnull PathToFileResolver fileResolver) {
		this(classpath, fileResolver, ManifestGenerator.instance());
	}

	OSGiJarManifest(@Nonnull Classpath classpath, @Nonnull PathToFileResolver fileResolver,
		@Nonnull ManifestGenerator mfGenerator) {
		super(fileResolver);
		this.classpath = classpath;
		this.mfGenerator = mfGenerator;
	}

	public void spec(@Nonnull String symbolicName, @Nonnull Object version,
		@Nullable Closure<?>... configs) {
		this.spec = new ManifestSpec(symbolicName, new Version(String.valueOf(version)));
		if (configs != null) {
			for (Closure<?> config: configs) {
				ConfigureUtil.configure(config, this.spec);
			}
		}
	}

	public void spec(@Nonnull Project project, @Nullable Closure<?>... configs) {
		spec(ManifestUtil.toSymbolicName(String.valueOf(project.getGroup()), project.getName()),
			new Version(String.valueOf(project.getVersion())), configs);
	}

	public String toSymbolicName(@Nonnull String groupId, @Nonnull String artifactId) {
		return ManifestUtil.toSymbolicName(groupId, artifactId);
	}

	@Override public DefaultManifest getEffectiveManifest() {
		try {
			DefaultManifest baseManifest = new DefaultManifest(null);
			baseManifest.attributes(getAttributes());

			Manifest mf = null;
			if (spec != null) {
				mf = mfGenerator.generate(spec, classpath);
			} else if (from != null) {
				try (FileInputStream s = new FileInputStream(from)) {
					mf = new Manifest(s);
				}
			} else {
				Preconditions.checkArgument(spec != null || from != null,
					"Please provide manifest 'spec' or 'from' file");
			}

			for (Map.Entry<Object, Object> entry : mf.getMainAttributes().entrySet()) {
				baseManifest.attributes(
					WrapUtil.toMap(entry.getKey().toString(), entry.getValue().toString()));
			}

			// this changing value prevented incremental builds...
			baseManifest.getAttributes().remove("Bnd-LastModified");
			return getEffectiveManifestInternal(baseManifest);
		} catch (IOException ex) {
			throw new GradleException(ex.getMessage(), ex);
		}
	}

}
