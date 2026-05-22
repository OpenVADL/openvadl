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

package vadl.iss.codegen;

import static vadl.iss.passes.TcgPassUtils.instrInfo;

import vadl.configuration.IssConfiguration;
import vadl.iss.passes.extensions.InstrExecPlan.ExecutionPath;
import vadl.viam.Instruction;

/**
 * The code generator for the {@code target/gen-arch/translate.c}.
 * It produces translate functions for all instructions
 * in the {@link vadl.viam.InstructionSetArchitecture}.
 *
 * <p>The dispatch is execution-plan-driven when an {@link vadl.iss.passes.extensions.InstrExecPlan}
 * is available and falls back to the legacy direct-TCG-vs-helper split otherwise. Lowered gvec
 * nodes stay on the shared non-helper renderer and are emitted from the instruction graph like any
 * other backend node.
 */
public class IssTranslateCodeGenerator {

  /**
   * The static entry point to get the translation function for a given instruction.
   */
  public static String fetch(Instruction def,
                             IssConfiguration configuration) {
    return translateGenerator(def, configuration).fetch();
  }

  static InstructionTranslateGenerator translateGenerator(Instruction def,
                                                          IssConfiguration configuration) {
    var executionPlan = instrInfo(def).executionPlan();
    if (executionPlan != null) {
      return plannedTranslateGenerator(def, configuration, executionPlan.selectedPath());
    }
    return legacyTranslateGenerator(def, configuration);
  }

  private static InstructionTranslateGenerator plannedTranslateGenerator(
      Instruction def,
      IssConfiguration configuration,
      ExecutionPath path
  ) {
    return switch (path) {
      case NORMAL_TCG -> new TcgTranslateGenerator(def, configuration);
      case HELPER_CALL -> new HelperCallTranslateGenerator(def, configuration);
    };
  }

  private static InstructionTranslateGenerator legacyTranslateGenerator(
      Instruction def,
      IssConfiguration configuration
  ) {
    return instrInfo(def).executionPath() == ExecutionPath.HELPER_CALL
        ? new HelperCallTranslateGenerator(def, configuration)
        : new TcgTranslateGenerator(def, configuration);
  }
}
