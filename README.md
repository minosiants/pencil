# Pencil 

![build](https://github.com/minosiants/pencil/workflows/build/badge.svg)

### Overview 
Pencil is a smtp-client. It is build on top of [cats](https://typelevel.org/cats/), [cats-effect](https://typelevel.org/cats-effect/), [fs2](https://fs2.io/), [scodec](http://scodec.org/)

### Usage
Add dependency to your `build.sbt`

```scala
resolvers += "Github packages minosiants" at "https://maven.pkg.github.com/minosiants/_"

libraryDependencies += "com.minosiatns" %% "pencil" % "0.0.1"
```

### Examples how to use it


#### Create text email

```scala
val email = Email.text(
      from"user1@mydomain.tld",
      to"user1@example.com",
      subject"first email",
      Body.Ascii("hello")
)
```
#### Create mime email

```scala
val email = Email.mime(
     from"user1@mydomain.tld",
     to"user1@example.com",
     subject"привет",
     Body.Utf8("hi there")
) + attachment"path/to/file"
```
#### Send email

```scala
object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    Blocker[IO]
      .use { blocker =>
        SocketGroup[IO](blocker).use { sg =>
          val client = Client("127.0.0.1")(sg)
          client
            .send(email)
            .attempt
            .map {
              case Right(value) =>
                ExitCode.Success
              case Left(error) =>
                error match {
                  case e: Error => println(e.show)
                  case e: Throwable => println(e.getMessage)
                }
                ExitCode.Error
            }
        }
      }
}

```
## Docker Mailserver
 For test purposes [Docker Mailserver](https://github.com/jeboehm/docker-mailserver) can be used