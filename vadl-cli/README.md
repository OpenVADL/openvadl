# OpenVADL CLI

## Running

With `./gradlew run --args="--help"` you can directly run the CLI without an additional compile step.

## Building

An execution-ready build of the CLI can be obtained via `./gradlew installDist`.
The distribution will be available at `vadl-cli/build/install/openvadl/bin/openvadl`.

## Creating a GraalVM native image

With `JAVA_HOME` or `GRAALVM_HOME` pointing to a GraalVM installation, run `./gradlew nativeCompile`.
This will create a binary at `vadl-cli/build/native/nativeCompile/openvadl`.
If the used Java installation is not a GraalVM distribution, you will see an error message like

> Execution failed for task ':vadl-cli:nativeCompile'.
> Determining GraalVM installation failed with message: 'gu' at '<snip>' tool wasn't found.
> This probably means that JDK at isn't a GraalVM distribution.
> Make sure to declare the GRAALVM_HOME environment variable or install GraalVM with native-image in a standard location
> recognized by Gradle Java toolchain support

Note: Native image builds may take several minutes.
