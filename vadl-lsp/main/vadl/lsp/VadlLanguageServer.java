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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.annotation.Nullable;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.PositionEncodingKind;
import org.eclipse.lsp4j.SemanticTokenTypes;
import org.eclipse.lsp4j.SemanticTokensLegend;
import org.eclipse.lsp4j.SemanticTokensServerFull;
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.ServerInfo;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextDocumentSyncOptions;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.ast.LspTokenizer;

/**
 * The openVADL language server, based on lsp4j.
 */
public class VadlLanguageServer implements LanguageServer, LanguageClientAware {
  private static final Logger log = LoggerFactory.getLogger(VadlLanguageServer.class);

  @Nullable
  private LanguageClient client;
  @Nullable
  private Future<Void> listeningFuture;
  private final ExecutorService executor = Executors.newCachedThreadPool();
  
  private final VadlTextDocumentService textService = new VadlTextDocumentService(this);

  @Nullable
  private InitializeParams params;
  @Nullable
  private ServerCapabilities serverCapabilities;

  @Override
  public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
    log.debug(">> initialize: {}", params);
    this.params = params;

    createCapabilities(params);
    var result = new InitializeResult(
        serverCapabilities,
        new ServerInfo("openVADL language server")
    );
    log.debug("<<- initialize: {}", result);
    
    return CompletableFuture.completedFuture(result);
  }
  
  @Override
  public void initialized(InitializedParams params) {
    log.debug(">> initialized: {}", params);
  }

  @Override
  public CompletableFuture<Object> shutdown() {
    log.debug(">> shutdown");
    // Nothing to do
    return CompletableFuture.completedFuture(new Object());
  }

  @Override
  public void exit() {
    log.debug(">> exit");
    if (listeningFuture == null) {
      throw new RuntimeException("listeningFuture isn't set yet");
    }
    listeningFuture.cancel(true);
  }

  /**
   * Should be called when the server process exits. This releases any resources this class owns,
   * e.g. background threads, open files.
   */
  public void tearDown() {
    executor.shutdownNow();
  }

  @Override
  public TextDocumentService getTextDocumentService() {
    return textService;
  }

  @Override
  public WorkspaceService getWorkspaceService() {
    return new WorkspaceService() {
      @Override
      public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
        // Nothing
      }
      
      @Override
      public void didChangeConfiguration(DidChangeConfigurationParams params) {
        // Nothing
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
  
  LanguageClient client() {
    if (this.client == null) {
      throw new RuntimeException("client isn't set yet");
    }
    return this.client;
  }

  ExecutorService executor() {
    return this.executor;
  }

  InitializeParams params() {
    if (this.params == null) {
      throw new RuntimeException("params aren't set yet");
    }
    return this.params;
  }
  
  
  /**
   * Creates the server capabilities that are returned to the client upon
   * initialized(). Sets {@code this.serverCapabilities}, {@code this.tokenTypesMap}, and
   * {@code this.tokenModifiersMap}.
   *
   * @param params for convenience
   */
  private void createCapabilities(InitializeParams params) {
    var c = new ServerCapabilities();
    
    c.setPositionEncoding(PositionEncodingKind.UTF16);
    
    var tdso = new TextDocumentSyncOptions();
    tdso.setOpenClose(true);
    tdso.setChange(TextDocumentSyncKind.Incremental);
    tdso.setWillSave(false);
    tdso.setWillSaveWaitUntil(false);
    tdso.setSave(false);
    c.setTextDocumentSync(tdso);

    // Semantic Tokens
    String[] desiredTokenTypes = new String[] {
        SemanticTokenTypes.Type,
        SemanticTokenTypes.Variable,
        SemanticTokenTypes.Keyword,
        SemanticTokenTypes.String,
        SemanticTokenTypes.Number,
        SemanticTokenTypes.Operator
    };
    List<String> tokenTypes = new ArrayList<>(desiredTokenTypes.length);
    // Limit token types to those supported by the client
    List<String> clientSupportedTypes = params.getCapabilities().getTextDocument()
        .getSemanticTokens().getTokenTypes();
    for (String token : desiredTokenTypes) {
      if (clientSupportedTypes.contains(token)) {
        tokenTypes.add(token);
      }
    }
    c.setSemanticTokensProvider(new SemanticTokensWithRegistrationOptions(
        new SemanticTokensLegend(
            tokenTypes,
            Arrays.asList(new String[] {
                // No modifiers
            })
        ),
        new SemanticTokensServerFull(false), // delta
        false // range
    ));
    this.serverCapabilities = c;

    // Create Tokenizer:
    var tokenTypesMap = new HashMap<String, Integer>();
    int index = 0;
    for (String type : c.getSemanticTokensProvider().getLegend().getTokenTypes()) {
      tokenTypesMap.put(type, index);
      index++;
    }
    var tokenModifiersMap = new HashMap<String, Integer>();
    index = 0;
    for (String modifier : c.getSemanticTokensProvider().getLegend().getTokenModifiers()) {
      tokenModifiersMap.put(modifier, index);
      index++;
    }
    textService.setTokenizer(new LspTokenizer(tokenTypesMap, tokenModifiersMap));
  }
}
