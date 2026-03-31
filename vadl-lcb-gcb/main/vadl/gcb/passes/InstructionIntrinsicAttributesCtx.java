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

package vadl.gcb.passes;

import java.util.List;
import vadl.viam.Definition;
import vadl.viam.DefinitionExtension;
import vadl.viam.Instruction;

/**
 * An extension for the {@link Instruction}. It will be used to indicate what the attributes for the
 * intrinsic's attributes of the {@link Instruction} are.
 */
public class InstructionIntrinsicAttributesCtx extends DefinitionExtension<Instruction> {
  /**
   * Attributes for the intrinsic.
   */
  public enum Attribute {
    NoMem,
    Speculatable,
    ReadMem,
    WriteMem,
    ReadWriteMem
  }

  private final List<Attribute> attributes;

  public InstructionIntrinsicAttributesCtx(List<Attribute> attributes) {
    this.attributes = attributes;
  }

  public List<Attribute> getAttributes() {
    return attributes;
  }

  @Override
  public Class<? extends Definition> extendsDefClass() {
    return Definition.class;
  }
}
