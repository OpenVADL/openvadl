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

import static vadl.viam.ViamError.ensurePresent;

import java.util.ArrayList;
import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.context.CNodeContext;
import vadl.cppCodeGen.mixins.CInvalidMixins;
import vadl.error.Diagnostic;
import vadl.javaannotations.DispatchFor;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstruction;
import vadl.pass.PassResults;
import vadl.utils.SourceLocation;
import vadl.viam.Format;
import vadl.viam.PrintableInstruction;
import vadl.viam.ViamError;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FuncParamNode;

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

  @Override
  protected void handleArbitraryExpression(BuiltInCall node, int radix, CGenContext<Node> ctx) {
    var children = new ArrayList<FieldAccessRefNode>();
    node.collectInputsWithChildren(children, FieldAccessRefNode.class);

    ViamError.ensure(children.size() == 1,
        () -> Diagnostic.error("Only one field access is allowed.", node.location()));

    var fieldAccessNode = children.getFirst();

    var indexInOperands = ensurePresent(indexInInputsOrOutputs(fieldAccessNode.fieldAccess()), () ->
        Diagnostic.error("Immediate must be part of an tablegen input or output.",
            node.location())
    );

    ctx.wr("AsmUtils::formatImm(MCOperandWrapper(MI->getOperand(%s)), %d, &MAI)",
        indexInOperands, radix);
  }

  // We are overwriting this from the parent class because we don't want any sign extension
  // of the immediate operand. This handler deals with labels and will crash if we call ".isImm()"

  @Override
  protected void writeImmediateWithRadix(Format.FieldAccess fieldAccess,
                                         CGenContext<Node> ctx,
                                         int radix,
                                         SourceLocation sourceLocation,
                                         boolean isSigned) {
    var indexInOperands = ensurePresent(indexInInputsOrOutputs(fieldAccess), () ->
        Diagnostic.error("Immediate must be part of an tablegen input or output.",
            sourceLocation)
    );

    ctx.wr("AsmUtils::formatImm(MCOperandWrapper(MI->getOperand(%s)), %d, &MAI)",
        indexInOperands, radix);
  }

  @Override
  protected void writeImmediateWithRadix(Format.Field field,
                                         CGenContext<Node> ctx,
                                         int radix,
                                         SourceLocation sourceLocation,
                                         boolean isSigned) {
    var indexInOperands = ensurePresent(indexInInputsOrOutputs(field), () ->
        Diagnostic.error("Immediate must be part of an tablegen input or output.",
            sourceLocation)
    );

    ctx.wr("AsmUtils::formatImm(MCOperandWrapper(MI->getOperand(%s)), %d, &MAI)",
        indexInOperands, radix);
  }

  @Override
  protected void writeImmediateWithRadix(FuncParamNode node,
                                         CGenContext<Node> ctx,
                                         int radix,
                                         SourceLocation sourceLocation,
                                         boolean isSigned) {
    var indexInOperands = ensurePresent(indexInInputsOrOutputs(node), () ->
        Diagnostic.error("Immediate must be part of an tablegen input or output.",
            sourceLocation)
    );

    ctx.wr("AsmUtils::formatImm(MCOperandWrapper(MI->getOperand(%s)), %d, &MAI)",
        indexInOperands, radix);
  }
}
