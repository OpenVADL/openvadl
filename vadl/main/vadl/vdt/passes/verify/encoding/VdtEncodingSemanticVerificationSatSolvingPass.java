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

import static vadl.error.Diagnostic.warning;
import static vadl.vdt.passes.verify.encoding.Utils.overlappingInstructionError;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import io.github.rascmatt.z3.Z3Bootstrap;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.error.DeferredDiagnosticStore;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.vdt.impl.irregular.model.DecodeEntry;
import vadl.vdt.passes.verify.encoding.model.Clause;
import vadl.vdt.passes.verify.encoding.model.CnfEncoding;
import vadl.vdt.utils.BitVector;
import vadl.viam.Specification;

/**
 * Verifies that the encoding definitions do not collide using the bundled Z3 solver.
 */
public class VdtEncodingSemanticVerificationSatSolvingPass extends Pass {

  /**
   * Constructor of the VDT encoding verification pass.
   *
   * @param configuration The configuration
   */
  public VdtEncodingSemanticVerificationSatSolvingPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("VDT Encoding Verification SAT Solving Pass");
  }

  @Nullable
  @Override
  @SuppressWarnings("unchecked")
  public Object execute(PassResults passResults, Specification viam) throws IOException {

    final CnfEncoding cnfEncoding =
        passResults.lastResultOf(VdtEncodingSemanticVerificationSatEncodingPass.class,
            CnfEncoding.class);

    if (!Z3Bootstrap.init()) {
      // Issue a warning and proceed. We can still attempt to construct the decoder.
      var diagnostic =
          warning(
              "Unable to verify encoding definitions due to missing verification tool "
                  + "dependencies.", cnfEncoding.entries().getFirst().source());
      DeferredDiagnosticStore.add(diagnostic);
      return null;
    }

    // Solve with the bundled Z3
    verifyWithZ3(cnfEncoding);

    return null;
  }

  private void verifyWithZ3(CnfEncoding cnf) {
    try (Context ctx = new Context()) {

      final Solver solver = ctx.mkSolver(ctx.mkTactic("sat"));

      final Map<Integer, BoolExpr> variables = new HashMap<>();

      for (Clause clause : cnf.clauses()) {
        final BoolExpr[] literals = clause.literals().stream()
            .map(l -> {
              final BoolExpr b =
                  variables.computeIfAbsent(l > 0 ? l : -l, k -> ctx.mkBoolConst("v" + k));
              return l > 0 ? b : ctx.mkNot(b);
            })
            .toArray(BoolExpr[]::new);
        solver.add(ctx.mkOr(literals));
      }

      final Status result = solver.check();

      if (result == Status.UNSATISFIABLE) {
        return;
      }

      if (result == Status.UNKNOWN) {
        var loc = cnf.entries().getFirst().source();
        var diagnostic = warning("Unable to verify encoding definitions.", loc);
        DeferredDiagnosticStore.add(diagnostic);
        return;
      }

      final var model = solver.getModel();

      final int maxWidth = cnf.entries().stream()
          .mapToInt(DecodeEntry::width)
          .max().orElse(0);

      BigInteger encoding = BigInteger.ZERO;
      for (int i = 0; i < cnf.bitVariables().size(); i++) {
        final BoolExpr variable = variables.get(cnf.bitVariables().get(i));
        if (model.eval(variable, true).isTrue()) {
          // BitVariables start from MSB
          encoding = encoding.setBit(maxWidth - 1 - i);
        }
      }

      throw overlappingInstructionError(new BitVector(encoding, maxWidth), cnf.entries());
    }
  }

}

