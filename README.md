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

## Compiling against the _target platform_ 

Building OSGi applications is akin to building against an application server, such as JBoss, with 
all dependencies `provided`. In context of Eclipse RCP they are provided from the _target platform_. 

While the runtime dependency resolution is handled by OSGi itself, we want gradle to handle 
the build. Therefore, it must understand how to resolve direct and transitive dependencies from 
the _target platform_. 

It is essential to understand that the dependency resolution mechanisms employed by gradle and OSGi 
are substantially different. We are not aiming here to mimic OSGi, rather to 
provide consistent dependencies to build and unit test the code. Under the assumption that 
the same package is only provided by one module the resulting dependency resolution within OSGi
should not be noticeably different. However, it may and this may cause runtime or integration test
errors! 

The `nox.Platform` plugin defines the `osgi` extension shared also by the `nox.OSGi`
plugin, that provides configuration options for wrapping the target platform into an Ivy
repository that gradle can use for dependency resolution.

The following snippet demonstrates how to add a gradle Ivy repository for a target
platform residing under the `targetPlatformRoot`:

```
plugins.apply(nox.Platform)

repositories.add(osgi.repo("e47", targetPlatformRoot))
```

In abcense of transitive dependencies the above declaration should be sufficient to 
add compile or runtime dependencies from the target platform as

```
dependencies {
  compile("plugins:org.slf4j.api:1.7.+")
}
```

Note that the group and artifactId of artifacts from the target platform are not the same
as for the same artifacts coming from a standard Maven repository like Maven Central. The
group will always be `plugins` and the artifact the bundle symbolic name. The bundles
in the target platform are supposed to follow the naming convention where jars are named 
with the bundle symbolic name followed by version separated with a dash or underscore.

To improve clarify that the dependency is coming from the target platform, the above
can be written as:

```
plugins.apply(nox.OSGi)

dependencies {
  compile(osgi.bundle("org.slf4j.api", "1.7.+"))
}
```

All of the above could be handled easily by gradle without any extra plugin by defining
a `flatDir` repository as we have not been resolving transitive dependencies until now.
The power of `nox` comes in resolving transitive dependencies from the target platform.
For that, one need to complement the target platform (now wrapped into an Ivy repository)
with Ivy metadata that define transitive dependencies for each module.

The `nox.Platform` plugin offers the `ivynize` task, which inspects _target platform_ manifests 
for imported packages and required bundles and matches those to exported packages and bundle 
symbolic names in the platform. It then generates Ivy metadata files for all the bundles 
(jars and directories) in the _target platform_ so that gradle can resolve them.

The trick is that the Ivy metadata must already be present by the time gradle tries to
resolve dependencies from the target platform. Therefore, it is best to include the 
`ivynize` task as a dependency of `assemble` in the `buildSrc` pre-build. So the content of
`buildSrc/build.gradle` may look something like this:

```
buildscript {
  repositories.jcenter()
  dependencies.add("classpath", "com.silvertern.gradle:nox:+")
}

dependencies {
  tp(group: "eclipse", name: "target-platform", version: tpVersion, ext: "zip", changing: true)
}

task("ensure-target-platform", type: Sync) {
  from(configurations.tp.collect { zipTree(it) })
  into(new File(targetDir, "platform"))
}

ivynize {
  dependsOn("ensure-target-platform")
  targetPlatform = new File(buildDir, "platform")
}

clean.dependsOn(ivynize)
assemble.dependsOn(ivynize)

build.dependsOn(assemble)
```

This will generate the `$targetPlatform/ivy-metadata` directory with Ivy metadata files. 
So your _target platform_ directory needs to be writable, but no existing files will be 
copied or altered.

## Generating OSGi-compliant manifests for source modules

The default handling of the manifest by the `nox.OSGi` plugin should be good enough to generate
a sensible OSGi compatible manifest. The `nox.OSGi` plugin is a complete rewrite of the 
standard `osgi` plugin by gradle. One can only amend the manifest of the `jar` task with it. 
No other manifests can be generated.

