# Building Eclipse RCP applications with `nox` on gradle 


The `nox` module provides a set of gradle plugins for building Eclipse RCP applications and
exposing custom modules as OSGi bundles. The original scope was to also provide plugins to
put together an Eclipse target platform based on Eclipse update sites/p2 repositories and
on a collection of automatically converted bundles from standard gradle dependencies, but
that functionality was left behind in the original version only and has not been followed 
upon.

## Importing and using the plugins
 
All provided plugins are packaged in a single module, `nox`. To import it into the root of 
the build use the following `buildscript` block (only available from `jcenter`):
 
```
buildscript {
  repositories.jcenter()
  dependencies.add("classpath", "com.silvertern.gradle:nox:+")
}
```

Now one can use all the provided plugins in any of the child projects as follows: 

* `plugins.apply(nox.Platform)` to define the location of the target platform and 
 the corresponding Ivy repositories to build against
* `plugins.apply(nox.OSGi)` to override the default `jar`-task manifest generation

## Configuring the _target platform_

Shared configuration elements for the _target platform_ are provided by the `platform` extension.
This extension is static and a single configuration will apply to all sub- or superprojects within
the same build.

If you generate the _target platform_ with the `nox.Platform` plugin everything you need to do is
to specify the `root` of all your platform related files:

```
apply plugin: nox.Platform

platform {
  root = file("/home/user/platform-root")
}
```

If the `root` is specified, the Eclipse SDK will be later downloaded into `$root/eclipse` on
Linux and Windows or `$root/Eclipse.app` on OSX, and the _target platform_ will be generated 
within `$root/platform`. The latter is then provided by the `targetPlatformDir` on the `platform`
extension.

If you just want to build against an existing _target platform_ just set the `targetPlatformDir`
in the `platform` extension, the root will be neither used no validated:

```
apply plugin: nox.Platform

platform {
  targetPlatformDir = file("/home/user/e46")
}
```

It makes sense to configure the actual location via a variable specified in the user specific 
`gradle.properties` files. The plugins will check that the platform location or the root location
is specified providing no defaults.

Further to the `targetPlatformDir` it is also possible to configure the platform build directory. By
default it is the build directory of the project where the plugin is applied. However, this directory
is removed when a `clean` taks is run, e.g. for Java build artifacts. To separate this behaviour and
to keep the incremental nature of the platform generation, one build bundles, write temporary 
inputs and generate the P2 repository elsewhere:

```
platform {
  targetPlatformDir = file("/home/user/e46")
  platformBuildDir = file("/home/user/e46/build")
}
```

## Compiling against the _target platform_ 

Building OSGi applications is akin to building against an application server, such as JBoss, with 
all dependencies `provided`. In context of Eclipse RCP they are provided from the _target platform_. 

While the runtime dependency resolution is handled by OSGi itself, we want grade to handle 
the build. Therefore, it must understand how to resolve direct and transitive dependencies from 
the _target platform_. 

It is essential to understand that the dependency resolution mechanisms employed by gradle and OSGi 
are substantially different. We are not aiming here to mimic OSGi, rather to 
provide consistent dependencies to build and unit test the code. Under the assumption that 
the same package is only provided by one module the resulting dependency resolution within OSGi
should not be noticeably different. However, it may and this may cause runtime or integration test
errors! 

To minimize dependency resolution differences the `bundle` task ()described below) tries to incorporate
bundle dependencies along with package dependencies when generating bundles, and the dependency
resolution for producing the Ivy metadata (described below) incorporates both package and bundle
dependency when generating dependency graphs, along with correct version ranges.

Gradle can handle flat dirs as a source of dependencies, however, the challenge is to provide the 
resolution of transitive dependencies on our _target platform_ flat dir. The `nox.Platform` plugin
offers the `ivynize` task, which inspects _target platform_ manifests for imported packages and
required bundles and matches those to exported packages and bundle symbolic names in the platform.
It then generates Ivy metadata files for all the bundles (jars and directories) in the _target platform_
so that gradle can resolve them.

Given the `platform` configuration above (via `root` or `targetPlatformDir`) run 
`./gradlew ivynize` to wrap the _target platform_ into an Ivy repository. This
generates the `$targetPlatformDir/ivy-metadata` directory with Ivy metadata files. So yes, your
_target platform_ directory needs to be writable, but no existing files will be copied or altered.

