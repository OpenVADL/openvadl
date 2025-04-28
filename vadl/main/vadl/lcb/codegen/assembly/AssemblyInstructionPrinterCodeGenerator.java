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

package vadl.lcb.codegen.assembly;

import java.io.StringWriter;
import vadl.cppCodeGen.model.CppFunctionCode;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.viam.PrintableInstruction;
import vadl.viam.graph.control.ReturnNode;

/**
 * Wrapper class for the visitor.
 */
public class AssemblyInstructionPrinterCodeGenerator {
  private final StringWriter writer = new StringWriter();

  /**
   * Generate a function which prints the assembly.
   */
  public CppFunctionCode generateFunctionBody(
      PrintableInstruction instruction,
      TableGenInstruction tableGenInstruction) {
    var visitor =
        new AssemblyInstructionPrinterCodeGeneratorVisitor(writer,
            instruction,
            tableGenInstruction);

    instruction.assembly().function().behavior().getNodes(ReturnNode.class)
        .forEach(visitor::visit);

    return new CppFunctionCode(writer.toString());
  }
}
