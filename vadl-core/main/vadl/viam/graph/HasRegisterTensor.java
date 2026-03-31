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

import vadl.viam.ArtificialResource;
import vadl.viam.RegisterResource;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.dependency.ExpressionNode;

/**
 * Interface to indicate that the implementing class has register file.
 */
public interface HasRegisterTensor {
  /**
   * Get the {@link RegisterTensor} or the {@link ArtificialResource}.
   */
  RegisterResource registerResource();

  /**
   * Get the {@link RegisterTensor}, also when it is an {@link ArtificialResource}.
   */
  RegisterTensor registerTensor();

  /**
   * Get the indices for the tensor instruction.
   */
  NodeList<ExpressionNode> indices();

  /**
   * Checks whether the node has a register file.
   */
  boolean hasRegisterFile();
}