Having generated the Ivy annotations, you can compile your first bundle against the _target platform_
as follows:

```
apply plugin: nox.Java
apply plugin: nox.OSGi

dependencies {
  compile bundle("com.google.guava", "+")
  testCompile bundle("org.junit", "4.+")
}
```

The default handling of the manifest by the `nox.OSGi` plugin should be good enough to generate
a sensible OSGi compatible manifest. The `nox.Java` plugin adds the _target platform_ Ivy repository 
as configured in the `platform` extension (most likely in a different module, somewhere in the root 
project) to the `repositories` configuration of the project and provides the `bundle` method that 
turns a dependency import into one that matches that repository format.

If you do not need a bundle and just want a classical module sharing the same dependencies as
bundles in another project, just use the `nox.Java` plugin for the dependency resolution and
drop  the `nox.OSGi` plugin. In this case you can optionally add further external, i.e. non-`bundle`, 
dependencies.

As a side note, you can also include non-bundled version of junit into the `testCompile` even if you
are generating a bundle: for unit tests OSGi will not be loaded and for gradle it does not matter
where the test code comes from. Same is valid for mockito and other testing libraries. In fact,
it feels like a crime to include a testing library into a _target platform_ which is then delivered
into production (and if it is not the same one, then how do you know that what you tested is good).

## Turning modules into OSGi bundles with `nox.OSGi`

The `nox.OSGi` plugin is a complete rewrite of the standard `osgi` plugin by gradle. It is based
around the same code and instructions as the `bundle` plugin described below to aid cohesion. It is
also simpler in sense of code structure. As with other `nox` functionalities it provides less 
features for the benefit of clarity and correctness: one can only amend the manifest of the `jar`
task with it. No other manifests can be generated.

### Fine tuning manifest generation

The API is defined by the underlying `ModuleDef` (all values optional, but can be overwritten):

* `string groupId`, implicitly set from the 'group' property of the project
* `string artifactId`, implicitly set from the project name
* `string version`, implicitly set from the 'version' property of the project
* `string symbolicName`, implicitly calculated from the groupId and artifactId
* `boolean singleton`, adds the singleton specifier to the resulting symbolic name
* `activator(string activator)`, adds the bundle activator entry
* `instruction(string instruction, string... values)`, adds arbitrary instructions except 
  for `Bundle-SymbolicName` and `Bundle-Version` (use `symbolidName` and `version` instead)
* `imports(string... pkgNames)`, adds package import directives to the analyzer (normally not needed)
* `exports(string... pkgNames)`, adds package export directives to the analyzer (normally not needed)
* `privates(string... pkgNames)`, marks export packages as private excluding them from exports
* `optionals(string... pkgNames)`, marks import packages as optional omitting those during dependency 
  resolution when not present
* `boolean replaceOSGiManifest`, unused in this context as there is no underlying manifest 
  (only used with 'bundle') 
* `boolean withQualifier`, default true, adds a version qualifier as `.v20161231-2359` 
  for 31st Dec 16, 23:59.

For example, if the module provides packages `com.pany.pack1` and `com.pany.pack2` and depends in
some path on Apache `commons-lang3` the following statements will exclude `com.pany.pack2` from
`Export-Package` and mark `commons-lang3` for optional resolution in `Import-Package`:

```
apply plugin: nox.Java
apply plugin: nox.OSGi

jar.manifest {
  optionals "org.apache.commons.lang3"
  privates "com.pany.pack2"
}
```

To override the default generated symbolic name, drop the qualifier from the version and exclude further 
packages one can use:

```
apply plugin: nox.Java
apply plugin: nox.OSGi

jar.manifest {
  symbolicName "com.pany.foo"
  optionals "org.apache.*"
  privates "com.pany.pack2", "com.pany.pack3.*"
  withQualifier false
}
```

All `bnd-lib` directives are supported under `instruction` and `exports`, including pattern matching 
with `*`. Unless overwritten, all packages are added to `Export-Package` along with their version 
(being the bundle version). Packages exported by the bundle are not by default added to the `Import-Package`
if they are also used internally.

### Further parameters for generating OSGi manifests

The following extended project properties are evaluated for every project using the `nox.OSGi` plugin:

