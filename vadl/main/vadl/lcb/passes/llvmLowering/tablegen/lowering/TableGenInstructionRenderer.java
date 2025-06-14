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

package vadl.lcb.passes.llvmLowering.tablegen.lowering;

import static vadl.viam.ViamError.ensure;

import java.util.BitSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import vadl.lcb.passes.llvmLowering.domain.RegisterRef;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbPseudoInstructionNode;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenCompilerInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenMachineInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPseudoInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.TableGenInstructionOperand;
import vadl.viam.CompilerInstruction;
import vadl.viam.Definition;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;

/**
 * Utility class for mapping into tablegen.
 */
public final class TableGenInstructionRenderer {

  /**
   * Transforms the given {@link Instruction} into a string which can be used by LLVM's TableGen.
   * It will *ONLY* print the anonymous pattern if the pattern is actually lowerable.
   */
  public static String lower(TableGenMachineInstruction instruction) {
    return String.format("""
            def %s : Instruction
            {
            let Namespace = "%s";
            
            let Size = %d;
            let CodeSize = %d;
            
            let OutOperandList = ( outs %s );
            let InOperandList = ( ins %s );
            
            field bits<%s> Inst;
            
            // SoftFail is a field the disassembler can use to provide a way for
            // instructions to not match without killing the whole decode process. It is
            // mainly used for ARM, but Tablegen expects this field to exist or it fails
            // to build the decode table.
            field bits<%s> SoftFail = 0;
            
            %s
            
            %s
            
            let isTerminator       = %d;
            let isBranch           = %d;
            let isCall             = %d;
            let isReturn           = %d;
            let isPseudo           = %d;
            let isCodeGenOnly      = %d;
            let mayLoad            = %d;
            let mayStore           = %d;
            let isBarrier          = %d;
            let isReMaterializable = %d;
            let isAsCheapAsAMove   = %d;
            
            let Constraints = "";
            let AddedComplexity = 0;
            
            let Pattern = [%s];
            
            let Uses = [ %s ];
            let Defs = [ %s ];
            }
            """,
        instruction.getName(),
        instruction.getNamespace(),
        instruction.getSize(),
        instruction.getCodeSize(),
        instruction.getOutOperands().stream().map(TableGenInstructionRenderer::lower).collect(
            Collectors.joining(", ")),
        instruction.getInOperands().stream().map(TableGenInstructionRenderer::lower).collect(
            Collectors.joining(", ")),
        instruction.getFormatSize(),
        instruction.getFormatSize(),
        instruction.getBitBlocks().stream().map(TableGenInstructionRenderer::lower)
            .collect(Collectors.joining("\n")),
        instruction.getFieldEncodings().stream().map(TableGenInstructionRenderer::lower)
            .collect(Collectors.joining("\n")),
        toInt(instruction.getFlags().isTerminator()),
        toInt(instruction.getFlags().isBranch()),
        toInt(instruction.getFlags().isCall()),
        toInt(instruction.getFlags().isReturn()),
        toInt(instruction.getFlags().isPseudo()),
        toInt(instruction.getFlags().isCodeGenOnly()),
        toInt(instruction.getFlags().mayLoad()),
        toInt(instruction.getFlags().mayStore()),
        toInt(instruction.getFlags().isBarrier()),
        toInt(instruction.getFlags().isRematerialisable()),
        toInt(instruction.getFlags().isAsCheapAsAMove()),
        instruction.getAnonymousPatterns()
            .stream()
            .filter(x -> x instanceof TableGenSelectionPattern)
            .map(x -> (TableGenSelectionPattern) x)
            .map(TableGenInstructionRenderer::lower)
            .collect(Collectors.joining(",")),
        instruction.getUses().stream().map(Definition::simpleName).collect(Collectors.joining(",")),
        instruction.getDefs().stream().map(Definition::simpleName).collect(Collectors.joining(","))
    );
  }


