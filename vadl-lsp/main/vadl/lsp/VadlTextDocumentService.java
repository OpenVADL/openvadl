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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import vadl.utils.DiskVirtualFileSystem;
import vadl.utils.SourceLocation;
import vadl.utils.VirtualFileSystem;

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

  public final VirtualFileSystem underlyingFileSystem = new DiskVirtualFileSystem();

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
      Document.Snapshot snapshot = document.getSnapshot();

      List<Integer> tokens = tokenizer != null
            ? tokenizer.getTokens(snapshot.text())
            : new ArrayList<>();
      SemanticTokens result = new SemanticTokens(snapshot.calculateUtf16Positions(tokens));
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
    Document.Snapshot snapshot = document.getSnapshot();

    var unused = server.executor().submit(() -> {
      try {
        // TODO Consider to instead delay for remaining time *after* generating diagnostics, to have
        //  more accurate delay timing
        Thread.sleep(DIAGNOSTICS_DELAY_MS);
      } catch (InterruptedException e) {
        return;
      }
      if (!documentVersionIsCurrent(snapshot)) {
        log.debug(
            "ABORT publishDiagnostics (before): outdated version {} of document {}",
            snapshot.version(),
            snapshot.uri()
        );
        return;
      }

      publishDiagnosticsForOneDocumentSnapshot(snapshot);

      // Update diagnostics for all dependent documents
      for (String uri : documentDependencies.getDependents(snapshot.uri())) {
        Document d = getDocument(uri);
        if (d != null) {
          publishDiagnosticsForOneDocumentSnapshot(d.getSnapshot());
        }
      }
    });
  }

  private void publishDiagnosticsForOneDocumentSnapshot(Document.Snapshot snapshot) {
    var unused = server.executor().submit(() -> {
      List<Diagnostic> lspItems = new ArrayList<>();
      Path path = snapshot.getPath();
      var fileSystem = new LspVirtualFileSystem(this);
      try {
        Ast ast = VadlParser.parse(snapshot.text(), fileSystem, Map.of(), path);
        new ModelRemover().removeModels(ast);
        new Ungrouper().ungroup(ast);
        new TypeChecker().verify(ast);

      } catch (DiagnosticList dl) {
        log.debug("Raw diagnostics ({}): {}", snapshot.uri(), dl.getMessage());
        List<String> importedFileErrors = new ArrayList<>();
        for (vadl.error.Diagnostic item : dl.items) {
          Path itemPath = item.multiLocation.primaryLocation().location().path();
          if (!Objects.equals(itemPath, path)) {
            if (itemPath == null) {
              continue;
            }
            // Error in imported file
            importedFileErrors.add(relativePath(itemPath, snapshot.getPath()));
            continue;
          }

          lspItems.add(buildLspDiagnostic(item, snapshot));
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

      if (!documentVersionIsCurrent(snapshot)) {
        log.debug(
            "ABORT publishDiagnostics (after): outdated version {} of document {}",
            snapshot.version(),
            snapshot.uri()
        );
        return;
      }
      documentDependencies.setDependencies(snapshot.uri(), fileSystem.getReadFiles());
      var data = new PublishDiagnosticsParams(snapshot.uri(), lspItems, snapshot.version());
      log.debug("<< publishDiagnostics ({}: {}", snapshot.uri(), data);
      server.client().publishDiagnostics(data);
    });
  }

  private Diagnostic buildLspDiagnostic(vadl.error.Diagnostic vadlDiagnostic,
                                        Document.Snapshot snapshot) {
    // TODO Look into secondary locations too? Maybe as relatedInformation? Or to put a
    //      diagnostic message there as well?
    SourceLocation location = vadlDiagnostic.multiLocation.primaryLocation().location();

    Diagnostic lspDiagnostic = new Diagnostic();
    lspDiagnostic.setRange(new Range(
        snapshot.calculateUtf16Position(location.begin(), false),
        snapshot.calculateUtf16Position(location.end(), true)
    ));
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

  private String relativePath(Path path, Path relativeTo) {
    // relativeTo is a file, but we need its directory as base
    relativeTo = relativeTo.getParent() != null ? relativeTo.getParent() : relativeTo;
    return relativeTo.relativize(path).toString();
  }

  private boolean documentVersionIsCurrent(Document.Snapshot snapshot) {
    Document document = getDocument(snapshot.uri());
    if (document == null) {
      return false;
    }
    return document.getCurrentVersion() == snapshot.version();
  }

  /**
   * Returns the open document identified by {@code uri}.
   *
   * @return Null if desired document is currently not opened in the client.
   */
  public @Nullable Document getDocument(String uri) {
    synchronized (openDocuments) {
      return openDocuments.get(uri);
    }
  }
}
