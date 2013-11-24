import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "CollabDraw"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "mysql" % "mysql-connector-java" % "5.1.19"    
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = JAVA).settings(
      // Add your own project settings here      
    )

}
