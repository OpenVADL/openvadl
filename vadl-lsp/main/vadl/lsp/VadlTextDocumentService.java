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

import static vadl.lsp.LspUtils.toPath;
import static vadl.lsp.LspUtils.toUri;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.openvadl.klsp.api.java.TextDocumentService;
import org.openvadl.klsp.jsonrpc.JsonRpcErrorCodes;
import org.openvadl.klsp.jsonrpc.ResponseError;
import org.openvadl.klsp.protocol.DefinitionParams;
import org.openvadl.klsp.protocol.Diagnostic;
import org.openvadl.klsp.protocol.DiagnosticSeverity;
import org.openvadl.klsp.protocol.DidChangeTextDocumentParams;
import org.openvadl.klsp.protocol.DidCloseTextDocumentParams;
import org.openvadl.klsp.protocol.DidOpenTextDocumentParams;
import org.openvadl.klsp.protocol.DidSaveTextDocumentParams;
import org.openvadl.klsp.protocol.Location;
import org.openvadl.klsp.protocol.Position;
import org.openvadl.klsp.protocol.PublishDiagnosticsParams;
import org.openvadl.klsp.protocol.Range;
import org.openvadl.klsp.protocol.SemanticTokens;
import org.openvadl.klsp.protocol.SemanticTokensParams;
import org.openvadl.klsp.server.ResponseErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.ast.Ast;
import vadl.ast.AstFinderByPosition;
import vadl.ast.Frontend;
import vadl.ast.LspTokenizer;
import vadl.ast.VadlParser;
import vadl.error.Diagnostic.MsgType;
import vadl.error.DiagnosticList;
import vadl.utils.SourceLocation;

/**
 * Handles document-related features of the language server.
 */
public class VadlTextDocumentService implements TextDocumentService {
  /**
   * How many milliseconds to wait before providing diagnostics for the latest change. If a new
   * document version is pushed by the client within this time, the now outdated version will not
   * receive diagnostics. This is intended to avoid showing transitory diagnostics while the
   * developer is typing.
   */
  private static final int DIAGNOSTICS_DELAY_MS = 500;

  private static final Logger log = LoggerFactory.getLogger(VadlTextDocumentService.class);

  private final VadlLanguageServer server;
  @Nullable
  private LspTokenizer tokenizer;

  private final Map<String, Document> openDocuments = new HashMap<>();
  private final DependencyMap<String> documentDependencies = new DependencyMap<>();

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
  void setTokenizer(@Nullable LspTokenizer tokenizer) {
    this.tokenizer = tokenizer;
  }

