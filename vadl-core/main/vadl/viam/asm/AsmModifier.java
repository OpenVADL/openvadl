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

package vadl.viam.asm;

import vadl.utils.SourceLocation;
import vadl.viam.Definition;
import vadl.viam.DefinitionVisitor;
import vadl.viam.Identifier;
import vadl.viam.Relocation;

/**
 * Defines a name to be used to reference a relocation
 * of the instruction set architecture in the assembly language.
 */
public class AsmModifier extends Definition {
  private final Relocation relocation;

  /**
   * Create a new asm modifier.
   * <p>
   * Sets the identifier of the definition superclass,
   * the related relocation and the source location.
   * </p>
   *
   * @param identifier the name of the modifier
   * @param relocation the relocation to be used
   * @param location   the source location of the modifier
   */
  public AsmModifier(Identifier identifier, Relocation relocation, SourceLocation location) {
    super(identifier);
    this.relocation = relocation;
    setSourceLocation(location);
  }

  public Relocation getRelocation() {
    return relocation;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }
}
