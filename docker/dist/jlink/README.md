# Java Runtime Builder for jlink

This Docker image provides a multi-platform Java development kit (JDK) environment for building cross-platform jlink
images.

## What it contains

- **Multiple JDK distributions** for cross-compilation:
    - Linux x64 and ARM64
    - Windows x64 and ARM64
    - macOS x64 and ARM64
- **Build tools**: binutils (for objcopy and other utilities)
- **Auto-detection**: Automatically selects the correct host JDK based on container architecture

All JDKs are stored in `/jdks/` with a symbolic link at `/jdks/jdk-host` pointing to the appropriate JDK for the
container's architecture.

## Building the image

```bash
docker build -t openvadl/java-runtime-builder .
```

## Usage

This container is designed to be used with the OpenVADL project's jlink build system. Mount your project directory and
run the Gradle build with the `-PbuildAllPlatforms` property:

```bash
docker run --rm -v $(pwd):/src/open-vadl openvadl/java-runtime-builder \
  ./gradlew :vadl-cli:jlink -PbuildAllPlatforms
```

The `-PbuildAllPlatforms` property instructs the build script to generate jlink images for all supported platforms using
the JDKs available in `/jdks/`.

## JDK Version

Currently using Azul Zulu JDK 25.0.1 (build 25.30.17-ca). Update the `JAVA_VERSION` and `ZULU_BUILD` build arguments in
the Dockerfile to use a different version.

