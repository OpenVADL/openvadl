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

import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import vadl.types.BitsType;
import vadl.types.DataType;
import vadl.types.Type;

/**
 * The register file is related to the {@link Register} but takes an address/index when accessing
 * it. It may also have constraints that restricts the possible values for statically defined
 * addresses.
 */
public class RegisterFile extends RegisterTensor {

  /**
   * Constructs a new RegisterFile object.
   *
   * @param identifier  The identifier of the RegisterFile.
   * @param addressType The data type of the file address/index.
   * @param resultType  The data type of the result value.
   */
  public RegisterFile(Identifier identifier, DataType addressType, DataType resultType,
                      Constraint[] constraints) {
    super(identifier, initDims(addressType, resultType), constraints);
  }

  private static List<Dimension> initDims(DataType addressType, DataType resultType) {
    var innerDimType = Type.bits(BitsType.minimalRequiredWidthFor(resultType.bitWidth()));
    var outerDim = new Dimension(0, addressType, (int) Math.pow(2, addressType.bitWidth()));
    var innerDim = new Dimension(1, innerDimType, resultType.toBitsType().bitWidth());
    return List.of(outerDim, innerDim);
  }

  @Override
  public boolean hasAddress() {
    return true;
  }

  public int numberOfRegisters() {
    return outermostDim().size();
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
    return generateName(index.intValue());
  }

  @Nonnull
  @Override
  public DataType addressType() {
    return Objects.requireNonNull(super.addressType());
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return identifier.simpleName() + ": " + addressType() + " -> " + resultType();
  }

}
