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


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * An abstraction layer over the file system, which allows the caller to "lie" to the frontend
 * about the state of the filesystem.
 * This is especially important for the LSP who wants to run the compiler on files that might not
 * yet exist on disk but only in the editor of the user.
 */
public interface VirtualFileSystem {

  /**
   * Checks whether a file exists.
   * The provided path might point to a directory in which it still would return null.
   *
   * @param path  to check for.
   * @return      true if the file exists, false otherwise.
   */
  boolean exists(Path path);

  /**
   * Open a stream to read from the provided file.
   *
   * @param path          of the file to be read.
   * @return              an input stream to read from the file.
   * @throws IOException  if the file cannot be opened.
   */
  InputStream getInputStream(Path path);


  /**
   * Convert a potential relative path to an absolute one.
   *
   * @param path  to be converted.
   * @return      the absolute version.
   */
  Path toAbsolutePath(Path path);

  /**
   * Convert a potential absolut path to a relative one (relative to the current working directory).
   *
   * @param path  to be converted.
   * @return      the relative version.
   */
  Path toRelativePath(Path path);

  /**
   * Read the file from the provided path line by line.
   *
   * @param path          to read from.
   * @return              a stream of the lines from the file.
   * @throws IOException  if the file cannot be read.
   */
  default Stream<String> readLines(Path path) throws IOException {
    return new BufferedReader(new InputStreamReader(getInputStream(path), StandardCharsets.UTF_8))
        .lines();
  }
}
