ThisBuild / name := "LuceneFileSearch"

ThisBuild / version := "0.1"

ThisBuild / scalaVersion := "2.13.1"

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation")

val luceneVersion = "8.3.0"

// https://mvnrepository.com/artifact/org.apache.lucene/lucene-highlighter
libraryDependencies += "org.apache.lucene" % "lucene-highlighter" % luceneVersion withSources() withJavadoc()

// https://mvnrepository.com/artifact/org.apache.lucene/lucene-misc
libraryDependencies += "org.apache.lucene" % "lucene-misc" % luceneVersion withSources() withJavadoc()

// https://mvnrepository.com/artifact/org.apache.lucene/lucene-demo
libraryDependencies += "org.apache.lucene" % "lucene-demo" % luceneVersion withSources() withJavadoc()

// https://mvnrepository.com/artifact/org.apache.lucene/lucene-codecs
libraryDependencies += "org.apache.lucene" % "lucene-codecs" % luceneVersion % Test withSources() withJavadoc()

// https://mvnrepository.com/artifact/org.apache.lucene/lucene-test-framework
libraryDependencies += "org.apache.lucene" % "lucene-test-framework" % luceneVersion % Test withSources() withJavadoc()

// https://mvnrepository.com/artifact/org.apache.tika/tika-parsers
libraryDependencies += "org.apache.tika" % "tika-parsers" % "1.22" withSources() withJavadoc()

// https://mvnrepository.com/artifact/com.typesafe/config
libraryDependencies += "com.typesafe" % "config" % "1.4.0" withSources() withJavadoc()


// https://mvnrepository.com/artifact/com.typesafe.akka/akka-http
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.1.10" withSources() withJavadoc()

// https://mvnrepository.com/artifact/com.typesafe.akka/akka-stream
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.6.0" withSources() withJavadoc()

// https://mvnrepository.com/artifact/com.google.code.gson/gson
libraryDependencies += "com.google.code.gson" % "gson" % "2.8.6"

// https://mvnrepository.com/artifact/io.undertow/undertow-core
libraryDependencies += "io.undertow" % "undertow-core" % "2.0.28.Final" withSources() withJavadoc()

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case  _ => MergeStrategy.last
}

mainClass in assembly := Some("web.WebServer")
