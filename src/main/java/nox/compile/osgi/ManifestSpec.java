/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT 
 */
package nox.compile.osgi;

import java.util.Arrays;
import javax.annotation.Nonnull;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import aQute.bnd.osgi.Analyzer;
import nox.core.BuildAPI;
import nox.core.Version;
import nox.core.manifest.Spec;


/**
 * ManifestSpec defines a specification for generating a manifest for an OSGi
 * module by analyzing its code and classpath. Directly assignable in build.gradle
 * to jar.manifest.spec after the nox.OSGi plugin has been applied.
 */
public class ManifestSpec implements Spec {

	@BuildAPI @Nonnull public final String symbolicName;

	@BuildAPI @Nonnull public final Version version;

	@BuildAPI public boolean singleton = false;

	@BuildAPI public boolean uses = true;

	private final Multimap<String, String> instructions = MultimapBuilder.hashKeys()
		.linkedHashSetValues()
		.build();

	ManifestSpec(@Nonnull String symbolicName, @Nonnull Version version) {
		this.symbolicName = symbolicName;
		this.version = version;
	}

	@Override public boolean singleton() {
		return singleton;
	}

	@Override public boolean uses() {
		return uses;
	}

	@Override public Multimap<String, String> instructions() {
		return instructions;
	}

	@BuildAPI public ManifestSpec instruction(@Nonnull String instruction, @Nonnull String... values) {
		this.instructions.putAll(instruction, Arrays.asList(values));
		return this;
	}

	@BuildAPI public ManifestSpec exports(@Nonnull String... packs) {
		return instruction(Analyzer.EXPORT_PACKAGE, packs);
	}

	@BuildAPI public ManifestSpec privates(@Nonnull String... packs) {
		String[] privates = Arrays.stream(packs).map(it -> "!" + it).toArray(String[]::new);
		return instruction(Analyzer.EXPORT_PACKAGE, privates);
	}

	@BuildAPI public ManifestSpec optionals(@Nonnull String... packs) {
		String[] optionals = Arrays.stream(packs)
			.map(it -> it + ";resolution:=optional")
			.toArray(String[]::new);
		return instruction(Analyzer.IMPORT_PACKAGE, optionals);
	}

	@BuildAPI public ManifestSpec imports(@Nonnull String... packs) {
		return instruction(Analyzer.IMPORT_PACKAGE, packs);
	}
}
