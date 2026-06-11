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
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.ast.Ast;
import vadl.ast.Frontend;
import vadl.ast.VadlParser;
import vadl.error.Diagnostic.MsgType;
import vadl.error.DiagnosticList;
import vadl.utils.DiskVirtualFileSystem;
import vadl.utils.SourceLocation;

/**
 * Handles document-related features of the language server.
 */
public class VadlTextDocumentService implements TextDocumentService {
  private static final String LANGUAGE_IDENTIFIER = "vadl";

  private static final Logger log = LoggerFactory.getLogger(VadlTextDocumentService.class);

  private final VadlLanguageServer server;

  private final Map<String, Document> openDocuments = new HashMap<>();
  private final DependencyMap<String> documentDependencies = new DependencyMap<>();

  VadlTextDocumentService(VadlLanguageServer server) {
    this.server = server;
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
  public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>>
      definition(DefinitionParams params) {
    log.debug(">> definition: {}", params);

    LspSnapshotFileSystem snapshots = createSnapshotFileSystem();
    return CompletableFuture.supplyAsync(() -> {
      Document document = getDocumentForParams(params, snapshots, "(Go to) definition");
      Ast ast;
      try {
        ast = VadlParser.parse(toPath(document.uri), snapshots);

      } catch (DiagnosticList dl) {
        log.debug("UNABLE definition: Parser produced diagnostics instead of AST for {}",
            document.uri);
        return definitionResult(null);
      }

      var position = document.calculateUtf8Position(params.getPosition(), false);
      SourceLocation location = AstFinderByPosition.findIdentifierTargetLocation(
          ast,
          toPath(document.uri),
          position
      );

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

  @Override
  public CompletableFuture<Hover> hover(HoverParams params) {
    log.debug(">> hover: {}", params);

    LspSnapshotFileSystem snapshots = createSnapshotFileSystem();
    return CompletableFuture.supplyAsync(() -> {
      Document document = getDocumentForParams(params, snapshots, "hover");
      Ast ast;
      try {
        ast = Frontend.compileToAst(toPath(document.uri), snapshots);

      } catch (DiagnosticList dl) {
        log.debug("UNABLE hover: Parser produced diagnostics instead of AST for {}", document.uri);
        return hoverResult(null, null);
      }

      var position = document.calculateUtf8Position(params.getPosition(), false);
      var node = AstFinderByPosition.findTypedNode(ast, toPath(document.uri), position);

      if (node == null) {
        return hoverResult(null, null);
      }

      return hoverResult(node.type().name(), document.calculateUtf16Range(node.location()));
    });
  }

  private @Nullable Hover hoverResult(@Nullable String text, @Nullable Range range) {
    Hover result = null;
    if (text != null) {
      var clientContentFormat = getClientMarkupContent();
      MarkupContent content = null;

      if (clientContentFormat.contains(MarkupKind.MARKDOWN)) {
        // For now, we assume that all hover texts are snippets of valid OpenVADL source code, so
        // let's use Markdown to mark them as such
        content = new MarkupContent(MarkupKind.MARKDOWN,
            "```" + LANGUAGE_IDENTIFIER + "\n" + text + "\n```");
      } else if (clientContentFormat.contains(MarkupKind.PLAINTEXT)) {
        // Fallback
        content = new MarkupContent(MarkupKind.PLAINTEXT, text);
      }

      if (content != null) {
        result = new Hover(content);
        result.setRange(range);
      }
    }
    log.debug("<<- hover: {}", result);
    return result;
  }

  private List<String> getClientMarkupContent() {
    var capabilities = server.params().getCapabilities().getTextDocument();
    if (capabilities == null || capabilities.getHover() == null
        || capabilities.getHover().getContentFormat() == null) {
      return List.of();
    }
    return capabilities.getHover().getContentFormat();
  }

  /**
   * Manages diagnostic publishing for a given document, incl. version checking, and
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
      } catch (DiagnosticList dl) {
        log.debug("Raw diagnostics ({}): {}", document.uri, dl.getMessage());
        List<String> importedFileErrors = new ArrayList<>();
        for (vadl.error.Diagnostic item : dl.collapseSimilar().items) {
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
            "ABORT publishDiagnostics: outdated version {} of document {}",
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

  /**
   * Returns the document identified by given LSP {@code params} and contained in {@code snapshots}.
   *
   * @param action Used in Exception message.
   * @throws ResponseErrorException If desired document cannot be found in {@code snapshots}.
   */
  private Document getDocumentForParams(
      TextDocumentPositionParams params, LspSnapshotFileSystem snapshots, String action) {

    var document = snapshots.getDocument(params.getTextDocument().getUri());
    if (document == null) {
      throw new ResponseErrorException(new ResponseError(
          ResponseErrorCode.RequestFailed,
          "Requested " + action + " for a document that is not open.",
          null
      ));
    }

    return document;
  }

  private LspSnapshotFileSystem createSnapshotFileSystem() {
    synchronized (openDocuments) {
      return new LspSnapshotFileSystem(openDocuments, new DiskVirtualFileSystem());
    }
  }
}
