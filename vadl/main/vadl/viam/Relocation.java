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
import vadl.viam.graph.dependency.ReadRegNode;

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
  public Relocation(Identifier identifier, Parameter[] parameters, Type returnType) {
    super(identifier, parameters, returnType);
  }

  public Relocation(Identifier identifier, Parameter[] parameters, Type returnType,
                    Graph behavior) {
    super(identifier, parameters, returnType, behavior);
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
   * A {@link Relocation} is relative when it references the {@link Counter} which is
   * declared in {@link InstructionSetArchitecture#pc()}.
   */
  public boolean isRelative() {
    return this.behavior().getNodes(ReadRegNode.class)
        .anyMatch(x -> x.staticCounterAccess() != null);
  }

  /**
   * A {@link Relocation} is absolute when it does not reference the {@link Counter} which
   * is declared in {@link InstructionSetArchitecture#pc()}.
   */
  public boolean isAbsolute() {
    return this.behavior().getNodes(ReadRegNode.class)
        .noneMatch(x -> x.staticCounterAccess() != null);
  }
}
