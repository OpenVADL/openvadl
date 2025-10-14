// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
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
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entrypoint to the OpenVADL language server.
 */
public class Main {
  private static final Logger log = LoggerFactory.getLogger(Main.class);
  
  /**
   * Runs a language server on a specific port.
   *
   * @param args (Optional) port on which to listen.
   */
  public static void main(String[] args) {
    int port = 10999;
    if (args.length >= 1) {
      port = Integer.valueOf(args[0]);
    }
    
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      log.info("Started openVADL language server on port " + serverSocket.getLocalPort());
      
      Socket socket = serverSocket.accept();
      serveClient(socket);
      
    } catch (IOException | InterruptedException | ExecutionException e) {
      log.error(e.toString());
    }
    
    log.info("Server stopped.");
    System.exit(0);
  }
  
  /**
   * Provides the actual language server functionality to a single client.
   *
   * @param socket the accepted socket
   */
  protected static void serveClient(Socket socket) throws
      IOException,
      InterruptedException,
      ExecutionException {
    log.info(
        "Connection established with " + socket.getInetAddress().getHostAddress()
        + ":" + socket.getPort()
    );
    
    // According to https://github.com/eclipse-lsp4j/lsp4j/blob/main/documentation/README.md
    VadlLanguageServer server = new VadlLanguageServer();
    Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(
        server,
        socket.getInputStream(),
        socket.getOutputStream()
    );
    server.connect(launcher.getRemoteProxy());
    Future<Void> future = launcher.startListening();
    
    server.setListeningFuture(future);
    try {
      future.get(); // Wait for listener to complete
      log.info("Client disconnected");
    } catch (CancellationException e) {
      // I.e. Future was cancelled by VadlLanguageServer
      log.info("Server disconnected");
    }
  }
}