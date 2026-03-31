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

/**
 * Introduces a new name in the assembly language for a directive.
 */
public class AsmDirectiveMapping extends Definition {
  private final String alias;
  private final String target;
  private final boolean alignmentIsInBytes;

  /**
   * Creates a new directive mapping.
   *
   * <p>
   * Sets the identifier of the definition superclass,
   * the related AsmDirective and the source location.
   * </p>
   *
   * @param identifier the identifier of the directive
   * @param alias      the alias of the directive
   * @param target     the original directive
   * @param location   the source location of the directive mapping
   */
  public AsmDirectiveMapping(Identifier identifier, String alias, String target,
                             boolean alignmentIsInBytes, SourceLocation location) {
    super(identifier);
    this.alias = alias;
    this.target = target;
    this.alignmentIsInBytes = alignmentIsInBytes;
    setSourceLocation(location);
  }

  public String getAlias() {
    return alias;
  }

  public String getTarget() {
    return target;
  }

  public boolean getAlignmentIsInBytes() {
    return alignmentIsInBytes;
  }

  @Override
  public void accept(DefinitionVisitor visitor) {
    visitor.visit(this);
  }
}
