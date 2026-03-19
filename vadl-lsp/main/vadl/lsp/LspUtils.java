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

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

class LspUtils {

  static Path toPath(String uri) {
    return Paths.get(URI.create(uri));
  }

  static String toUri(Path path) {
    return path.toUri().toString();
  }

  /**
   * Returns a string representation of one file's path relative to the directory another file is
   * contained in.
   *
   * @param path The file for which to return a relative path
   * @param relativeTo Path to another file
   *
   * @return {@code path} relative to the directory of {@code relativeTo}.
   */
  static String relativePath(Path path, Path relativeTo) {
    relativeTo = relativeTo.getParent() != null ? relativeTo.getParent() : relativeTo;
    return relativeTo.relativize(path).toString();
  }
}
