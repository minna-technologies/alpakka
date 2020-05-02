import sbt._, Keys._

object Publish extends AutoPlugin {
  override def trigger = allRequirements

  override def projectSettings = Seq(
    publishMavenStyle in ThisBuild := true,
    publishTo in ThisBuild := {
      val bucket = "s3://minna-tech-maven.s3.amazonaws.com"
      if (isSnapshot.value)
        Some("Minna Tech Maven snapshots" at bucket + "/snapshots")
      else Some("Minna Tech Maven releases" at bucket + "/releases")
    }
  )
}
