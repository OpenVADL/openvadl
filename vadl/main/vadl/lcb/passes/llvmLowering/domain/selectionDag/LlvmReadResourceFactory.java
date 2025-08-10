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

package vadl.lcb.passes.llvmLowering.domain.selectionDag;

import javax.annotation.Nullable;
import vadl.error.Diagnostic;
import vadl.types.DataType;
import vadl.viam.ArtificialResource;
import vadl.viam.Counter;
import vadl.viam.GeneratesRegisterFileName;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldRefNode;

/**
 * Factory for creating reading instances.
 */
public class LlvmReadResourceFactory {
  /**
   * Based on the {@code registerFile} creates a node.
   */
  public ExpressionNode create(GeneratesRegisterFileName registerFile,
                               FieldRefNode address,
                               DataType type,
                               @Nullable Counter counter) {
    if (registerFile instanceof RegisterTensor registerTensor) {
      return new LlvmReadRegFileNode(registerTensor, address, type, counter);
    } else if (registerFile instanceof ArtificialResource artificialResource) {
      return new LlvmReadArtificialResourceNode(artificialResource, address, type);
    } else {
      throw Diagnostic.error("Cannot create a llvm type", registerFile.location()).build();
    }
  }
}
