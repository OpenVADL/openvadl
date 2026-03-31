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

package vadl.viam.graph.dependency;

import java.util.ArrayList;
import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.types.DataType;
import vadl.viam.ArtificialResource;
import vadl.viam.RegisterResource;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.IsInstructionOperand;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.ReadsRegisterTensor;

/**
 * A read of an {@link ArtificialResource}.
 */
public class ReadArtificialResNode extends ReadResourceNode implements ReadsRegisterTensor,
    IsInstructionOperand {

  @DataValue
  private final ArtificialResource resource;

  public ReadArtificialResNode(ArtificialResource artificialResource,
                               NodeList<ExpressionNode> indices,
                               DataType type) {
    super(indices, type);
    this.resource = artificialResource;
  }

  @Override
  public ArtificialResource resourceDefinition() {
    return resource;
  }

  @Override
  public ExpressionNode copy() {
    return new ReadArtificialResNode(resource, indices.copy(), type());
  }

  @Override
  public Node shallowCopy() {
    return new ReadArtificialResNode(resource, indices, type());
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(resource);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    visitor.visit(this);
  }

  @Override
  public RegisterResource registerResource() {
    return resource;
  }

  @Override
  public RegisterTensor registerTensor() {
    return (RegisterTensor) resource.innerResourceRef();
  }

  @Override
  public boolean hasRegisterFile() {
    // If parameter's length is 1 then it is a register file.
    // If parameter's length is 0 then it is a register.
    return resource.isRegisterFile() && resource.readFunction().parameters().length == 1;
  }

  /**
   * Theoretically, it is possible to have aliases to aliases. This method returns the
   * underlying {@link RegisterTensor}.
   */
  public RegisterTensor getBaseTensor() {
    ArtificialResource resource = resourceDefinition();

    while (!(resource.innerResourceRef() instanceof RegisterTensor)) {
      resource = (ArtificialResource) resource.innerResourceRef();
    }

    return (RegisterTensor) resource.innerResourceRef();
  }

  /**
   * Get all the constraints recursively.
   */
  public List<RegisterResource.Constraint> getAllConstraintsRecursively() {
    ArtificialResource resource = resourceDefinition();
    ArrayList<RegisterResource.Constraint> constraints = new ArrayList<>(resource.constraints());

    while (!(resource.innerResourceRef() instanceof RegisterTensor)) {
      resource = (ArtificialResource) resource.innerResourceRef();
      constraints.addAll(resource.constraints());
    }

    return constraints;
  }

  @Override
  public boolean canBeInstructionOperand() {
    var registerTensor = getBaseTensor();
    var constraints = getAllConstraintsRecursively();

    // We have three cases:
    // (1): It's a register file and has no constant address -> ok
    // (2): It's a register file and has a constant address with constraint -> ok
    // (3): else -> not ok

    // Case (2)
    if (registerTensor.isRegisterFile() && hasAddress() && address().isConstant()) {
      var cnst = ((ConstantNode) address()).constant.asVal().intValue();
      // Case (1)
      if (hasConstantAddress() && constraints.stream()
          .flatMap(x -> x.indices().stream()).anyMatch(addr -> addr.intValue() == cnst)) {
        return true;
      } else {
        return registerTensor.isRegisterFile() && hasAddress() && !address().isConstant();
      }
    } else {
      return registerTensor.isRegisterFile() && hasAddress();
    }
  }
}
