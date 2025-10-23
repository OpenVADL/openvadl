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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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
import org.eclipse.lsp4j.PositionEncodingKind;
import org.eclipse.lsp4j.SemanticTokenTypes;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensLegend;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.SemanticTokensServerFull;
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.ServerInfo;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextDocumentSyncOptions;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
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
  
  private @Nullable LanguageClient client;
  private @Nullable Future<Void> listeningFuture;
  
  private final VadlTextDocumentService textService = new VadlTextDocumentService();

  private final Map<String, TextDocumentItem> openDocuments = new HashMap<>();
  
  //private InitializeParams params;
  private @Nullable ServerCapabilities serverCapabilities;

  private @Nullable LspTokenizer tokenizer;
  
  @Override
  public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
    // TODO Use debug (same below)
    log.info(">> initialize: {}", params);
    //this.params = params;
    
    createCapabilities();
    var result = new InitializeResult(
        serverCapabilities,
        new ServerInfo("openVADL language server")
    );
    log.info("<<- initialize: {}", result);
    
    return CompletableFuture.completedFuture(result);
  }
  
  @Override
  public void initialized(InitializedParams params) {
    log.info(">> initialized: {}", params);
    
    // TODO Remove
    var data = new MessageParams(
        MessageType.Info,
        "Hello vadl developer! I'm your language server today ❤️"
    );
    log.info("<< showMessage: {}", data);
    client().showMessage(data);
  }

  @Override
  public CompletableFuture<Object> shutdown() {
    log.info(">> shutdown");
    // Nothing to do
    return CompletableFuture.completedFuture(new Object());
  }

  @Override
  public void exit() {
    log.info(">> exit");
    if (listeningFuture == null) {
      throw new RuntimeException("listeningFuture isn't set yet");
    }
    listeningFuture.cancel(true);
  }

  @Override
  public TextDocumentService getTextDocumentService() {
    return textService;
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
  
  
  /**
   * Creates the server capabilities that are returned to the client upon
   * initialized(). Sets {@code this.serverCapabilities}, {@code this.tokenTypesMap}, and
   * {@code this.tokenModifiersMap}.
   */
  private void createCapabilities() {
    var c = new ServerCapabilities();
    
    c.setPositionEncoding(PositionEncodingKind.UTF16);
    
    var tdso = new TextDocumentSyncOptions();
    tdso.setOpenClose(true);
    tdso.setChange(TextDocumentSyncKind.Full);
    tdso.setWillSave(false);
    tdso.setWillSaveWaitUntil(false);
    tdso.setSave(false);
    c.setTextDocumentSync(tdso);
    
    c.setSemanticTokensProvider(new SemanticTokensWithRegistrationOptions(
        // TODO Extend legend & take client capabilities into account
        new SemanticTokensLegend(
            Arrays.asList(new String[]{
                SemanticTokenTypes.Type,
                SemanticTokenTypes.Variable,
                SemanticTokenTypes.Keyword,
                SemanticTokenTypes.String,
                SemanticTokenTypes.Number,
                SemanticTokenTypes.Operator
            }),
            Arrays.asList(new String[] {
                // None
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
    this.tokenizer = new LspTokenizer(tokenTypesMap, tokenModifiersMap);
  }
  
  
  private class VadlTextDocumentService implements TextDocumentService {
    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
      log.info(">> didOpen: {}", params);
      openDocuments.put(params.getTextDocument().getUri(), params.getTextDocument());
    }
    
    @Override
    public void didClose(DidCloseTextDocumentParams params) {
      log.info(">> didClose: {}", params);
      openDocuments.remove(params.getTextDocument().getUri());
    }
    
    @Override
    public void didChange(DidChangeTextDocumentParams params) {
      log.info(">> didChange: {}", params);

      var document = openDocuments.get(params.getTextDocument().getUri());
      if (document == null) {
        return;
      }
      document.setVersion(params.getTextDocument().getVersion());
      // TODO Support Incremental changes as well (& switch on in capabilities)
      document.setText(params.getContentChanges().getLast().getText());
    }
    
    @Override
    public void didSave(DidSaveTextDocumentParams params) {
      log.info(">> didSave: {}", params);
      // Nothing (server capabilities currently don't support this)
    }
    
    @Override
    public CompletableFuture<SemanticTokens> semanticTokensFull(SemanticTokensParams params) {
      log.info(">> semanticTokens/full: {}", params);
      
      return CompletableFuture.supplyAsync(() -> {
        var document = openDocuments.get(params.getTextDocument().getUri());
        if (document == null) {
          throw new ResponseErrorException(new ResponseError(
              ResponseErrorCode.RequestFailed,
              "Requested semantic tokens for a document that is not open.",
              null
          ));
        }

        var tokens = tokenizer != null
            ? tokenizer.getTokens(document.getText())
            : new ArrayList<Integer>();
        var result = new SemanticTokens(tokens);
        log.info("<<- semanticTokens/full: <omitted>({} tokens)", tokens.size() / 5);
        return result;
      });
    }
  }
}
