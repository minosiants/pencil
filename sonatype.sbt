// Your profile name of the sonatype account. The default is the same with the organization value
sonatypeProfileName := "com.minosiants"

// To sync with Maven central, you need to supply the following information:
publishMavenStyle := true

// Open-source license of your choice
licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

// Where is the source code hosted: GitHub or GitLab?
import xerial.sbt.Sonatype._
sonatypeProjectHosting := Some(
  GitHubHosting("minosiants", "pencil", "k@minosiants.com")
)

developers := List(
  Developer(
    id = "minosiants",
    name = "kaspar",
    email = "k@minosiants.com",
    url = url("http://minosiants.com")
  )
)
