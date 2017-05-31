# MinimalFTP [![Build Status](https://travis-ci.org/Guichaguri/MinimalFTP.svg?branch=master)](https://travis-ci.org/Guichaguri/MinimalFTP)
A lightweight, simple FTP server. Pure Java, no libraries.

## Specification Support
The required minimum implementation is already done, however, there are still commands that can be implemented.

* [RFC 959](https://tools.ietf.org/html/rfc959) - File Transfer Protocol (33/33)
* [RFC 697](https://tools.ietf.org/html/rfc697) - CWD Command of FTP (1/1)
* [RFC 737](https://tools.ietf.org/html/rfc737) - FTP Extension: XSEN (0/3)
* [RFC 743](https://tools.ietf.org/html/rfc743) - FTP extension: XRSQ/XRCP (0/4)
* [RFC 775](https://tools.ietf.org/html/rfc775) - Directory oriented FTP commands (5/5)
* [RFC 1123](https://tools.ietf.org/html/rfc1123#page-29) - Requirements for Internet Hosts
* [RFC 1639](https://tools.ietf.org/html/rfc1639) - FTP Operation Over Big Address Records (0/2)
* [RFC 2228](https://tools.ietf.org/html/rfc2228) - FTP Security Extensions (0/8)
* [RFC 2389](https://tools.ietf.org/html/rfc2389) - Feature negotiation mechanism for the File Transfer Protocol (0/2)
* [RFC 2428](https://tools.ietf.org/html/rfc2428) - FTP Extensions for IPv6 and NATs (0/2)
* [RFC 2640](https://tools.ietf.org/html/rfc2640) - Internationalization of the File Transfer Protocol (0/1)
* [RFC 2773](https://tools.ietf.org/html/rfc2773) - Encryption using KEA and SKIPJACK
* [RFC 3659](https://tools.ietf.org/html/rfc3659) - Extensions to FTP (2/4)
* [RFC 4217](https://tools.ietf.org/html/rfc4217) - Securing FTP with TLS
* [RFC 7151](https://tools.ietf.org/html/rfc7151) - File Transfer Protocol HOST Command for Virtual Hosts (0/1)

# Usage

### Dependency
#### Maven
```xml
<dependency>
  <groupId>guichaguri.minimalftp</groupId>
  <artifactId>minimalftp</artifactId>
  <version>1.0.1</version>
  <type>pom</type>
</dependency>
```

#### Gradle
```groovy
compile 'guichaguri.minimalftp:minimalftp:1.0.1'
```

#### Ivy
```xml
<dependency org='guichaguri.minimalftp' name='minimalftp' rev='1.0.1'>
  <artifact name='minimalftp' ext='pom' />
</dependency>
```

### API
Check out more examples [here](https://github.com/Guichaguri/MinimalFTP/tree/master/src/test/java/guichaguri/minimalftp) :)

```java
// Uses the current working directory as the root
File root = new File(System.getProperty("user.dir"));

// Creates a native file system
NativeFileSystem fs = new NativeFileSystem(root);

// Creates a noop authenticator
NoOpAuthenticator auth = new NoOpAuthenticator(fs);

// Creates the server with the authenticator
FTPServer server = new FTPServer(auth);

// Start listening synchronously
server.listenSync(21);
```