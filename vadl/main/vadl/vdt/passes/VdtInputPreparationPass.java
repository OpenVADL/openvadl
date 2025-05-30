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

import static vadl.vdt.utils.PatternUtils.toFixedBitPattern;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.vdt.impl.irregular.model.DecodeEntry;
import vadl.vdt.impl.irregular.model.ExclusionCondition;
import vadl.vdt.utils.BitPattern;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.Specification;

/**
 * Prepares the instruction definitions for the VDT generation.
 */
public class VdtInputPreparationPass extends Pass {

  /**
   * Constructor of the VDT preparation pass.
   *
   * @param configuration The configuration
   */
  public VdtInputPreparationPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("VDT Input Preparation");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {

    var isa = viam.isa().orElse(null);
    if (isa == null) {
      return null;
    }

    if (isa.ownInstructions().isEmpty()) {
      // just skip if there are no instructions.
      // this will only happen if we use the check command
      return null;
    }

    // TODO: get the byte order from the VADL specification -> Implement memory annotations
    final ByteOrder bo = ByteOrder.LITTLE_ENDIAN;

    // Prepare a lookup map for possible constraint synthesis
    final var grouped = groupedInstructions(isa.ownInstructions());

    return isa.ownInstructions()
        .stream()
        .map(i -> {
          final var pattern = toFixedBitPattern(i, bo);
          final var constraints = getSynthesizedExclusions(bo, grouped, i);
          return new DecodeEntry(i, pattern.width(), pattern, constraints);
        })
        .toList();
  }

  /**
   * Synthesize possible exclusion conditions. For now this considers subsumed instructions within
   * the same format definition. E.g.: If instruction {@code A} specifies fixed encodings of format
   * fields {@code a = 0}, {@code b = 1} and instruction {@code B} (of the same format) specifies
   * fixed encodings of format field {@code a = 0}, then we will automatically add a constraint to
   * {@code B} to exclude the encoding of {@code b = 1 }.
   * <br>
   * As a result, if {@code b == 1} the decoder matches instruction {@code A} and in any other case
   * it will match instruction {@code B}.
   *
   * @param bo      The byte order
   * @param grouped The instruction encodings grouped by format
   * @param i       The current instruction to consider
   * @return The exclusion conditions, if any.
   */
  private Set<ExclusionCondition> getSynthesizedExclusions(
      ByteOrder bo, Map<Format, List<InstructionEncoding>> grouped, Instruction i) {

    if (!grouped.containsKey(i.format())) {
      return Set.of();
    }

    final Set<ExclusionCondition> constraints = new HashSet<>();
    final Set<FixedEncoding> encodedFields = getFixedFields(i);

    grouped.get(i.format()).stream()
        .filter(p -> isActualSubset(p.fields(), encodedFields))
        .forEach(p -> {

          // Determine the encodings to exclude
          var exclusions = new HashSet<>(p.fields());
          exclusions.removeAll(encodedFields);

          // Add one exclusion condition per encoding
          for (var encoding : exclusions) {
            BitPattern exclusionPattern = toFixedBitPattern(encoding.field(), encoding.value(), bo);
            constraints.add(new ExclusionCondition(exclusionPattern, Set.of()));
          }
        });

    return constraints;
  }

  private boolean isActualSubset(Set<FixedEncoding> superset,
                                 Set<FixedEncoding> subset) {
    return superset.containsAll(subset) && superset.size() > subset.size();
  }

  private Map<Format, List<InstructionEncoding>> groupedInstructions(List<Instruction> insns) {
    final Map<Format, List<InstructionEncoding>> result = new HashMap<>();

    for (Instruction i : insns) {
      var encoding = new InstructionEncoding(i, getFixedFields(i));

      result.merge(i.format(), List.of(encoding), (a, b) -> {
        var merged = new ArrayList<>(a);
        merged.addAll(b);
        return merged;
      });
    }

    return result;
  }

  private Set<FixedEncoding> getFixedFields(Instruction insn) {
    return Arrays.stream(insn.encoding().fieldEncodings())
        .map(e -> new FixedEncoding(e.formatField(), e.constant()))
        .collect(Collectors.toSet());
  }

  private record FixedEncoding(Format.Field field, Constant.Value value) {
  }

  private record InstructionEncoding(Instruction source, Set<FixedEncoding> fields) {
  }
}
