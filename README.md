Graphite Importer
=================

Imports data from Elasticsearch to Graphite.

Building
--------

### Setup

* [sbt](http://www.scala-sbt.org/)
* [sbt-idea](https://github.com/mpeltonen/sbt-idea)

Run `sbt gen-idea` to generate an IDEA project.

### Build

Run `sbt assembly` and a fat JAR will be created in the `target/scala-2.10` folder.

Usage
-----

The fat JAR can be started with `java -jar <path to JAR file>`.
