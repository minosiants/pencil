# smtp-client


### Apache James
 
[Apache James](https://james.apache.org/index.html) is used as a test smtp server

Default domain named james.local and three default users: user01, user02, user03, with their default password being 1234.
Note: this James server will respond to IMAP port 143 and SMTP port 25.

```aidl
docker run -p "25:25" -p "143:143" linagora/james-jpa-sample:3.4.0 -e log4j.logger.james.smtpserver=DEBUG, SMTPSERVER -v /Users/kaspar/james/logs:/logs
``` 
[Quick start](https://james.apache.org/server/quick-start.html) might be interesting if run it without container


* cd bin
  * james-cli -h localhost -p 9999 adddomain mydomain.tld
  * james-cli -h localhost -p 9999 adduser user1@mydomain.tld 1234

  * The username to use in your mail client will be myuser@mydomain.tld


mailbox or address = name@domain

commands starts with verb
responses starts with 3 digits code
ascii symbols

ehlo = EHLO SP ( Domain / address-literal ) CRLF
mail = MAIL FROM:<userx@y.foo.org> CRLF
rcpt = "RCPT TO:" ( "<Postmaster@" Domain ">" / "<Postmaster>"
/ Forward-path ) [SP Rcpt-parameters] CRLF

data = "DATA" CRLF  response 354

rset = "RSET" CRLF

quit = "QUIT" CRLF

    



https://serversmtp.com/smtp-error/