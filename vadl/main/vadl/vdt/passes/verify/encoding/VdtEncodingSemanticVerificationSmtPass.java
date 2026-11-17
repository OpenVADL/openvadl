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

package vadl.vdt.passes.verify.encoding;

import static vadl.configuration.DecoderOptions.OptionalStep.OPT_ENCODING_VERIFICATION_SMT;
import static vadl.error.Diagnostic.warning;
import static vadl.vdt.passes.verify.encoding.Utils.dump;
import static vadl.vdt.passes.verify.encoding.Utils.overlappingInstructionError;

import com.microsoft.z3.BitVecExpr;
import com.microsoft.z3.BitVecNum;
import com.microsoft.z3.BitVecSort;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import io.github.rascmatt.z3.Z3Bootstrap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import vadl.configuration.DumpMode;
import vadl.configuration.GeneralConfiguration;
import vadl.error.DeferredDiagnosticStore;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.vdt.impl.irregular.model.DecodeEntry;
import vadl.vdt.passes.VdtConstraintSynthesisPass;
import vadl.vdt.passes.VdtInputPreparationPass;
import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.BitVector;
import vadl.viam.Specification;

/**
 * Verifies that the encoding definitions do not collide.
 */
public class VdtEncodingSemanticVerificationSmtPass extends Pass {

  /**
   * Constructor of the VDT constraint synthesis pass.
   *
   * @param configuration The configuration
   */
  public VdtEncodingSemanticVerificationSmtPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("VDT Encoding Verification SMT Pass");
  }

  @Nullable
  @Override
  @SuppressWarnings("unchecked")
  public Object execute(PassResults passResults, Specification viam) throws IOException {

    final List<DecodeEntry> entries;
    if (passResults.hasRunPassOnce(VdtConstraintSynthesisPass.class)) {
      entries =
          (List<DecodeEntry>) passResults.lastNullableResultOf(VdtConstraintSynthesisPass.class);
    } else {
      entries = (List<DecodeEntry>) passResults.lastNullableResultOf(VdtInputPreparationPass.class);
    }

    if (entries == null || entries.isEmpty()) {
      // just skip if there are no instructions.
      // this will only happen if we use the check command
      return null;
    }

    if (!Z3Bootstrap.init()) {
      // Issue a warning and proceed. We can still attempt to construct the decoder.
      var loc = entries.getFirst().source();
      var diagnostic =
          warning(
              "Unable to verify encoding definitions due to missing verification tool "
                  + "dependencies.", loc);
      DeferredDiagnosticStore.add(diagnostic);
      return null;
    }

    try (Context ctx = new Context()) {

      int maxWidth = entries.stream().mapToInt(DecodeEntry::width).max().orElse(0);
      BitVecSort sort = ctx.mkBitVecSort(maxWidth);
      BitVecExpr insn = (BitVecExpr) ctx.mkConst("insn", sort);

      List<BoolExpr> constraints = toConstraints(entries, ctx, insn);

      // Search for at least two constraints to be true at the same time
      int[] weights = IntStream.range(0, constraints.size()).map(i -> 1).toArray();
      BoolExpr[] constraintsArr = constraints.toArray(new BoolExpr[0]);
      BoolExpr atLeastTwo = ctx.mkPBGe(weights, constraintsArr, 2);

      final Solver solver = ctx.mkSolver();
      solver.add(atLeastTwo);

      if (configuration().doDump() || configuration().dumpMode() == DumpMode.VERIFICATION) {
        final String smt2 = solver + "\n(check-sat)\n";
        dump(configuration(), viam, smt2, "smt2");
      }

      if (configuration().getDecoderOptions().isDisabled(OPT_ENCODING_VERIFICATION_SMT)) {
        return null;
      }

      final Status result = solver.check();

      if (result == Status.UNSATISFIABLE) {
        return null;
      }

      if (result == Status.UNKNOWN) {
        var loc = entries.getFirst().source();
        var diagnostic = warning("Unable to verify encoding definitions.", loc);
        DeferredDiagnosticStore.add(diagnostic);
        return null;
      }

      final BitVecNum counterexample = (BitVecNum) solver.getModel().getConstInterp(insn);
      throw overlappingInstructionError(
          new BitVector(counterexample.getBigInteger(), counterexample.getSortSize()), entries);
    }
  }

  private List<BoolExpr> toConstraints(List<DecodeEntry> entries, Context ctx, BitVecExpr insn) {
    List<BoolExpr> constraints = new ArrayList<>();
    for (DecodeEntry instruction : entries) {

      final List<BoolExpr> c = new ArrayList<>();

      // Always add fixed bits as initial constraints
      c.add(
          match(ctx, insn, instruction.pattern())
      );

      // Now encode the different constraints
      instruction.exclusionConditions().forEach(ex -> {

        final List<BoolExpr> or = new ArrayList<>();
        or.add(notMatch(ctx, insn, ex.matching()));

        ex.unmatching().stream()
            .map(u -> match(ctx, insn, u))
            .forEach(or::add);

        c.add(
            ctx.mkOr(
                or.toArray(new BoolExpr[0])
            )
        );

      });

      if (c.size() == 1) {
        constraints.add(c.getFirst());
        continue;
      }

      constraints.add(ctx.mkAnd(c.toArray(new BoolExpr[0])));
    }

    return constraints;
  }

  private BoolExpr match(Context ctx, BitVecExpr i, BitPattern pattern) {

    // Pad the pattern to the sort width for comparison
    int sortWidth = i.getSortSize();
    pattern = pattern.rightPad(sortWidth - pattern.width());

    return ctx.mkEq(
        ctx.mkBVAND(i, ctx.mkBV(pattern.toMaskVector().value().toString(), sortWidth)),
        ctx.mkBV(pattern.toBitVector().value().toString(), sortWidth)
    );
  }

  private BoolExpr notMatch(Context ctx, BitVecExpr i, BitPattern pattern) {
    return ctx.mkNot(match(ctx, i, pattern));
  }

}

