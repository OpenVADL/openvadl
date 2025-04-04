<p align="center">
  <p align="center">
  <a href="https://openvadl.org" target="_blank">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="assets/imgs/logo_light.svg">
      <source media="(prefers-color-scheme: light)" srcset="assets/imgs/logo_dark.svg">
      <img alt="OpenVADL" src="assets/imgs/logo_dark.svg" width="320">
    </picture>
  </a>
</p>
  <p align="center">
    <a href="https://openvadl.github.io/open-vadl/"><strong>Explore the docs »</strong></a>
    <br />
  </p>
</p>

## Getting Started

For example, you can create the iss (Instruction Set Simulator) for a minimal risc-v example with:

```bash
./gradlew run --args="iss sys/risc-v/rv64im.vadl"
```

To get a description of the complete usage, you can run: `./gradlew run --args="--help"`

## Building

You can build and run in two steps with

```bash
./gradlew installDist
```

Which will create an executable script at: `vadl-cli/build/install/openvadl/bin/openvadl`.

To build a [GraalVM Native Image](https://www.graalvm.org/latest/reference-manual/native-image/) you first need to set
`$JAVA_HOME` to point to your GraalVM installation.
With that you can run:

```bash
./gradlew nativeCompile
```

Which will create an executable at: `vadl-cli/build/native/nativeCompile/openvadl`

## Run all tests

To run all tests you need to have docker running on your system.

```bash
./gradlew test
```

**Note:** The tests are quite resource intensive (especially on memory and disk space) so make sure docker has enough
available, otherwise the tests might fail.

Expect the tests to run a long time (up to an hour isn't unrealistic).

## Development

Before contributing, please read [OpenVADL's contribution guidelines](CONTRIBUTING.md).

### Project Structure

The `open-vadl` project includes multiple Gradle modules.

- `vadl` is the main module that contains all the logic of OpenVADL
- `vadl-cli` implements the CLI for OpenVADL users. It uses the `vadl` module as a library
- `java-annotations` provides Java (!) annotations (e.g. `@Input`) that are used in the VIAM.
  Additionally it provides `errorprone` bug detectors, that statically check if certain properties
  in the VIAM are correctly implemented.

### Checkstyle

We are using Checkstyle to ensure a consistent format and documentation of the source code.

Install the Checkstyle plugin for your IDE and import our Checkstyle configuration.
The configuration is located under `config/checkstyle/checkstyle.xml`.

To locally test if the checkstyle CI pipeline would fail, run the `checkstyleAll` gradle task.

#### Using Intellij

To use the Checkstyle confirm IntelliJ code style follow these steps:

1. Go to `Settings > Editor > Code Style > Java`
2. For the `Scheme` select `Project`

If you still get formatting conflicts between Intellij and Checkstyle follow these steps:

1. Go to
   `Settings > Editor > Code Style > Java`
2. Click on the settings icon and select `Import Scheme > Checkstyle configuration`
3. Choose the Checkstyle config file under `config/checkstyle/checkstyle.xml`
4. Enable the Java code style setting `JavaDocs > Other > Indent continuation lines`
   This prevents a JavaDocs formatting conflict between IntelliJ and Checkstyle.

With this, IntelliJ uses the code style rules as specified in the Checkstyle config.
Note that Checkstyle and code style are not 100% compatible,
so IntelliJ will eventually generate some invalid formatted code (such as Java docs
paragraph separation).

