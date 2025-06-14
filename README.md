# MinimalFTP
A lightweight, simple FTP server. Pure Java, no libraries.

## Features
Although it's named "minimal", it supports a bunch of features:

* 100% Java, no libraries
* Lightweight
* Supports 57 FTP commands
* TLS/SSL support
* Custom File System support
* Custom User Authentication support
* Custom Commands support
* Support to obsolete commands (some FTP clients might still use them)

## Specification Support
The required minimum implementation is already done, however, there are still commands that can be implemented.

* [RFC 959](https://tools.ietf.org/html/rfc959) - File Transfer Protocol (33/33)
* [RFC 697](https://tools.ietf.org/html/rfc697) - CWD Command of FTP (1/1) (Obsolete)
* [RFC 737](https://tools.ietf.org/html/rfc737) - FTP Extension: XSEN (0/3) (Obsolete)
* [RFC 743](https://tools.ietf.org/html/rfc743) - FTP extension: XRSQ/XRCP (0/4) (Obsolete)
* [RFC 775](https://tools.ietf.org/html/rfc775) - Directory oriented FTP commands (5/5) (Obsolete)
* [RFC 1123](https://tools.ietf.org/html/rfc1123#page-29) - Requirements for Internet Hosts
* [RFC 1639](https://tools.ietf.org/html/rfc1639) - FTP Operation Over Big Address Records (2/2) (Obsolete)
* [RFC 2228](https://tools.ietf.org/html/rfc2228) - FTP Security Extensions (3/8)
* [RFC 2389](https://tools.ietf.org/html/rfc2389) - Feature negotiation mechanism for the File Transfer Protocol (2/2)
* [RFC 2428](https://tools.ietf.org/html/rfc2428) - FTP Extensions for IPv6 and NATs (2/2)
* [RFC 2640](https://tools.ietf.org/html/rfc2640) - Internationalization of the File Transfer Protocol (0/1)
* [RFC 2773](https://tools.ietf.org/html/rfc2773) - Encryption using KEA and SKIPJACK (0/1)
* [RFC 3659](https://tools.ietf.org/html/rfc3659) - Extensions to FTP (4/4)
* [RFC 4217](https://tools.ietf.org/html/rfc4217) - Securing FTP with TLS
* [RFC 5797](https://tools.ietf.org/html/rfc5797) - FTP Command and Extension Registry
* [RFC 7151](https://tools.ietf.org/html/rfc7151) - File Transfer Protocol HOST Command for Virtual Hosts (1/1)
* [draft-twine-ftpmd5-00](https://tools.ietf.org/html/draft-twine-ftpmd5-00) - The "MD5" and "MMD5" FTP Command Extensions (2/2) (Obsolete)
* [draft-somers-ftp-mfxx-04](https://tools.ietf.org/html/draft-somers-ftp-mfxx-04) - The "MFMT", "MFCT", and "MFF" Command Extensions for FTP (1/3)
* [draft-bryan-ftpext-hash-02](https://tools.ietf.org/html/draft-bryan-ftpext-hash-02) - File Transfer Protocol HASH Command for Cryptographic Hashes (1/1)
* [draft-bryan-ftp-range-08](https://tools.ietf.org/html/draft-bryan-ftp-range-08) - File Transfer Protocol RANG Command for Octet Ranges (0/1)

# Usage

### Dependency
MinimalFTP is published on JCenter and Maven Central

#### Maven
```xml
<dependency>
  <groupId>com.guichaguri</groupId>
  <artifactId>minimalftp</artifactId>
  <version>1.0.7</version>
  <type>pom</type>
</dependency>
```

#### Gradle
```groovy
compile 'com.guichaguri:minimalftp:1.0.7'
```

#### Ivy
```xml
<dependency org='com.guichaguri' name='minimalftp' rev='1.0.7'>
  <artifact name='minimalftp' ext='pom' />
</dependency>
```

### API
Check out more examples [here](https://github.com/Guichaguri/MinimalFTP/tree/master/src/test/java/com/guichaguri/minimalftp) :)

```java
// Uses the current working directory as the root
File root = new File(System.getProperty("user.dir"));

// Creates a native file system
NativeFileSystem fs = new NativeFileSystem(root);

// Creates a noop authenticator, which allows anonymous authentication
NoOpAuthenticator auth = new NoOpAuthenticator(fs);

// Creates the server with the authenticator
FTPServer server = new FTPServer(auth);

// Start listening synchronously
server.listenSync(21);
```