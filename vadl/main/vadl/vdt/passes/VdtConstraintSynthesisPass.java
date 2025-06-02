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
import vadl.vdt.utils.PatternUtils;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.Specification;

/**
 * Attempts to synthesize some additional constraints, if possible.
 */
public class VdtConstraintSynthesisPass extends Pass {

  /**
   * Constructor of the VDT constraint synthesis pass.
   *
   * @param configuration The configuration
   */
  public VdtConstraintSynthesisPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("VDT Constraint Synthesis");
  }

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam) throws IOException {

    final List<DecodeEntry> entries =
        (List<DecodeEntry>) passResults.lastNullableResultOf(VdtInputPreparationPass.class);

    if (entries == null) {
      return null;
    }

    // TODO: get the byte order from the VADL specification -> Implement memory annotations
    final ByteOrder bo = ByteOrder.LITTLE_ENDIAN;

    // Prepare a lookup map for possible constraint synthesis
    final var grouped = groupedInstructions(entries);

    return entries
        .stream()
        .map(entry -> {

          if (!entry.exclusionConditions().isEmpty()) {
            // Only attempt to synthesize constraints if no manual constraints are specified.
            return entry;
          }

          var constraints = getSynthesizedExclusions(bo, grouped, entry);
          return new DecodeEntry(entry.source(), entry.width(), entry.pattern(), constraints);
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
   * @param e       The current instruction to consider
   * @return The exclusion conditions, if any.
   */
  private Set<ExclusionCondition> getSynthesizedExclusions(
      ByteOrder bo, Map<Format, List<InstructionEncoding>> grouped, DecodeEntry e) {

    if (!grouped.containsKey(e.source().format())) {
      return Set.of();
    }

    final Set<ExclusionCondition> constraints = new HashSet<>();
    final Set<FixedEncoding> encodedFields = getFixedFields(e.source());

    grouped.get(e.source().format()).stream()
        // TODO: Possibly exclude supersets with constrains of their owns
        .filter(p -> isActualSubset(p.fields(), encodedFields))
        .forEach(p -> {

          // Determine the encodings to exclude
          var exclusions = new HashSet<>(p.fields());
          exclusions.removeAll(encodedFields);

          BitPattern exclusionPattern = BitPattern.empty(e.width());
          for (var encoding : exclusions) {
            BitPattern fieldPattern = toFixedBitPattern(encoding.field(), encoding.value(), bo);
            exclusionPattern = PatternUtils.combinePatterns(exclusionPattern, fieldPattern);
          }

          constraints.add(new ExclusionCondition(exclusionPattern, Set.of()));
        });

    return constraints;
  }

  private boolean isActualSubset(Set<FixedEncoding> superset,
                                 Set<FixedEncoding> subset) {
    return superset.containsAll(subset) && superset.size() > subset.size();
  }

  private Map<Format, List<InstructionEncoding>> groupedInstructions(List<DecodeEntry> entries) {
    final Map<Format, List<InstructionEncoding>> result = new HashMap<>();

    for (DecodeEntry e : entries) {
      var encoding = new InstructionEncoding(e, getFixedFields(e.source()));

      result.merge(e.source().format(), List.of(encoding), (a, b) -> {
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

  private record InstructionEncoding(DecodeEntry entry, Set<FixedEncoding> fields) {
  }
}
