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

package vadl.lcb.passes.llvmLowering.domain;


import java.util.Collections;
import java.util.List;
import vadl.lcb.passes.llvmLowering.LlvmLoweringPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenInstAlias;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenPattern;
import vadl.viam.CompilerInstruction;
import vadl.viam.Instruction;
import vadl.viam.PseudoInstruction;

/**
 * Contains information for the lowering of instructions.
 */
public abstract class LlvmLoweringRecord {
  private final LlvmLoweringPass.BaseInstructionInfo info;
  private final List<TableGenPattern> patterns;

  /**
   * Constructor.
   */
  public LlvmLoweringRecord(
      LlvmLoweringPass.BaseInstructionInfo info,
      List<TableGenPattern> patterns) {
    this.info = info;
    this.patterns = patterns;
  }

  public List<TableGenPattern> patterns() {
    return patterns;
  }

  public LlvmLoweringPass.BaseInstructionInfo info() {
    return info;
  }

  /**
   * Apply the {@link #info} with the given parameter and return a new instance.
   */
  public abstract LlvmLoweringRecord withInfo(
      LlvmLoweringPass.BaseInstructionInfo baseInstructionInfo);

  /**
   * Represents a {@link LlvmLoweringRecord} for {@link Instruction}.
   */
  public static class Machine extends LlvmLoweringRecord {
    private final Instruction instructionRef;

    /**
     * Constructor.
     */
    public Machine(Instruction instructionRef,
                   LlvmLoweringPass.BaseInstructionInfo info,
                   List<TableGenPattern> patterns) {
      super(info, patterns);
      this.instructionRef = instructionRef;
    }


    public Instruction instruction() {
      return instructionRef;
    }

    @Override
    public LlvmLoweringRecord withInfo(LlvmLoweringPass.BaseInstructionInfo baseInstructionInfo) {
      return new Machine(instructionRef, baseInstructionInfo, patterns());
    }
  }

  /**
   * Represents a {@link LlvmLoweringRecord} for {@link PseudoInstruction}.
   */
  public static class Pseudo extends LlvmLoweringRecord {
    private final List<TableGenInstAlias> instAliases;

    /**
     * Constructor.
     */
    public Pseudo(LlvmLoweringPass.BaseInstructionInfo info,
                  List<TableGenPattern> patterns,
                  List<TableGenInstAlias> instAliases
    ) {
      super(info, patterns);
      this.instAliases = instAliases;
    }

    public List<TableGenInstAlias> instAliases() {
      return instAliases;
    }

    @Override
    public LlvmLoweringRecord withInfo(LlvmLoweringPass.BaseInstructionInfo baseInstructionInfo) {
      return new Pseudo(baseInstructionInfo, patterns(), instAliases);
    }
  }

  /**
   * Represents a {@link LlvmLoweringRecord} for {@link CompilerInstruction}.
   */
  public static class Compiler extends LlvmLoweringRecord {

    /**
     * Constructor.
     */
    public Compiler(LlvmLoweringPass.BaseInstructionInfo info) {
      super(info, Collections.emptyList());
    }

    @Override
    public LlvmLoweringRecord withInfo(LlvmLoweringPass.BaseInstructionInfo baseInstructionInfo) {
      return new Compiler(baseInstructionInfo);
    }
  }
}
