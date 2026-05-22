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
import java.net.ServerSocket;
import java.net.Socket;
import javax.annotation.Nullable;
import org.openvadl.klsp.api.java.LspBindings;
import org.openvadl.klsp.server.LspServers;
import org.openvadl.klsp.transport.StdioMessageTransport;
import org.openvadl.klsp.transport.StreamMessageTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.lsp.VadlLanguageServer.Settings;

/**
 * Entrypoint to the OpenVADL language server.
 */
public class LspEntryPoint {
  private static final Logger log = LoggerFactory.getLogger(LspEntryPoint.class);

  @Nullable
  private final Integer port;
  private final Settings settings;

  private LspEntryPoint(@Nullable Integer port, Settings settings) {
    this.port = port;
    this.settings = settings;
  }

  private int runServer() {
    int exitCode = 0;
    try {
      if (port == null) {
        log.info("Started OpenVADL language server on stdin/stdout");
        serveClient(new StdioMessageTransport());
      } else {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
          log.info("Started OpenVADL language server on port {}", serverSocket.getLocalPort());

          Socket socket = serverSocket.accept();
          log.info(
              "Connection established with {}:{}",
              socket.getInetAddress().getHostAddress(),
              socket.getPort()
          );
          serveClient(new StreamMessageTransport(socket.getInputStream(), socket.getOutputStream()));
        }
      }
    } catch (IOException e) {
      log.error(e.toString());
      exitCode = 1;
    }

    log.info("Server stopped.");
    return exitCode;
  }

  /**
   * Provides the actual language server functionality to a single client.
   */
  private void serveClient(org.openvadl.klsp.transport.MessageTransport transport) {
    VadlLanguageServer languageServer = new VadlLanguageServer(settings);
    var server = LspBindings.languageServer(
        LspServers.builder(transport),
        languageServer,
        languageServer.features()
    ).build();
    try {
      log.info("Server exited with status {}", server.runBlocking());
    } finally {
      languageServer.tearDown();
    }
  }

  /**
   * Runs a language server, which will either communicate via a specific TCP port or stdin/stdout.
   *
   * @param port Port on which to listen on. Null: Use stdin/stdout for communication instead.
   * @param settings Various settings for the language server itself.
   * @return exit code
   */
  public static int run(@Nullable Integer port, Settings settings) {
    return new LspEntryPoint(port, settings).runServer();
  }
}
