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

import static vadl.lsp.LspUtils.isWithin;
import static vadl.lsp.LspUtils.toPath;
import static vadl.lsp.LspUtils.toUri;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import org.eclipse.lsp4j.DefinitionParams;
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
import vadl.lsp.document.Document;
import vadl.lsp.document.DocumentStore;
import vadl.utils.SourceLocation;

/**
 * Handles document-related features of the language server (responds to client requests).
 *
 * @see DiagnosticsPublisher
 */
public class VadlTextDocumentService implements TextDocumentService {
  private static final String LANGUAGE_IDENTIFIER = "vadl";

  private static final Logger log = LoggerFactory.getLogger(VadlTextDocumentService.class);

  public final VadlLanguageServer server;
  final DocumentStore documentStore = new DocumentStore(this);

  VadlTextDocumentService(VadlLanguageServer server) {
    this.server = server;
  }


  @Override
  public void didOpen(DidOpenTextDocumentParams params) {
    log.debug(">> didOpen: {}", params);
    documentStore.open(params.getTextDocument());
  }

  @Override
  public void didClose(DidCloseTextDocumentParams params) {
    log.debug(">> didClose: {}", params);
    documentStore.close(toPath(params.getTextDocument().getUri()));
  }

  @Override
  public void didChange(DidChangeTextDocumentParams params) {
    log.debug(">> didChange: {}", params);
    documentStore.change(
        toPath(params.getTextDocument().getUri()),
        params.getTextDocument().getVersion(),
        params.getContentChanges()
    );
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
      Document.CompilationInputAndResult compilation;
      try {
        compilation = getCurrentCompilationForParams(params, "definition");
      } catch (InterruptedException e) {
        return emptyDefinitionResult();
      }

      if (compilation.result().ast() == null
          || !compilation.result().completedPass()
          .includes(Frontend.AstPass.PARTIALLY_NAMES_RESOLVED)
      ) {
        log.debug("UNABLE definition: Parser produced no AST for {}",
            compilation.documentSnapshot().path);
        return emptyDefinitionResult();
      }

      var position = compilation.documentSnapshot()
          .calculateUtf8Position(params.getPosition(), false);
      IsId identifier = AstFinderByPosition.findIdentifier(
          compilation.result().ast(),
          compilation.documentSnapshot().path,
          position
      );
      if (identifier == null) {
        return emptyDefinitionResult();
      }
      var target = identifier.target();
      if (target == null || !target.location().isValid()) {
        return emptyDefinitionResult();
      }
      var targetPath = Objects.requireNonNull(target.location().path());
      var targetDocument = compilation.fileSystemSnapshot()
          .getFileBasedDocumentSnapshot(targetPath);
      if (targetDocument == null) {
        log.debug("Unexpected: Definition target file {} does not exist", targetPath);
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
      var originSelectionRange = compilation.documentSnapshot()
          .calculateUtf16Range(identifier.location());

      return definitionResult(targetDocument.path, targetRange, targetSelectionRange,
          originSelectionRange);

    }, server.executor);
  }

  private Either<List<? extends Location>, List<? extends LocationLink>> definitionResult(
      Path targetPath, Range targetRange, Range targetSelectionRange, Range originSelectionRange) {

    if (!clientSupportsDefinitionLink()) {
      var location = new Location(toUri(targetPath), targetSelectionRange);
      log.debug("<<- definition: {}", location);
      return Either.forLeft(List.of(location));
    }

    var locationLink = new LocationLink(toUri(targetPath), targetRange, targetSelectionRange,
        originSelectionRange);
    log.debug("<<- definition: {}", locationLink);
    return Either.forRight(List.of(locationLink));
  }

  private Either<List<? extends Location>, List<? extends LocationLink>> emptyDefinitionResult() {
    log.debug("<<- definition: []");
    return Either.forLeft(List.of());
  }


  @Override
  public CompletableFuture<Hover> hover(HoverParams params) {
    log.debug(">> hover: {}", params);

    return CompletableFuture.supplyAsync(() -> {
      Document.CompilationInputAndResult compilation;
      try {
        compilation = getCurrentCompilationForParams(params, "hover");
      } catch (InterruptedException e) {
        log.debug("<<- hover: null");
        return null;
      }

      if (compilation.result().ast() == null
          || !compilation.result().completedPass().includes(Frontend.AstPass.PARTIALLY_TYPE_CHECKED)
      ) {
        log.debug("UNABLE hover: Parser didn't typecheck AST for {}",
            compilation.documentSnapshot().path);
        log.debug("<<- hover: null");
        return null;
      }

      var position = compilation.documentSnapshot()
          .calculateUtf8Position(params.getPosition(), false);

      // 1) Show type information
      Hover result = typeHover(compilation, position);

      // 2) Show expanded code (for model invocations)
      if (result == null) {
        result = modelExpansionHover(compilation, position);
      }

      log.debug("<<- hover: {}", result);
      return result;
    }, server.executor);
  }

  private @Nullable Hover typeHover(Document.CompilationInputAndResult compilation,
                                    SourceLocation.Position position) {
    var node = AstFinderByPosition.findTypedNode(Objects.requireNonNull(compilation.result().ast()),
        compilation.documentSnapshot().path, position);
    if (node == null) {
      return null;
    }

    return hoverResult(null, node.type().name(),
        compilation.documentSnapshot().calculateUtf16Range(node.location()));
  }

  private @Nullable Hover modelExpansionHover(
      Document.CompilationInputAndResult compilation, SourceLocation.Position position) {
    var nodes = AstFinder.findExpandedNodes(Objects.requireNonNull(compilation.result().ast()),
        compilation.documentSnapshot().path, position);
    if (nodes.isEmpty()) {
      return null;
    }

    var range = compilation.documentSnapshot().calculateUtf16Range(
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

  /**
   * Wrapper for {@link DocumentStore#getCurrentCompilation(Path)}.
   *
   * @see DocumentStore#getCurrentCompilation(Path)
   * @param action Used in Exception message.
   * @throws ResponseErrorException If desired document is currently not open in the client.
   */
  private Document.CompilationInputAndResult getCurrentCompilationForParams(
      TextDocumentPositionParams params, String action) throws InterruptedException {

    var compilation = documentStore.getCurrentCompilation(
        toPath(params.getTextDocument().getUri()));
    if (compilation == null) {
      throw new ResponseErrorException(new ResponseError(
          ResponseErrorCode.RequestFailed,
          "Requested " + action + " for a document that is not open.",
          null
      ));
    }

    return compilation;
  }
}
