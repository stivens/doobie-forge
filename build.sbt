publishTo := localStaging.value

organization := "io.github.stivens"
name         := "doobie-forge"
homepage     := Some(url("https://github.com/stivens/doobie-forge"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/stivens/doobie-forge"),
    "scm:git@github.com:stivens/doobie-forge.git"
  )
)
licenses := Seq("MIT" -> url("https://github.com/stivens/doobie-forge/blob/main/LICENSE"))
developers := List(
  Developer(
    id = "stivens",
    name = "Jacek Bizub",
    email = "jacekbizub@gmail.com",
    url = url("https://github.com/stivens")
  )
)

version := "0.3.0"

scalaVersion := "3.3.8"

resolvers += Resolver.sonatypeCentralSnapshots

enablePlugins(ScalafixPlugin, SemanticdbPlugin)

inThisBuild(
  List(
    semanticdbEnabled := true
  )
)

scalacOptions ++= Seq(
  "-Wunused:imports",
  "-feature",
  "-language:implicitConversions",
  "-no-indent",
  "-Xmax-inlines",
  "128",
  "-Xfatal-warnings"
)

val DoobieVersion = "1.0.0-RC12"

libraryDependencies ++= Seq(
  "io.github.stivens" %% "casecomplete" % "1.0.0",
  // Cats
  "org.typelevel" %% "cats-effect" % "3.7.1",
  // Persistence - doobie
  "org.tpolecat" %% "doobie-core"           % DoobieVersion,
  "org.tpolecat" %% "doobie-postgres"       % DoobieVersion,
  "org.tpolecat" %% "doobie-postgres-circe" % DoobieVersion,
  "org.tpolecat" %% "doobie-hikari"         % DoobieVersion,
  "org.tpolecat" %% "doobie-refined"        % DoobieVersion,
  "org.tpolecat" %% "doobie-scalatest"      % DoobieVersion % Test,
  // scalatest
  "org.scalactic" %% "scalactic" % "3.2.20",
  "org.scalatest" %% "scalatest" % "3.2.20" % Test,
  // zio
  "dev.zio" %% "zio"              % "2.1.26"    % Test,
  "dev.zio" %% "zio-interop-cats" % "23.1.0.13" % Test
)

lazy val root = project
  .in(file("."))
  .settings()
