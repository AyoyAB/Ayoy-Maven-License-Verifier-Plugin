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
    <version>1.0.6</version>
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

# Running tests

Run a single integration test like this:
```bash
mvn verify -Dinvoker.test=artifact-with-license-and-ignored
```

# Configuration settings

- licenseFile: The location of the licenses.xml file. Defaults to src/licenses/licenses.xml
- excludedMissingLicensesFile: The location of the file listing dependencies that should not be checked for licenses. Default blank
- excludedScopes: A list of scopes to exclude. May be used to exclude artifacts with test or provided scope from license check.
- failOnForbidden. Default true
- failOnMissing. Default true
- failOnUnknown. Default true
- failOnWarning. Default true
- requireAllValid: If a dependency provides several licenses, do you require all of them to be among your accepted licenses, or just one? Default true (requires all)
- verbose: Default false

# Contributors
- [John Allberg](https://github.com/smuda)
- [Tibor Digana](https://github.com/Tibor17)