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
import vadl.ast.Ast;
import vadl.ast.LspTokenizer;
import vadl.ast.ModelRemover;
import vadl.ast.TypeChecker;
import vadl.ast.Ungrouper;
import vadl.ast.VadlParser;
import vadl.error.Diagnostic.MsgType;
import vadl.error.DiagnosticList;
import vadl.utils.SourceLocation;

/**
 * Handles document-related features of the language server.
 */
class VadlTextDocumentService implements TextDocumentService {
  /**
   * How many milliseconds to wait before providing diagnostics for the latest change. If a new
   * document version is pushed by the client within this time, the now outdated version will not
   * receive diagnostics. This is intended to avoid showing transitory diagnostics while the
   * developer is typing.
   */
  private static final int DIAGNOSTICS_DELAY_MS = 500;

  /**
   * The URI the Vadl Parser assigns to the String we give it for parsing. With this we can check
   * if a location refers to this "file" or some other file that the Parser included along the way.
   */
  private static final URI PRIMARY_FILE = URI.create("memory://internal");

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
  void setTokenizer(@NonNull LspTokenizer tokenizer) {
    this.tokenizer = tokenizer;
  }

  @Override
  public void didOpen(DidOpenTextDocumentParams params) {
    log.debug(">> didOpen: {}", params);
    var document = new Document(params.getTextDocument());
    synchronized (openDocuments) {
      openDocuments.put(params.getTextDocument().getUri(), document);
    }
    publishDiagnostics(document);
  }

  @Override
  public void didClose(DidCloseTextDocumentParams params) {
    log.debug(">> didClose: {}", params);
    synchronized (openDocuments) {
      openDocuments.remove(params.getTextDocument().getUri());
    }
  }

  @Override
  public void didChange(DidChangeTextDocumentParams params) {
    log.debug(">> didChange: {}", params);

    Document document = getDocument(params.getTextDocument().getUri());
    if (document == null) {
      return;
    }
    document.change(params.getTextDocument().getVersion(), params.getContentChanges());
    publishDiagnostics(document);
  }

  @Override
  public void didSave(DidSaveTextDocumentParams params) {
    log.debug(">> didSave: {}", params);
    // Nothing (server capabilities currently don't support this)
  }

  @Override
  public CompletableFuture<SemanticTokens> semanticTokensFull(SemanticTokensParams params) {
    log.debug(">> semanticTokens/full: {}", params);

    return CompletableFuture.supplyAsync(() -> {
      Document document = getDocument(params.getTextDocument().getUri());
      if (document == null) {
        throw new ResponseErrorException(new ResponseError(
            ResponseErrorCode.RequestFailed,
            "Requested semantic tokens for a document that is not open.",
            null
        ));
      }

      List<Integer> tokens;
      SemanticTokens result;
      synchronized (document) {
        tokens = tokenizer != null
            ? tokenizer.getTokens(document.getText())
            : new ArrayList<>();
        result = new SemanticTokens(document.calculateUtf16Positions(tokens));
      }
      log.debug("<<- semanticTokens/full: <omitted>({} tokens)", tokens.size() / 5);
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

    // Work with current state of document
    String text = document.getText();
    int version = document.getVersion();

    var unused = server.executor().submit(() -> {
      try {
        // TODO Consider to instead delay for remaining time *after* generating diagnostics, to have
        //  more accurate delay timing
        Thread.sleep(DIAGNOSTICS_DELAY_MS);
      } catch (InterruptedException e) {
        return;
      }
      if (!documentVersionIsCurrent(document.getUri(), version)) {
        log.debug(
            "ABORT publishDiagnostics (before): outdated version {} of document {}",
            version,
            document.getUri()
        );
        return;
      }

      List<Diagnostic> lspItems = new ArrayList<>();
      try {
        Ast ast = VadlParser.parse(text, URI.create(document.getUri()));
        new Ungrouper().ungroup(ast);
        new ModelRemover().removeModels(ast);
        new TypeChecker().verify(ast);

      } catch (DiagnosticList dl) {
        log.debug("Raw diagnostics: {}", dl.getMessage());
        for (vadl.error.Diagnostic item : dl.items) {
          // TODO Look into secondary locations too? Maybe as relatedInformation? Or to put a
          //      diagnostic message there as well?
          SourceLocation location = item.multiLocation.primaryLocation().location();
          if (!location.uri().equals(PRIMARY_FILE)) {
            // Ignore errors for other files
            // TODO this means that errors in included files are not reported unless that file is
            //      opened in the client, even though the Parser gives us diagnostics for them
            //      (BUT: They are based on the file-system contents of that file, so maybe only
            //      provide these diagnostics if the file in question isn't currently owned by
            //      the client?)
            continue;
          }

          Diagnostic lspItem = new Diagnostic();
          try {
            lspItem.setRange(new Range(
                document.calculateUtf16Position(location.begin(), version, false),
                document.calculateUtf16Position(location.end(), version, true)
            ));
          } catch (Document.ObsoleteDocumentVersionException e) {
            return;
          }
          lspItem.setSeverity(
              switch (item.level) {
                case ERROR -> DiagnosticSeverity.Error;
                case WARNING -> DiagnosticSeverity.Warning;
              }
          );
          // labels (aka messages) per location
          String labelsString = item.multiLocation.primaryLocation().labels().stream()
              .map(vadl.error.Diagnostic.Message::content)
              .collect(Collectors.joining(". "));
          // messages per Diagnostic - they may offer help or give additional notes
          String messagesString = item.messages.stream()
              .filter(m -> m.type().equals(MsgType.HELP) || m.type().equals(MsgType.NOTE))
              .map(vadl.error.Diagnostic.Message::content)
              .collect(Collectors.joining(". "));

          String fullMessage = item.reason + (!labelsString.isBlank() ? ": " + labelsString : "")
              + (!messagesString.isBlank() ? "\n" + messagesString : "");
          lspItem.setMessage(fullMessage);
          lspItems.add(lspItem);
        }
      }
      // TODO There may be diagnostics in DeferredDiagnosticStore, but that is a static list and
      //      has no clear() method (i.e. outdated diagnostics remain visible)

      if (!documentVersionIsCurrent(document.getUri(), version)) {
        log.debug(
            "ABORT publishDiagnostics (after): outdated version {} of document {}",
            version,
            document.getUri()
        );
        return;
      }
      var data = new PublishDiagnosticsParams(document.getUri(), lspItems, version);
      log.debug("<< publishDiagnostics: {}", data);
      server.client().publishDiagnostics(data);
    });
  }

  private boolean documentVersionIsCurrent(String uri, int version) {
    Document document = getDocument(uri);
    if (document == null) {
      return false;
    }
    return document.getVersion() == version;
  }

  private @Nullable Document getDocument(String uri) {
    synchronized (openDocuments) {
      return openDocuments.get(uri);
    }
  }
}
