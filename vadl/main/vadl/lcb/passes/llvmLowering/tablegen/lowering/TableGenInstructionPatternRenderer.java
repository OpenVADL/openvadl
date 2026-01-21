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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import vadl.gcb.passes.GenerateGcbIntrinsicsPass;
import vadl.gcb.passes.operands.model.InstructionOperandPrintable;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbMachineInstructionNode;
import vadl.lcb.passes.llvmLowering.domain.machineDag.LcbPseudoInstructionNode;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenMachineInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPseudoInstruction;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenSelectionWithOutputPattern;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;
import vadl.viam.graph.Graph;

/**
 * Utility class for mapping into TableGen. But it only prints the anonymous patterns.
 * The split between {@link TableGenInstructionPatternRenderer} and
 * {@link TableGenInstructionRenderer} is required because TableGen does not allow forward
 * declarations. Therefore, all instructions must be defined before they can be used in patterns.
 * This might be problem for some patterns.
 */
public final class TableGenInstructionPatternRenderer {
  /**
   * Transforms the given {@link Instruction} into a string which can be used by LLVM's TableGen.
   * It will *ONLY* print the anonymous pattern if the pattern is actually lowerable.
   */
  public static String lower(TableGenMachineInstruction instruction) {
    var anonymousPatterns = instruction.getAnonymousPatterns().stream()
        .filter(TableGenPattern::isPatternLowerable)
        .filter(x -> x instanceof TableGenSelectionWithOutputPattern)
        .map(x -> (TableGenSelectionWithOutputPattern) x)
        .toList();
    return String.format("""
            %s
            """,
        anonymousPatterns
            .stream()
            .map(TableGenInstructionPatternRenderer::lower)
            .collect(Collectors.joining("\n"))
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
            %s
            """,
        anonymousPatterns.stream()
            .map(TableGenInstructionPatternRenderer::lower)
            .collect(Collectors.joining("\n"))
    );

    return y;
  }

  /**
   * Lowering patterns.
   */
  public static String lower(TableGenSelectionWithOutputPattern tableGenPattern) {
    ensure(tableGenPattern.isPatternLowerable(), "TableGen pattern must be lowerable");

    return String.format("""
        def : Pat<%s,
                %s>;
        """, lowerSelector(tableGenPattern.selector()), lowerMachine(tableGenPattern.machine()));

  }

  /**
   * Render the mapping between intrinsic and instruction.
   */
  public static String lower(List<TableGenMachineInstruction> tableGenMachineRecords,
                             GenerateGcbIntrinsicsPass.GcbIntrinsic intrinsic) {
    var records =
        tableGenMachineRecords.stream().collect(Collectors.toMap(
            TableGenMachineInstruction::instruction, x -> x));
    var record = Objects.requireNonNull(records.get(intrinsic.instruction()));

    return String.format("""
        def : Pat<%s,
                %s>;
        """, lowerSelector(record, intrinsic), lowerMachine(record));
  }

  /**
   * Render the selector pattern.
   */
  public static String lowerSelector(Graph graph) {
    var visitor = new TableGenPatternPrinterVisitor();

    for (var root : graph.getDataflowRoots()) {
      visitor.visit(root);
    }

    return visitor.getResult();
  }

  private static String lowerSelector(TableGenMachineInstruction record,
                                      GenerateGcbIntrinsicsPass.GcbIntrinsic intrinsic) {
    return "(int_" + intrinsic.intrinsicName() + " " + record.getInOperands().stream().map(
        InstructionOperandPrintable::render).collect(
        Collectors.joining(", ")) + ")";
  }

  /**
   * Render the machine pattern.
   */
  public static String lowerMachine(Graph graph) {
    var machineVisitor = new TableGenMachineInstructionPrinterVisitor();

    for (var root : graph.getDataflowRoots()) {
      ensure(root instanceof LcbPseudoInstructionNode
              || root instanceof LcbMachineInstructionNode,
          "root node must be pseudo or machine node");
      if (root instanceof LcbMachineInstructionNode machineInstructionNode) {
        machineVisitor.visit(machineInstructionNode);
      } else {
        LcbPseudoInstructionNode pseudoInstructionNode = (LcbPseudoInstructionNode) root;
        machineVisitor.visit(pseudoInstructionNode);
      }
    }

    return machineVisitor.getResult();
  }

  private static String lowerMachine(TableGenMachineInstruction record) {
    return "(" + record.getName() + " " + record.getInOperands().stream().map(
        InstructionOperandPrintable::render).collect(
        Collectors.joining(", ")) + ")";
  }
}