  @Override
  public CompletableFuture<Void> didOpen(DidOpenTextDocumentParams params) {
    log.debug(">> didOpen: {}", params);

    Document document = new Document(params.getTextDocument());
    LspSnapshotFileSystem snapshots;
    synchronized (openDocuments) {
      openDocuments.put(params.getTextDocument().getUri(), document);
      snapshots = createSnapshotFileSystem();
    }
    publishDiagnostics(document, snapshots);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Void> didClose(DidCloseTextDocumentParams params) {
    log.debug(">> didClose: {}", params);
    synchronized (openDocuments) {
      openDocuments.remove(params.getTextDocument().getUri());
    }
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Void> didChange(DidChangeTextDocumentParams params) {
    log.debug(">> didChange: {}", params);

    Document document;
    LspSnapshotFileSystem snapshots;
    synchronized (openDocuments) {
      document = openDocuments.computeIfPresent(params.getTextDocument().getUri(), (k, d) ->
          d.withChanges(params.getTextDocument().getVersion(), params.getContentChanges()));
      snapshots = createSnapshotFileSystem();
    }
    if (document != null) {
      publishDiagnostics(document, snapshots);
    }

    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Void> didSave(DidSaveTextDocumentParams params) {
    log.debug(">> didSave: {}", params);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<SemanticTokens> semanticTokensFull(SemanticTokensParams params) {
    log.debug(">> semanticTokens/full: {}", params);

    return CompletableFuture.supplyAsync(() -> {
      Document document = getDocument(params.getTextDocument().getUri());
      if (document == null) {
        throw new ResponseErrorException(new ResponseError(
            JsonRpcErrorCodes.REQUEST_FAILED,
            "Requested semantic tokens for a document that is not open.",
            null
        ));
      }

      List<Integer> tokens = tokenizer != null
          ? tokenizer.getTokens(document.getText())
          : new ArrayList<>();
      SemanticTokens result = new SemanticTokens(document.calculateUtf16Positions(tokens), null);
      log.debug("<<- semanticTokens/full: <omitted>({} tokens)", tokens.size() / 5);
      return result;
    });
  }

  @Override
  public CompletableFuture<List<Location>> definition(DefinitionParams params) {
    log.debug(">> definition: {}", params);

    LspSnapshotFileSystem snapshots = createSnapshotFileSystem();
    return CompletableFuture.supplyAsync(() -> {
      String uri = params.getTextDocument().getUri();
      Document document = snapshots.getDocument(uri);
      if (document == null) {
        throw new ResponseErrorException(new ResponseError(
            JsonRpcErrorCodes.REQUEST_FAILED,
            "Requested (Go to) definition for a document that is not open.",
            null
        ));
      }

      Ast ast;
      try {
        ast = VadlParser.parse(toPath(uri), snapshots);
      } catch (DiagnosticList dl) {
        log.debug("UNABLE definition: Parser produced diagnostics instead of AST for {}", uri);
        return List.of();
      }

      var position = document.calculateUtf8Position(params.getPosition(), false);
      SourceLocation location = AstFinderByPosition.findIdentifierTargetLocation(ast, toPath(uri),
          position);

      if (location == null || location.path() == null) {
        return List.<Location>of();
      }
      var targetDocument = snapshots.getFileBasedDocument(toUri(location.path()));
      if (targetDocument == null) {
        log.debug("Unexpected: Definition target file {} does not exist", toUri(location.path()));
        return List.<Location>of();
      }

      var lspLocation = new Location(targetDocument.uri, targetDocument.calculateUtf16Range(location));
      log.debug("<<- definition: {}", lspLocation);
      return List.of(lspLocation);
    });
  }

  /**
   * Manages diagnostic publishing for a given document, incl. debouncing, version checking, and
   * updating dependent documents.
   *
   * @param document  Must be contained in {@code snapshots}
   * @param snapshots Must be fresh, i.e. not used in the VADL parser yet
   */
  private void publishDiagnostics(Document document, LspSnapshotFileSystem snapshots) {
    var capabilities = server.params().getCapabilities().getTextDocument();
    if (capabilities == null || capabilities.getPublishDiagnostics() == null) {
      return;
    }

    var unused = server.executor().submit(() -> {
      try {
        Thread.sleep(DIAGNOSTICS_DELAY_MS);
      } catch (InterruptedException e) {
        return;
      }
      if (!documentVersionIsCurrent(document)) {
        log.debug(
            "ABORT publishDiagnostics (before): outdated version {} of document {}",
            document.version,
            document.uri
        );
        return;
      }

      publishDiagnosticsForOneDocument(document, snapshots);

      for (String uri : documentDependencies.getDependents(document.uri)) {
        Document d = snapshots.getDocument(uri);
        if (d != null) {
          publishDiagnosticsForOneDocument(d, new LspSnapshotFileSystem(snapshots));
        }
      }
    });
  }

  /**
   * Takes care of the actual diagnostic processing for one document.
   *
   * @param document  Must be contained in {@code snapshots}
   * @param snapshots Must be fresh, i.e. not used in the VADL parser yet
   */
  private void publishDiagnosticsForOneDocument(Document document,
                                                LspSnapshotFileSystem snapshots) {
    var unused = server.executor().submit(() -> {
      List<Diagnostic> lspItems = new ArrayList<>();
      Path path = document.getPath();
      try {
        Frontend.compileToAst(path, snapshots);
      } catch (DiagnosticList dl) {
        log.debug("Raw diagnostics ({}): {}", document.uri, dl.getMessage());
        List<String> importedFileErrors = new ArrayList<>();
        for (vadl.error.Diagnostic item : dl.deflateSimilar().items) {
          Path itemPath = item.multiLocation.primaryLocation().location().path();
          if (!Objects.equals(itemPath, path)) {
            if (itemPath == null) {
              continue;
            }
            importedFileErrors.add(LspUtils.relativePath(itemPath, document.getPath()));
            continue;
          }

          lspItems.add(buildLspDiagnostic(item, document));
        }

        if (!importedFileErrors.isEmpty()) {
          String message = importedFileErrors.size() == 1
              ? "Errors in imported file: \n" + importedFileErrors.getFirst()
              : "Errors in imported files:\n- " + String.join("\n- ", importedFileErrors);
          lspItems.addFirst(new Diagnostic(
              new Range(new Position(0, 0), new Position(0, 0)),
              DiagnosticSeverity.ERROR,
              null,
              null,
              message
          ));
        }
      }

      if (!documentVersionIsCurrent(document)) {
        log.debug(
            "ABORT publishDiagnostics (after): outdated version {} of document {}",
            document.version,
            document.uri
        );
        return;
      }
      documentDependencies.setDependencies(document.uri, snapshots.getReadFiles());
      var data = new PublishDiagnosticsParams(document.uri, lspItems, document.version);
      log.debug("<< publishDiagnostics ({}): {}", document.uri, data);
      server.client().publishDiagnostics(data).exceptionally(exception -> {
        log.warn("Unable to publish diagnostics for {}", document.uri, exception);
        return null;
      });
    });
  }

  private Diagnostic buildLspDiagnostic(vadl.error.Diagnostic vadlDiagnostic,
                                        Document document) {
    SourceLocation location = vadlDiagnostic.multiLocation.primaryLocation().location();

    String labelsString = vadlDiagnostic.multiLocation.primaryLocation().labels().stream()
        .map(vadl.error.Diagnostic.Message::content)
        .collect(Collectors.joining("\n"));
    String messagesString = vadlDiagnostic.messages.stream()
        .filter(m -> !m.type().equals(MsgType.PLAIN)
            || !m.content().contains("parser got confused at this point"))
        .map(vadl.error.Diagnostic.Message::content)
        .collect(Collectors.joining("\n"));

    String fullMessage = vadlDiagnostic.reason
        + (!labelsString.isBlank() ? "\n" + labelsString : "")
        + (!messagesString.isBlank() ? "\n" + messagesString : "");

    return new Diagnostic(
        document.calculateUtf16Range(location),
        switch (vadlDiagnostic.level) {
          case ERROR -> DiagnosticSeverity.ERROR;
          case WARNING -> DiagnosticSeverity.WARNING;
        },
        null,
        null,
        fullMessage
    );
  }

  private boolean documentVersionIsCurrent(Document document) {
    Document currentDocument = getDocument(document.uri);
    if (currentDocument == null) {
      return false;
    }
    return document.version == currentDocument.version;
  }

  /**
   * Returns the open document identified by {@code uri}.
   *
   * @return Null if desired document is currently not opened in the client.
   */
  private @Nullable Document getDocument(String uri) {
    synchronized (openDocuments) {
      return openDocuments.get(uri);
    }
  }

  private LspSnapshotFileSystem createSnapshotFileSystem() {
    synchronized (openDocuments) {
      return new LspSnapshotFileSystem(openDocuments);
    }
  }
}
