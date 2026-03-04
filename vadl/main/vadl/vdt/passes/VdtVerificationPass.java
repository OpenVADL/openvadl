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

package vadl.vdt.passes;

import static vadl.error.Diagnostic.error;
import static vadl.error.Diagnostic.warning;

import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.BitVecNum;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.Model;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import io.github.rascmatt.z3.Z3Bootstrap;
import java.io.IOException;
import java.util.List;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.error.DeferredDiagnosticStore;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.vdt.model.Node;
import vadl.vdt.target.common.DecisionTreeCompletenessVerifier;
import vadl.vdt.target.common.DecisionTreeSoundnessVerifier;
import vadl.vdt.target.common.dto.PathVerificationInfo;
import vadl.viam.Instruction;
import vadl.viam.Specification;

/**
 * Verification pass to verify soundness and completeness of the constructed decision tree.
 */
public class VdtVerificationPass extends Pass {

  /**
   * Constructor for the VDT Verification Pass.
   *
   * @param configuration the configuration
   */
  public VdtVerificationPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("VDT Verification Pass");
  }

  @Override
  @SuppressWarnings("unchecked")
  public @Nullable Void execute(PassResults passResults, Specification viam)
      throws IOException {

    final Node vdt = (Node) passResults.lastNullableResultOf(VdtLoweringPass.class);
    if (vdt == null) {
      return null;
    }

    final List<vadl.vdt.utils.Instruction> entries;
    if (passResults.hasRunPassOnce(VdtConstraintSynthesisPass.class)) {
      entries = (List<vadl.vdt.utils.Instruction>)
          passResults.lastNullableResultOf(VdtConstraintSynthesisPass.class);
    } else {
      entries = (List<vadl.vdt.utils.Instruction>)
          passResults.lastNullableResultOf(VdtInputPreparationPass.class);
    }

    if (entries == null) {
      return null;
    }

    if (!Z3Bootstrap.init()) {
      // Issue a warning and proceed.
      var diagnostic = warning(
          "Unable to verify correctness of the constructed decoder due to missing "
              + "verification tool dependencies.", viam.location());
      DeferredDiagnosticStore.add(diagnostic);
      return null;
    }

    boolean hasError = false;

    // Verify soundness (no false-positives)
    try (Context ctx = new Context()) {

      final var soundCondGenerator = new DecisionTreeSoundnessVerifier(ctx, vdt, entries);
      final var conditions = soundCondGenerator.generateGuardedConditions();
      final BitVecExpr insnConst = soundCondGenerator.getInsnConst();

      final Solver solver = ctx.mkSolver();
      conditions.forEach(p -> solver.add(p.right()));

      for (var path : conditions) {
        hasError |= !checkSoundnessUnsat(solver, insnConst, path.left());
      }
    }

    // Verify completeness (no false-negatives)
    try (Context ctx = new Context()) {

      final var compCondGenerator = new DecisionTreeCompletenessVerifier(ctx, vdt, entries);
      final var conditions = compCondGenerator.generateGuardedConditions();
      final BitVecExpr insnConst = compCondGenerator.getInsnConst();

      final Solver solver = ctx.mkSolver();
      conditions.forEach(p -> solver.add(p.right()));

      for (var path : conditions) {
        hasError |= !checkCompletenessUnsat(solver, insnConst, path.left(), path.middle());
      }
    }

    if (hasError) {
      throw error("Invalid decoder generated. See additional errors for details.", viam)
          .build();
    }

    return null;
  }

  private static boolean checkSoundnessUnsat(Solver solver, BitVecExpr insn,
                                             PathVerificationInfo info) {
    final var assumption = info.leafCondition();

    final Status result = solver.check(assumption);

    if (result == Status.UNSATISFIABLE) {
      // All good
      return true;
    }

    if (result == Status.UNKNOWN) {
      var diagnostic = warning("Unable to verify encoding definitions.", info.leaf().source());
      DeferredDiagnosticStore.add(diagnostic);
      return true;
    }

    final Model model = solver.getModel();
    final FuncDecl<?>[] consts = model.getConstDecls();

    if (consts.length == 0) {
      // Should not happen, if it's SAT we must have model
      final var e = error("Unsound decoder generated.", info.leaf().source())
          .description("This error indicates an implementation error. Please try a "
              + "different generation strategy, and raise a bug report.");
      DeferredDiagnosticStore.add(e);
    }

    // Extract the encoding as counterexample
    final BitVecNum counterExample = (BitVecNum) solver.getModel().getConstInterp(insn);

    final Instruction loc = info.leaf().source();

    final var e = error(
        "Unsound decoder generated. This instruction is falsely selected for encoding 0x%x."
            .formatted(counterExample.getBigInteger()), loc)
        .description("This error indicates an implementation error. Please try a "
            + "different generation strategy, and raise a bug report.");
    DeferredDiagnosticStore.add(e);

    return false;
  }

  private static boolean checkCompletenessUnsat(Solver solver, BitVecExpr insn,
                                                Instruction viamInsn, BoolExpr assumption) {

    final Status result = solver.check(assumption);

    if (result == Status.UNSATISFIABLE) {
      // All good
      return true;
    }

    if (result == Status.UNKNOWN) {
      var diagnostic = warning("Unable to verify encoding definitions.", viamInsn);
      DeferredDiagnosticStore.add(diagnostic);
      return true;
    }

    final Model model = solver.getModel();
    final FuncDecl<?>[] consts = model.getConstDecls();

    if (consts.length == 0) {
      // Should not happen, if it's SAT we must have model
      final var e = error("Incomplete decoder generated.", viamInsn)
          .description("This error indicates an implementation error. Please try a "
              + "different generation strategy, and raise a bug report.");
      DeferredDiagnosticStore.add(e);
    }

    // Extract the encoding as counterexample
    final BitVecNum counterExample = (BitVecNum) solver.getModel().getConstInterp(insn);

    final var e = error(
        ("Incomplete decoder generated. It does not correctly select instruction %s for"
            + " encoding 0x%x.")
            .formatted(viamInsn.simpleName(), counterExample.getBigInteger()), viamInsn)
        .description("This error indicates an implementation error. Please try a "
            + "different generation strategy, and raise a bug report.");
    DeferredDiagnosticStore.add(e);

    return false;
  }
}
