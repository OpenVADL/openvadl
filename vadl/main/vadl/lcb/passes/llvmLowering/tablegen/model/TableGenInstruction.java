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

package vadl.lcb.passes.llvmLowering.tablegen.model;

import java.util.List;
import java.util.stream.Stream;
import vadl.error.Diagnostic;
import vadl.gcb.passes.RegisterRef;
import vadl.gcb.passes.operands.model.GcbInstructionImmediateOperand;
import vadl.gcb.passes.operands.model.GcbInstructionOperand;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.tableGenOperand.ReferencesImmediateOperand;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;

/**
 * Models an {@link Instruction} and {@link PseudoInstruction} for TableGen.
 */
public abstract class TableGenInstruction {
  private final String name;
  private final String namespace;
  private final List<TableGenPattern> anonymousPatterns;
  private final List<GcbInstructionOperand> inOperands;
  private final List<GcbInstructionOperand> outOperands;
  private final List<RegisterRef> uses;
  private final List<RegisterRef> defs;
  private final LlvmLoweringPass.Flags flags;


  /**
   * Constructor.
   */
  public TableGenInstruction(String name,
                             String namespace,
                             LlvmLoweringPass.Flags flags,
                             List<GcbInstructionOperand> inOperands,
                             List<GcbInstructionOperand> outOperands,
                             List<RegisterRef> uses,
                             List<RegisterRef> defs,
                             List<TableGenPattern> anonymousPatterns) {
    this.name = name;
    this.namespace = namespace;
    this.flags = flags;
    this.inOperands = inOperands;
    this.outOperands = outOperands;
    this.uses = uses;
    this.defs = defs;
    this.anonymousPatterns = anonymousPatterns;
  }

  public String getNamespace() {
    return namespace;
  }

  public List<TableGenPattern> getAnonymousPatterns() {
    return anonymousPatterns;
  }

  public List<RegisterRef> getUses() {
    return uses;
  }

  public List<RegisterRef> getDefs() {
    return defs;
  }

  public LlvmLoweringPass.Flags getFlags() {
    return flags;
  }

  public List<GcbInstructionOperand> getInOperands() {
    return inOperands;
  }

  public List<GcbInstructionOperand> getOutOperands() {
    return outOperands;
  }

  public String getName() {
    return name;
  }

  /**
   * Get operand by index.
   */
  public GcbInstructionOperand getOperand(int index) {
    return Stream.concat(outOperands.stream(), inOperands.stream())
        .toList()
        .get(index);
  }

  /**
   * Get the operand index in {@link #inOperands}. It is offset by the number of operands
   * in {@link #outOperands}.
   */
  public int indexInOperands(GcbInstructionOperand operand) {
    return outOperands.size() + inOperands.indexOf(operand);
  }

  /**
   * Get the operand index in {@link #inOperands}. It is offset by the number of operands
   * in {@link #outOperands}.
   */
  public int indexInOperands(Format.FieldAccess operand) {
    int offset = outOperands.size();
    int index = 0;

    while (index < offset + inOperands.size()) {
      if (inOperands.get(index) instanceof ReferencesImmediateOperand immediateOperand
          && immediateOperand.immediateOperand().fieldAccessRef().equals(operand)) {
        break;
      }

      index++;
    }

    if (index == offset + inOperands.size()) {
      throw Diagnostic.error("Cannot find operand in the inputs", operand.location()).build();
    }

    return index + offset;
  }

  /**
   * Get the operands which are immediates from the input operand list but only if the
   * operands have been already lowered.
   *
   * @return a list of {@link ReferencesImmediateOperand}.
   */
  public List<ReferencesImmediateOperand> immediateInputOperands() {
    return inOperands.stream()
        .filter(x -> x instanceof ReferencesImmediateOperand)
        .map(x -> (ReferencesImmediateOperand) x)
        .toList();
  }

  /**
   * Get the operands which are immediates from the input operand list. This will also work if the
   * immediate was not lowered.
   */
  public List<GcbInstructionImmediateOperand> gcbImmediateInputOperands() {
    return inOperands.stream()
        .filter(x -> x instanceof GcbInstructionImmediateOperand)
        .map(x -> (GcbInstructionImmediateOperand) x)
        .toList();
  }
}
