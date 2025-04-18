# jash

## Table of content

* [What is it?](#what-is-it)
* [How to use it](#how-to-use-it)
* [How to build](#how-to-build)
* [Maven Profiles](#maven-profiles)
* [Origin story](#origin-story)

## What is it?

A Java library to provide a Process interface that is fluent, predictable and with a great developer experience.

*fluent* - because it provides a fluent API to start and manage processes
*predictable* - because it provides a predictable API to start and manage processes, incl. throwing an exception when the process exits with a non-zero/not-allowed exit code.
*great developer experience* - because it provides a great developer experience to work with processes, incl. streaming process output to a logger or collecting it as a string or a list of strings.

## How to use it

Just use one of `Jash.start(String command, String...args)` or `Jash.builder(String command, String args...args)` for a more fine grained configuration:

* Will stream `Stream.of("hello", "world")`:

```java
Jash.start(
	"sh",
	"-c",
	"echo hello; echo world")
		.stream()
```

* Same as above but using a shell-style call:

```java
import static io.ongres.jash.Jash.*;

$("echo hello; echo world").stream()
```

* Same result as above but pipelining with `cat`:

```java
import static io.ongres.jash.Jash.*;
$("echo hello; echo world").pipe("cat").stream()
```

* Same result as above but passing to `cat` a pure Java Stream:

```java
Jash.start("cat")
		.inputStream(
		Stream.of("hello", "world"))
		.stream()
```

* This will print "hello" followed by "world" but will fail when terminating the Java Stream:

```java
Jash.start(
	"sh",
	"-c",
	"echo hello; echo world; exit 79")
		.stream()
		.peek(System.out::println)
		.count() // <- exception will be thrown here
```

* Same output as the above but will fail when leaving the `try` block:

```java
try (Stream<String> stream = Jash.start(
	"sh",
	"-c",
	"echo hello; echo world; exit 79")
		.withoutCloseAfterLast()
		.stream()) {
	stream
		.peek(System.out::println)
		.count();
} // <- exception will be thrown here when Stream.close() will be called
```

* Same result as above but will not fail at all:

```java
Jash.start(
	"sh",
	"-c",
	"echo hello; echo world; exit 79")
		.withAllowedExitCode(79)
		.stream()
		.peek(System.out::println)
		.count();
```

* You can also specify a timeout that will result in a `ProcessTimeoutException` exception:

```java
Jash.start(
	"sh",
	"-c",
	"sleep 3600")
		.withTimeout(Duration.of(1, ChronoUnit.SECONDS))
		.stream()
		.count(); // <- will throw an ProcessTimeoutException exception
```

## How to build

Java 8+ and Maven are required to build this project.

Run following command:

```
mvn clean package
```

## Maven Profiles

- Safer: Slower but safer profile used to look for errors before pushing to SCM

```
mvn verify -P safer
```

### Integration tests

The integration test suite requires a Unix compatible system is installed on the system and and
some very common commands (sh, cat, env and sed).
To launch the integrations tests run the following command:

```
mvn verify -P integration
```

To run integration tests with Java debugging enabled on port 8000:

```
mvn verify -P integration -Dmaven.failsafe.debug="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000"
```

## Origin story

This project was originally created as "fluent-process" by OnGres, Inc. in 2020. In 2025, it was renamed to "jash" (pronounced Jazz, a reference to Java and Shell) to reflect its focus on providing a more idiomatic Java 17+ interface for working with shell processes and streams.
