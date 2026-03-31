// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
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

package vadl.dump;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A global store to record which artifacts and dumps were generated to later inform the user about.
 *
 * <p>Often the user requests certain dumps. However, in certain error conditions, some dumps might
 * not be created (for example, if the typechecker fails, all passes further down the road cannot
 * that depend on types cannot be executed and cannot produce any dumps).
 */
public class ArtifactTracker {
  private static final List<Path> artifactPaths = new ArrayList<>();
  private static final List<Path> dumpPaths = new ArrayList<>();

  private ArtifactTracker() {
  }

  /**
   * Add a path of a dump to be recorded.
   * The path should be relative to the working directory.
   *
   * @param path to be stored.
   */
  public static void addDump(Path path) {
    dumpPaths.add(path);
  }

  public static List<Path> getDumpPaths() {
    return dumpPaths;
  }

  /**
   * Add a path of a artifact to be recorded.
   * The path should be relative to the working directory.
   *
   * @param path to be stored.
   */
  public static void addArtifact(Path path) {
    artifactPaths.add(path);
  }

  public static List<Path> getArtifactPathsPaths() {
    return artifactPaths;
  }
}