  /**
   * Transforms the given {@link PseudoInstruction} into a string which can be used by LLVM's
   * TableGen.
   */
  public static String lower(TableGenPseudoInstruction instruction) {
    var anonymousPatterns = instruction.getAnonymousPatterns().stream()
        .filter(TableGenPattern::isPatternLowerable)
        .filter(x -> x instanceof TableGenSelectionWithOutputPattern)
        .map(x -> (TableGenSelectionWithOutputPattern) x)
        .toList();
    var y = String.format("""
            def %s : Instruction
            {
            let Namespace = "%s";
            
            let OutOperandList = ( outs %s );
            let InOperandList = ( ins %s );
            
            let isTerminator  = %d;
            let isBranch      = %d;
            let isCall        = %d;
            let isReturn      = %d;
            let isPseudo      = %d;
            let isCodeGenOnly = %d;
            let mayLoad       = %d;
            let mayStore      = %d;
            let isBarrier     = %d;
            let isReMaterializable = %d;
            let isAsCheapAsAMove   = %d;
            
            let Constraints = "";
            let AddedComplexity = 0;
            
            let Uses = [ %s ];
            let Defs = [ %s ];
            }
            
            %s
            """,
        instruction.getName(),
        instruction.getNamespace(),
        instruction.getOutOperands().stream().map(TableGenInstructionRenderer::lower).collect(
            Collectors.joining(", ")),
        instruction.getInOperands().stream().map(TableGenInstructionRenderer::lower).collect(
            Collectors.joining(", ")),
        toInt(instruction.getFlags().isTerminator()),
        toInt(instruction.getFlags().isBranch()),
        toInt(instruction.getFlags().isCall()),
        toInt(instruction.getFlags().isReturn()),
        toInt(instruction.getFlags().isPseudo()),
        toInt(instruction.getFlags().isCodeGenOnly()),
        toInt(instruction.getFlags().mayLoad()),
        toInt(instruction.getFlags().mayStore()),
        toInt(instruction.getFlags().isBarrier()),
        toInt(instruction.getFlags().isRematerialisable()),
        toInt(instruction.getFlags().isAsCheapAsAMove()),
        instruction.getUses().stream().map(RegisterRef::lowerName).collect(Collectors.joining(",")),
        instruction.getDefs().stream().map(RegisterRef::lowerName).collect(Collectors.joining(",")),
        anonymousPatterns.stream()
            .map(TableGenInstructionRenderer::lower)
            .collect(Collectors.joining("\n"))
    );

    return y;
  }

  /**
   * Transforms the given {@link CompilerInstruction} into a string which can be used by LLVM's
   * TableGen.
   */
  public static String lower(TableGenCompilerInstruction instruction) {
    var anonymousPatterns = instruction.getAnonymousPatterns().stream()
        .filter(TableGenPattern::isPatternLowerable)
        .filter(x -> x instanceof TableGenSelectionWithOutputPattern)
        .map(x -> (TableGenSelectionWithOutputPattern) x)
        .toList();
    var y = String.format("""
            def %s : Instruction
            {
            let Namespace = "%s";
            
            let OutOperandList = ( outs %s );
            let InOperandList = ( ins %s );
            
            let isTerminator  = %d;
            let isBranch      = %d;
            let isCall        = %d;
            let isReturn      = %d;
            let isPseudo      = %d;
            let isCodeGenOnly = %d;
            let mayLoad       = %d;
            let mayStore      = %d;
            let isBarrier     = %d;
            let isReMaterializable = %d;
            let isAsCheapAsAMove   = %d;
            
            let Constraints = "";
            let AddedComplexity = 0;
            
            let Uses = [ %s ];
            let Defs = [ %s ];
            }
            
            %s
            """,
        instruction.getName(),
        instruction.getNamespace(),
        instruction.getOutOperands().stream().map(TableGenInstructionRenderer::lower).collect(
            Collectors.joining(", ")),
        instruction.getInOperands().stream().map(TableGenInstructionRenderer::lower).collect(
            Collectors.joining(", ")),
        toInt(instruction.getFlags().isTerminator()),
        toInt(instruction.getFlags().isBranch()),
        toInt(instruction.getFlags().isCall()),
        toInt(instruction.getFlags().isReturn()),
        toInt(instruction.getFlags().isPseudo()),
        toInt(instruction.getFlags().isCodeGenOnly()),
        toInt(instruction.getFlags().mayLoad()),
        toInt(instruction.getFlags().mayStore()),
        toInt(instruction.getFlags().isBarrier()),
        toInt(instruction.getFlags().isRematerialisable()),
        toInt(instruction.getFlags().isAsCheapAsAMove()),
        instruction.getUses().stream().map(RegisterRef::lowerName).collect(Collectors.joining(",")),
        instruction.getDefs().stream().map(RegisterRef::lowerName).collect(Collectors.joining(",")),
        anonymousPatterns.stream()
            .map(TableGenInstructionRenderer::lower)
            .collect(Collectors.joining("\n"))
    );

    return y;
  }

