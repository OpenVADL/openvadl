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

import static vadl.vdt.utils.PatternUtils.invalidate;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

    // Prepare a lookup index for possible constraint synthesis
    final var index = indexInstructions(entries);

    return entries
        .stream()
        .map(entry -> {

          if (!entry.exclusionConditions().isEmpty()) {
            // Only attempt to synthesize constraints if no manual constraints are specified.
            return entry;
          }

          var constraints = getSynthesizedExclusions(index, entry);
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
   * @param indexByFormat The instruction encodings indexed by format
   * @param e             The current instruction to consider
   * @return The exclusion conditions, if any.
   */
  private Set<ExclusionCondition> getSynthesizedExclusions(
      Map<Format, FormatIndex> indexByFormat, DecodeEntry e) {

    final var format = e.source().format();
    final var formatIdx = indexByFormat.get(format);

    if (formatIdx == null) {
      return Set.of();
    }

    final Set<FixedEncoding> fieldEncodings = getFixedFields(e.source());

    // Find possible candidates, i.e. instructions that encode all fixed fields of the current
    // instruction (and possibly more)

    BitSet candidates = null;
    if (fieldEncodings.isEmpty()) {
      // All encodings are candidates
      candidates = new BitSet(formatIdx.insns().size());
      candidates.set(0, formatIdx.insns().size());
    } else {
      for (var fixedEncoding : fieldEncodings) {

        final BitSet matching = formatIdx.instructionsByFixedEncoding().get(fixedEncoding);
        if (matching == null) {
          return Set.of();
        }

        if (candidates == null) {
          candidates = (BitSet) matching.clone();
        } else {
          candidates.and(matching);
        }
      }
    }

    if (candidates == null || candidates.isEmpty()) {
      return Set.of();
    }

    final Set<ExclusionCondition> constraints = new LinkedHashSet<>();

    for (int i = candidates.nextSetBit(0); i >= 0; i = candidates.nextSetBit(i + 1)) {

      final var insn = formatIdx.insns().get(i);
      if (insn.fixedFields().size() <= fieldEncodings.size()) {
        // We only allow strict supersets of the current instruction's fixed fields
        continue;
      }

      // Found a subsumed instruction, so we generate an exclusion for it
      final var exclusionPattern = invalidate(insn.entry().pattern(), e.pattern());
      constraints.add(new ExclusionCondition(exclusionPattern, Set.of()));
    }

    return constraints;
  }

  private Map<Format, FormatIndex> indexInstructions(List<DecodeEntry> entries) {
    final Map<Format, FormatIndex> result = new LinkedHashMap<>();

    for (DecodeEntry e : entries) {
      final var formatIndex = result.computeIfAbsent(e.source().format(),
          k -> new FormatIndex(new ArrayList<>(), new LinkedHashMap<>()));

      final var fields = getFixedFields(e.source());

      final var insn = new IndexedInstruction(e, fields);
      formatIndex.insns().add(insn);

      for (var fixedEncoding : fields) {
        formatIndex.instructionsByFixedEncoding()
            .computeIfAbsent(fixedEncoding, k -> new BitSet())
            .set(formatIndex.insns().size() - 1);
      }
    }

    return result;
  }

  private Set<FixedEncoding> getFixedFields(Instruction insn) {
    return Arrays.stream(insn.encoding().fieldEncodings())
        .map(e -> new FixedEncoding(e.formatField(),
            e.constant().trivialCastTo(e.formatField().type()).unsignedInteger()))
        .collect(Collectors.toSet());
  }

  private record FixedEncoding(Format.Field field, BigInteger value) {
  }

  private record IndexedInstruction(DecodeEntry entry, Set<FixedEncoding> fixedFields) {
  }

  private record FormatIndex(List<IndexedInstruction> insns,
                             Map<FixedEncoding, BitSet> instructionsByFixedEncoding) {
  }
}
