// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Bundles {@link RegisterTensor} and {@link ArtificialResource} together.
 */
public abstract class RegisterResource extends Resource {
  private final List<Constraint> constraints;

  public RegisterResource(Identifier identifier) {
    super(identifier);
    this.constraints = new ArrayList<>();
  }

  public List<Constraint> constraints() {
    return constraints;
  }

  public void setConstraints(Constraint... constraints) {
    this.constraints.clear();
    this.constraints.addAll(Arrays.asList(constraints));
  }

  public void addConstraint(Constraint constraint) {
    constraints.add(constraint);
  }

  public Identifier identifier() {
    return identifier;
  }

  /**
   * Returns the dimensions of this register resource.
   */
  public abstract List<RegisterTensor.Dimension> dimensions();

  /**
   * Returns whether this register resource represents a register file.
   */
  public abstract boolean isRegisterFile();


  /**
   * A register file constraint that statically defines the result value for a specific
   * index.
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
   * @param indices of constraint
   * @param value   of constraint
   */
  public record Constraint(
      List<Constant.Value> indices,
      Constant.Value value
  ) {
  }

}
