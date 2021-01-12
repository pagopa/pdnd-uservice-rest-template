import scala.sys.process.Process

ThisBuild / scalaVersion := "3.0.0-M3"
ThisBuild / organization := "it.pagopa"
ThisBuild / organizationName := "Pagopa S.p.A."
ThisBuild / libraryDependencies ++= Dependencies.Jars.`server`
ThisBuild / parallelExecution in Test := false

scalacOptions ++= Seq(
  "-Xfatal-warnings"
)

lazy val generateCode = taskKey[Unit]("A task for generating the code starting from the swagger definition")

generateCode := {
  import sys.process._
  val output = Process(
    s"""openapi-generator generate -t template/scala-akka-http-server
      |                           -i src/main/resources/interface-specification.yml
      |                           -g scala-akka-http-server
      |                           -p projectName=${name.value}
      |                           -p invokerPackage=it.pagopa.pdnd.uservice.template.server
      |                           -p modelPackage=it.pagopa.pdnd.uservice.template.model
      |                           -p apiPackage=it.pagopa.pdnd.uservice.template.api
      |                           -p dateLibrary=java8
      |                           -o generated""".stripMargin
  ).!!
  println(output)
}

(compile in Compile) := ((compile in Compile) dependsOn generateCode).value

lazy val generated = project.in(file("generated")).settings()

lazy val root = (project in file(".")).
  settings(
    name := "pdnd-uservice-template",
    libraryDependencies ++= Dependencies.Jars.`server`,
    parallelExecution in Test := false,
    packageName in Docker := s"services/${name.value}",
    daemonUser in Docker  := "daemon",
    dockerRepository in Docker := Some(System.getenv("DOCKER_REPO")),
    version in Docker := s"${(version in ThisBuild).value}-${Process("git log -n 1 --pretty=format:%h").lineStream.head}",
    dockerExposedPorts in Docker := Seq(8080),
    dockerBaseImage in Docker := "openjdk:8-jre-alpine",
    dockerUpdateLatest in Docker := true).
  dependsOn(generated).
  aggregate(generated).
  enablePlugins(AshScriptPlugin, DockerPlugin)
