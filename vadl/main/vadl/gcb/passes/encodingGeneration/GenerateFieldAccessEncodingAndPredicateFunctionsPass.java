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

package vadl.gcb.passes.encodingGeneration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import vadl.configuration.GcbConfiguration;
import vadl.error.DeferredDiagnosticStore;
import vadl.error.Diagnostic;
import vadl.gcb.passes.encodingGeneration.strategies.EncodingPredicateGenerationStrategy;
import vadl.gcb.passes.encodingGeneration.strategies.impl.ArithmeticImmediateEncodingPredicateStrategy;
import vadl.gcb.passes.encodingGeneration.strategies.impl.ShiftedImmediateEncodingPredicateStrategy;
import vadl.gcb.passes.encodingGeneration.strategies.impl.TrivialImmediateEncodingPredicateStrategy;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.viam.Format.FieldAccess;
import vadl.viam.Specification;

/**
 * This pass generates the {@link vadl.viam.Format.FieldEncoding} when there is no encoding
 * for a {@link FieldAccess} defined.
 * <pre>{@code
 * format Utype : Inst =
 * { imm    : Bits<20>
 * , rd     : Index
 * , opcode : Bits7
 * , immU = imm as UInt<32>
 * }
 * }</pre>
 * This class should compute the following encoding function automatically:
 * <pre>{@code
 * encode {
 * imm => immU(19..0)
 * }
 * }</pre>
 */
public class GenerateFieldAccessEncodingAndPredicateFunctionsPass extends Pass {

  public static final List<EncodingPredicateGenerationStrategy> strategies = List.of(
      new TrivialImmediateEncodingPredicateStrategy(),
      new ShiftedImmediateEncodingPredicateStrategy(),
      new ArithmeticImmediateEncodingPredicateStrategy());

  public GenerateFieldAccessEncodingAndPredicateFunctionsPass(GcbConfiguration gcbConfiguration) {
    super(gcbConfiguration);
  }

  @Override
  public PassName getName() {
    return new PassName("GenerateFieldAccessEncodingFunctionPass");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) {
    for (var instruction : viam.isa().orElseThrow().ownInstructions()) {
      var format = instruction.format();
      for (var fieldAccess : instruction.format().fieldAccesses()) {
        if (format.fieldEncodingsOf(Set.of(fieldAccess)).isEmpty()) {
          // We need to compute multiple encoding functions based on the field access function.
          // Different field access functions require different heuristics for the encoding.
          for (var strategy : strategies) {
            if (strategy.checkIfApplicable(fieldAccess)) {
              strategy.generateEncodingAndPredicateFunction(instruction, fieldAccess);
              break;
            }
          }
        }
      }
    }

    // Every field access function requires a predicate function. If there aren't any,
    // or it couldn't be automatically generated then throw an error.
    for (var format : viam.isa().orElseThrow().ownFormats()) {
      var fieldAccesses = new HashSet<>(format.fieldAccesses());
      for (var fieldAccess : format.fieldAccesses()) {
        if (fieldAccess.predicate() != null) {
          fieldAccesses.remove(fieldAccess);
        }
      }

      for (var leftOverFieldAccess : fieldAccesses) {
        DeferredDiagnosticStore.add(
            Diagnostic.error("Field access function has no predicate.",
                leftOverFieldAccess.location()));
      }
    }

    return null;
  }
}
