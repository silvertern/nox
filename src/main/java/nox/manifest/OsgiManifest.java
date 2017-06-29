/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT 
 */
package nox.manifest;

import aQute.bnd.osgi.Analyzer;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Sets;
import groovy.lang.Closure;
import nox.core.Version;
import org.apache.commons.lang3.StringUtils;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.jar.Manifest;


/**
 * OSGiJarManifest provides a drop in replacement for jar.manifest
 */
public class OsgiManifest extends DefaultManifest {

	@Nullable public ManifestSpec spec = null;

	@Nullable public File from = null;

	private final Supplier<Set<File>> classpathSupplier;

	private final Supplier<File> jarSupplier;

	public OsgiManifest(@Nonnull Supplier<Set<File>> classpathSupplier, @Nonnull Supplier<File> jarSupplier, @Nonnull PathToFileResolver fileResolver) {
		super(fileResolver);
		this.classpathSupplier = classpathSupplier;
		this.jarSupplier = jarSupplier;
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
		spec(toSymbolicName(String.valueOf(project.getGroup()), project.getName()),
			new Version(String.valueOf(project.getVersion())), configs);
	}

	public String toSymbolicName(@Nonnull String groupId, @Nonnull String artifactId) {
		if (artifactId.startsWith(groupId)) {
			return artifactId;
		}
		String[] parts = (groupId + "." + artifactId).split("[\\.-]");
		Collection<String> elements = Sets.newLinkedHashSet(Arrays.asList(parts));
		return StringUtils.join(elements, ".");
	}

	@Override public DefaultManifest getEffectiveManifest() {
		try {
			DefaultManifest baseManifest = new DefaultManifest(null);
			baseManifest.attributes(getAttributes());

			for (Map.Entry<Object, Object> entry : generateManifest().getMainAttributes().entrySet()) {
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

	private Manifest generateManifest() throws IOException {
		if (from != null) {
			try (FileInputStream s = new FileInputStream(from)) {
				return new Manifest(s);
			}
		}
		if (spec == null) {
			throw new GradleException("Please provide manifest 'spec' or 'from' file");
		}

		Analyzer analyzer = new Analyzer();
		analyzer.setBundleSymbolicName(spec.symbolicName + (spec.singleton ? ";singleton:=true" : ""));
		analyzer.setBundleVersion(spec.version.toString());

		Multimap<String, String> instructions = MultimapBuilder.hashKeys().linkedHashSetValues().build();
		instructions.putAll(spec.instructions());

		instructions.put(Analyzer.IMPORT_PACKAGE, "*");
		instructions.put(Analyzer.EXPORT_PACKAGE, "*;-noimport:=true;version=" + spec.version.toString(Version.Component.Build));

		for (String instruction: spec.instructions().keySet()) {
			List<String> values = Lists.newArrayList(spec.instructions().get(instruction));
			String value = StringUtils.join(values, ",");
			analyzer.getProperties().setProperty(instruction, value);
		}
		if (StringUtils.isNotBlank(spec.activator)) {
			analyzer.getProperties().setProperty(Analyzer.BUNDLE_ACTIVATOR, spec.activator);
		}
		if (!spec.uses) {
			analyzer.setProperty(Analyzer.NOUSES, "true");
		}

		File jar = jarSupplier.get();
		if (jar == null) {
			jar = File.createTempFile("osgi", UUID.randomUUID().toString());
			jar.delete();
			jar.mkdir();
			jar.deleteOnExit();
		}
		analyzer.setJar(jar);

		Set<File> classpath = classpathSupplier.get();
		if (!classpath.isEmpty()) {
			analyzer.setClasspath(classpath);
		}

		try {
			Manifest res = analyzer.calcManifest();
			String imports = res.getMainAttributes().getValue(Analyzer.IMPORT_PACKAGE);
			if (StringUtils.isNotBlank(imports)) {
				imports = imports.replace(";common=split", "");
				res.getMainAttributes().putValue(Analyzer.IMPORT_PACKAGE, imports);
			}
			return res;
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

}
