# OpenVADL Language Server

This is usually bundled into an IDE-specific extension.

## Running

The language server can be started manually using gradle:

`./gradlew :vadl-cli:run`

The server will wait for a connection on local TCP port 10999.
Note that the server will serve one single client and shut down when the client disconnects.

For instructions on how to use our vscode extension with this language server,
see [its wiki](https://github.com/OpenVADL/vscode-openvadl/wiki/Language-server-development-notes).

## Building

### Local Development Build

For local development, you can build a jlink image for your host platform:

```bash
./gradlew :vadl-lsp:jlink
```

This creates a platform-specific runtime in `vadl-lsp/build/image/`.

### Multi-Platform Build

To build distributable images for all supported platforms (Linux x64/ARM64, macOS x64/ARM64, Windows x64/ARM64), use the
`ghcr.io/openvadl/java-runtime-builder` Docker container:

```bash
docker run --rm -v $(pwd):/src/open-vadl ghcr.io/openvadl/java-runtime-builder \
  ./gradlew :vadl-lsp:jlink -PbuildAllPlatforms
```

The `-PbuildAllPlatforms` property enables cross-platform compilation. Platform-specific images will be generated in
`vadl-lsp/build/image/` with subdirectories for each platform.

See `docker/dist/jlink/README.md` for more details on the build container.