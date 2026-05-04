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

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import vadl.javaannotations.viam.DataValue;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.Counter;
import vadl.viam.RegisterResource;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.CanAccessCounter;
import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.IsInstructionOperand;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.ReadsRegisterTensor;

/**
 * Represents a read access to a {@link RegisterTensor}.
 * The provided {@link #indices()} indicate which dimensions of the register tensor are read.
 *
 * <p>E.g., given {@code register A: Bits<4><32>}, we could read
 * {@code A(0)} which would read register 0 in "register file" A.
 * In this case the size of the provided indices list would be 1 (value 0).
 * It is also possible to read {@code A}, which would write
 * across all dimensions of the register tensor.
 * In this case, the indices list would be empty.
 * The result type can be computed with {@link RegisterTensor#resultType(int)}.
 */
public class ReadRegTensorNode extends ReadResourceNode implements ReadsRegisterTensor,
    CanAccessCounter, IsInstructionOperand {

  @DataValue
  protected RegisterTensor regTensor;

  // a register-read might read from a counter.
  // if this can be inferred, the counter is set.
  // however, not all counter-accesses are statically known, as if the register file
  // is known, but the concrete index isn't,
  // it could be a counter written, but doesn't have to be.
  // it is generally set during the `StaticCounterAccessResolvingPass`
  @DataValue
  @Nullable
  private Counter staticCounterAccess;

  /**
   * Construct the {@link ReadRegTensorNode}.
   *
   * @param regTensor           register to be read
   * @param indices             indices to accessed certain dimension
   * @param type                result type of the read-node (>= register tensor result type)
   * @param staticCounterAccess if this read access a {@link Counter} (program counter)
   */
  public ReadRegTensorNode(RegisterTensor regTensor, NodeList<ExpressionNode> indices,
                           DataType type, @Nullable Counter staticCounterAccess) {
    super(indices, type.toBitsType());
    this.regTensor = regTensor;
    this.staticCounterAccess = staticCounterAccess;
  }

  @Override
  public void setType(Type type) {
    // types may cause double read from the same register without benefit.
    // so we normalize the type to BitsType.
    super.setType(type.asDataType().toBitsType());
  }

  @Override
  public RegisterTensor resourceDefinition() {
    return regTensor;
  }

  public RegisterTensor regTensor() {
    return regTensor;
  }

  @Override
  public boolean isPcAccess() {
    return staticCounterAccess != null;
  }

  @Override
  @Nullable
  public Counter staticCounterAccess() {
    return staticCounterAccess;
  }

  @Override
  public void setStaticCounterAccess(@Nonnull Counter staticCounterAccess) {
    this.staticCounterAccess = staticCounterAccess;
  }

  /**
   * If this node has a constant address and the {@link RegisterTensor}
   * has a {@link vadl.viam.RegisterTensor.Constraint} for {@link #indices}.
   */
  public boolean hasConstraintForAddress() {
    if (hasConstantAddress()) {
      for (var index : indices) {
        var addressIndex = ((ConstantNode) index).constant;

        for (var constraint : regTensor.constraints()) {
          for (var constraintIndex : constraint.indices()) {
            if (!addressIndex.equals(constraintIndex)) {
              return false;
            }
          }

          return true;
        }
      }
    }

    return false;
  }


  @Override
  public void verifyState() {
    super.verifyState();
    ensure(indices().size() <= regTensor.maxNumberOfAccessIndices(),
        "Too many indices for tensor access. Read uses %d indices, tensor has %d indices",
        indices().size(), regTensor.maxNumberOfAccessIndices());
    ensure(type().bitWidth() >= regTensor.resultType(indices.size()).bitWidth(),
        "Read result width is smaller than register tensor result width.");
    regTensor.ensureMatchingIndexTypes(indices.stream().map(e -> e.type().asDataType()).toList());
  }

  @Override
  public ReadRegTensorNode copy() {
    return new ReadRegTensorNode(regTensor, indices.copy(), type(), staticCounterAccess);
  }

  @Override
  public ReadRegTensorNode shallowCopy() {
    return new ReadRegTensorNode(regTensor, indices, type(), staticCounterAccess);
  }

  @Override
  public <T extends GraphNodeVisitor> void accept(T visitor) {
    visitor.visit(this);
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(regTensor);
    collection.add(staticCounterAccess);
  }

  @Override
  public RegisterResource registerResource() {
    return registerTensor();
  }

  @Override
  public RegisterTensor registerTensor() {
    return regTensor;
  }

  @Override
  public boolean hasRegisterFile() {
    return regTensor.isRegisterFile();
  }

  @Override
  public boolean canBeInstructionOperand() {
    return hasRegisterFile() && !indices.isEmpty() && !hasConstantAddress();
  }
}
