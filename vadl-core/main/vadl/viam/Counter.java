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
import java.util.stream.Collectors;
import vadl.types.DataType;

/**
 * The Counter represents a program in a VADL specification.
 *
 * <p>There are 3 ways to define a program counter (same for group counters) in VADL:
 * <pre>
 *   1. program counter PC: Bits<32>
 *   2. alias program counter PC: Bits<32> = ONE_DIM_REGISTER
 *   3. alias program counter PC: Bits<32> = MULTI_DIM_REGISTER( < constant >, ... )
 * </pre>
 * This means the program counter refers to some entry in a register.
 * The concrete dimension and entry is given by the {@link #indices()}, which
 * provides a constant for each dimension that is accessed to reference the program counter.</p>
 *
 * <p>Currently, generators only support one-dimensional program counters, e.i.,
 * with an empty {@link #indices()} list.</p>
 *
 * <p>In a VADL specification a program counter may be annotated with a position annotation
 * ({@code CURRENT, NEXT, NEXT NEXT}).
 * However, in the VIAM, the program counter always points to the current instruction.
 * The logic required to adjust the program counter in case of a position other than
 * {@code CURRENT}, is handled and inserted in the frontend.</p>
 *
 * <p><b>Note:</b> The register tensor definition is not owned by the counter but
 * only referenced by it.</p>
 */
public class Counter extends Definition {

  private final RegisterTensor registerTensor;
  private final List<Constant.Value> indices;

  /**
   * Constructs the Counter.
   *
   * @param registerTensor the register tensor that contains (or represents) the program counter
   * @param indices        a list of constant indices that is used to reference the program counter
   *                       in a register.
   *                       For a program counter represented by a one-dimensional register,
   *                       this list is empty.
   */
  public Counter(Identifier identifier, RegisterTensor registerTensor,
                 List<Constant.Value> indices) {
    super(identifier);
    this.registerTensor = registerTensor;
    this.indices = indices;
  }

  public List<Constant.Value> indices() {
    return indices;
  }

  public DataType resultType() {
    return registerTensor.resultType(indices.size());
  }

  /**
   * Returns the corresponding {@link RegisterTensor}.
   */
  public RegisterTensor registerTensor() {
    return registerTensor;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    var args = indices.stream().map(Object::toString).collect(Collectors.joining(", "));
    args = args.isEmpty() ? "" : "(" + args + ")";
    var result = identifier + ": " + resultType();
    result += " = " + registerTensor.identifier + args;
    return result;
  }

}