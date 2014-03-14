Graphite Importer
=================

Continuously import data from Elasticsearch to Graphite.
Its built as an Akka application to be extensible and easy to create different measurements.

Currently imports Jenkins data created with [elasticsearch-jenkins](https://github.com/speedledger/elasticsearch-jenkins).

Building
--------

### Setup

* [sbt](http://www.scala-sbt.org/)

Recommended IDE is IntelliJ IDEA (version >= 13). Simply clone the repo and open the `build.sbt` file with IntelliJ.

### Build

Run `sbt assembly` and a fat JAR will be created in the `target/scala-2.10` folder.

Usage
-----

The fat JAR can be started with `java -jar <path to JAR file>`.
