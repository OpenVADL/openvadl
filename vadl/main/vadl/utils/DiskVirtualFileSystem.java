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

package vadl.utils;

import static vadl.error.Diagnostic.error;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * An abstraction layer to inject behavior into file loading.
 * Some tests and the LSP might want to hide or inject files into the parser. For example the
 * LSP has to work on files that are open in the editor but not yet saved, so it can overwrite
 * this implementation.
 *
 * <p>This implementation passes everything through to the real file system.
 */
public class DiskVirtualFileSystem implements VirtualFileSystem {
  @Override
  public boolean exists(Path path) {
    var file = new File(path.toUri());
    return file.exists();
  }

  @Override
  public InputStream getInputStream(Path path) {
    var file = new File(path.toUri());
    try {
      return new FileInputStream(file);
    } catch (IOException e) {
      throw error("File not found: " + path, SourceLocation.INVALID_SOURCE_LOCATION).build();
    }
  }

  @Override
  public Path toAbsolutePath(Path path) {
    var file = new File(path.toUri());
    return file.toPath();
  }

  @Override
  public Path toRelativePath(Path path) {
    var currentWorkingDir = Paths.get(System.getProperty("user.dir"));
    return currentWorkingDir.relativize(path);
  }
}
