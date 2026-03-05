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

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A virtual file system containing of a single file.
 * This is especially useful for testing, where we want to run the compiler on a single file.
 */
public class SingleFileVirtualFileSystem implements VirtualFileSystem {
  public static final Path PATH = Paths.get("spec.vadl");
  private final String content;

  public SingleFileVirtualFileSystem(String content) {
    this.content = content;
  }

  @Override
  public boolean exists(Path path) {
    return path.equals(PATH);
  }

  @Override
  public InputStream getInputStream(Path path) throws IOException {
    if (!path.equals(PATH)) {
      throw new FileNotFoundException("File not found: " + path);
    }
    return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public Path toAbsolutePath(Path path) {
    return path;
  }

  @Override
  public Path toRelativePath(Path path) {
    return path;
  }
}
