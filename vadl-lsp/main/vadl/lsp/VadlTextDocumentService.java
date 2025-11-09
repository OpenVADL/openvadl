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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
import org.eclipse.lsp4j.jsonrpc.validation.NonNull;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.ast.LspTokenizer;
import vadl.ast.VadlParser;
import vadl.error.DiagnosticList;
import vadl.utils.SourceLocation;

/**
 * Handles document-related features of the language server.
 */
class VadlTextDocumentService implements TextDocumentService {
  /**
   * The URI the Vadl Parser assigns to the String we give it for parsing. With this we can check
   * if a location refers to this "file" or some other file that the Parser included along the way.
   */
  private static final URI primaryFile = URI.create("memory://internal");

  private static final Logger log = LoggerFactory.getLogger(VadlTextDocumentService.class);

  @NonNull
  private final VadlLanguageServer server;
  private @Nullable LspTokenizer tokenizer;

  private final Map<String, Document> openDocuments = new HashMap<>();

  VadlTextDocumentService(VadlLanguageServer server) {
    this.server = server;
  }

  /**
   * Sets the tokenizer to use. As the tokenizer's configuration depends on
   * server capabilities, this shall be called once by the Server object upon
   * initializing connection with the LSP client.
   *
   * @param tokenizer A fully configured Tokenizer for VADL
   */
  // TODO Consider setting this via Constructor instead
  void setTokenizer(@NonNull LspTokenizer tokenizer) {
    this.tokenizer = tokenizer;
  }

  @Override
  public void didOpen(DidOpenTextDocumentParams params) {
    log.info(">> didOpen: {}", params);
    var document = new Document(params.getTextDocument());
    openDocuments.put(params.getTextDocument().getUri(), document);
    publishDiagnostics(document);
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
    document.change(params.getTextDocument().getVersion(), params.getContentChanges());
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

  private void publishDiagnostics(Document document) {
    var capabilities = server.params().getCapabilities().getTextDocument();
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

    var unused = server.executor().submit(() -> {
      List<Diagnostic> lspItems = new ArrayList<>();
      try {
        VadlParser.parse(text, uri);
      } catch (DiagnosticList dl) {
        log.info("Raw diagnostics: {}", dl.getMessage());
        for (vadl.error.Diagnostic item : dl.items) {
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

          Diagnostic lspItem = new Diagnostic();
          // TODO Handle: transform from utf-8 positions to utf-16
          lspItem.setRange(new Range(
              // VADL: line & column are 1-based
              // LSP: both are 0-based...
              new Position(
                  Math.max(location.begin().line() - 1, 0),
                  Math.max(location.begin().column() - 1, 0)
              ),
              new Position(
                  Math.max(location.end().line() - 1, 0),
                  // ... but end column is exclusive
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
              .map(vadl.error.Diagnostic.Message::content)
              .collect(Collectors.joining(". "));
          lspItem.setMessage(item.reason + (!labelsString.isBlank() ? ": " + labelsString : ""));
          lspItems.add(lspItem);
        }
      }

      var data = new PublishDiagnosticsParams(uriString, lspItems, version);
      log.info("<< publishDiagnostics: {}", data);
      server.client().publishDiagnostics(data);
    });
  }
}
