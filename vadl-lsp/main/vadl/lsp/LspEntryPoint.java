// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.lsp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.annotation.Nullable;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entrypoint to the OpenVADL language server.
 */
public class LspEntryPoint {
  private static final Logger log = LoggerFactory.getLogger(LspEntryPoint.class);

  @Nullable
  private final Integer port;

  private LspEntryPoint(@Nullable Integer port) {
    this.port = port;
  }

  private int runServer() {
    var exitCode = 0;
    try {
      if (port == null) {
        log.info("Started OpenVADL language server on stdin/stdout");
        serveClient(System.in, System.out);

      } else {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
          log.info("Started OpenVADL language server on port {}", serverSocket.getLocalPort());

          Socket socket = serverSocket.accept();
          log.info(
              "Connection established with {}:{}", socket.getInetAddress().getHostAddress(),
              socket.getPort()
          );
          serveClient(socket.getInputStream(), socket.getOutputStream());
        }
      }

    } catch (IOException | InterruptedException | ExecutionException e) {
      log.error(e.toString());
      exitCode = 1;
    }

    log.info("Server stopped.");
    return exitCode;
  }

  /**
   * Provides the actual language server functionality to a single client.
   *
   * @param in Input Stream for receiving messages from client
   * @param out Output Stream for sending messages to client
   */
  private void serveClient(InputStream in, OutputStream out) throws
      IOException,
      InterruptedException,
      ExecutionException {

    // According to https://github.com/eclipse-lsp4j/lsp4j/blob/main/documentation/README.md
    VadlLanguageServer server = new VadlLanguageServer();
    Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(
        server,
        in,
        out
    );
    server.connect(launcher.getRemoteProxy());
    Future<Void> future = launcher.startListening();

    server.setListeningFuture(future);
    try {
      future.get(); // Wait for listener to complete
      log.info("Client disconnected");
    } catch (CancellationException e) {
      // I.e. Future was canceled by VadlLanguageServer
      log.info("Server disconnected");
    } finally {
      server.tearDown();
    }
  }


  /**
   * Runs a language server, which will either communicate via a specific TCP port or stdin/stdout.
   *
   * @param port Port on which to listen on. Null: Use stdin/stdout for communication instead.
   * @return exit code
   */
  public static int run(@Nullable Integer port) {
    return new LspEntryPoint(port).runServer();
  }
}
