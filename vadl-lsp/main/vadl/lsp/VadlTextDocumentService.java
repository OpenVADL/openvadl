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

import java.nio.file.Path;
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
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.lsp.document.Document;
import vadl.lsp.document.DocumentStore;
import vadl.lsp.features.DefinitionFeature;
import vadl.lsp.features.HoverFeature;
import vadl.lsp.features.PublishDiagnosticsFeature;
import vadl.lsp.features.SemanticTokensFeature;

/**
 * Handles document-related features of the language server (responds to client requests).
 *
 * <p>Effectively, this is a glue layer between LSP, documents, and individual features.
 */
public class VadlTextDocumentService implements TextDocumentService {
  public static final String LANGUAGE_IDENTIFIER = "vadl";

  private static final Logger log = LoggerFactory.getLogger(VadlTextDocumentService.class);

  private final VadlLanguageServer server;
  private final DocumentStore documentStore;

  @Nullable
  private SemanticTokensFeature semanticTokensFeature;
  @Nullable
  private DefinitionFeature definitionFeature;
  @Nullable
  private HoverFeature hoverFeature;

  VadlTextDocumentService(VadlLanguageServer server) {
    this.server = server;
    this.documentStore = new DocumentStore(server.executor);
  }

  /**
   * Should be invoked by {@link VadlLanguageServer} when its {@code initialize()} method is
   * invoked. This allows this text service to fully initialize itself for the specific client.
   *
   * @param params as provided to
   *               {@link org.eclipse.lsp4j.services.LanguageServer#initialize(InitializeParams)}
   * @param serverCapabilities as determined by {@link VadlLanguageServer}
   */
  void initialize(InitializeParams params, ServerCapabilities serverCapabilities) {
    this.semanticTokensFeature = new SemanticTokensFeature(serverCapabilities);
    this.definitionFeature = new DefinitionFeature(params);
    this.hoverFeature = new HoverFeature(params);

    documentStore.setPublishDiagnosticsFeature(
        new PublishDiagnosticsFeature(server.client(), params)
    );
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
  public CompletableFuture<SemanticTokens> semanticTokensFull(SemanticTokensParams params) {
    log.debug(">> semanticTokens/full: {}", params);

    return CompletableFuture.supplyAsync(() -> {
      Document.CompilationInputAndResult compilation;
      SemanticTokens result;
      try {
        compilation = getCurrentCompilationForLspDocument(params.getTextDocument(),
            "semanticTokens/full");
        result = Objects.requireNonNull(semanticTokensFeature).semanticTokensFull(compilation);

      } catch (InterruptedException e) {
        result = SemanticTokensFeature.emptyResult();
      }

      log.debug("<<- semanticTokens/full: <omitted>({} tokens)", result.getData().size() / 5);
      return result;

    }, server.executor);
  }

  @Override
  public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>>
      definition(DefinitionParams params) {
    log.debug(">> definition: {}", params);

    return CompletableFuture.supplyAsync(() -> {
      Document.CompilationInputAndResult compilation;
      Either<List<? extends Location>, List<? extends LocationLink>> result;
      try {
        compilation = getCurrentCompilationForLspDocument(params.getTextDocument(), "definition");
        result = Objects.requireNonNull(definitionFeature).definition(params, compilation);

      } catch (InterruptedException e) {
        result = DefinitionFeature.emptyResult();
      }

      log.debug("<<- definition: {}", result);
      return result;

    }, server.executor);
  }

  @Override
  public CompletableFuture<Hover> hover(HoverParams params) {
    log.debug(">> hover: {}", params);

    return CompletableFuture.supplyAsync(() -> {
      Document.CompilationInputAndResult compilation;
      Hover result;
      try {
        compilation = getCurrentCompilationForLspDocument(params.getTextDocument(), "hover");
        result = Objects.requireNonNull(hoverFeature).hover(params, compilation);

      } catch (InterruptedException e) {
        result = null;
      }

      log.debug("<<- hover: {}", result);
      return result;

    }, server.executor);
  }


  /**
   * Wrapper for {@link DocumentStore#getCurrentCompilation(Path)}.
   *
   * @see DocumentStore#getCurrentCompilation(Path)
   * @param lspIdentifier Identifies the lsp text document.
   * @param action Used in Exception message.
   * @throws ResponseErrorException If desired document is currently not open in the client.
   */
  private Document.CompilationInputAndResult getCurrentCompilationForLspDocument(
      TextDocumentIdentifier lspIdentifier, String action) throws InterruptedException {

    var compilation = documentStore.getCurrentCompilation(
        toPath(lspIdentifier.getUri()));
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
