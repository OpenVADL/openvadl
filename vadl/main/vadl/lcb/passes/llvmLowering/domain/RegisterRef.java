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

package vadl.lcb.passes.llvmLowering.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.types.ConcreteRelationType;
import vadl.types.DataType;
import vadl.viam.Constant;
import vadl.viam.DefinitionVisitor;
import vadl.viam.Format;
import vadl.viam.Register;
import vadl.viam.RegisterFile;
import vadl.viam.RegisterTensor;
import vadl.viam.Resource;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteResourceNode;

/**
 * A {@link RegisterRef} can be a {@link Register} which comes from {@link ReadResourceNode} or
 * {@link WriteResourceNode}. But it can also come from {@link ReadRegFileNode} or
 * {@link WriteRegFileNode} when the address is constant. Since, we have no way to reduce a
 * {@link RegisterFile} to a {@link Register}, we use {@link RegisterRef} as joined type for
 * both "worlds".
 */
public class RegisterRef extends Resource {
  private final DataType resultType;
  private final ConcreteRelationType relationType;

  @Nullable
  private Format refFormat;
  @Nullable
  private Constant address;
  private final List<RegisterTensor.Constraint> constraints;

  /**
   * Constructor.
   */
  public RegisterRef(Register register) {
    super(register.identifier);
    this.resultType = register.resultType();
    this.relationType = register.relationType();
    this.address = null;
    // When constructed over register then they are no constraints.
    this.constraints = Collections.emptyList();
  }

  /**
   * Constructor.
   */
  public RegisterRef(RegisterFile registerFile, Constant address) {
    super(registerFile.identifier);
    this.resultType = registerFile.resultType();
    this.relationType = registerFile.relationType();
    this.refFormat = null;
    this.address = address;
    // Get all the constraints for this register.
    // But types might not match, so just compare the values.
    this.constraints =
        Arrays.stream(registerFile.constraints())
            .filter(x -> x.indices().getFirst().intValue() == address.asVal().intValue()).toList();
  }

  @Override
  public boolean hasAddress() {
    return address != null;
  }

  @Nullable
  @Override
  public DataType addressType() {
    if (hasAddress()) {
      ensure(address != null, "Address must no be null");
      return (DataType) address.type();
    }

    return null;
  }

  @Override
  public List<DataType> indexTypes() {
    var addrType = addressType();
    if (addrType == null) {
      return List.of();
    }
    return List.of(addrType);
  }

  @Override
  public DataType resultType() {
    return resultType;
  }

  @Override
  public DataType resultType(int providedDimensions) {
    return resultType();
  }

  @Override
  public ConcreteRelationType relationType() {
    return relationType;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {

  }

  public List<RegisterTensor.Constraint> constraints() {
    return constraints;
  }

  /**
   * Get the name of the register or the name of register file with index.
   */
  public String lowerName() {
    if (hasAddress()) {
      ensure(address != null, "address must not be null");
      return simpleName() + address.asVal().decimal();
    } else {
      return simpleName();
    }
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.address)
        & Objects.hashCode(this.relationType)
        & Objects.hashCode(this.constraints)
        & Objects.hashCode(this.refFormat);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof RegisterRef registerRef) {
      return Objects.equals(this.address, registerRef.address)
          && Objects.equals(this.relationType, registerRef.relationType)
          && Objects.equals(this.constraints, registerRef.constraints)
          && Objects.equals(this.refFormat, registerRef.refFormat);
    }

    return false;
  }
}
