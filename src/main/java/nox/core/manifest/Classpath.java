/*
 * Copyright (c) Oleg Sklyar 2017. License: MIT
 */
package nox.core.manifest;

import java.io.File;
import java.util.Set;


public interface Classpath {

	Set<File> classPath();

	File jarFile();
}
