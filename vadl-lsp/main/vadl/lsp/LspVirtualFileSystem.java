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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.utils.VirtualFileSystem;

/**
 * Virtual file system used by the language server. For all files currently open in the LSP client
 * the client's file content is provided instead of data from the underlying file system.
 *
 * <p>One instance is used per {@link Document.Snapshot} so that it can be recorded which other
 * (virtual) files are opened when parsing this document's current content.
 */
class LspVirtualFileSystem implements VirtualFileSystem {
  private final VadlTextDocumentService textService;
  private final Set<String> readFiles = new HashSet<>();

  /**
   * Creates a VirtualFileSystem backed by the documents data contained in the given
   * {@code textService} and falling back to {@link VadlTextDocumentService#underlyingFileSystem}.
   */
  LspVirtualFileSystem(VadlTextDocumentService textService) {
    this.textService = textService;
  }

  @Override
  public boolean exists(Path path) {
    if (getDocument(path) != null) {
      return true;
    }
    return textService.underlyingFileSystem.exists(path);
  }

  @Override
  public InputStream getInputStream(Path path) throws IOException {
    synchronized (this) {
      readFiles.add(path.toUri().toString());
    }

    var document = getDocument(path);
    if (document == null) {
      return textService.underlyingFileSystem.getInputStream(path);
    }

    return new ByteArrayInputStream(document.getCurrentText().getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public Path toAbsolutePath(Path path) {
    return textService.underlyingFileSystem.toAbsolutePath(path);
  }

  @Override
  public Path toRelativePath(Path path) {
    return textService.underlyingFileSystem.toRelativePath(path);
  }

  /**
   * The URIs of all files that have been read via this virtual file system.
   */
  public Set<String> getReadFiles() {
    return Set.copyOf(readFiles);
  }

  private @Nullable Document getDocument(Path path) {
    return textService.getDocument(path.toUri().toString());
  }
}
