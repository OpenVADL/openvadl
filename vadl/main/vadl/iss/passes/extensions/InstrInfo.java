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

package vadl.iss.passes.extensions;

import java.util.Comparator;
import java.util.stream.Stream;
import vadl.viam.Definition;
import vadl.viam.DefinitionExtension;
import vadl.viam.Instruction;
import vadl.viam.graph.dependency.ParamNode;

public class InstrInfo extends DefinitionExtension<Instruction> {


  /**
   * Determines if the instruction is rendered as a helper call to
   * a C implementation of this instruction.
   */
  public boolean asHelperCall() {
    // TODO: Actually determine if we are a helper call
    return true;
  }

  /**
   * Generates a lowercase representation of the instruction's simple name.
   */
  public String cIdentName() {
    return instr().simpleName().toLowerCase();
  }

  public Instruction instr() {
    return extendingDef();
  }

  @Override
  public Class<? extends Definition> extendsDefClass() {
    return Instruction.class;
  }

  public Stream<ParamNode> helperFormatParamOrder() {
    return instr().behavior().getNodes(ParamNode.class)
        .sorted(Comparator.comparing((a) -> a.definition().simpleName()));
  }

}
