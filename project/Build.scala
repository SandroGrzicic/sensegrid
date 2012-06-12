import sbt._
import Keys._
import java.io.File
import AndroidKeys._
import cc.spray.revolver.RevolverPlugin._

/**
 * SenseGrid Android SBT build file.
 *
 */
object SenseGridBuild extends Build {

	lazy val buildSettings = Seq(
		name := "SenseGrid",
		organization := "net.sandrogrzicic.sensegrid",
		version := "0.1",
		scalaVersion := "2.9.2",
		logLevel := Level.Info
	)

	override lazy val settings = super.settings ++ buildSettings

	lazy val defaultSettings = Defaults.defaultSettings ++ Revolver.settings ++ Seq(

		resolvers ++= Seq(
			"Sonatype Snapshot Repo" at "https://oss.sonatype.org/content/repositories/snapshots/",
			"Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
			"JBoss Repo" at "http://repository.jboss.org/nexus/content/groups/public-jboss/"
		),

		scalacOptions ++= Seq("-encoding", "utf8", "-unchecked", "-deprecation"),
		javacOptions ++= Seq("-encoding", "utf8", "-Xlint:unchecked", "-Xlint:deprecation"),

		parallelExecution in GlobalScope := true,

		scalaSource in Compile <<= baseDirectory(_ / "src/main"),
		scalaSource in Test <<= baseDirectory(_ / "src/test"),

		javaSource in Compile <<= baseDirectory(_ / "src/main"),
		javaSource in Test <<= baseDirectory(_ / "src/test"),

//		classDirectory in Compile := file("R:/bin/main/"),
//		classDirectory in Test := file("R:/bin/test/"),
		classDirectory in Compile <<= baseDirectory(_ / "bin/main"),
		classDirectory in Test <<= baseDirectory(_ / "bin/main"),

		docDirectory in Compile <<= baseDirectory(_ / "doc"),

		unmanagedBase <<= baseDirectory(_ / "lib"),

		compileOrder := CompileOrder.Mixed

	)
	
	lazy val androidSettings = AndroidSdkPlugin.androidBuildSettings ++ Seq(
		libraryDependencies ++= Seq(
		),
		useProguard in Android := true,
		proguardConfig in Android <<= baseDirectory map (b => IO.readLines(b/"proguard.cfg"))
		/*binPath in Android := file("R:/bin/")*/
		/*++ (proguardConfig in Android := Seq("proguard.cfg"))*/

	)
	
	lazy val serverSettings = Seq(
		scalaVersion := "2.9.2",
		libraryDependencies ++= Seq(
			"net.databinder" %% "unfiltered" % "0.6.3",
			"net.databinder" %% "unfiltered-netty" % "0.6.3",
			"net.databinder" %% "unfiltered-netty-server" % "0.6.3"
		)
	)

	lazy val senseGridAndroidProject = Project(
		id = "android",
		base = file("android"),
		settings = defaultSettings ++ androidSettings

	) dependsOn (mapviewballoonsAndroidLibraryProject)

	lazy val mapviewballoonsAndroidLibraryProject = Project(
		id = "mapviewballoons",
		base = file("mapviewballoons"),
		settings = defaultSettings ++ androidSettings ++ Seq(
			useProguard in Android := false
		)
	)

	lazy val senseGridServerProject = Project(
		id = "server",
		base = file("server"),
		settings = defaultSettings ++ Seq(
			mainClass in (Compile, run) := Some("net.sandrogrzicic.sensegrid.Server")
		) ++ serverSettings
	)

}



