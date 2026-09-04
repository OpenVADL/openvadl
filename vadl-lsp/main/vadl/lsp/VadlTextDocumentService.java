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
import java.util.Set;
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
import vadl.ast.Frontend;
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

  final VadlLanguageServer server;

  private final Map<String, Document> openDocuments = new HashMap<>();
  private final Map<String, DocumentCompilation> documentCompilations = new HashMap<>();
  private final DependencyMap<String> documentDependencies = new DependencyMap<>();

  VadlTextDocumentService(VadlLanguageServer server) {
    this.server = server;
  }

  @Override
  public void didOpen(DidOpenTextDocumentParams params) {
    log.debug(">> didOpen: {}", params);

    Document document = new Document(params.getTextDocument());
    DocumentCompilation documentCompilation = new DocumentCompilation(document.uri, this);
    synchronized (openDocuments) {
      openDocuments.put(document.uri, document);
      documentCompilations.put(document.uri, documentCompilation);
    }
    clearDependentCompilations(documentCompilation);

    publishDiagnostics(documentCompilation);
    publishDiagnosticsForDependentDocuments(documentCompilation);
  }

  @Override
  public void didClose(DidCloseTextDocumentParams params) {
    log.debug(">> didClose: {}", params);
    DocumentCompilation documentCompilation;
    synchronized (openDocuments) {
      openDocuments.remove(params.getTextDocument().getUri());
      documentCompilation = documentCompilations.remove(params.getTextDocument().getUri());
    }

    if (documentCompilation == null) {
      return;
    }
    clearDependentCompilations(documentCompilation);
    documentCompilation.clearCompilation();

    publishDiagnosticsForDependentDocuments(documentCompilation);
    documentDependencies.setDependencies(documentCompilation.uri, Set.of());
  }

  @Override
  public void didChange(DidChangeTextDocumentParams params) {
    log.debug(">> didChange: {}", params);

    DocumentCompilation documentCompilation;
    synchronized (openDocuments) {
      openDocuments.computeIfPresent(params.getTextDocument().getUri(), (k, d) ->
          d.withChanges(params.getTextDocument().getVersion(), params.getContentChanges()));
      documentCompilation = documentCompilations.get(params.getTextDocument().getUri());
    }

    if (documentCompilation == null) {
      return;
    }
    documentCompilation.clearCompilation();
    clearDependentCompilations(documentCompilation);

    publishDiagnostics(documentCompilation);
    publishDiagnosticsForDependentDocuments(documentCompilation);
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

    return CompletableFuture.supplyAsync(() -> {
      DocumentCompilation document = getDocumentCompilationForParams(params, "definition");
      DocumentCompilation.CompilationInputAndResult compilation;
      try {
        compilation = document.getCurrentCompilation();
      } catch (InterruptedException e) {
        return emptyDefinitionResult();
      }

      if (compilation.result().ast() == null) {
        log.debug("UNABLE definition: Parser produced no AST for {}", document.uri);
        return emptyDefinitionResult();
      }

      var position = compilation.document()
          .calculateUtf8Position(params.getPosition(), false);
      IsId identifier = AstFinderByPosition.findIdentifier(
          compilation.result().ast(),
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
      var targetDocument = compilation.fileSystemSnapshot().getFileBasedDocument(targetUri);
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
      var originSelectionRange = compilation.document()
          .calculateUtf16Range(identifier.location());

      return definitionResult(targetDocument.uri, targetRange, targetSelectionRange,
          originSelectionRange);

    }, server.executor);
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

    return CompletableFuture.supplyAsync(() -> {
      DocumentCompilation document = getDocumentCompilationForParams(params, "hover");
      DocumentCompilation.CompilationInputAndResult compilation;
      try {
        compilation = document.getCurrentCompilation();
      } catch (InterruptedException e) {
        log.debug("<<- hover: null");
        return null;
      }

      if (compilation.result().ast() == null
          || !compilation.result().completedPass().includes(Frontend.AstPass.PARTIALLY_TYPE_CHECKED)
      ) {
        log.debug("UNABLE hover: Parser didn't fully typecheck AST for {}", document.uri);
        log.debug("<<- hover: null");
        return null;
      }

      var position = compilation.document()
          .calculateUtf8Position(params.getPosition(), false);

      // 1) Show type information
      Hover result = typeHover(compilation, position);

      // 2) Show expanded code (for model invocations)
      if (result == null) {
        result = modelExpansionHover(compilation, position);
      }

      log.debug("<<- hover: {}", result);
      return result;
    });
  }

  private @Nullable Hover typeHover(DocumentCompilation.CompilationInputAndResult compilation,
      SourceLocation.Position position) {
    var node = AstFinderByPosition.findTypedNode(Objects.requireNonNull(compilation.result().ast()),
        compilation.document().getPath(), position);
    if (node == null) {
      return null;
    }

    return hoverResult(null, node.type().name(),
        compilation.document().calculateUtf16Range(node.location()));
  }

  private @Nullable Hover modelExpansionHover(
      DocumentCompilation.CompilationInputAndResult compilation, SourceLocation.Position position) {
    var nodes = AstFinder.findExpandedNodes(Objects.requireNonNull(compilation.result().ast()),
        compilation.document().getPath(), position);
    if (nodes.isEmpty()) {
      return null;
    }

    var range = compilation.document().calculateUtf16Range(
        nodes.getFirst().location().outermostDirectLocation());
    var prettyPrinted = new ArrayList<String>(nodes.size());
    for (var node : nodes) {
      var builder = new StringBuilder();
      node.prettyPrint(0, builder);
      prettyPrinted.add(builder.toString().trim());
    }
    return hoverResult("This model invocation expands to:",
        String.join("\n", prettyPrinted), range);
  }

  private @Nullable Hover hoverResult(@Nullable String text, @Nullable String sourceCode,
      @Nullable Range range) {

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
      return null;
    }

    var result = new Hover(content);
    result.setRange(range);
    return result;
  }

  /**
   * Publishes new diagnostics for all dependent documents of the given document.
   */
  private void publishDiagnosticsForDependentDocuments(DocumentCompilation document) {
    if (!clientSupportsPublishDiagnostics()) {
      return;
    }

    for (String uri : documentDependencies.getDependents(document.uri)) {
      var d = getDocumentCompilation(uri);
      if (d != null) {
        publishDiagnostics(d);
      }
    }
  }

  /**
   * Publishes new diagnostics for a particular document.
   */
  private void publishDiagnostics(DocumentCompilation document) {
    if (!clientSupportsPublishDiagnostics()) {
      return;
    }

    var unused = server.executor.submit(() -> {
      DocumentCompilation.CompilationInputAndResult compilation;
      try {
        compilation = document.getCurrentCompilation();
      } catch (InterruptedException e) {
        return;
      }
      if (!compilation.publishedDiagnostics().compareAndSet(false, true)) {
        // Several publishDiagnostics() instances attached to the same compilation (race condition);
        // let's avoid doing the exact same work more than once.
        return;
      }

      DiagnosticList diagnostics = compilation.result().diagnostics();
      List<Diagnostic> lspItems = new ArrayList<>();
      if (diagnostics != null) {
        log.debug("Raw diagnostics ({}): {}", document.uri, diagnostics.getMessage());

        Path path = compilation.document().getPath();
        List<String> importedFileErrors = new ArrayList<>();
        for (vadl.error.Diagnostic item : diagnostics.collapseSimilar().items) {
          Path itemPath = item.multiLocation.primaryLocation().location().path();
          if (!Objects.equals(itemPath, path)) {
            if (itemPath == null) {
              continue;
            }
            // Error in imported file
            importedFileErrors.add(LspUtils.relativePath(itemPath, path));
            continue;
          }
          lspItems.add(buildLspDiagnostic(item, compilation.document()));
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

      if (!documentVersionIsCurrent(compilation.document())) {
        return;
      }

      var data = new PublishDiagnosticsParams(document.uri, lspItems,
          compilation.document().version);
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
    Document currentDocument;
    synchronized (openDocuments) {
      currentDocument = openDocuments.get(document.uri);
    }
    if (currentDocument == null) {
      return false;
    }
    return document.version == currentDocument.version;
  }

  /**
   * Returns the Document Compilation state identified by {@code uri}.
   *
   * @return Null if desired document is currently not open in the client.
   */
  private @Nullable DocumentCompilation getDocumentCompilation(String uri) {
    synchronized (openDocuments) {
      return documentCompilations.get(uri);
    }
  }

  /**
   * Returns the Document Compilation identified by given LSP {@code params}.
   *
   * @param action Used in Exception message.
   * @throws ResponseErrorException If desired document is currently not open in the client.
   */
  private DocumentCompilation getDocumentCompilationForParams(TextDocumentPositionParams params,
                                                              String action) {

    var documentCompilation = getDocumentCompilation(params.getTextDocument().getUri());
    if (documentCompilation == null) {
      throw new ResponseErrorException(new ResponseError(
          ResponseErrorCode.RequestFailed,
          "Requested " + action + " for a document that is not open.",
          null
      ));
    }

    return documentCompilation;
  }

  /**
   * Creates a new files snapshot as used in a compiler run.
   */
  LspSnapshotFileSystem createSnapshotFileSystem() {
    synchronized (openDocuments) {
      return new LspSnapshotFileSystem(openDocuments, new DiskVirtualFileSystem());
    }
  }

  /**
   * Updates file dependency data based on the given compilation result.
   */
  void updateDependencies(DocumentCompilation.CompilationInputAndResult compilation) {
    synchronized (openDocuments) {
      if (!documentVersionIsCurrent(compilation.document())) {
        return;
      }
      documentDependencies.setDependencies(compilation.document().uri,
          compilation.fileSystemSnapshot().getReadFiles());
    }
  }

  private void clearDependentCompilations(DocumentCompilation documentCompilation) {
    for (String uri : documentDependencies.getDependents(documentCompilation.uri)) {
      var d = getDocumentCompilation(uri);
      if (d != null) {
        d.clearCompilation();
      }
    }
  }
}
