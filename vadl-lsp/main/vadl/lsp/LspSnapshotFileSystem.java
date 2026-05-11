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

import static vadl.lsp.LspUtils.toPath;
import static vadl.lsp.LspUtils.toUri;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import vadl.error.Diagnostic;
import vadl.utils.DiskVirtualFileSystem;
import vadl.utils.VirtualFileSystem;

/**
 * Virtual file system used by the language server. This contains one snapshot containing several
 * {@link Document}s; the contents of those documents (which are snapshots themselves, i.e. frozen
 * in time) is made available instead of reading from the underlying file system. All other files
 * are read from the underlying file system directly.
 *
 * <p>Note: As the contained documents are immutable, a new instance of this VFS must be created
 * when a new file state shall be parsed.
 *
 * <p>Note: If {@link #getReadFiles()} shall be used, you can reset this data by creating a new
 * copy of this VFs (via the copy constructor).
 */
class LspSnapshotFileSystem implements VirtualFileSystem {
  private final Map<String, Document> documents;
  private final VirtualFileSystem underlyingFileSystem;
  private final Set<String> readFiles = new HashSet<>();

  /**
   * Creates a VirtualFileSystem backed with the given documents.
   *
   * @param documents Maps URI string to corresponding document. This map is copied.
   * @param underlyingFileSystem is used for all files for which this instance has no corresponding
   *                             document.
   */
  LspSnapshotFileSystem(Map<String, Document> documents, VirtualFileSystem underlyingFileSystem) {
    this.documents = Map.copyOf(documents);
    this.underlyingFileSystem = underlyingFileSystem;
  }

  /**
   * Creates a VirtualFileSystem backed with the given documents.
   *
   * @param documents Maps URI string to corresponding document. This map is copied.
   */
  LspSnapshotFileSystem(Map<String, Document> documents) {
    this(documents, new DiskVirtualFileSystem());
  }

  /**
   * Copy constructor.
   */
  LspSnapshotFileSystem(LspSnapshotFileSystem snapshots) {
    this.documents = snapshots.documents;
    this.underlyingFileSystem = snapshots.underlyingFileSystem;
  }

  @Override
  public boolean exists(Path path) {
    if (documents.containsKey(toUri(path))) {
      return true;
    }
    return underlyingFileSystem.exists(path);
  }

  @Override
  public InputStream getInputStream(Path path) {
    String uri = toUri(path);
    readFiles.add(uri);

    var document = documents.get(uri);
    if (document == null) {
      return underlyingFileSystem.getInputStream(path);
    }

    return new ByteArrayInputStream(document.getText().getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public Stream<String> readLines(Path path) {
    String uri = toUri(path);
    readFiles.add(uri);

    var document = documents.get(uri);
    if (document == null) {
      return underlyingFileSystem.readLines(path);
    }

    return document.textLines.stream();
  }

  @Override
  public Path toAbsolutePath(Path path) {
    return underlyingFileSystem.toAbsolutePath(path);
  }

  @Override
  public Path toRelativePath(Path path) {
    return underlyingFileSystem.toRelativePath(path);
  }

  public @Nullable Document getDocument(String uri) {
    return documents.get(uri);
  }

  /**
   * Attempts to always return a Document, even if it has to be read from the underlying filesystem.
   */
  public @Nullable Document getFileBasedDocument(String uri) {
    var result = getDocument(uri);
    if (result != null) {
      return result;
    }

    List<String> textLines;
    try {
      textLines = underlyingFileSystem.readLines(toPath(uri)).toList();
    } catch (Diagnostic e) {
      return null;
    }
    return new Document(uri, -1, textLines);
  }

  /**
   * The URIs of all files that have been read via this virtual file system so far.
   *
   * <p>Note: This is not affected by non-VFS methods like {@link #getDocument(String)} and
   * {@link #getFileBasedDocument(String)}.
   *
   * <p>In order to "reset" this data, you can create a copy of this VFS instance by using the
   * copy constructor.
   *
   * @return unmodifiable Set of what has been read so far.
   */
  public Set<String> getReadFiles() {
    return Set.copyOf(readFiles);
  }
}
