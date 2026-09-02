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
import vadl.ast.nodes.IdentifiableNode;
import vadl.ast.nodes.IsId;
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
        return emptyDefinitionResult();
      }

      var position = document.calculateUtf8Position(params.getPosition(), false);
      IsId identifier = AstFinderByPosition.findIdentifier(
          ast,
          toPath(document.uri),
          position
      );
      if (identifier == null) {
        return emptyDefinitionResult();
      }
      var target = identifier.target();
      if (target == null || !target.location().isValid()) {
        return emptyDefinitionResult();
      }
      var targetUri = toUri(Objects.requireNonNull(target.location().path()));
      var targetDocument = snapshots.getFileBasedDocument(targetUri);
      if (targetDocument == null) {
        log.debug("Unexpected: Definition target file {} does not exist", targetUri);
        return emptyDefinitionResult();
      }

      // targetSelectionRange is the location the cursor jumps to, whereas targetRange refers to the
      // whole definition we jump to.
      // See https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#locationLink
      var targetRange = targetDocument.calculateUtf16Range(target.location());
      Range targetSelectionRange = targetRange; // Fallback
      if (target instanceof IdentifiableNode identifiableTarget) {
        targetSelectionRange = targetDocument.calculateUtf16Range(
            identifiableTarget.identifier().location());

        if (!isWithin(targetSelectionRange, targetRange)) {
          // Selection range MUST be contained in target range. If that is not the case, the target
          // identifier is provided by a model invocation, and it is better not to jump anywhere.
          return emptyDefinitionResult();
        }
      }
      var originSelectionRange = document.calculateUtf16Range(identifier.location());

      return definitionResult(targetDocument.uri, targetRange, targetSelectionRange,
          originSelectionRange);
    });
  }

  private Either<List<? extends Location>, List<? extends LocationLink>> definitionResult(
      String targetUri, Range targetRange, Range targetSelectionRange, Range originSelectionRange) {

    if (!clientSupportsDefinitionLink()) {
      var location = new Location(targetUri, targetSelectionRange);
      log.debug("<<- definition: {}", location);
      return Either.forLeft(List.of(location));
    }

    var locationLink = new LocationLink(targetUri, targetRange, targetSelectionRange,
        originSelectionRange);
    log.debug("<<- definition: {}", locationLink);
    return Either.forRight(List.of(locationLink));
  }

  private Either<List<? extends Location>, List<? extends LocationLink>> emptyDefinitionResult() {
    log.debug("<<- definition: []");
    return Either.forLeft(List.of());
  }

  private boolean isWithin(Range a, Range b) {
    if (a.getStart().getLine() < b.getStart().getLine()
        || a.getEnd().getLine() > b.getEnd().getLine()) {
      return false;
    }
    if (a.getStart().getLine() == b.getStart().getLine()
        && a.getStart().getCharacter() < b.getStart().getCharacter()) {
      return false;
    }
    if (a.getEnd().getLine() == b.getEnd().getLine()
        && a.getEnd().getCharacter() > b.getEnd().getCharacter()) {
      return false;
    }
    return true;
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
        return emptyHoverResult();
      }

      var position = document.calculateUtf8Position(params.getPosition(), false);
      var path = toPath(document.uri);

        {
          // Show type information
          var node = AstFinderByPosition.findTypedNode(ast, path, position);
          if (node != null) {
            return hoverResult(null, node.type().name(),
                document.calculateUtf16Range(node.location()));
          }
        }

        {
          // Show expanded code (for model invocations)
          var nodes = AstFinder.findExpandedNodes(ast, path, position);
          if (!nodes.isEmpty()) {
            var range = document.calculateUtf16Range(
                nodes.getFirst().location().expandedFromStack().getLast());
            var prettyPrinted = new ArrayList<String>(nodes.size());
            for (var node : nodes) {
              var builder = new StringBuilder();
              node.prettyPrint(0, builder);
              prettyPrinted.add(builder.toString().trim());
            }
            return hoverResult("This model invocation expands to:",
                String.join("\n", prettyPrinted), range);
          }
        }

      return emptyHoverResult();
    });
  }

  private @Nullable Hover hoverResult(@Nullable String text, @Nullable String sourceCode,
      @Nullable Range range) {
    if (text == null && sourceCode == null) {
      log.debug("<<- hover: null");
      return null;
    }

    var clientContentFormat = getClientMarkupContent();
    MarkupContent content = null;
    List<String> parts = new ArrayList<>();
    if (text != null) {
      parts.add(text);
    }

    if (clientContentFormat.contains(MarkupKind.MARKDOWN)) {
      if (sourceCode != null) {
        parts.add("```" + LANGUAGE_IDENTIFIER + "\n" + sourceCode + "\n```");
      }
      content = new MarkupContent(MarkupKind.MARKDOWN, String.join("  \n", parts));

    } else if (clientContentFormat.contains(MarkupKind.PLAINTEXT)) {
      // Fallback
      if (sourceCode != null) {
        parts.add(sourceCode);
      }
      content = new MarkupContent(MarkupKind.PLAINTEXT, String.join("\n", parts));
    }

    if (content == null) {
      log.debug("<<- hover: null");
      return null;
    }

    var result = new Hover(content);
    result.setRange(range);
    log.debug("<<- hover: {}", result);
    return result;
  }

  private @Nullable Hover emptyHoverResult() {
    return hoverResult(null, null, null);
  }

  /**
   * Manages diagnostic publishing for a given document, incl. version checking, and
   * updating dependent documents.
   *
   * @param document  Must be contained in {@code snapshots}
   * @param snapshots Must be fresh, i.e. not used in the VADL parser yet
   */
  private void publishDiagnostics(Document document, LspSnapshotFileSystem snapshots) {
    if (!clientSupportsPublishDiagnostics()) {
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


  private boolean clientSupportsDefinitionLink() {
    var capabilities = server.params().getCapabilities().getTextDocument();
    if (capabilities == null || capabilities.getDefinition() == null
        || capabilities.getDefinition().getLinkSupport() == null) {
      return false;
    }
    return capabilities.getDefinition().getLinkSupport();
  }

  private List<String> getClientMarkupContent() {
    var capabilities = server.params().getCapabilities().getTextDocument();
    if (capabilities == null || capabilities.getHover() == null
        || capabilities.getHover().getContentFormat() == null) {
      return List.of();
    }
    return capabilities.getHover().getContentFormat();
  }

  private boolean clientSupportsPublishDiagnostics() {
    var capabilities = server.params().getCapabilities().getTextDocument();
    return capabilities != null && capabilities.getPublishDiagnostics() != null;
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
