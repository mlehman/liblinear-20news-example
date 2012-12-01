name := "Liblinear 20 Newsgroups Example"

version := "0.0.1"

scalaVersion := "2.9.2"

libraryDependencies ++= Seq(
  "org.apache.lucene" % "lucene-core" % "3.6.1",
  "org.apache.lucene" % "lucene-analyzers" % "3.6.1",
  "de.bwaldvogel" % "liblinear" % "1.92"
)