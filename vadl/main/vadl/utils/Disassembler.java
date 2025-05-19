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

package vadl.utils;

import vadl.vdt.model.DecodeTreeGenerator;
import vadl.vdt.model.Node;
import vadl.vdt.target.common.DecisionTreeDecoder;
import vadl.vdt.utils.Instruction;
import vadl.viam.Constant;
import vadl.viam.InstructionSetArchitecture;

public class Disassembler {

  private final DecisionTreeDecoder decoder;
  private final AssemblyPrinter printer;

  public Disassembler(InstructionSetArchitecture isa, DecodeTreeGenerator<Instruction> vdtGen) {
    var vdtInstrs = isa.ownInstructions().stream().map(Instruction::from).toList();
    Node vdtRoot = vdtGen.generate(vdtInstrs);
    this.decoder = new DecisionTreeDecoder(vdtRoot);
    this.printer = new AssemblyPrinter();
  }

  public String disassemble(Constant.Value assembly) {
    // TODO: Catch no decision found errors
    var instr = decoder.decode(assembly);
    return printer.print(instr);
  }

}
