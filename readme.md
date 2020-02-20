[<img src="http://sling.apache.org/res/logos/sling.png"/>](http://sling.apache.org)

 [![Build Status](https://builds.apache.org/buildStatus/icon?job=sling-org-apache-sling-feature-analyser-1.8)](https://builds.apache.org/view/S-Z/view/Sling/job/sling-org-apache-sling-feature-analyser-1.8) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.sling/org.apache.sling.feature.analyser/badge.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.apache.sling%22%20a%3A%22org.apache.sling.feature.analyser%22) [![JavaDocs](https://www.javadoc.io/badge/org.apache.sling/org.apache.sling.feature.analyser.svg)](https://www.javadoc.io/doc/org.apache.sling/org.apache.sling.feature.analyser) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0) [![feature](https://sling.apache.org/badges/group-feature.svg)](https://github.com/apache/sling-aggregator/blob/master/docs/groups/feature.md)

# Feature Model Analyser

The Analyser can analyse features for completeness and correctness. The analyser is pluggable and can also perform other checks.

The analyser can be run from the commandline by running the following main class:

```
java org.apache.sling.feature.analyser.main.Main
```

# Feature Model Analyser as a Maven Plugin

The Analyser can also be run as part of a maven build via the `slingfeature-maven-plugin`: https://github.com/apache/sling-slingfeature-maven-plugin

The following analysers are defined:

* `bundle-packages`: Checks bundle import/export package statements for consistency and completeness. If API Regions are used this analyser includes this 
information as part of the check, to ensure that bundles don't import packages of which they have no visibility because of API Regions restrictions.

* `bundle-content`: Gives a warning if a bundle container initial content specified with `Sling-Initial-Content`.

* `bundle-resources`: Gives a warning if a bundle contains resources specified with `Sling-Bundle-Resources`.

* `requirements-capabilities`: Checks bundle requirements/capabilities for consistency and completeness.


* `apis-jar`: validates that the `sourceId` property of a bundle, if defined, is a comma-separated value list of artifact ids.

There are a number of analysers which relate to API Region definitions in Feature Models. 

* `api-regions`: This analyser ensures that packages listed as exports in API-Regions sections are actually exported by a bundle that's part of the feature.

* `api-regions-dependencies`: This analyser checks that packages in API regions listed earlier in the API-Regions declaration have no dependency on API regions listed later in the list. This include `Import-Package` style dependencies and also uses-clause type dependencies. Later API regions also include packages from earlier declared API regions, but not the other way around.
  * Configuration parameters:
  * `exporting-apis`: the name of the region that provides the visible APIs.
  * `hiding-apis`: the name of the region that is 'hidden' i.e. not as visible as the exporting one. The
packages in the `exporting-api` cannot depend on any packages from this region. 

* `api-regions-duplicates`: This analyser ensures that packages are only listed in one region
in a given feature. If the same package is listed in multiple regions this will be an error.

* `api-regions-check-order`: This analyser checks that regions are defined in the specified 
order and that the same region is only declared once. Later regions inherit the packages
expose in earlier regions in the list, so the order is important.
  * Configuration parameters:
  * `order`: A comma separated list of the region names declaring the order in which they should be found. Not all regions declared must be present, but if they are present this
order must be obeyed.
 

For further documentation see: https://github.com/apache/sling-org-apache-sling-feature/blob/master/readme.md
