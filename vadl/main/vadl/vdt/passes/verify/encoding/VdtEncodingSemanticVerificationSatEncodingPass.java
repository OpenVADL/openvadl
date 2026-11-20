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

import static vadl.vdt.passes.verify.encoding.Utils.dump;
import static vadl.vdt.utils.BitPattern.PatternBit.ONE;
import static vadl.vdt.utils.BitPattern.PatternBit.ZERO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import vadl.configuration.DumpMode;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.vdt.impl.irregular.model.DecodeEntry;
import vadl.vdt.impl.irregular.model.ExclusionCondition;
import vadl.vdt.passes.VdtConstraintSynthesisPass;
import vadl.vdt.passes.VdtInputPreparationPass;
import vadl.vdt.passes.verify.encoding.model.Clause;
import vadl.vdt.passes.verify.encoding.model.CnfEncoding;
import vadl.vdt.passes.verify.encoding.model.VariablePool;
import vadl.vdt.utils.BitPattern;
import vadl.viam.Specification;

/**
 * Constructs a SAT encoding to verify that instruction encoding definitions do not collide.
 */
public class VdtEncodingSemanticVerificationSatEncodingPass extends Pass {

  /**
   * Constructor of the VDT constraint synthesis pass.
   *
   * @param configuration The configuration
   */
  public VdtEncodingSemanticVerificationSatEncodingPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("VDT Encoding Verification SAT Encoding Pass");
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

    // We keep our own CNF representation, so we can directly output DIMACS.
    final CnfEncoding cnf = encodeToCnf(entries);

    if (configuration().doDump() || configuration().dumpMode() == DumpMode.VERIFICATION) {
      dump(configuration(), viam, cnf.toDimacs(), "dimacs");
    }

    return cnf;
  }

  private CnfEncoding encodeToCnf(List<DecodeEntry> entries) {

    final int maxWidth = entries.stream()
        .mapToInt(DecodeEntry::width)
        .max().orElse(0);

    final VariablePool pool = new VariablePool();
    final List<Clause> clauses = new ArrayList<>();

    final List<Integer> bits = IntStream.range(0, maxWidth)
        .mapToObj(i -> pool.id(new AuxBit(i)))
        .toList();

    final List<Integer> insnSelectors = new ArrayList<>();

    for (DecodeEntry entry : entries) {
      final var result = entryToConstraints(entry, bits, pool);
      clauses.addAll(result.clauses());
      insnSelectors.add(result.selector());
    }

    // Add an at-least-two constraint for the selectors, to be able to directly dump the CNF
    // formula, we construct our own cardinality constraint (rather than native Z3 PBC)

    // Sequential counter
    final int bound = 2;
    for (int i = 0; i <= insnSelectors.size(); i++) {
      clauses.add(new Clause(pool.id(new AuxS(i, 0))));
    }
    for (int j = 1; j <= bound; j++) {
      clauses.add(new Clause(-pool.id(new AuxS(0, j))));
    }
    for (int i = 1; i <= insnSelectors.size(); i++) {
      for (int j = 1; j <= bound; j++) {
        clauses.add(new Clause(
            pool.id(new AuxS(i - 1, j - 1)),
            pool.id(new AuxS(i - 1, j)),
            -pool.id(new AuxS(i, j))
        ));
        clauses.add(new Clause(
            pool.id(new AuxS(i - 1, j)),
            insnSelectors.get(i - 1),
            -pool.id(new AuxS(i, j))
        ));
      }
    }
    clauses.add(new Clause(pool.id(new AuxS(insnSelectors.size(), bound))));

    return new CnfEncoding(entries, pool, bits, clauses);
  }

  private SingleInsnConstraints entryToConstraints(DecodeEntry entry, List<Integer> bits,
                                                   VariablePool pool) {

    final List<Clause> clauses = new ArrayList<>();
    final int insnSelector = pool.id(new AuxInsn(entry.source().simpleName()));

    // Encode fixed-bit pattern

    // Pad bit vectors and patterns to sort width
    final int width = bits.size();
    final BitPattern pattern = entry.pattern().rightPad(width - entry.pattern().width());
    for (int i = 0; i < bits.size(); i++) {
      if (pattern.get(i) == ONE) {
        clauses.add(new Clause(-insnSelector, bits.get(i)));
      } else if (pattern.get(i) == ZERO) {
        clauses.add(new Clause(-insnSelector, -bits.get(i)));
      }
    }

    for (ExclusionCondition ex : entry.exclusionConditions()) {

      Clause exClause = new Clause(-insnSelector);

      BitPattern m = ex.matching().rightPad(width - ex.matching().width());
      for (int i = 0; i < bits.size(); i++) {
        if (m.get(i) == ONE) {
          exClause = exClause.extend(-bits.get(i));
        } else if (m.get(i) == ZERO) {
          exClause = exClause.extend(bits.get(i));
        }
      }

      for (BitPattern inc : ex.unmatching()) {
        BitPattern u = inc.rightPad(width - inc.width());
        int umSelector = pool.next();

        exClause = exClause.extend(umSelector);

        for (int i = 0; i < bits.size(); i++) {
          if (u.get(i) == ONE) {
            clauses.add(new Clause(-umSelector, bits.get(i)));
          } else if (u.get(i) == ZERO) {
            clauses.add(new Clause(-umSelector, -bits.get(i)));
          }
        }
      }

      clauses.add(exClause);
    }

    return new SingleInsnConstraints(clauses, insnSelector);
  }

  private record SingleInsnConstraints(List<Clause> clauses, int selector) {
  }

  private record AuxBit(int i) {
  }

  private record AuxInsn(String name) {

  }

  private record AuxS(int i, int j) {
  }

}

