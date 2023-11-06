# Ayoy-License-Verifier-Plugin

[![Travis-CI](https://travis-ci.org/AyoyAB/Ayoy-Maven-License-Verifier-Plugin.svg?branch=master "CI status")](https://travis-ci.org/AyoyAB/Ayoy-Maven-License-Verifier-Plugin)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/se.ayoy.maven-plugins/ayoy-license-verifier-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/se.ayoy.maven-plugins/ayoy-license-verifier-maven-plugin)

When developing commercial software with OSS dependencies its
very important to verify that you only use dependencies and transitive dependencies with
acceptable licenses.

This plugin will verify the licenses of the current 
project and abort build if requirements are not met.

The plugin is heavily inspired from 
[khmarbaise/Maven-License-Verifier-Plugin](https://github.com/khmarbaise/Maven-License-Verifier-Plugin).

I wanted maven 3 support and I had never written
a maven plugin before, which is a reason in itself. :-)

To use on command line:
```bash
mvn se.ayoy.maven-plugins:ayoy-license-verifier-maven-plugin:verify
```

To use in maven pom file:
```xml
<plugin>
    <groupId>se.ayoy.maven-plugins</groupId>
    <artifactId>ayoy-license-verifier-maven-plugin</artifactId>
    <version>1.1.0</version>
    <executions>
        <execution>
            <phase>compile</phase>
            <goals>
                <goal>verify</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <licenseFile>${project.basedir}/licenses/licenses.xml</licenseFile>
        <excludedMissingLicensesFile>${project.basedir}/licenses/allowedMissingLicense.xml</excludedMissingLicensesFile>
        <failOnForbidden>true</failOnForbidden>
        <failOnMissing>true</failOnMissing>
        <failOnUnknown>true</failOnUnknown>
    </configuration>
</plugin>
```

The parameter path in `licenseFile` and `excludedMissingLicensesFile`can be relative to the child POM
or the nearest parent POM.
This is very useful in Maven multi-module projects where `src/licenses/licenses.xml` is defined only in parent location.

# Building from source
This is a maven project. Simply clone from git

```bash
git clone https://github.com/AyoyAB/Ayoy-Maven-License-Verifier-Plugin.git
```

Go to the directory and run

```bash
mvn clean install
```

You have now installed the snapshot-version.

# Adding license info on a Maven Artifact
Sometimes, to use the same files for many projects or to separate Maven build files from code files, do you want to put the files on other project and import it during the build. 
It is possible to do this with license info and allowed missing licenses files.
In order to do this, you need to add to your plugin configuration a dependency section with the artifact containing the files, like this : 

```
                    <dependency>
                        <groupId>com.mycompany</groupId>
                        <artifactId>AyoyLicenseManagement</artifactId>
                        <version>1.0.0</version>  
                    </dependency> 
```
Then, the project containing the files should : 
1. Be packaged as a jar
2. Contain on folder src/main/resources/se/ayoy/maven/plugins/licenseverifier the license and exclusion files

On demo module, the project child2 performs an analysis using the files of the project LicenseManagement. 

# Running tests

Run a single integration test like this:
```bash
mvn verify -Dinvoker.test=artifact-with-license-and-ignored
```

# Configuration settings

- licenseFile: The location of the licenses.xml file. Defaults to src/licenses/licenses.xml
- excludedMissingLicensesFile: The location of the file listing dependencies that should not be checked for licenses. Default blank
- excludedScopes: A list of scopes to exclude. May be used to exclude artifacts with test or provided scope from license check.
- failOnForbidden. If the plugin should fail on forbidden licenses. Default true
- failOnMissing. If the plugin should fail on missing licenses. Default true
- failOnUnknown. If the plugin should fail on unknown licenses. Default true
- failOnWarning.If the plugin should fail on licenses marked as warning. Default true
- requireAllValid: If a dependency provides several licenses, do you require all of them to be among your accepted licenses, or just one? Default true (requires all)
- verbose: Default false
- skip: Default false

# Contributors
- [John Allberg](https://github.com/smuda)
- [Tibor Digana](https://github.com/Tibor17)
- [Mads Opheim](https://github.com/madsop)
