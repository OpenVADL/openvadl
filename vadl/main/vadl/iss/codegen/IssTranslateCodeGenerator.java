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
import vadl.iss.passes.extensions.InstrExecPlan.StrategyKind;
import vadl.viam.Instruction;

/**
 * The code generator for the {@code target/gen-arch/translate.c}.
 * It produces translate functions for all instructions
 * in the {@link vadl.viam.InstructionSetArchitecture}.
 *
 * <p>The dispatch is execution-plan-driven when an {@link vadl.iss.passes.extensions.InstrExecPlan}
 * is available and falls back to the legacy scalar-vs-helper split otherwise. Strategy-specific
 * generators provide dedicated seams for scalar TCG, direct gvec, and helper-based translation.
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
      return plannedTranslateGenerator(def, configuration, executionPlan.selectedStrategy());
    }
    return legacyTranslateGenerator(def, configuration);
  }

  private static InstructionTranslateGenerator plannedTranslateGenerator(
      Instruction def,
      IssConfiguration configuration,
      StrategyKind strategy
  ) {
    return switch (strategy) {
      case TCG_SCALAR -> new ScalarTcgTranslateGenerator(def, configuration);
      case DIRECT_GVEC -> new DirectGvecTranslateGenerator(def, configuration);
      case HELPER_CALL -> new HelperCallTranslateGenerator(def, configuration);
    };
  }

  private static InstructionTranslateGenerator legacyTranslateGenerator(
      Instruction def,
      IssConfiguration configuration
  ) {
    return instrInfo(def).asHelperCall()
        ? new HelperCallTranslateGenerator(def, configuration)
        : new ScalarTcgTranslateGenerator(def, configuration);
  }
}
