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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import javax.annotation.Nullable;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.MessageType;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.ServerInfo;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The openVADL language server, based on lsp4j.
 */
public class VadlLanguageServer implements LanguageServer, LanguageClientAware {
  private static final Logger log = LoggerFactory.getLogger(VadlLanguageServer.class);
  
  private @Nullable LanguageClient client = null;
  
  private @Nullable Future<Void> listeningFuture;
  
  //private InitializeParams params;
  
  @Override
  public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
    // TODO Use debug
    log.info(">> initialize: " + params);
    //this.params = params;
    
    var result = new InitializeResult(
        new ServerCapabilities(),
        new ServerInfo("openVADL language server")
    );
    // TODO Use debug
    log.info("<<- initialize: " + result);
    
    return CompletableFuture.completedFuture(result);
  }
  
  @Override
  public void initialized(InitializedParams params) {
    // TODO Use debug
    log.info(">> initialized: " + params);
    
    // TODo Remove
    client().showMessage(new MessageParams(
        MessageType.Info,
        "Hello vadl developer! I'm your language server today ❤️"
    ));
  }

  @Override
  public CompletableFuture<Object> shutdown() {
    // TODO Use debug
    log.info(">> shutdown");
    // Nothing to do
    return CompletableFuture.completedFuture(new Object());
  }

  @Override
  public void exit() {
    // TODO Use debug
    log.info(">> exit");
    if (listeningFuture == null) {
      throw new RuntimeException("listeningFuture isn't set yet");
    }
    listeningFuture.cancel(true);
  }

  @Override
  public TextDocumentService getTextDocumentService() {
    // TODO Implement?
    return new TextDocumentService() {
      @Override
      public void didSave(DidSaveTextDocumentParams params) {
      }
      
      @Override
      public void didOpen(DidOpenTextDocumentParams params) {
      }
      
      @Override
      public void didClose(DidCloseTextDocumentParams params) {
      }
      
      @Override
      public void didChange(DidChangeTextDocumentParams params) {
      }
    };
  }

  @Override
  public WorkspaceService getWorkspaceService() {
    // TODO Implement?
    return new WorkspaceService() {
      @Override
      public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
      }
      
      @Override
      public void didChangeConfiguration(DidChangeConfigurationParams params) {
      }
    };
  }

  @Override
  public void connect(LanguageClient client) {
    if (this.client != null) {
      throw new RuntimeException("Client is already connected");
    }
    this.client = client;
  }
  
  /**
   * Sets the Future the Launcher produced. Canceling this Future stops the
   * language server implementation.
   *
   * @param listeningFuture As produced by LSPLauncher.startListening()
   */
  public void setListeningFuture(Future<Void> listeningFuture) {
    this.listeningFuture = listeningFuture;
  }
  
  private LanguageClient client() {
    if (this.client == null) {
      throw new RuntimeException("client isn't set yet");
    }
    return this.client;
  }
}
