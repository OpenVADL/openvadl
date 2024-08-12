# The `vadl-test` Module

This module exists to run integration tests on OpenVADL.
To write integration tests (tests from VADL source code), we need a frontend.
As the current OpenVADL frontend is in very early stages, we have to use the (old-)VADL frontend.
While the `vadl` project has `open-vadl` as dependency,
this is not the case for the other way around.
So we can’t use the VADL frontend directly within OpenVADL.

To still write integration test, this module provides the
`TestFrontend` and the `TestFrontend.Provider` interface. In the `vadl` project
is a class called `OpenVADLTestSuite` which provides and sets an implementation
of these interfaces before calling the tests written in this module. So you can
write tests by interacting with the provided `TestFrontend` to let vadl source
code compile and execute tests.

If you write tests, your test class should extend the `AbstractTest` class
which provides the `runAndGetVIAMSpecification` and `runAndAssumeFailure` methods
so you don't have to deal with the `TestFrontend` yourself.

## Test sources

All test sources (vadl source code) should be placed into the `resources/testSource`
directory of this module. This is the root of reference when using methods
of the `AbstractTest` class.

## Running the tests

All tests that use the frontend (or extend `AbstractTest`) must be started
from the (old-)`vadl` project.

The command to run the OpenVADLTestSuite (all tests in `vadl-test`)

```bash
./.gradlew test --tests at.ac.tuwien.complang.vadl.tests.OpenVADLTestSuite -PnoXtendCompile      
```

The `-PnoXtendCompile` prevents that Gradle recompiles the `xtend` source code when something in `open-vadl` changes.
However, if you made changes to the `vadl` project, you have to either remove the flag from the command
or rebuilt the project in another way.

### Running specific tests

I you don't want to run all tests, you can specify the `-PopenVadl.tests` property.
The value format is `(<className>[#<methodName>],)+`.
The following example executes all tests in the `FormatTest` class.

```bash
./.gradlew test --tests at.ac.tuwien.complang.vadl.tests.OpenVADLTestSuite -PnoXtendCompile -PopenVadl.tests=FormatTest
```

**Note:** If you use `@ParameterizedTest` or `@TestFactory` in your tests, you won't be able to execute
them specifically.
However, they’re executed when executing all of them.

### Using Intellij

The above method is often not very useful (no debugging, a lot of logs). Therefore, you should
use a Intellij configuration to run the tests:

1. Create a new run configuration of type `Gradle`
2. Set the `Run` field to
    ```bash
   --tests at.ac.tuwien.complang.vadl.tests.OpenVADLTestSuite -PnoXtendCompile
   ```
3. For the `Gralde project` select the (old-)`vadl` project path

## Macros using Apache Velocity

The `AbstractTest` class uses Apache Velocity to pre-process all test files before
they get passed to the frontend. This allows you to use macros in your test source,
which is handy if your test has a lot of boilerplate vadl code.

Take a look at the `canonicalization/valid_builtin_constant_eval.vadl` test source,
which makes extensive use of macros.