* `osgiUnpackManifest`, default `true`, specifies whether the manifest generated for the jar should
 be unpacked into `$projectDir/META-INF/MANIFEST.MF` overwriting any existing file. 
 This functionality is useful for Eclipse IDE integration with the Plugin Nature;
* `osgiRequireBundles`, default `false`, specifies whether gradle resolved module dependencies
 should be included as required bundles (alternatively only package imports are used);
* `osgiWithExportUses`, default `true`, specifies whether exported packages should include the
 `uses` clause.

Unlike other gradle plugins for OSGi, the `nox.Java` does not try to take into account any information
found in manifests within the project tree. It generates manifests purely for the purpose of OSGi
runtime and integration into the Eclipse IDE. This intentionally makes it gradle-first and gradle-only plugin! 


## Creating the _target platform_

The creation of a _target platform_ consists of four steps/tasks:

* `getsdk` to downloads and unpack the Eclipse SDK
* `bundle` to generate OSGi bundles from maven dependencies and local jars,
* `create` to create the platform from remote p2 repositories and locally generated bundles,
* `ivynize`to annotate the _target platform_ with Ivy metadata for the use in the gradle build.

See the _Compiling against the target platform_ section above for the description of how to 
configure the location of the future target platform. In essence you need to define the `root` in
the `platform` extension. This also defines where the SDK will be downloaded to. The configuration
of individual tasks is done within each task block as in the following comprehensive example:

```
apply plugin: nox.Platform

platform {
  root = file("/home/user/platform-root")
  bundleMappingFile = file("platform/bundlemapping.properties")
}

getsdk {
  version = "4.7M5"
}

repositories {
  jcenter()
}

bundles {
  rule "org.springframework", {
    optionals "org.jruby.*", "org.xml.*"
  }

  bundle "junit", "junit", "4.+"
  bundle "com.google.guava", "guava", "+"
  bundle "org.apache.commons", "commons-lang3", "3.5", {
    replaceOSGiManifest true
  }
  bundle "org.springframework", "spring-context", "4.3.+", {
    optionals "org.hibernate.*"
    rule "org.springframework", "spring-instrument", {
      optionals "org.aspectj.*"
    }
  }
  bundle "com.pany", "foo", "3.5", {
    jarFile file("foo.jar")
    sourceJarFile file("foo-source.jar")
  }
  
}

create {
	location "http://download.eclipse.org/eclipse/updates/4.6/", {
		unit "org.eclipse.sdk.ide", "4.6.0.I20160606-1100"
		unit "javax.servlet_3.1.0.v201410161800.jar"
	}
	location "http://download.eclipse.org/tools/orbit/downloads/drops/R20150821153341/repository/", {
		unit "org.apache.commons.lang3", "3.1.0.v201403281430"
		unit "com.google.guava_15.0.0.v201403281430.jar"
	}
}
```

Let's go through the example line by line analysing what it does and why. 

First, we import the `nox.Platform` plugin and define the location for our target platform 
(and the downloaded) Eclipse SDK. Here we also specify the (optional) file to map bundle names
used as gradle bundle dependencies to actual names of bundles in the platform. This way one can 
use a platform with somewhat deviating bundle names while keeping all the build scripts constant.

Then, we define the SDK version to download within the `getsdk` task. If no version is specified 
(or the section is missing) the 4.6.2 will be used. Currently supported are 4.6, 4.6.2 and 4.7M5.

Then, we define the repositories to be used for resolving maven dependencies for bundles that we
want to generate in the next step. Here it is `jcenter`, but can be any other one.

Then, we define all the bundles we want to integrate into the target platform from maven repositories
or local files. We will discuss this section in a bit more detail below.

Finally, we describe the target platform to create and specify all bundles we want to download from
remote p2 repositories.

That is it. Now run `./gradlew getsdk bundle create ivynize`, or just `./gradlew ivynize` because
of task transitive dependencies on each other, to generate your target platform.

For a more comprehensive example see the `example` directory.


### Configuring the `bundles`

The task `bundle` provides the `bundles` extension to configure all the bundles imported into the
target platform from non-p2 repositories. It uses the standard gradle dependency resolution and
repositories provided via the `repositories` directive, as in the above example. The extension API is 
simple:

