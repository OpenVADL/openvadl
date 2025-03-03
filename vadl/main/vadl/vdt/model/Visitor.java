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

package vadl.vdt.model;

import javax.annotation.Nullable;

/**
 * A visitor for nodes in the decode tree.
 *
 * @param <T> the type of the result of visiting a node (can be {@code Void})
 */
public interface Visitor<T> {

  default @Nullable T visit(InnerNode node) {
    // Do nothing by default
    return null;
  }

  default @Nullable T visit(LeafNode node) {
    // Do nothing by default
    return null;
  }

}
