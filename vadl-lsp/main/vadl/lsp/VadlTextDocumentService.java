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

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
import org.eclipse.lsp4j.services.TextDocumentService;
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
  public void didOpen(DidOpenTextDocumentParams params) {
    log.debug(">> didOpen: {}", params);

    Document document = new Document(params.getTextDocument());
    LspSnapshotFileSystem snapshots;
    synchronized (openDocuments) {
      openDocuments.put(params.getTextDocument().getUri(), document);
      snapshots = createSnapshotFileSystem();
    }
    publishDiagnostics(document, snapshots);
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

    Document document;
    LspSnapshotFileSystem snapshots;
    synchronized (openDocuments) {
      document = openDocuments.computeIfPresent(params.getTextDocument().getUri(), (k, d) ->
          d.withChanges(params.getTextDocument().getVersion(), params.getContentChanges()));
      snapshots = createSnapshotFileSystem();
    }
    if (document == null) {
      return;
    }

    publishDiagnostics(document, snapshots);
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

      List<Integer> tokens = tokenizer != null
            ? tokenizer.getTokens(document.getText())
            : new ArrayList<>();
      SemanticTokens result = new SemanticTokens(document.calculateUtf16Positions(tokens));
      log.debug("<<- semanticTokens/full: <omitted>({} tokens)", tokens.size() / 5);
      return result;
    });
  }

  @Override
  public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>>
      definition(DefinitionParams params) {
    log.debug(">> definition: {}", params);

    LspSnapshotFileSystem snapshots = createSnapshotFileSystem();
    return CompletableFuture.supplyAsync(() -> {
      String uri = params.getTextDocument().getUri();
      Document document = snapshots.getDocument(uri);
      if (document == null) {
        throw new ResponseErrorException(new ResponseError(
            ResponseErrorCode.RequestFailed,
            "Requested (Go to) definition for a document that is not open.",
            null
        ));
      }

      Ast ast;
      try {
        ast = VadlParser.parse(toPath(uri), snapshots);

      } catch (IOException e) {
        log.error("Unexpected Exception occurred when parsing {}", uri, e);
        return definitionResult(null);
      } catch (DiagnosticList dl) {
        log.debug("UNABLE definition: Parser produced diagnostics instead of AST for {}", uri);
        return definitionResult(null);
      }

      var position = document.calculateUtf8Position(params.getPosition(), false);
      SourceLocation location = AstFinderByPosition.findIdentifierTargetLocation(ast, toPath(uri),
          position);
      // TODO AST doesn't provide all the data we would like to have:
      //      - These Identifiers in ImportDefinition have no target set and are not visited
      //        (missing @Child annotations): fileId; importedSymbols[x]
      //      - Model usages appear to be applied, i.e. we don't know that the searched position is
      //        on a model usage, hence we cannot Goto Definition to the model. (Except if we
      //        analyze the expandedFrom data, but that is complex and/or points to only part of the
      //        model.)
      //      - References to Model parameters (within the model body) appear to not be an
      //        identifier nor have a target

      if (location == null || location.path() == null) {
        return definitionResult(null);
      }
      var targetDocument = snapshots.getFileBasedDocument(toUri(location.path()));
      if (targetDocument == null) {
        log.debug("Unexpected: Definition target file {} does not exist", toUri(location.path()));
        return definitionResult(null);
      }

      var lspLocation = new Location(targetDocument.uri,
          targetDocument.calculateUtf16Range(location));
      return definitionResult(lspLocation);
    });
  }

  private Either<List<? extends Location>, List<? extends LocationLink>> definitionResult(
      @Nullable Location lspLocation) {
    log.debug("<<- definition: {}", lspLocation);
    return Either.forLeft(lspLocation != null ? List.of(lspLocation) : List.of());
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
    if (capabilities == null
        || capabilities.getPublishDiagnostics() == null) {
      // Don't push diagnostics if client doesn't support it
      return;
    }

    var unused = server.executor().submit(() -> {
      try {
        // TODO Consider to instead delay for remaining time *after* generating diagnostics, to have
        //  more accurate delay timing
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

      // Update diagnostics for all dependent documents
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
      } catch (IOException e) {
        log.error("Unexpected Exception occurred when parsing {}", document.uri, e);

      } catch (DiagnosticList dl) {
        log.debug("Raw diagnostics ({}): {}", document.uri, dl.getMessage());
        List<String> importedFileErrors = new ArrayList<>();
        for (vadl.error.Diagnostic item : dl.items) {
          Path itemPath = item.multiLocation.primaryLocation().location().path();
          if (!Objects.equals(itemPath, path)) {
            if (itemPath == null) {
              continue;
            }
            // Error in imported file
            importedFileErrors.add(LspUtils.relativePath(itemPath, document.getPath()));
            continue;
          }

          lspItems.add(buildLspDiagnostic(item, document));
        }

        if (!importedFileErrors.isEmpty()) {
          // Putting one diagnostic at the top of the file, which points out which imported files
          // have errors
          Diagnostic importedFilesDiagnostic = new Diagnostic();
          importedFilesDiagnostic.setRange(new Range(new Position(0, 0),
              new Position(0, 0)));
          // TODO Consider using different severity if all diagnostics represented by this are only
          //      Warnings
          importedFilesDiagnostic.setSeverity(DiagnosticSeverity.Error);

          String message = importedFileErrors.size() == 1
              ? "Errors in imported file: \n" + importedFileErrors.getFirst()
              : "Errors in imported files:\n- " + String.join("\n- ", importedFileErrors);
          importedFilesDiagnostic.setMessage(message);
          lspItems.addFirst(importedFilesDiagnostic);
        }
      }
      // TODO There may be diagnostics in DeferredDiagnosticStore, but that is a static list and
      //      has no clear() method (i.e. outdated diagnostics remain visible)

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
      log.debug("<< publishDiagnostics ({}: {}", document.uri, data);
      server.client().publishDiagnostics(data);
    });
  }

  private Diagnostic buildLspDiagnostic(vadl.error.Diagnostic vadlDiagnostic,
                                        Document document) {
    // TODO Look into secondary locations too? Maybe as relatedInformation? Or to put a
    //      diagnostic message there as well?
    SourceLocation location = vadlDiagnostic.multiLocation.primaryLocation().location();

    Diagnostic lspDiagnostic = new Diagnostic();
    lspDiagnostic.setRange(document.calculateUtf16Range(location));
    lspDiagnostic.setSeverity(
        switch (vadlDiagnostic.level) {
          case ERROR -> DiagnosticSeverity.Error;
          case WARNING -> DiagnosticSeverity.Warning;
        }
    );
    // labels (aka messages) per location
    String labelsString = vadlDiagnostic.multiLocation.primaryLocation().labels().stream()
        .map(vadl.error.Diagnostic.Message::content)
        .collect(Collectors.joining("\n"));
    // messages per Diagnostic - they may offer help or give additional notes
    String messagesString = vadlDiagnostic.messages.stream()
        .filter(m -> !m.type().equals(MsgType.PLAIN)
            || !m.content().contains("parser got confused at this point"))
        .map(vadl.error.Diagnostic.Message::content)
        .collect(Collectors.joining("\n"));

    String fullMessage = vadlDiagnostic.reason
        + (!labelsString.isBlank() ? "\n" + labelsString : "")
        + (!messagesString.isBlank() ? "\n" + messagesString : "");
    lspDiagnostic.setMessage(fullMessage);

    return lspDiagnostic;
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