* `bundle "group", "artifact", "version pattern" [, {configuration closure}]`, to specify a bundle 
  to import and rules to generate it within the configuration closure
* `rule ["group"[, "artifact"[, "version"]]], {configuration closure}`, to specify a rule that
  applies to all the artifacts matching the group and optionally the name and version within a 
  given scope. The rule can be applied at the top level, in which case it will apply to all bundles
  and their dependencies, or within a `bundle` clause, in which case it will apply only to the 
  dependencies of that bundle. Each `bundle` clause is a rule itself applied to that bundle only.
  A rule defined without any pattern (that is no group, no artifact etc.) represents a global
  rule applied to all bundles. Here one can turn some platform dependencies optional, such as
  `sun.misc.*` or turn off bundle qualifier generation, `	withQualifier false`.
  
The `rule` and `bundle` take all the same directives as the manifest of the `nox.OSGi`plugin above 
with the following differences:

* `string groupId`, can be reset over the first call argument, but makes little sense to do so,
* `string artifactId`, can be reset over the second call argument, but makes little sense to do so, 
* `string version`, can be reset over the third call argument, but makes little sense to do so.

Furthermore, in the `bundle` directive the `groupId`, `artifactId` and `version` surve the purpose
of identifying the bundle in the repository and are all required, albeit one can use `+" in the version
to specify the "next available". In the `rule` directive these attributes constitute the condition to
check if the rule applies to the artifact or not. Therefore, only the group is required for the `rule`
directive.

Additionally the `bundle` directive can take the `boolean replaceOSGiManifest` value to indicate that
a manifest of a bundle that is already OSGi compliant should be fully regenerated.

Every bundle definition is also a global rule matching the same pattern. Further, if there is 
a bundle definition for a transitive dependency, the dependency will be introduced with the bundle
definition following the rules in that definition.


Let's now go through the `bundles` section of the above example in detail:


```
rule "org.springframework", {
	optionals "org.jruby.*", "org.xml.*"
}
```
This rule specifies that all artifacts with group id `org.springframework`, or all artifacts from
the Spring framework, should mark their imports under `org.jruby` and `org.xml` as optional.

`bundle "junit", "junit", "4.+"` converts the non-bundled junit (latest 4th release) module into a 
bundle along with its transitive dependencies (`org.hamcrest`).

`bundle "com.google.guava", "guava", "+"` takes the already bundled guava module as is. Surely I
happen to know that junit is a standard module and guava is already a bundle, but as you can see there
is no difference in the definition.

```
bundle "org.apache.commons", "commons-lang3", "3.5", {
  replaceOSGiManifest true
}
```
Given that `commons-lang3` is already bundled this drops the original manifest completely and
generates a new one instead.

```
  bundle "org.springframework", "spring-context", "4.3.+", {
    optionals "org.hibernate.*"
    rule "org.springframework", "spring-instrument", {
      optionals "org.aspectj.*"
    }
  }
```
This directive imports the `spring-context` artifact along with all its transitive 
dependencies. It turns all hibernate dependencies to optional on all dependencies that have it. 
It turns aspectj dependency to optional only on `spring-instrument`.

```
bundle "com.pany", "foo", "3.5", {
  jarFile file("foo.jar")
  sourceJarFile file("foo-source.jar")
}
```
Finally, this directive takes a local jar, and its source counterpart, and turns them into a bundle
and a source bundle with the symbolic name `com.pany.foo` and version `3.5.0.v20170323_1549` (or 
other suffix depending on date and time).

### Configuring the platform generation

The `create` task accepts a collection of locations parametrized with the location path and
a closure describing the enclosed units. Each unit can be described either by a pair of name and version
or by a single string matching the pattern `name_version.jar`.

Bundles generated locally with the `bundle` task are automatically included. 
Sources are automatically included in all locations, including that of local bundles.

### Cleaning up the platform

Generated bundles, ivy repository or the complete platform (excluding the SDK) can be reset with the
following tasks of the `nox.Platform` plugin:

* `cleanbundles` to clean generated bundles and the corresponding p2 repository.
* `cleanivy` to clean the generate Ivy metadata, implies `cleanbundles`.
* `cleanplatform` to cleans the target platform, implies `cleanbundles` and `cleanivy`.
