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

package vadl.viam.graph;

import vadl.viam.passes.canonicalization.Canonicalizer;

/**
 * Marks nodes that provide a canonical form via the {@link Canonicalizable#canonical()} method.
 */
public interface Canonicalizable {

  /**
   * Returns the canonical form of itself.
   *
   * <p>The method implementation...
   * <ul>
   * <li>must have no side effects (e.g. mutation of inputs, data and successors)</li>
   * <li>must not modify the graph in any way (add or delete nodes)</li>
   * <li>must not call {@code canonical()} on any other nodes</li>
   * <li>may return a new uninitialized node that must be added to the graph by the
   * {@link Canonicalizer}</li>
   * <li>may return {@code this} or already existing nodes (active in same graph)</li>
   * <li>should probably call {@code super.canonical()} and check if it is not this </li>
   * </ul>
   *
   * <p>The canonical form includes constant evaluation, constant folding, operand reordering,...
   */
  Node canonical();

}
