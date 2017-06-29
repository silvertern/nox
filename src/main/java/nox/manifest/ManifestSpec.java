/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT 
 */
package nox.manifest;

import aQute.bnd.osgi.Analyzer;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import nox.core.Version;
import org.gradle.api.GradleException;

import javax.annotation.Nonnull;
import java.util.Arrays;


/**
 * ManifestSpec defines a specification for generating a manifest for an OSGi
 * module by analyzing its code and classpath. Directly assignable in build.gradle
 * to jar.manifest.spec after the nox.OSGi plugin has been applied.
 */
public class ManifestSpec {

	@Nonnull public final String symbolicName;

	@Nonnull public final Version version;

	public boolean singleton = false;

	public boolean uses = true;

	public String activator = null;

	private final Multimap<String, String> instructions = MultimapBuilder.hashKeys()
		.linkedHashSetValues()
		.build();

	ManifestSpec(@Nonnull String symbolicName, @Nonnull Version version) {
		this.symbolicName = symbolicName;
		this.version = version;
	}

	public Multimap<String, String> instructions() {
		return instructions;
	}

	public ManifestSpec instruction(@Nonnull String instruction, @Nonnull String... values) {
		if (Analyzer.BUNDLE_SYMBOLICNAME.equals(instruction) || Analyzer.BUNDLE_VERSION.equals(instruction)) {
			throw new GradleException("Must use symbolicName and version attributes instead of corresponding instructions");
		}
		this.instructions.putAll(instruction, Arrays.asList(values));
		return this;
	}

	public ManifestSpec exports(@Nonnull String... packs) {
		return instruction(Analyzer.EXPORT_PACKAGE, packs);
	}

	public ManifestSpec privates(@Nonnull String... packs) {
		String[] privates = Arrays.stream(packs).map(it -> "!" + it).toArray(String[]::new);
		return instruction(Analyzer.EXPORT_PACKAGE, privates);
	}

	public ManifestSpec optionals(@Nonnull String... packs) {
		String[] optionals = Arrays.stream(packs)
			.map(it -> it + ";resolution:=optional")
			.toArray(String[]::new);
		return instruction(Analyzer.IMPORT_PACKAGE, optionals);
	}

	public ManifestSpec imports(@Nonnull String... packs) {
		return instruction(Analyzer.IMPORT_PACKAGE, packs);
	}
}
