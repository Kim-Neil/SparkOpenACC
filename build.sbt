name := "SparkMatrixMultiply"

version := "1.0"

scalaVersion := "2.12.10"  // Match your Spark version's Scala version

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "3.3.1",
  "org.apache.spark" %% "spark-sql" % "3.3.1",
  "org.apache.spark" %% "spark-hive" % "3.3.1" % "provided",
  "org.apache.spark" %% "spark-mllib" % "3.3.1",
  "org.apache.spark" %% "spark-graphx" % "3.3.1",
  // Add any other dependencies your project needs
)


