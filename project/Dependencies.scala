import sbt._

object Dependencies {

  private val gatlingVersion = "3.4.2"

  val test = Seq(
    "uk.gov.hmrc"          %% "performance-test-runner"   % "6.3.0"         % Test,
  )

}
