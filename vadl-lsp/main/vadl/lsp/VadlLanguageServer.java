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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.eclipse.lsp4j.DiagnosticSeverity;
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
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PositionEncodingKind;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
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
import vadl.ast.VadlParser;
import vadl.error.Diagnostic;
import vadl.error.DiagnosticList;
import vadl.utils.SourceLocation;

/**
 * The openVADL language server, based on lsp4j.
 */
public class VadlLanguageServer implements LanguageServer, LanguageClientAware {
  /**
   * The URI the Vadl Parser assigns to the String we give it for parsing. With this we can check
   * if a location refers to this "file" or some other file that the Parser included along the way.
   */
  private static final URI primaryFile = URI.create("memory://internal");

  private static final Logger log = LoggerFactory.getLogger(VadlLanguageServer.class);

  private @Nullable LanguageClient client;
  private @Nullable Future<Void> listeningFuture;
  private ExecutorService executor = Executors.newCachedThreadPool();
  
  private final VadlTextDocumentService textService = new VadlTextDocumentService();
  private @Nullable LspTokenizer tokenizer;

  private final Map<String, TextDocumentItem> openDocuments = new HashMap<>();

  private @Nullable InitializeParams params;
  private @Nullable ServerCapabilities serverCapabilities;

  @Override
  public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
    // TODO Use debug (same below)
    log.info(">> initialize: {}", params);
    this.params = params;

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

  private InitializeParams params() {
    if (this.params == null) {
      throw new RuntimeException("params aren't set yet");
    }
    return this.params;
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
      publishDiagnostics(params.getTextDocument());
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
      publishDiagnostics(document);
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

    private void publishDiagnostics(TextDocumentItem document) {
      var capabilities = params().getCapabilities().getTextDocument();
      if (capabilities == null
          || capabilities.getPublishDiagnostics() == null) {
        // Don't push diagnostics if client doesn't support it
        return;
      }

      // Unpacking document before going to background thread, to avoid race conditions
      // - didChange() may change document at any point after this.
      String text = document.getText();
      String uriString = document.getUri();
      URI uri = URI.create(uriString);
      int version = document.getVersion();

      var unused = executor.submit(() -> {
        List<org.eclipse.lsp4j.Diagnostic> lspItems = new ArrayList<>();
        try {
          VadlParser.parse(text, uri);
        } catch (DiagnosticList dl) {
          log.info("Raw diagnostics: {}", dl.getMessage());
          for (Diagnostic item : dl.items) {
            // TODO Look into secondary locations too? Maybe as relatedInformation?
            SourceLocation location = item.multiLocation.primaryLocation().location();
            if (!location.uri().equals(primaryFile)) {
              // Ignore errors for other files
              // TODO this means that errors in included files are not reported unless that file is
              //      opened in the client, even though the Parser gives us diagnostics for them
              //      (BUT: They are based on the file-system contents of that file, so maybe only
              //      provided these diagnostics if the file in question isn't currently owned by
              //      the client?)
              continue;
            }

            org.eclipse.lsp4j.Diagnostic lspItem = new org.eclipse.lsp4j.Diagnostic();
            // TODO Handle: transform from utf-8 positions to utf-16
            lspItem.setRange(new Range(
                new Position(
                    Math.max(location.begin().line() - 1, 0),
                    Math.max(location.begin().column() - 1, 0)
                ),
                new Position(
                    Math.max(location.end().line() - 1, 0),
                    Math.max(location.end().column(), 0)
                )
            ));
            lspItem.setSeverity(
                switch (item.level) {
                  case ERROR -> DiagnosticSeverity.Error;
                  case WARNING -> DiagnosticSeverity.Warning;
                }
            );
            // TODO there are messages attached to the Diagnostic itself - are they useful?
            String labelsString = item.multiLocation.primaryLocation().labels().stream()
                .map(Diagnostic.Message::content)
                .collect(Collectors.joining(". "));
            lspItem.setMessage(item.reason + (!labelsString.isBlank() ? ": " + labelsString : ""));
            lspItems.add(lspItem);
          }
        }

        var data = new PublishDiagnosticsParams(uriString, lspItems, version);
        log.info("<< publishDiagnostics: {}", data);
        client().publishDiagnostics(data);
      });
    }
  }
}