Importing the `nox.OSGi` plugin will require a decision on how the manifest should be generated.
The two possible options include: supervised instruction-based geneation based on code
analysis using bnd-tools and using a manually defined manifest file and only possibly amending
the bundle version. In the former case, the manifest file will also be automatically copied
into `META-INF` at the root of the module so that the Eclipse IDE could benefit from
having it in the correct place when using source as a bundle. Additionally, the content of
`META-INF`, if complemented with further config files etc, will be automatically copied into
the resulting jar similarly to the content of `main/resources` while not being considered a
resources path.

To generate a manifest dynamically the API looks like this:

```
plugins.apply(nox.OSGi)

jar.manifest {
  spec(project, {
    optionals("com.sun.*")
    instruction("Fragment-Host", "com.company.remote.proxy")
    // other options
  })
}
```

In the above example the bundle symbolic name and version will be taken from the name and
version of the project. An alternative API assumes: `spec(name, version, Closure)`.

Instructions that can be used within the `spec` clause include:

* `fun instruction(instruction: String, vararg values: String)`
* `fun exports(vararg packs: String)`
* `fun privates(vararg packs: String)`
* `fun optionals(vararg packs: String)`
* `fun imports(vararg packs: String)`

As an alternative to generating, a manifest can be directly loaded from a manifest file. 
Here the manifest is placed under the `META-INF` folder in the project root (turning the
source into a bundle):

```
plugins.apply(nox.OSGi)

jar.manifest {
  spec(Paths.get(projectDir.absolutePath, "META-INF", "MANIFEST.MF"), version)
}
```

Additionally to manifest operations, the `nox.OSGi` plugin will generate a `build.properties`
file in the project root that can be used by Eclipse and the PDE build complementary.
By default the plugin will use main source and resource paths to generate `build.properties`
`source` clause and will add `META-INF/,.` to `bin.includes`. The plugin provides
a nested `buildProperties` extension on the `osgi` extension to affect the build properties
configuration, e.g.:

```
osgi.buildProperties {
  binincludes("provided/")
}
```

The API includes:

```
fun sources(vararg sources: String)
fun binincludes(vararg binincludes: String)
fun outputs(vararg outputs: String)
fun instruction(key: String, value: String)
```

Generally speaking the `build.properties` should not be used if there is no trace of PDE
in your build.

## Eclipse IDE integration

The following is not provided by the `nox` plugins, but is a useful complement when developing
Eclipse RCP or OSGi applications and building them with gradle. Gradle provides a mechanism
to generate Eclipse project setup from gradle. Most of the time this process is 
automated and one can really exclude all of Eclipse specific artifacts from Git commits
(`.project`, `.classpath`, `.settings/` etc). With OSGi a bit more work is required, but
it is ok just to copy & paste the following block to support Eclipse plugin nature for
those modules that are in fact OSGi bundles:


```
import org.gradle.plugins.ide.eclipse.model.EclipseModel

import java.nio.file.Paths

allprojects {
  plugins.apply(EclipsePlugin)

  plugins.withType(JavaPlugin).whenPluginAdded {
    EclipseModel model = (EclipseModel) extensions.findByName("eclipse")
    model.project {
      name(name)
      buildCommand("org.eclipse.jdt.core.javabuilder")
      if (Paths.get(projectDir.absolutePath, "META-INF", "spring").toFile().exists()) {
        buildCommand("org.springframework.ide.eclipse.core.springbuilder")
      }
    }
  }

  plugins.matching { plugin -> plugin.class.name == "nox.OSGi" }.all({
    EclipseModel model = (EclipseModel) extensions.findByName("eclipse")
    model.project {
      natures("org.eclipse.pde.PluginNature")
      buildCommand("org.eclipse.pde.ManifestBuilder")
      buildCommand("org.eclipse.pde.SchemaBuilder")
    }
    model.classpath {
      containers "org.eclipse.pde.core.requiredPlugins"
    }
  })

  plugins.matching { plugin -> plugin.class.name == "nox.AspectJ" }.all({
    EclipseModel model = (EclipseModel) extensions.findByName("eclipse")
    model.project {
      natures("org.eclipse.ajdt.ui.ajnature")
      buildCommand("org.eclipse.ajdt.core.ajbuilder")
    }
  })

  cleanEclipse {
    doLast {
      new File(project.projectDir, ".settings").deleteDir()
    }
  }
}
```
