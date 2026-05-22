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
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.Nullable;
import org.openvadl.klsp.api.java.LanguageClient;
import org.openvadl.klsp.api.java.LanguageServer;
import org.openvadl.klsp.api.java.LanguageServerSession;
import org.openvadl.klsp.api.java.ServerFeatures;
import org.openvadl.klsp.api.java.SessionAware;
import org.openvadl.klsp.api.java.TextDocumentService;
import org.openvadl.klsp.api.java.WorkspaceService;
import org.openvadl.klsp.protocol.DidChangeConfigurationParams;
import org.openvadl.klsp.protocol.DidChangeWatchedFilesParams;
import org.openvadl.klsp.protocol.InitializeParams;
import org.openvadl.klsp.protocol.InitializeResult;
import org.openvadl.klsp.protocol.InitializedParams;
import org.openvadl.klsp.protocol.PositionEncodingKind;
import org.openvadl.klsp.protocol.SemanticTokenTypes;
import org.openvadl.klsp.protocol.SemanticTokensFullOptions;
import org.openvadl.klsp.protocol.SemanticTokensLegend;
import org.openvadl.klsp.protocol.SemanticTokensOptions;
import org.openvadl.klsp.protocol.ServerInfo;
import org.openvadl.klsp.protocol.TextDocumentSyncKind;
import org.openvadl.klsp.server.ServerExitStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.ast.LspTokenizer;

/**
 * The openVADL language server.
 */
public class VadlLanguageServer implements LanguageServer, SessionAware {
  private static final Logger log = LoggerFactory.getLogger(VadlLanguageServer.class);

  @Nullable
  private LanguageClient client;
  private final ExecutorService executor = Executors.newCachedThreadPool();

  private final VadlTextDocumentService textService = new VadlTextDocumentService(this);

  private final Settings settings;
  private final ServerFeatures features;

  @Nullable
  private InitializeParams params;


  /**
   * Creates a new Language Server with the given settings.
   */
  public VadlLanguageServer(Settings settings) {
    this.settings = settings;
    this.features = ServerFeatures.create()
        .positionEncoding(PositionEncodingKind.UTF16)
        .textDocumentSync(TextDocumentSyncKind.INCREMENTAL, true)
        .definition()
        .semanticTokens(this::createSemanticTokensOptions)
        .workspaceDidChangeConfiguration()
        .workspaceDidChangeWatchedFiles();
  }

  @Override
  public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
    log.debug(">> initialize: {}", params);
    this.params = params;

    var result = features.initializeResult(params, new ServerInfo("openVADL language server", null));
    log.debug("<<- initialize: {}", result);

    return CompletableFuture.completedFuture(result);
  }

  @Override
  public CompletableFuture<Void> initialized(InitializedParams params) {
    log.debug(">> initialized: {}", params);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Void> shutdown() {
    log.debug(">> shutdown");
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void exit(ServerExitStatus status) {
    log.debug(">> exit: {}", status);
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
      public CompletableFuture<Void> didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
        return CompletableFuture.completedFuture(null);
      }

      @Override
      public CompletableFuture<Void> didChangeConfiguration(DidChangeConfigurationParams params) {
        return CompletableFuture.completedFuture(null);
      }
    };
  }

  @Override
  public void connect(LanguageServerSession session) {
    if (this.client != null) {
      throw new RuntimeException("Client is already connected");
    }
    this.client = session.getLanguageClient();
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

  ServerFeatures features() {
    return features;
  }

  /**
   * Builds semantic-token options for the current client and configures the matching tokenizer.
   */
  @Nullable
  private SemanticTokensOptions createSemanticTokensOptions(InitializeParams params) {
    if (!settings.noSyntaxHighlighting()) {
      String[] desiredTokenTypes = new String[] {
          SemanticTokenTypes.TYPE,
          SemanticTokenTypes.VARIABLE,
          SemanticTokenTypes.KEYWORD,
          SemanticTokenTypes.STRING,
          SemanticTokenTypes.NUMBER,
          SemanticTokenTypes.OPERATOR
      };
      List<String> tokenTypes = new ArrayList<>(desiredTokenTypes.length);

      List<String> clientSupportedTypes = List.of();
      if (params.getCapabilities().getTextDocument() != null
          && params.getCapabilities().getTextDocument().getSemanticTokens() != null) {
        clientSupportedTypes = params.getCapabilities().getTextDocument()
            .getSemanticTokens().getTokenTypes();
      }
      for (String token : desiredTokenTypes) {
        if (clientSupportedTypes.isEmpty() || clientSupportedTypes.contains(token)) {
          tokenTypes.add(token);
        }
      }

      var semanticTokensProvider = new SemanticTokensOptions(
          new SemanticTokensLegend(tokenTypes, List.of()),
          new SemanticTokensFullOptions(false),
          false
      );

      var tokenTypesMap = new HashMap<String, Integer>();
      int index = 0;
      for (String type : semanticTokensProvider.getLegend().getTokenTypes()) {
        tokenTypesMap.put(type, index);
        index++;
      }
      var tokenModifiersMap = new HashMap<String, Integer>();
      index = 0;
      for (String modifier : semanticTokensProvider.getLegend().getTokenModifiers()) {
        tokenModifiersMap.put(modifier, index);
        index++;
      }
      textService.setTokenizer(new LspTokenizer(tokenTypesMap, tokenModifiersMap));
      return semanticTokensProvider;
    }
    textService.setTokenizer(null);
    return null;
  }

  /**
   * Various Server Settings.
   *
   * @param noSyntaxHighlighting True: Disable syntax highlighting (aka semantic tokens).
   */
  public record Settings(boolean noSyntaxHighlighting) {
  }
}
