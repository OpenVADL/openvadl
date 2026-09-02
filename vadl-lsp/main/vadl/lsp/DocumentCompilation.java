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

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.ast.Frontend;
import vadl.ast.Frontend.BestEffortCompilation;

/**
 * Contains the compilation state of one Document that is currently open in the language server.
 * This is used to re-use a compilation result that is still valid.
 *
 * @see Document
 */
class DocumentCompilation {
  private static final Logger log = LoggerFactory.getLogger(DocumentCompilation.class);

  final String uri;
  private final VadlTextDocumentService documentService;

  @Nullable
  private Future<CompilationInputAndResult> compilationTask = null;

  DocumentCompilation(String uri, VadlTextDocumentService documentService) {
    this.uri = uri;
    this.documentService = documentService;
  }

  /**
   * Clears the current compilation. I.e. the next call to {@link #getCurrentCompilation()} will
   * trigger a re-compilation.
   *
   * <p>If a compilation is currently being produced, then that task and all tasks waiting for that
   * compilation are interrupted.
   */
  synchronized void clearCompilation() {
    if (compilationTask != null) {
      log.debug("{} compilation of {}", compilationTask.isDone() ? "Discard" : "Interrupt", uri);
      compilationTask.cancel(true);
      compilationTask = null;
    }
  }

  /**
   * Compilation result with full context.
   *
   * @param result The compiler's result
   * @param document Document state that was used in this compilation
   * @param fileSystemSnapshot Snapshot of all files at time of compilation
   * @param publishedDiagnostics True if diagnostics have already been published for this
   *                             compilation. Initially {@code false}.
   */
  record CompilationInputAndResult(
      BestEffortCompilation result,
      Document document,
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
  CompilationInputAndResult getCurrentCompilation() throws InterruptedException {
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
    final var fileSystemSnapshot = documentService.createSnapshotFileSystem();
    final var document = fileSystemSnapshot.getDocument(uri);
    if (document == null) {
      // Document may simply not be open anymore
      throw new InterruptedException();
    }

    compilationTask = documentService.server.executor.submit(() -> {
      var compilerResult = Frontend.compileToAstBestEffort(document.getPath(),
          fileSystemSnapshot);

      var result = new CompilationInputAndResult(compilerResult, document,
          fileSystemSnapshot, new AtomicBoolean(false));
      if (Thread.interrupted()) {
        throw new InterruptedException();
      }
      documentService.updateDependencies(result);

      log.debug("Compiled {} (version {})", uri, document.version);
      return result;
    });

    return compilationTask;
  }
}
