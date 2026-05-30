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

package vadl.iss.passes.common.planning.analysis;

import vadl.iss.passes.nodes.IssWriteRegNode;
import vadl.viam.graph.control.ForallEndNode;
import vadl.viam.graph.control.ForallNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.ForIdxNode;

/**
 * One vector-shaped region discovered inside an instruction behavior graph.
 */
public record VectorRegion(
    String regionId,
    ForallNode forall,
    ForallEndNode forallEnd,
    IssWriteRegNode write,
    ExpressionNode valueExpression,
    ForIdxNode idx
) {
}
