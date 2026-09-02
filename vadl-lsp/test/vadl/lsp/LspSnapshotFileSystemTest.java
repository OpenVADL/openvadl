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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import vadl.error.Diagnostic;
import vadl.utils.SingleFileVirtualFileSystem;

/**
 * Tests for LspSnapshotFileSystem.
 */
public class LspSnapshotFileSystemTest {

  @Test
  public void emptyVfsDelegates() {
    var existingPath = SingleFileVirtualFileSystem.DEFAULT_PATH.toAbsolutePath();
    var invalidPath = DocumentTest.TEST_PATH;
    var underlyingFs = new SingleFileVirtualFileSystem(DocumentTest.TEST_TEXT, existingPath);

    LspSnapshotFileSystem vfs = new LspSnapshotFileSystem(Map.of(), underlyingFs);

    assertThat(vfs.getReadFiles()).isEmpty();

    // exists()
    assertThat(vfs.exists(existingPath))
        .isTrue().isEqualTo(underlyingFs.exists(existingPath));
    assertThat(vfs.exists(invalidPath))
        .isFalse().isEqualTo(underlyingFs.exists(invalidPath));

    // getInputStream()
    assertThat(inputStreamToString(vfs.getInputStream(existingPath)))
        .isEqualTo(inputStreamToString(underlyingFs.getInputStream(existingPath)));
    assertThrows(Diagnostic.class, () -> vfs.getInputStream(invalidPath));

    // readLines()
    assertThat(vfs.readLines(existingPath).collect(Collectors.joining("\n")))
        .isEqualTo(underlyingFs.readLines(existingPath).collect(Collectors.joining("\n")));
    assertThrows(Diagnostic.class, () -> vfs.readLines(invalidPath));

    // toAbsolutePath()
    assertThat(vfs.toAbsolutePath(existingPath))
        .isEqualTo(underlyingFs.toAbsolutePath(existingPath));
    assertThat(vfs.toAbsolutePath(invalidPath))
        .isEqualTo(underlyingFs.toAbsolutePath(invalidPath));

    // toRelativePath()
    assertThat(vfs.toRelativePath(existingPath))
        .isEqualTo(underlyingFs.toRelativePath(existingPath));
    assertThat(vfs.toRelativePath(invalidPath))
        .isEqualTo(underlyingFs.toRelativePath(invalidPath));

    // getDocument()
    assertThat(vfs.getDocument(existingPath)).isNull();
    assertThat(vfs.getDocument(DocumentTest.TEST_PATH)).isNull();

    // getFileBasedDocument()
    assertThat(vfs.getFileBasedDocument(existingPath))
        .extracting("path", "text").containsExactly(existingPath, DocumentTest.TEST_TEXT);
    assertThat(vfs.getFileBasedDocument(DocumentTest.TEST_PATH)).isNull();

    assertThat(vfs.getReadFiles()).hasSize(2).contains(existingPath, DocumentTest.TEST_PATH);
  }

  @Test
  public void vfsWithDocuments() {
    var existingPath = DocumentTest.TEST_PATH;
    var overridenDocument = new Document(DocumentTest.TEST_PATH, 3, DocumentTest.TEST_TEXT);

    var newPath = DocumentTest.TEST_PATH2;
    var newDocument = new Document(DocumentTest.TEST_PATH2, 1, DocumentTest.TEST_TEXT2);

    var invalidPath = SingleFileVirtualFileSystem.DEFAULT_PATH.toAbsolutePath();
    var underlyingFs = new SingleFileVirtualFileSystem(DocumentTest.TEST_UNICODE_TEXT, existingPath);

    LspSnapshotFileSystem vfs = new LspSnapshotFileSystem(
        Map.of(overridenDocument.path, overridenDocument, newDocument.path, newDocument),
        underlyingFs);

    assertThat(vfs.getReadFiles()).isEmpty();

    // exists()
    assertThat(vfs.exists(existingPath)).isTrue();
    assertThat(vfs.exists(newPath)).isTrue();
    assertThat(vfs.exists(invalidPath)).isFalse();

    // getInputStream()
    assertThat(inputStreamToString(vfs.getInputStream(existingPath)))
        .isEqualTo(DocumentTest.TEST_TEXT)
        .isNotEqualTo(inputStreamToString(underlyingFs.getInputStream(existingPath)));
    assertThat(inputStreamToString(vfs.getInputStream(newPath)))
        .isEqualTo(DocumentTest.TEST_TEXT2);
    assertThrows(Diagnostic.class, () -> vfs.getInputStream(invalidPath));

    // readLines()
    assertThat(vfs.readLines(existingPath).collect(Collectors.joining("\n")))
        .isEqualTo(DocumentTest.TEST_TEXT);
    assertThat(vfs.readLines(newPath).collect(Collectors.joining("\n")))
        .isEqualTo(DocumentTest.TEST_TEXT2);
    assertThrows(Diagnostic.class, () -> vfs.readLines(invalidPath));

    // toAbsolutePath()
    assertThat(vfs.toAbsolutePath(existingPath))
        .isEqualTo(underlyingFs.toAbsolutePath(existingPath));
    assertThat(vfs.toAbsolutePath(newPath))
        .isEqualTo(underlyingFs.toAbsolutePath(newPath));
    assertThat(vfs.toAbsolutePath(invalidPath))
        .isEqualTo(underlyingFs.toAbsolutePath(invalidPath));

    // toRelativePath()
    assertThat(vfs.toRelativePath(existingPath))
        .isEqualTo(underlyingFs.toRelativePath(existingPath));
    assertThat(vfs.toRelativePath(newPath))
        .isEqualTo(underlyingFs.toRelativePath(newPath));
    assertThat(vfs.toRelativePath(invalidPath))
        .isEqualTo(underlyingFs.toRelativePath(invalidPath));

    // getDocument()
    assertThat(vfs.getDocument(existingPath)).isEqualTo(overridenDocument);
    assertThat(vfs.getDocument(newPath)).isEqualTo(newDocument);
    assertThat(vfs.getDocument(invalidPath)).isNull();

    // getFileBasedDocument()
    assertThat(vfs.getFileBasedDocument(existingPath))
        .isEqualTo(overridenDocument);
    assertThat(vfs.getFileBasedDocument(newPath))
        .isEqualTo(newDocument);
    assertThat(vfs.getFileBasedDocument(invalidPath)).isNull();

    assertThat(vfs.getReadFiles()).hasSize(3)
        .contains(existingPath, newPath, invalidPath);
  }


  private String inputStreamToString(InputStream inputStream) {
    return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
        .lines().collect(Collectors.joining("\n"));
  }
}
