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

package vadl.iss.passes.opDecomposition.decomposer;

import vadl.viam.graph.dependency.ExpressionNode;

/**
 * The super interface of all decomposer mix-ins like the {@link ShiftDecomposer}.
 * It provides required functions that the actual decomposer implements.
 */
public interface IDecomposer {

  int targetSize();

  ExpressionNode request(ExpressionNode node, int hi, int lo);

}
