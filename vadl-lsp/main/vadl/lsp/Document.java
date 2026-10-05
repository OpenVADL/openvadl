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
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.ast.Frontend;
import vadl.ast.Frontend.BestEffortCompilation;

/**
 * Represents one document that is currently open in the language server. This is a mutable object
 * that always provides the current state. Caches the latest compilation result of this document
 * (if it is still valid).
 *
 * @see DocumentSnapshot
 */
public class Document {
  private static final Logger log = LoggerFactory.getLogger(Document.class);

  private volatile DocumentSnapshot currentSnapshot;
  private final DocumentStore documentStore;

  @Nullable
  private Future<CompilationInputAndResult> compilationTask = null;

  /**
   * Creates a new Document.
   *
   * @param snapshot the initial file content
   * @param documentStore which this document is managed by
   */
  Document(DocumentSnapshot snapshot, DocumentStore documentStore) {
    this.currentSnapshot = snapshot;
    this.documentStore = documentStore;
  }

  public Path getPath() {
    return currentSnapshot.path;
  }

  /**
   * Returns this document's current snapshot. Thread-safe.
   */
  public DocumentSnapshot getCurrentSnapshot() {
    return currentSnapshot;
  }

  /**
   * Updates this document's snapshot. Thread-safe.
   *
   * @throws IllegalStateException if {@code newVersion} is older than the current snapshot's
   *                               version
   */
  public synchronized void changeSnapshot(
      int newVersion, List<TextDocumentContentChangeEvent> contentChanges) {
    currentSnapshot = currentSnapshot.withChanges(newVersion, contentChanges);
  }

  /**
   * Clears the current compilation. I.e. the next call to {@link #getCurrentCompilation()} will
   * trigger a re-compilation.
   *
   * <p>If a compilation is currently being produced, then that task and all tasks waiting for that
   * compilation are interrupted.
   */
  public synchronized void clearCompilation() {
    if (compilationTask != null) {
      log.debug(
          "{} compilation of {}", compilationTask.isDone() ? "Discard" : "Interrupt", getPath()
      );
      compilationTask.cancel(true);
      compilationTask = null;
    }
  }

  /**
   * Compilation result with full context.
   *
   * @param result The compiler's result
   * @param documentSnapshot Document state that was used in this compilation
   * @param fileSystemSnapshot Snapshot of all files at time of compilation
   * @param publishedDiagnostics True if diagnostics have already been published for this
   *                             compilation. Initially {@code false}.
   */
  public record CompilationInputAndResult(
      BestEffortCompilation result,
      DocumentSnapshot documentSnapshot,
      LspSnapshotFileSystem fileSystemSnapshot,
      AtomicBoolean publishedDiagnostics
  ) {}

  /**
   * Returns the current compilation of this document. This will either re-use an existing (still
   * valid) compilation or wait until a new compilation has been produced.
   *
   * <p>This method is thread-safe.
   *
   * @return All the relevant data associated with this compilation. This data MUST NOT be modified
   *         as it is shared with other operations.
   * @throws InterruptedException if producing the compilation was interrupted because it would no
   *                              longer be up-to-date (and thus the LSP operation calling this
   *                              should be considered outdated as well)
   */
  public CompilationInputAndResult getCurrentCompilation() throws InterruptedException {
    Future<CompilationInputAndResult> currentCompilationTask;
    synchronized (this) {
      currentCompilationTask = compilationTask == null ? startCompilation() : compilationTask;
    }

    try {
      return currentCompilationTask.get();
    } catch (CancellationException | ExecutionException e) {
      throw new InterruptedException();
    }
  }

  private synchronized Future<CompilationInputAndResult> startCompilation()
      throws InterruptedException {
    final var fileSystemSnapshot = documentStore.createSnapshotFileSystem();

    compilationTask = documentStore.documentService.server.executor.submit(() -> {
      // Reading document snapshot from VFS to be consistent with it
      var documentSnapshot = fileSystemSnapshot.getDocumentSnapshot(getPath());
      if (documentSnapshot == null) {
        // Document may simply not be open anymore
        throw new InterruptedException();
      }

      var compilerResult = Frontend.compileToAstBestEffort(documentSnapshot.path,
          fileSystemSnapshot);

      var result = new CompilationInputAndResult(compilerResult, documentSnapshot,
          fileSystemSnapshot, new AtomicBoolean(false));
      if (Thread.interrupted()) {
        throw new InterruptedException();
      }
      documentStore.updateDependencies(result);

      log.debug("Compiled {} (version {})", getPath(), documentSnapshot.version);
      return result;
    });

    return compilationTask;
  }
}
