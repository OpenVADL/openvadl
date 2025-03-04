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

package vadl.viam;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import vadl.types.ConcreteRelationType;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.utils.Pair;

/**
 * The register file is related to the {@link Register} but takes an address/index when accessing
 * it. It may also have constraints that restricts the possible values for statically defined
 * addresses.
 */
public class RegisterFile extends Resource {

  private final DataType addressType;
  private final DataType resultType;
  private final Constraint[] constraints;

  /**
   * Constructs a new RegisterFile object.
   *
   * @param identifier  The identifier of the RegisterFile.
   * @param addressType The data type of the file address/index.
   * @param resultType  The data type of the result value.
   */
  public RegisterFile(Identifier identifier, DataType addressType, DataType resultType,
                      Constraint[] constraints) {
    super(identifier);
    this.addressType = addressType;
    this.resultType = resultType;
    this.constraints = constraints;
  }


  @Override
  public boolean hasAddress() {
    return true;
  }

  @Override
  @Nonnull
  public DataType addressType() {
    return addressType;
  }

  @Override
  public DataType resultType() {
    return resultType;
  }

  public int numberOfRegisters() {
    return (int) Math.pow(2, addressType.bitWidth());
  }

  @Override
  public ConcreteRelationType relationType() {
    return Type.concreteRelation(addressType, resultType);
  }

  public Constraint[] constraints() {
    return constraints;
  }

  /**
   * Get a stream over all the constant registers which are defined in {@link #constraints}.
   */
  public Stream<Pair<Constant.Value, Constant.Value>> constantRegisters() {
    return Arrays.stream(constraints)
        .map(constraint -> Pair.of(constraint.address, constraint.value));
  }

  /**
   * Return the address of a zero register if it exists.
   */
  public Optional<Constant.Value> zeroRegister() {
    return constantRegisters()
        .filter(constantRegister -> constantRegister.right().intValue() == 0)
        .map(Pair::left)
        .findFirst();
  }

  /**
   * Generate the name from this register file with an {@code index}.
   */
  public String generateName(int index) {
    return identifier.simpleName() + index;
  }


  /**
   * Generate the name from this register file with an {@code index}.
   */
  public String generateName(Constant.Value index) {
    return identifier.simpleName() + index.intValue();
  }

  @Override
  public void verify() {
    super.verify();

    for (Constraint constraint : constraints) {
      ensure(constraint.value.type().isTrivialCastTo(resultType),
          "Type mismatch: Can't cast value type %s to register file result type %s.",
          constraint.value.type(), this.resultType);

      ensure(constraint.address.type().isTrivialCastTo(addressType),
          "Type mismatch: Can't cast address type %s to register file address type %s.",
          constraint.address.type(), this.resultType);
    }
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return identifier.simpleName() + ": " + addressType + " -> " + resultType;
  }

  /**
   * A register file constraint that statically defines the result value for a specific
   * address.
   *
   * <p>For example<pre>
   *  {@code
   * [X(0) = 0]
   * register file X: Index -> Regs
   * }
   * </pre>
   * defines that the address 0 always results in 0 on register file X.
   * </p>
   *
   * @param address of constraint
   * @param value   of constraint
   */
  public record Constraint(
      Constant.Value address,
      Constant.Value value
  ) {
  }
}
