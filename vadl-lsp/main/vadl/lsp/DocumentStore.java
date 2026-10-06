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

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentItem;
import vadl.utils.DiskVirtualFileSystem;

/**
 * Contains and manages currently open documents. Tracks document dependencies and invalidates
 * compilation results as required. Triggers diagnostics publishing as required.
 *
 * <p>All methods are thread-safe.
 */
public class DocumentStore {
  final VadlTextDocumentService documentService;

  private final Map<Path, Document> openDocuments = new ConcurrentHashMap<>();
  private final DependencyMap<Path> documentDependencies = new DependencyMap<>();

  DocumentStore(VadlTextDocumentService documentService) {
    this.documentService = documentService;
  }

  /**
   * Opens a document.
   *
   * @param lspDocument as provided by the LSP client
   */
  public void open(TextDocumentItem lspDocument) {
    Document document = new Document(new DocumentSnapshot(lspDocument), this);
    openDocuments.put(document.getPath(), document);
    clearDependentCompilations(document);

    documentService.publishDiagnostics(document);
    publishDiagnosticsForDependentDocuments(document);
  }

  /**
   * Closes a document.
   *
   * @param path refers to the document to close
   */
  public void close(Path path) {
    Document document = openDocuments.remove(path);
    if (document == null) {
      return;
    }
    clearDependentCompilations(document);
    document.clearCompilation();

    publishDiagnosticsForDependentDocuments(document);
    documentDependencies.setDependencies(document.getPath(), Set.of());
  }

  /**
   * Updates an open document's content.
   *
   * @param path refers to the document
   * @throws IllegalStateException if {@code newVersion} is older than the current document
   *                               snapshot's version
   */
  public void change(Path path,
      int newVersion, List<TextDocumentContentChangeEvent> contentChanges) {

    Document document = openDocuments.get(path);
    if (document == null) {
      return;
    }
    document.changeSnapshot(newVersion, contentChanges);

    document.clearCompilation();
    clearDependentCompilations(document);

    documentService.publishDiagnostics(document);
    publishDiagnosticsForDependentDocuments(document);
  }

  /**
   * Returns the current compilation of an open document. This will either re-use an existing (still
   * valid) compilation or wait until a new compilation has been produced.
   *
   * @param path refers to the document
   * @return All the relevant data associated with this compilation. This data MUST NOT be modified
   *         as it is shared with other operations.<br>
   *         Null: if desired document is currently not open.
   * @throws InterruptedException if producing the compilation was interrupted because it would no
   *                              longer be up-to-date (and thus the LSP operation calling this
   *                              should be considered outdated as well)
   */
  public @Nullable Document.CompilationInputAndResult getCurrentCompilation(Path path)
      throws InterruptedException {
    var document = openDocuments.get(path);
    if (document == null) {
      return null;
    }
    return document.getCurrentCompilation();
  }

  /**
   * Returns true if given snapshot is the current version of its associated document.
   */
  public boolean documentVersionIsCurrent(DocumentSnapshot documentSnapshot) {
    Document document = openDocuments.get(documentSnapshot.path);
    if (document == null) {
      return false;
    }
    return documentSnapshot.version == document.getCurrentSnapshot().version;
  }


  /**
   * Creates a new files snapshot as used in a compiler run.
   */
  LspSnapshotFileSystem createSnapshotFileSystem() {
    return new LspSnapshotFileSystem(openDocuments.values(), new DiskVirtualFileSystem());
  }

  /**
   * Updates file dependency data based on the given compilation result.
   *
   * <p>Should be called by the Document when a compilation has finished.
   */
  synchronized void updateDependencies(Document.CompilationInputAndResult compilation) {
    if (!documentVersionIsCurrent(compilation.documentSnapshot())) {
      return;
    }
    documentDependencies.setDependencies(compilation.documentSnapshot().path,
        compilation.fileSystemSnapshot().getReadFiles());
  }


  private void clearDependentCompilations(Document document) {
    for (Path path : documentDependencies.getDependents(document.getPath())) {
      var d = openDocuments.get(path);
      if (d != null) {
        d.clearCompilation();
      }
    }
  }

  /**
   * Publishes new diagnostics for all dependent documents of the given document.
   */
  private void publishDiagnosticsForDependentDocuments(Document document) {
    for (Path path : documentDependencies.getDependents(document.getPath())) {
      var d = openDocuments.get(path);
      if (d != null) {
        documentService.publishDiagnostics(d);
      }
    }
  }
}
