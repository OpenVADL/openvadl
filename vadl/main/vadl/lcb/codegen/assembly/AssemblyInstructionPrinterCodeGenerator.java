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

import java.util.stream.Collectors;
import vadl.cppCodeGen.model.CppFunctionCode;
import vadl.gcb.passes.operands.model.GcbInstructionBareSymbolOperand;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.ReferencesImmediateOperand;
import vadl.pass.PassResults;
import vadl.viam.PrintableInstruction;
import vadl.viam.graph.control.ReturnNode;

/**
 * Wrapper class for the visitor.
 */
public class AssemblyInstructionPrinterCodeGenerator {

  /**
   * Generate a function which prints the assembly.
   */
  public CppFunctionCode generateFunctionBody(
      PassResults passResults,
      PrintableInstruction instruction,
      TableGenInstruction tableGenInstruction) {
    // There are two cases
    // The first case is that all immediates are really immediates.
    // And the second case is that the immediate is actually a label.
    var immediates = tableGenInstruction.getInOperands().stream()
        .filter(x -> x instanceof ReferencesImmediateOperand
            || x instanceof GcbInstructionBareSymbolOperand)
        .toList();

    var isImm = immediates.stream().map(x -> String.format("MI->getOperand(%s).isImm()",
            tableGenInstruction.getInOperands().indexOf(x) + tableGenInstruction.getOutOperands()
                .size()))
        .collect(Collectors.joining(" && "));

    var handler = new AssemblyInstructionPrinterImmediateHandler(passResults, instruction,
        tableGenInstruction);
    var handler2 =
        new AssemblyInstructionPrinterLabelHandler(passResults, instruction, tableGenInstruction);

    var returnNodes =
        instruction.assembly().function().behavior().getNodes(ReturnNode.class).toList();
    var returnNode = returnNodes.getFirst();

    AssemblyInstructionPrinterImmediateHandlerDispatcher.dispatch(handler, handler.ctx(),
        returnNode);
    AssemblyInstructionPrinterLabelHandlerDispatcher.dispatch(handler2, handler2.ctx(), returnNode);

    return new CppFunctionCode(String.format("""
          if(%s) {
            %s
          } else {
            %s
          }
        """, immediates.isEmpty() ? "true" : isImm, handler.builder(), handler2.builder()));
  }
}
