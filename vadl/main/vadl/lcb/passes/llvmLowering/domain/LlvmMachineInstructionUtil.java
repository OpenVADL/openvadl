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

import java.util.HashMap;
import javax.annotation.Nullable;
import vadl.gcb.passes.MachineInstructionLabel;
import vadl.lcb.passes.llvmLowering.domain.selectionDag.LlvmCondCode;

/**
 * Helper class to extend {@link MachineInstructionLabel}.
 */
public class LlvmMachineInstructionUtil {

  /**
   * The names of LLVM's cond code given a machine instruction label.
   */
  public static final HashMap<MachineInstructionLabel, LlvmCondCode> branchInstructionMapping =
      new HashMap<>();

  static {
    branchInstructionMapping.put(MachineInstructionLabel.BEQ, LlvmCondCode.SETEQ);
    branchInstructionMapping.put(MachineInstructionLabel.BSGEQ, LlvmCondCode.SETGE);
    branchInstructionMapping.put(MachineInstructionLabel.BSGTH, LlvmCondCode.SETGT);
    branchInstructionMapping.put(MachineInstructionLabel.BSLEQ, LlvmCondCode.SETLE);
    branchInstructionMapping.put(MachineInstructionLabel.BSLTH, LlvmCondCode.SETLT);
    branchInstructionMapping.put(MachineInstructionLabel.BUGEQ, LlvmCondCode.SETUGE);
    branchInstructionMapping.put(MachineInstructionLabel.BUGTH, LlvmCondCode.SETUGT);
    branchInstructionMapping.put(MachineInstructionLabel.BULEQ, LlvmCondCode.SETULE);
    branchInstructionMapping.put(MachineInstructionLabel.BULTH, LlvmCondCode.SETULT);
    branchInstructionMapping.put(MachineInstructionLabel.BNEQ, LlvmCondCode.SETNE);
  }

  /**
   * Return the {@link LlvmCondCode} given a branch {@link MachineInstructionLabel}.
   * This method will return {@code null} when there is no mapping or the
   * {@link MachineInstructionLabel} is not a branch.
   */
  @Nullable
  public static LlvmCondCode getLlvmCondCodeByLabel(MachineInstructionLabel label) {
    return branchInstructionMapping.get(label);
  }
}
