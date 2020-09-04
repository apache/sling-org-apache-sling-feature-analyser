[![Apache Sling](https://sling.apache.org/res/logos/sling.png)](https://sling.apache.org)

&#32;[![Build Status](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-feature-analyser/job/master/badge/icon)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-feature-analyser/job/master/)&#32;[![Test Status](https://img.shields.io/jenkins/tests.svg?jobUrl=https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-feature-analyser/job/master/)](https://ci-builds.apache.org/job/Sling/job/modules/job/sling-org-apache-sling-feature-analyser/job/master/test/?width=800&height=600)&#32;[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-org-apache-sling-feature-analyser&metric=coverage)](https://sonarcloud.io/dashboard?id=apache_sling-org-apache-sling-feature-analyser)&#32;[![Sonarcloud Status](https://sonarcloud.io/api/project_badges/measure?project=apache_sling-org-apache-sling-feature-analyser&metric=alert_status)](https://sonarcloud.io/dashboard?id=apache_sling-org-apache-sling-feature-analyser)&#32;[![JavaDoc](https://www.javadoc.io/badge/org.apache.sling/org.apache.sling.feature.analyser.svg)](https://www.javadoc.io/doc/org.apache.sling/org-apache-sling-feature-analyser)&#32;[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.sling/org.apache.sling.feature.analyser/badge.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.sling%22%20a%3A%22org.apache.sling.feature.analyser%22)&#32;[![feature](https://sling.apache.org/badges/group-feature.svg)](https://github.com/apache/sling-aggregator/blob/master/docs/group/feature.md) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

# Feature Model Analyser

The Analyser can analyse features for completeness and correctness. The analyser is pluggable and can also perform other checks.

The analyser can be run from the commandline by running the following main class:

```
java org.apache.sling.feature.analyser.main.Main
```

# Feature Model Analyser as a Maven Plugin

The Analyser can also be run as part of a maven build via the `slingfeature-maven-plugin`: https://github.com/apache/sling-slingfeature-maven-plugin

The following analysers are defined:

* `bundle-packages`: Checks bundle import/export package statements for consistency and completeness. Does _not_ take API Regions into account. An expanded variant of this analyser is available in [org-apache-sling-feature-extension-apiregions](https://github.com/apache/sling-org-apache-sling-feature-extension-apiregions) under the name `api-regions-exportsimports`.

* `bundle-content`: Gives a warning if a bundle container initial content specified with `Sling-Initial-Content`.

* `bundle-resources`: Gives a warning if a bundle contains resources specified with `Sling-Bundle-Resources`.

* `compare-features`: Compares the artifacts in the bundles sections or in an extension between two feature models. For more information see [below](#compare-features).

* `requirements-capabilities`: Checks bundle requirements/capabilities for consistency and completeness.

* `apis-jar`: validates that the `sourceId` property of a bundle, if defined, is a comma-separated value list of artifact ids.

Additional analysers in relation to Feature Model API Regions can be found here: https://github.com/apache/sling-org-apache-sling-feature-extension-apiregions

For further documentation see: https://github.com/apache/sling-org-apache-sling-feature/blob/master/readme.md

## `compare-features`

This analyser compares certain sections of two feature models.

This analyser requires additional configuration:

 Configuration key | Allowed values | Description 
 ----- | ----- | -----
`compare-type` | `ARTIFACTS` | The types of entities being compared. Currently only artifacts can be compared.
`compare-with` | Maven ID, e.g. `mygroup:myart:1.2.3` | The _golden_ feature to compare the features selected for the analyser with.
`compare-extension` | extension name | If this configuration is absent, the feature's bundles are compared. Otherwise the extensions with the specified name are compared. These extensions must be of type `ARTIFACTS`.
`compare-mode` | `SAME` or `DIFFERENT` | Whether the sections must be the same or must be different. Defaults to `SAME`.
`compare-metadata` | `true` or `false` | Whether to include the artifact metadata in the comparison. Defaults to `false`.
