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

import vadl.error.Diagnostic;
import vadl.types.Type;
import vadl.viam.graph.Graph;

/**
 * Represents a relocation definition in a VIAM specification.
 *
 * <p>Defined relocations can be embedded in the source code to refere to labels who's target
 * address is unknown. The assembler emits these relocations in the object file when expanding
 * pseudo instructions or sequences.
 * Relocations are used to change immediate values during link time.
 * They are needed either for optimization purposes or because the value is not known beforehand.
 * </p>
 */
public class Relocation extends Function {
  /**
   * A {@link Relocation} has a certain kind. It tells the linker how to update the field.
   */
  public enum Kind {
    ABSOLUTE,
    RELATIVE,
    GLOBAL_OFFSET_TABLE
  }

  private final Kind kind;

  public Relocation(Identifier identifier, Kind kind, Parameter[] parameters, Type returnType) {
    super(identifier, parameters, returnType);
    this.kind = kind;
  }

  public Relocation(Identifier identifier, Kind kind, Parameter[] parameters, Type returnType,
                    Graph behavior) {
    super(identifier, parameters, returnType, behavior);
    this.kind = kind;
  }

  @Override
  public void verify() {
    super.verify();

    ViamError.ensure(parameters().length == 1,
        () -> Diagnostic.error("Relocations must have exactly one argument.",
            this.sourceLocation()));
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }

  /**
   * Returns {@code true} when the relocation is relative.
   */
  public boolean isRelative() {
    return kind == Kind.RELATIVE;
  }

  /**
   * Returns {@code true} when the relocation is absolute.
   */
  public boolean isAbsolute() {
    return kind == Kind.ABSOLUTE;
  }

  /**
   * Returns {@code true} when the relocation references the global offset table.
   */
  public boolean isGlobalOffsetTable() {
    return kind == Kind.GLOBAL_OFFSET_TABLE;
  }
}
