# OpenVADL Language Server

This is usually bundled into an IDE-specific extension.

## Running

The language server can be started manually using gradle:

`./gradlew run --args="lsp"`

The server will wait for a connection on local TCP port 10999.
Note that the server will serve one single client and shut down when the client disconnects.

For instructions on how to use our vscode extension with this language server,
see [its wiki](https://github.com/OpenVADL/vscode-openvadl/wiki/Language-server-development-notes).
