# Pencil 
[![Join the chat at https://gitter.im/minosiants/pencil](https://badges.gitter.im/minosiants/pencil.svg)](https://gitter.im/minosiants/pencil?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
![build](https://github.com/minosiants/pencil/workflows/build/badge.svg)

### Overview 
`Pencil` is a simple smtp client. The main goal is to be able to send emails in the simplest way.   
It is build on top of [cats](https://typelevel.org/cats/), [cats-effect](https://typelevel.org/cats-effect/), [fs2](https://fs2.io/), [scodec](http://scodec.org/)

`Pencil` supports: 
* Text email (ascii)
* Mime email 
* TLS
* Authentication

### Specifications

* [RFC 5321 - Simple Mail Transfer Protocol](https://tools.ietf.org/html/rfc5321)
* Multipurpose Internet Mail Extensions
  * [RFC 2045 - Part one - Format of Internet message bodies](https://tools.ietf.org/html/rfc2045)
  * [RFC 2046 - Part two - Media Types](https://tools.ietf.org/html/rfc2046)
  * [RFC 2047 - Part three - Message Header Extensions for Non-ASCII Text](https://tools.ietf.org/html/rfc2047)
  * [RFC 2049 - Part five - Confirmance criteria and examples](https://tools.ietf.org/html/rfc2049)
  * [RFC 4288 - Media Type Specifications and Registration Procedures](https://tools.ietf.org/html/rfc4288)
  * [RFC 1521 - MIME Part one - Mechanisms for Specifying and Describing the Format of Internet Message Bodies](https://tools.ietf.org/html/rfc1521)
  * [RFC 1522 - MIME Part two - Message Header Extensions for Non-ASCII Text](https://tools.ietf.org/html/rfc1522)
  * [RFC 4954 - SMTP Service Extension for Authentication](https://tools.ietf.org/html/rfc4954)


### Usage
Add dependency to your `build.sbt`

#### for scala 3 
```scala
libraryDependencies += "com.minosiants" %% "pencil" % "2.0.0"
```
#### for scala 2.13
```scala
libraryDependencies += "com.minosiants" %% "pencil" % "1.2.0"
```

### Examples how to use it


#### Create text email

```scala
val email = Email.text(
      from"user name <user1@mydomain.tld>",
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

#### Create mime email

```scala
val email = Email.mime(
     from"user1@mydomain.tld",
     to"user1@example.com",
     subject"привет",
     Body.Alternative(List(Body.Utf8("hi there3"), Body.Ascii("hi there2")))
)
```

#### Send email

```scala
object Main extends IOApp {

  val logger    = Slf4jLogger.getLogger[IO]
  override def run(args: List[String]): IO[ExitCode] = {
    val credentials = Credentials(
      Username("user1@mydomain.tld"),
      Password("password")
    )
    val action = for {
      tls <- Network[IO].tlsContext.system
      client = Client[IO](SocketAddress(host"localhost", port"25"), Some(credentials))(tls,logger)
      response <- client.send(email)
    }yield response

    action.attempt
            .map {
              case Right(replies) =>
                println(replies)
                ExitCode.Success
              case Left(error) =>
                error match {
                  case e: Error => println(e.toString)
                  case e: Throwable => println(e.getMessage)
                }
                ExitCode.Error
            }
  }
}

```
## Docker Mailserver
 For test purposes [Docker Mailserver](https://github.com/jeboehm/docker-mailserver) can be used
