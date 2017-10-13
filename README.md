# Ayoy-License-Verifier-Plugin

Build status: 
![alt text](https://travis-ci.org/AyoyAB/Ayoy-Maven-License-Verifier-Plugin.svg?branch=master "CI status")
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/cz.jirutka.rsql/rsql-parser/badge.svg)](https://maven-badges.herokuapp.com/maven-central/cz.jirutka.rsql/rsql-parser)
[![Dependency Status](https://gemnasium.com/badges/github.com/AyoyAB/Ayoy-Maven-License-Verifier-Plugin.svg)](https://gemnasium.com/github.com/AyoyAB/Ayoy-Maven-License-Verifier-Plugin)

When developing commercial software with OSS dependencies its
very important to verify that you only use dependencies with
acceptable licenses.

This plugin will verify the licenses of the current 
project and abort build if requirements are not met.

The plugin is heavily inspired from 
https://github.com/khmarbaise/Maven-License-Verifier-Plugin
and was build simply because I could not get any
attention to pull-requests. 

I also wanted maven 3 support and I had never written
a maven plugin before, which is a reason in itself. :-)

The instructions for building a maven plugin was found
here: 
https://maven.apache.org/guides/plugin/guide-java-plugin-development.html

The instructions for reading dependencies was found
here:
https://maven.apache.org/guides/plugin/guide-java-plugin-development.html

To use on command line:
```bash
mvn se.ayoy.maven-plugins:ayoy-license-verifier-maven-plugin:verify
```

This should work but doesn't:
```bash
mvn ayoy-license-verifier:1.0.0:verify
```
