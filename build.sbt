scalaVersion := "3.3.6"

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

val DoobieVersion = "1.0.0-RC10"

libraryDependencies ++= Seq(
  "io.github.stivens" %% "casecomplete" % "0.2.2",
  // Cats
  "org.typelevel" %% "cats-effect" % "3.6.3",
  // Persistence - doobie
  "org.tpolecat" %% "doobie-core"           % DoobieVersion,
  "org.tpolecat" %% "doobie-postgres"       % DoobieVersion,
  "org.tpolecat" %% "doobie-postgres-circe" % DoobieVersion,
  "org.tpolecat" %% "doobie-hikari"         % DoobieVersion,
  "org.tpolecat" %% "doobie-refined"        % DoobieVersion,
  "org.tpolecat" %% "doobie-scalatest"      % DoobieVersion % "test",
  // scalatest
  "org.scalactic" %% "scalactic" % "3.2.19",
  "org.scalatest" %% "scalatest" % "3.2.19" % "test"
)

lazy val root = project
  .in(file("."))
  .settings()
