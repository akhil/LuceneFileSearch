ThisBuild / name := "LuceneFileSearch"

ThisBuild / version := "0.1"

ThisBuild / scalaVersion := "2.13.0"

// https://mvnrepository.com/artifact/org.apache.lucene/lucene-highlighter
libraryDependencies += "org.apache.lucene" % "lucene-highlighter" % "8.2.0" withSources() withJavadoc()

// https://mvnrepository.com/artifact/org.apache.lucene/lucene-misc
libraryDependencies += "org.apache.lucene" % "lucene-misc" % "8.2.0" withSources() withJavadoc()

// https://mvnrepository.com/artifact/org.apache.lucene/lucene-demo
libraryDependencies += "org.apache.lucene" % "lucene-demo" % "8.2.0" withSources() withJavadoc()

// https://mvnrepository.com/artifact/org.apache.lucene/lucene-codecs
libraryDependencies += "org.apache.lucene" % "lucene-codecs" % "8.2.0" % Test withSources() withJavadoc()

// https://mvnrepository.com/artifact/org.apache.lucene/lucene-test-framework
libraryDependencies += "org.apache.lucene" % "lucene-test-framework" % "8.2.0" % Test withSources() withJavadoc()

// https://mvnrepository.com/artifact/org.apache.tika/tika-parsers
libraryDependencies += "org.apache.tika" % "tika-parsers" % "1.22" withSources() withJavadoc()

// https://mvnrepository.com/artifact/com.typesafe/config
libraryDependencies += "com.typesafe" % "config" % "1.3.4" withSources() withJavadoc()


assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case  _ => MergeStrategy.last
}

mainClass in assembly := Some("ui.DesktopApplication")

enablePlugins(JavaAppPackaging)

scriptClasspath := Seq("*")