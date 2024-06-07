[![Apache Sling](https://sling.apache.org/res/logos/sling.png)](https://sling.apache.org)

&#32;[![Build Status](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-feature-analyser/job/master/badge/icon)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-feature-analyser/job/master/)&#32;[![Test Status](https://img.shields.io/jenkins/tests.svg?jobUrl=https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-feature-analyser/job/master/)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-feature-analyser/job/master/test/?width=800&height=600)&#32;[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-org-apache-sling-feature-analyser&metric=coverage)](https://sonarcloud.io/dashboard?id=apache_sling-org-apache-sling-feature-analyser)&#32;[![Sonarcloud Status](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-org-apache-sling-feature-analyser&metric=alert_status)](https://sonarcloud.io/dashboard?id=apache_sling-org-apache-sling-feature-analyser)&#32;[![JavaDoc](https://www.javadoc.io/badge/org.apache.sling/org.apache.sling.feature.analyser.svg)](https://www.javadoc.io/doc/org.apache.sling/org-apache-sling-feature-analyser)&#32;[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.sling/org.apache.sling.feature.analyser/badge.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.sling%22%20a%3A%22org.apache.sling.feature.analyser%22)&#32;[![feature](https://sling.apache.org/badges/group-feature.svg)](https://github.com/apache/sling-aggregator/blob/master/docs/group/feature.md) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# Feature Model Analyser

The Analyser can analyse features for completeness and correctness. The analyser is pluggable and can perform custom checks.

## Running the Analyser as a Maven Plugin

The Analyser can also be run as part of a Maven build via the [slingfeature-maven-plugin](https://github.com/apache/sling-slingfeature-maven-plugin)

# Analyser Tasks

Below is a list of built-in analysers. Additional analysers in relation to Feature Model API Regions can be found in [org-apache-sling-feature-extension-apiregions](https://github.com/apache/sling-org-apache-sling-feature-extension-apiregions), analysers performing checks on class level can be found in [org-apache-sling-feature-analyser-classes](https://github.com/apache/sling-org-apache-sling-feature-analyser-classes).

For further documentation see: [Feature Model](https://github.com/apache/sling-org-apache-sling-feature/blob/master/readme.md)

## `apis-jar`

This analyser task validates the metadata in the feature model for the `apis-jar` goal of the [slingfeature-maven-plugin](https://github.com/apache/sling-slingfeature-maven-plugin).

## `bundle-connect`

Checks whether the feature is ready for [OSGi connect](http://docs.osgi.org/specification/osgi.core/8.0.0/framework.connect.html). Bundle with embedded jars are not allowed and packages between bundles must not overlap.

## `bundle-content`

Gives a warning if a bundle contains initial content specified with `Sling-Initial-Content`.

## `bundle-nativecode`

Checks for native code instructions in bundles and errors if found.

## `bundle-packages`

Checks bundle import/export package statements for completeness. Does _not_ take API Regions into account. An expanded variant of this analyser is available in [org-apache-sling-feature-extension-apiregions](https://github.com/apache/sling-org-apache-sling-feature-extension-apiregions) under the name `api-regions-exportsimports`.

## `bundle-resources`

Gives a warning if a bundle contains resources specified with `Sling-Bundle-Resources`.

## `bundle-unversioned-packages`

Checks bundle import/export package statements for missing version information.

## `check-unused-bundles`

Checks for unused bundles, bundles with exports which are not imported.

## `compare-features`

Compares the artifacts in the bundles sections or in an extension between two feature models.

This analyser requires additional configuration:

 Configuration key | Allowed values | Description
 ----- | ----- | -----
`compare-type` | `ARTIFACTS` | The types of entities being compared. Currently only artifacts can be compared.
`compare-with` | Maven ID, e.g. `mygroup:myart:1.2.3` | The _golden_ feature to compare the features selected for the analyser with.
`compare-extension` | extension name | If this configuration is absent, the feature's bundles are compared. Otherwise the extensions with the specified name are compared. These extensions must be of type `ARTIFACTS`.
`compare-mode` | `SAME` or `DIFFERENT` | Whether the sections must be the same or must be different. Defaults to `SAME`.
`compare-metadata` | `true` or `false` | Whether to include the artifact metadata in the comparison. Defaults to `false`.

## `content-packages-dependencies`

Checks the dependencies between content packages.

## `content-packages-installables`

Checks that content packages do not contain installables for the OSGi installer like bundles or configurations.

## `content-packages-paths`

This analyser checks for allowed and denied paths inside content packages. This analyser requires additional configuration:

 Configuration key | Allowed values | Description
 ----- | ----- | -----
`includes` | Content paths | A comma separated list of content paths. If this is specified all content in the content package must match at least one of these.
`excludes` | Content paths | A comma separated list of content paths. If this is specified all content in the content package must not match any of these - except it matches an include.

## `content-packages-validation`

Runs the default [filevault validators](https://jackrabbit.apache.org/filevault/validation.html) on the content packages.

 Configuration key | Allowed values | Description
 ----------------- | -------------- | -----
`enabled-validators`     | validator ids  | A comma separated list of validator-ids to enable
`max-report-level`       | severity level | Maximum severity level to report. (INFO, WARN, ERROR) defaults to WARN. Higher level messages will be downgraded to the sepcified level. The default will never break a build.

## `duplicate-symbolic-names`

Checks if there are duplicates of symbolic names for bundles.

## `feature-id`

This analyser checks that the feature id matches one of the given accepted feature ids. If it doesn't it will emit an error.

This analyser requires additional configuration:

 Configuration key | Allowed values | Description
 ----- | ----- | -----
`accepted-feature-ids` | comma-separated list of Maven IDs | The Maven ID/coordinates have the format `groupId:artifactId[:packaging[:classifier]]:version`. Each item is either a string which must be equal to the according item of the feature id, or a `*` which acts as wildcard (i.e. everything matches).

## `repoinit`

Checks the syntax of all repoinit sections.

## `requirements-capabilities`

Checks bundle requirements/capabilities for consistency and completeness.

# Extensions

## `analyser-metadata`

Generates additional metadata that will be recorded in the feature model definition. It is configured by defining an `analyser-metadata` section in the feature model definition. The section will be processed by the extension when the feature models are aggregated and will be replaced with the required entries for bundles matching the configuration.

The section can have entries that match individual bundle names and entries that match based on regular expressions (if the key contains the "*" character).

Each individual entry can contain the following keys:


Configuration key | Allowed values | Description
 ----- | ----- | -----
`manifest` | `null` or Object | If null, the manifest is not generated. If an object, the values are copied over. If absent, the values are extracted from the OSGi bundle
`report` | Object with keys `warning` and `error` | If any of the values are set to `false`, reporting is suppressed for those kind of occurences.

A typical configuration for platform applications is:

```javascript
{
    "analyser-metadata:JSON|true":
    {
      ".*" : {
        "manifest": null,
        "report": {
          "error": false,
          "warning": false
        }
      }
    }
}
```

This ensures that warnings related to the platform are not reported when the feature is aggregated with downstream (consumer) applications. The manifests should not be inlined under normal circumstances, since it greatly increases the size of the resulting features.
