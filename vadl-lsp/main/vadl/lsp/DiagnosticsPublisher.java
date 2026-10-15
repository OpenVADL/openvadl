// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

import static vadl.lsp.LspUtils.toUri;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.error.DiagnosticList;
import vadl.lsp.document.Document;
import vadl.lsp.document.DocumentSnapshot;
import vadl.lsp.document.DocumentStore;
import vadl.utils.SourceLocation;

/**
 * Publishes diagnostics for documents. This is not done on client request, but triggered when
 * necessary (i.e. by {@link DocumentStore}).
 */
public class DiagnosticsPublisher {
  private static final Logger log = LoggerFactory.getLogger(DiagnosticsPublisher.class);

  private final DocumentStore documentStore;

  public DiagnosticsPublisher(DocumentStore documentStore) {
    this.documentStore = documentStore;
  }

  /**
   * Publishes new diagnostics for a particular document. This is done asynchronously.
   */
  public void publishDiagnostics(Document document) {
    if (!clientSupportsPublishDiagnostics()) {
      return;
    }

    var unused = documentStore.documentService.server.executor.submit(() -> {
      Document.CompilationInputAndResult compilation;
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
        log.debug("Raw diagnostics ({}): {}", document.getPath(), diagnostics.getMessage());

        Path path = compilation.documentSnapshot().path;
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
          lspItems.add(buildLspDiagnostic(item, compilation.documentSnapshot()));
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

      if (!documentStore.documentVersionIsCurrent(compilation.documentSnapshot())) {
        return;
      }

      var data = new PublishDiagnosticsParams(toUri(document.getPath()), lspItems,
          compilation.documentSnapshot().version);
      log.debug("<< publishDiagnostics ({}: {}", document.getPath(), data);
      documentStore.documentService.server.client().publishDiagnostics(data);
    });
  }

  private Diagnostic buildLspDiagnostic(vadl.error.Diagnostic vadlDiagnostic,
                                        DocumentSnapshot documentSnapshot) {
    // TODO Look into secondary locations too? Maybe as relatedInformation? Or to put a
    //      diagnostic message there as well?
    SourceLocation location = vadlDiagnostic.multiLocation.primaryLocation().location();

    Diagnostic lspDiagnostic = new Diagnostic();
    lspDiagnostic.setRange(documentSnapshot.calculateUtf16Range(location));
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
        .filter(m -> !m.type().equals(vadl.error.Diagnostic.MsgType.PLAIN)
            || !m.content().contains("parser got confused at this point"))
        .map(vadl.error.Diagnostic.Message::content)
        .collect(Collectors.joining("\n"));

    String fullMessage = vadlDiagnostic.reason
        + (!labelsString.isBlank() ? "\n" + labelsString : "")
        + (!messagesString.isBlank() ? "\n" + messagesString : "");
    lspDiagnostic.setMessage(fullMessage);

    return lspDiagnostic;
  }

  private boolean clientSupportsPublishDiagnostics() {
    var capabilities = documentStore.documentService.server.params()
        .getCapabilities().getTextDocument();
    return capabilities != null && capabilities.getPublishDiagnostics() != null;
  }
}
