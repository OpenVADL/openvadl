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

package vadl.lcb.codegen.assembly;

import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.context.CNodeContext;
import vadl.javaannotations.DispatchFor;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.pass.PassResults;
import vadl.viam.PrintableInstruction;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.BuiltInCall;

/**
 * Generates the code blocks to construct an assembly string. This handler creates the code path
 * where the operand is a label.
 */
@DispatchFor(
    value = Node.class,
    context = CNodeContext.class,
    include = {"vadl.viam", "vadl.lcb.graph"}
)
public class AssemblyInstructionPrinterLabelHandler
    extends AssemblyInstructionPrinterImmediateHandler {

  /**
   * Constructor.
   */
  public AssemblyInstructionPrinterLabelHandler(PassResults passResults,
                                                PrintableInstruction instruction,
                                                TableGenInstruction tableGenInstruction) {
    super(passResults, instruction, tableGenInstruction);
    this.ctx = new CNodeContext(
        builder::append,
        (ctx, node)
            -> AssemblyInstructionPrinterLabelHandlerDispatcher.dispatch(this, ctx, node));
  }

  // Labels are never encoded or sign extended, so we always print the raw operand.
  @Override
  protected void writeImmediateWithRadix(BuiltInCall node, CGenContext<Node> ctx, int radix,
                                         boolean isSigned) {
    ctx.wr("AsmUtils::formatImm(MCOperandWrapper(MI->getOperand(%s)), %d, &MAI)",
        operandRefIndexInOperands(node), radix);
  }
}
