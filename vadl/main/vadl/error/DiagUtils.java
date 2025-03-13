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

package vadl.error;

import vadl.utils.SourceLocation;
import vadl.viam.graph.Node;

/**
 * A utility collection for {@link Diagnostic}.
 */
public class DiagUtils {

  /**
   * Throws a {@code <something> is not allowed here} diagnostic exception.
   * The source location will be the node, if it is not an invalid source location.
   * Otherwise, it will be the node's parent definition's source location.
   *
   * @param node The node that caused the diagnostic error.
   * @param what The plural phrase that prefixes {@code are not allowed here}.
   */
  public static void throwNotAllowed(Node node, String what) {
    SourceLocation loc = !node.sourceLocation().equals(SourceLocation.INVALID_SOURCE_LOCATION)
        ? node.sourceLocation()
        : node.ensureGraph().parentDefinition().sourceLocation();
    throw Diagnostic.error(what + " are not allowed here", loc)
        .note("THIS IS AN INTERNAL ERROR")
        .build();
  }

}