  private static String lower(TableGenSelectionPattern tableGenPattern) {
    ensure(tableGenPattern.isPatternLowerable(), "TableGen pattern must be lowerable");
    var visitor = new TableGenPatternPrinterVisitor();

    for (var root : tableGenPattern.selector().getDataflowRoots()) {
      visitor.visit(root);
    }

    return "(" + visitor.getResult() + ")";
  }

  private static String lower(TableGenSelectionWithOutputPattern tableGenPattern) {
    ensure(tableGenPattern.isPatternLowerable(), "TableGen pattern must be lowerable");
    var visitor = new TableGenPatternPrinterVisitor();
    var machineVisitor = new TableGenMachineInstructionPrinterVisitor();

    for (var root : tableGenPattern.selector().getDataflowRoots()) {
      visitor.visit(root);
    }

    for (var root : tableGenPattern.machine().getDataflowRoots()) {
      ensure(root instanceof LcbPseudoInstructionNode
              || root instanceof LcbMachineInstructionNode,
          "root node must be pseudo or machine node");
      if (root instanceof LcbMachineInstructionNode machineInstructionNode) {
        machineVisitor.visit(machineInstructionNode);
      } else if (root instanceof LcbPseudoInstructionNode pseudoInstructionNode) {
        machineVisitor.visit(pseudoInstructionNode);
      }
    }

    return String.format("""
        def : Pat<%s,
                %s>;
        """, visitor.getResult(), machineVisitor.getResult());

  }

  /**
   * Renders an operand into a string.
   */
  public static String lower(TableGenInstructionOperand operand) {
    return operand.render();
  }

  private static String lower(TableGenMachineInstruction.BitBlock bitBlock) {
    if (bitBlock.getBitSet().isPresent()) {
      return String.format("bits<%s> %s = 0b%s;", bitBlock.getSize(), bitBlock.getName(),
          toBinaryString(bitBlock.getBitSet().get(), bitBlock.getSize()));
    } else {
      return String.format("bits<%s> %s;", 64, bitBlock.getName());
    }
  }

  private static String lower(TableGenMachineInstruction.FieldEncoding fieldEncoding) {
    var inst = fieldEncoding.getTargetHigh() != fieldEncoding.getTargetLow()
        ? fieldEncoding.getTargetHigh() + "-"
        + fieldEncoding.getTargetLow() : fieldEncoding.getTargetHigh();

    var sourceHigh = fieldEncoding.getSourceHigh() + fieldEncoding.immediateOffset();
    var sourceLow = fieldEncoding.getSourceLow() + fieldEncoding.immediateOffset();

    var source = fieldEncoding.getSourceHigh() != fieldEncoding.getSourceLow()
        ? sourceHigh + "-" + sourceLow :
        sourceHigh;

    return String.format("let Inst{%s} = %s{%s};", inst,
        fieldEncoding.getSourceBitBlockName(),
        source);
  }

  /**
   * Converts a bitset into string representation.
   *
   * @param bitSet bitset
   * @param size   the real size of the {@code bitSet}. {@code bitSet} returns only
   *               the highest bit + 1.
   * @return "01010000" binary string
   */
  @Nullable
  private static String toBinaryString(BitSet bitSet, int size) {
    if (bitSet == null) {
      return null;
    }
    return IntStream.range(0, size)
        .map(i -> size - i - 1)
        .mapToObj(b -> String.valueOf(bitSet.get(b) ? 1 : 0))
        .collect(Collectors.joining());
  }

  private static int toInt(boolean b) {
    return b ? 1 : 0;
  }
}
