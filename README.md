# Ayoy-License-Verifier-Plugin

[![Travis-CI](https://travis-ci.org/AyoyAB/Ayoy-Maven-License-Verifier-Plugin.svg?branch=master "CI status")](https://travis-ci.org/AyoyAB/Ayoy-Maven-License-Verifier-Plugin)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/se.ayoy.maven-plugins/ayoy-license-verifier-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/se.ayoy.maven-plugins/ayoy-license-verifier-maven-plugin)

When developing commercial software with OSS dependencies its
very important to verify that you only use dependencies with
acceptable licenses.

This plugin will verify the licenses of the current 
project and abort build if requirements are not met.

The plugin is heavily inspired from 
[https://github.com/khmarbaise/Maven-License-Verifier-Plugin](khmarbaise/Maven-License-Verifier-Plugin). 

I wanted maven 3 support and I had never written
a maven plugin before, which is a reason in itself. :-)

To use on command line:
```bash
mvn se.ayoy.maven-plugins:ayoy-license-verifier-maven-plugin:verify
```
