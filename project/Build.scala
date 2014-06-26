/*
 * ecalogic: a tool for performing energy consumption analysis.
 *
 * Copyright (c) 2013, J. Neutelings, D. Peelen, M. Schoolderman
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *   Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 *
 *   Neither the name of the Radboud University Nijmegen nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import sbt._
import sbt.Keys._

import com.earldouglas.xsbtwebplugin.WebPlugin._

import java.io.IOException

object Build extends sbt.Build {

  lazy val distribution = taskKey[Seq[File]]("Creates distributable archives.")

  lazy val baseSettings = Seq (
    organization        := "nl.ru.cs.ecalogic",
    version             := "0.2-SNAPSHOT",
    scalaVersion        := "2.11.1",
    crossPaths          := false,
    scalacOptions      ++= Seq("-deprecation", "-unchecked", "-encoding", "UTF8")
  )

  lazy val main = project in file(".") aggregate LocalProject("web") settings(baseSettings: _*) settings (
    name                               := "ecalogic",
    mainClass in (Compile, packageBin) := Some("nl.ru.cs.ecalogic.ECALogic"),
    unmanagedClasspath in Runtime      += baseDirectory.value / "conf",
    libraryDependencies               ++= Seq(
      "org.scalatest"             %% "scalatest"        % "2.2.0"  % "test",
//      "org.apache.httpcomponents" %  "httpclient"       % "4.3.4"  exclude("commons-logging", "commons-logging"),
//      "org.apache.commons"        %  "commons-math3"    % "3.3",
//      "org.slf4j"                 %  "log4j-over-slf4j" % "1.7.7",
//      "org.slf4j"                 %  "jcl-over-slf4j"   % "1.7.7",
//      "org.slf4j"                 %  "jul-to-slf4j"     % "1.7.7",
      "org.slf4j"                 %  "slf4j-api"        % "1.7.7",
      "ch.qos.logback"            %  "logback-classic"  % "1.0.13" % "runtime"
    ),
//    unmanagedJars in Compile          <<= (unmanagedJars in Compile) dependsOn ((unmanagedBase in Compile, streams) map {
//      (unmanagedBase, streams) =>
//        val name = "jas-2.5.4821-bin.jar"
//        val url = "http://krum.rz.uni-mannheim.de/jas/" + name
//        val file = unmanagedBase / name
//        if (!file.exists()) {
//          streams.log.info("Downloading JAS library ...")
//          IO.download(new URL(url), file)
//          streams.log.info("Done.")
//        }
//    }),
    exportJars                         := true,
    fork in run                        := true,

    resourceDirectory in distribution  := (sourceDirectory in Compile).value / "dist",
    distribution                       := {
      val artifactPath = (Keys.artifactPath in (Compile, packageBin)).value
      val streams = Keys.streams.value
      val target = Keys.target.value / "dist" / (name.value + "-" + version.value)

      IO.delete(target)
      IO.createDirectory(target)
      IO.copyDirectory((resourceDirectory in distribution).value, target)
      (fullClasspath in Runtime).value.filterNot(_.data.isDirectory).foreach { f =>
        IO.copyFile(f.data, target / "lib" / f.data.getName)
      }

      val files = (target ** ("*" -- DirectoryFilter)).pair(Path.relativeTo(target.getParentFile))

      val result = Seq.newBuilder[File]

      val zipFile = target.getParentFile / artifactPath.getName.replace(".jar", ".zip")
      val tarFile = target.getParentFile / artifactPath.getName.replace(".jar", ".tar.gz")
      IO.zip(files, zipFile)
      result += zipFile

      try {
        val exitCode = Process(Seq("tar", "-cvzf", tarFile.getName, target.getName + "/"), target.getParentFile).!
        if (exitCode == 0) {
          result += tarFile
        } else {
          streams.log.error("Error creating tar file.")
        }
      } catch {
        case e: IOException => streams.log.warn("Unable to spawn tar process. Skipped tar generation.")
      }

      result.result()
    }
  )

  lazy val web = project dependsOn main settings(baseSettings: _*) settings (webSettings: _*) settings (
    name                 := "ecalogic-webapp",
    libraryDependencies ++= Seq(
      "net.liftweb"             %% "lift-webkit"               % "2.6-M4",
//      "org.slf4j"               %  "jul-to-slf4j"              % "1.7.7",
//      "org.slf4j"               %  "slf4j-api"                 % "1.7.7",
//      "ch.qos.logback"          %  "logback-classic"           % "1.0.13"  % "runtime",

      "javax.servlet"           %  "javax.servlet-api"         % "3.0.1"   % "provided",

//      "ch.qos.logback"          %  "logback-access"            % "1.0.13"  % "container",
      "org.eclipse.jetty"       % "jetty-webapp"               % "9.1.0.+" % "container",
      "org.eclipse.jetty"       % "jetty-plus"                 % "9.1.0.+" % "container"
//      "org.apache.tomcat.embed" %  "tomcat-embed-core"         % "7.0.22" % "container",
//      "org.apache.tomcat.embed" %  "tomcat-embed-logging-juli" % "7.0.22" % "container",
//      "org.apache.tomcat.embed" %  "tomcat-embed-jasper"       % "7.0.22" % "container"
    ),
    distribution         := Seq((Keys.`package` in Compile).value)
    // unmanagedResourceDirectories in Test <++= PluginKeys.webappResources in Compile
  )

}
