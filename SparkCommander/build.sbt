name := "SparkCommander"

version := "1.0"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "1.2.1",
  "org.apache.hadoop" %% "hadoop-client" % "2.6.0",
  "com.github.scopt" %% "scopt" % "3.3.0"
)

resolvers += Resolver.sonatypeRepo("public")