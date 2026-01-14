# OpenVADL CLI

## Running

With `./gradlew run --args="--help"` you can directly run the CLI without an additional compile step.

## Building

An execution-ready build of the CLI can be obtained via `./gradlew installDist`.
The distribution will be available at `vadl-cli/build/install/openvadl/bin/openvadl`.
To run this distribution, the user must have Java 25 installed with the Java Virtual Machine (JVM).

## Creating a JVM Runtime Image

With `./gradlew jlink`, a runtime image can be created, that allows users to run the CLI without a JVM installation.
This creates a platform-specific runtime in `vadl-cli/build/image/`.

### Multi-Platform Build

To build distributable images for all supported platforms (Linux x64/ARM64, macOS x64/ARM64, Windows x64/ARM64), use the
`ghcr.io/openvadl/java-runtime-builder` Docker container:

```bash
docker run --rm -v $(pwd):/src/open-vadl ghcr.io/openvadl/java-runtime-builder \
  ./gradlew :vadl-cli:jlink -PbuildAllPlatforms
```

The `-PbuildAllPlatforms` property enables cross-platform compilation. Platform-specific images will be generated in
`vadl-cli/build/image/` with subdirectories for each platform.

See [docker/dist/jlink/README.md](../docker/dist/jlink/README.md) for more details on the build container.

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
