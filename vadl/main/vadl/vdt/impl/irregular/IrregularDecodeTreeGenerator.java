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

package vadl.vdt.impl.irregular;

import static vadl.error.Diagnostic.error;
import static vadl.vdt.utils.BitPattern.fromBitVector;
import static vadl.vdt.utils.PatternUtils.combinePatterns;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import vadl.error.Diagnostic;
import vadl.vdt.impl.irregular.model.DecodeEntry;
import vadl.vdt.impl.irregular.model.ExclusionCondition;
import vadl.vdt.impl.irregular.tree.MultiDecisionNode;
import vadl.vdt.impl.irregular.tree.SingleDecisionNode;
import vadl.vdt.model.DecodeTreeGenerator;
import vadl.vdt.model.Node;
import vadl.vdt.model.impl.LeafNodeImpl;
import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.BitVector;
import vadl.vdt.utils.PBit;
import vadl.viam.Definition;

/**
 * Decode tree generator largely based on the Okuda et al. algorithm to generate a decode tree for
 * irregular instruction sets.
 *
 * <p>See: <a
 * href="https://past.date-conference.com/proceedings-archive/2016/pdf/0066.pdf">Decision Tree Generation for
 * Decoding Irregular Instructions (Katsumi Okuda, Haruhiko Takeyama)</a>
 *
 * <p>This algorithm supports exclusion conditions with matching and unmatching constraints in
 * the instruction entry set. As a result this generator is able to handle irregular instruction
 * sets, albeit the constraint conditions are quite limited.
 */
public class IrregularDecodeTreeGenerator implements DecodeTreeGenerator<DecodeEntry> {

  /**
   * Entry point for the decode tree generator. This method will prepare the input entries and
   * generate the decode tree.
   *
   * @param decodeEntries The entry set
   * @return The generated decode tree
   */
  @Override
  public Node generate(Collection<DecodeEntry> decodeEntries) {

    if (decodeEntries.isEmpty()) {
      throw new IllegalArgumentException("Entry set must not be empty");
    }

    DecodeEntries entries = toRelevantBits(BitPattern.empty(1), decodeEntries);

    // After padding (and truncating) to relevant bits, initialize the checked bits pattern, which
    // always considers the full possible width (without offset/truncate).
    BitPattern checkedBits =
        BitPattern.empty(entries.offset() + entries.entries().getFirst().width());
    entries = new DecodeEntries(entries.offset(), entries.length(), checkedBits, entries.entries());

    return generateInternal(entries);
  }

  /**
   * Top level method to generate the decode tree. This method will recursively build the tree
   * based on the input entries.
   *
   * @param decodeEntries The entry set
   * @return The generated decode (sub-) tree
   */
  private Node generateInternal(DecodeEntries decodeEntries) {

    if (decodeEntries.entries().size() != 1) {
      // Split the entry set
      return makeNode(decodeEntries);
    }

    // TODO: If the instruction has additional constraints, we should also check them here

    var entry = decodeEntries.entries().getFirst();
    var unchecked =
        getUncheckedBits(decodeEntries.checkedBits(), decodeEntries.offset(), entry.pattern());

    if (unchecked.toMaskVector().toValue().compareTo(BigInteger.ZERO) == 0) {
      // Nothing to, no remaining unchecked bits left
      return new LeafNodeImpl(entry);
    }

    // Add another condition on the (so far) unchecked bits
    return new SingleDecisionNode(0, unchecked.width(), unchecked,
        new LeafNodeImpl(entry), null);
  }

  private Node makeNode(DecodeEntries decodeEntries) {

    final MultiPatterns patterns = makePatterns(decodeEntries);

    if (!patterns.hasDecision()) {
      // Split entry set by exclusion conditions instead
      return makeConditionNode(decodeEntries);
    }

    final Map<BitPattern, Node> children = new HashMap<>();
    for (BitPattern p : patterns.patterns()) {
      final List<DecodeEntry> matchingEntries = makeMatchingEntries(decodeEntries.entries(), p);

      if (matchingEntries.isEmpty()) {
        continue;
      }

      final BitPattern checked =
          combinePatterns(decodeEntries.checkedBits(), decodeEntries.offset(), p);

      final DecodeEntries entries =
          toRelevantBits(decodeEntries.offset(), checked, matchingEntries);
      final Node childNode = generateInternal(entries);
      children.put(p, childNode);
    }

    return new MultiDecisionNode(decodeEntries.offset(), decodeEntries.length(), patterns.mask(),
        children);
  }

  private Node makeConditionNode(DecodeEntries decodeEntries) {

    // Select best splitting pattern based on exclusion conditions
    final BitPattern pattern = selectPattern(decodeEntries.entries());

    // Split the entry set
    final List<DecodeEntry> matching = makeMatchingEntries(decodeEntries.entries(), pattern);
    final List<DecodeEntry> others = makeOtherEntries(decodeEntries.entries(), pattern);

    // We can only consider the splitting pattern as 'checked' for the matching entries
    final BitPattern parentChecked = decodeEntries.checkedBits();
    final BitPattern checked = combinePatterns(parentChecked, decodeEntries.offset(), pattern);

    // Recursively build child-trees
    final DecodeEntries me = toRelevantBits(decodeEntries.offset(), checked, matching);
    final Node matchingChild = generateInternal(me);

    final DecodeEntries oe = toRelevantBits(decodeEntries.offset(), parentChecked, others);
    final Node otherChild = generateInternal(oe);

    return new SingleDecisionNode(decodeEntries.offset(), decodeEntries.length(), pattern,
        matchingChild, otherChild);
  }

  private MultiPatterns makePatterns(DecodeEntries decodeEntries) {

    List<DecodeEntry> entries = decodeEntries.entries();

    // Construct the bit mask to select the significant bits
    BitVector mask = entries.getFirst().pattern().toMaskVector();
    for (DecodeEntry e : entries) {
      mask = mask.and(e.pattern().toMaskVector());
    }

    // We don't need to check bits more than once
    BitVector checked = decodeEntries.checkedBits().toMaskVector()
        .truncate(decodeEntries.offset(), decodeEntries.length());
    mask = mask.xor(checked);

    final Set<BitPattern> options = new LinkedHashSet<>();
    for (DecodeEntry e : entries) {
      final BitVector b = e.pattern().toBitVector().and(mask);
      final BitPattern p = BitPattern.fromBitVector(mask, b);
      options.add(p);
    }

    return new MultiPatterns(mask, options);
  }

  private List<DecodeEntry> makeMatchingEntries(List<DecodeEntry> decodeEntries,
                                                BitPattern pattern) {

    // Step 1
    final List<DecodeEntry> matchingEntries = decodeEntries.stream()
        .filter(d -> match(d.pattern(), pattern))
        .filter(d -> d.exclusionConditions().stream()
            .noneMatch(c -> contain(pattern, c.matching())
                && c.unmatching().stream().noneMatch(p -> match(pattern, p))))
        .toList();

    // Step 2
    final List<DecodeEntry> matchingEntries2 = new ArrayList<>();
    for (DecodeEntry e : matchingEntries) {

      final Set<ExclusionCondition> ex = e.exclusionConditions().stream()
          .filter(c -> match(pattern, c.matching()) && c.unmatching().stream()
              .noneMatch(pu -> contain(pattern, pu)))
          .map(c -> {
            final Set<BitPattern> newUnmatching = c.unmatching().stream()
                .filter(pu -> match(pattern, pu))
                .collect(Collectors.toSet());
            return new ExclusionCondition(c.matching(), newUnmatching);
          })
          .collect(Collectors.toSet());

      matchingEntries2.add(new DecodeEntry(e.source(), e.width(), e.pattern(), ex));
    }

    // Step 3
    final List<DecodeEntry> matchingEntries3 = new ArrayList<>();
    for (DecodeEntry e : matchingEntries2) {
      final Set<ExclusionCondition> newExclusions = e.exclusionConditions().stream()
          .map(c -> {
            final BitPattern newMatching = invalidate(c.matching(), pattern);
            final Set<BitPattern> newUnmatching = c.unmatching().stream()
                .map(pu -> invalidate(pu, pattern))
                .collect(Collectors.toSet());
            return new ExclusionCondition(newMatching, newUnmatching);
          })
          .collect(Collectors.toSet());
      matchingEntries3.add(new DecodeEntry(e.source(), e.width(), e.pattern(), newExclusions));
    }

    // Step 4
    final List<DecodeEntry> matchingEntries4 = new ArrayList<>();
    for (DecodeEntry e : matchingEntries3) {

      if (e.exclusionConditions().isEmpty()) {
        matchingEntries4.add(e);
        continue;
      }

      if (e.exclusionConditions().stream().noneMatch(c -> c.matching().doesMatchAll())) {
        matchingEntries4.add(e);
        continue;
      }

      // Collect exclusions which do not become the match-all pattern
      final Set<ExclusionCondition> validExclusions = e.exclusionConditions().stream()
          .filter(c -> !c.matching().doesMatchAll())
          .collect(Collectors.toSet());

      // Expand unmatching conditions to their own decode entries
      e.exclusionConditions().stream()
          .filter(c -> c.matching().doesMatchAll())
          .flatMap(c -> c.unmatching().stream())
          .map(pu -> {
            final PBit[] newOpcodePattern = new PBit[e.width()];
            for (int i = 0; i < e.width(); i++) {
              newOpcodePattern[i] =
                  pu.get(i).getValue() == PBit.Value.DONT_CARE ? e.pattern().get(i) : pu.get(i);
            }
            final BitPattern po = new BitPattern(newOpcodePattern);
            return new DecodeEntry(e.source(), e.width(), po, validExclusions);
          })
          .forEach(matchingEntries4::add);
    }

    return matchingEntries4;
  }

  private List<DecodeEntry> makeOtherEntries(List<DecodeEntry> decodeEntries,
                                             BitPattern pattern) {
    // Step 1
    final List<DecodeEntry> otherEntries = decodeEntries.stream()
        .filter(d -> !contain(d.pattern(), pattern))
        .toList();

    // Step 2
    final List<DecodeEntry> otherEntries2 = new ArrayList<>();
    for (DecodeEntry e : otherEntries) {

      final Set<ExclusionCondition> newExclusions = e.exclusionConditions().stream()
          .filter(c -> !contain(c.matching(), pattern))
          .map(c -> {
            final Set<BitPattern> newUnmatching = c.unmatching().stream()
                .filter(pu -> !contain(pu, pattern))
                .collect(Collectors.toSet());
            return new ExclusionCondition(c.matching(), newUnmatching);
          })
          .collect(Collectors.toSet());

      otherEntries2.add(new DecodeEntry(e.source(), e.width(), e.pattern(), newExclusions));
    }

    return otherEntries2;
  }

  private BitPattern selectPattern(List<DecodeEntry> decodeEntries) {

    final Set<BitPattern> patternCandidates = decodeEntries.stream()
        .flatMap(e -> e.exclusionConditions().stream())
        .map(ExclusionCondition::matching)
        .collect(Collectors.toSet());

    if (patternCandidates.isEmpty()) {
      throw toOverlappingInstructionDiagnostic(decodeEntries);
    }

    int bestSplit = Integer.MAX_VALUE;
    BitPattern minimizingPattern = null;

    for (BitPattern p : patternCandidates) {
      var matching = makeMatchingEntries(decodeEntries, p);
      var others = makeOtherEntries(decodeEntries, p);

      // If the candidate pattern does not split the entry set at all, skip it
      if (matching.isEmpty() || others.isEmpty()) {
        continue;
      }

      var splitSize = matching.size() + others.size();

      if (bestSplit <= splitSize) {
        continue;
      }

      bestSplit = splitSize;
      minimizingPattern = p;
    }

    if (minimizingPattern == null) {
      throw toOverlappingInstructionDiagnostic(decodeEntries);
    }

    return minimizingPattern;
  }

  private Diagnostic toOverlappingInstructionDiagnostic(List<DecodeEntry> decodeEntries) {
    var primary = decodeEntries.getFirst().source();
    var insnNames = decodeEntries.stream()
        .map(DecodeEntry::source)
        .map(Definition::simpleName)
        .toList();

    var diagnostic = error(("Overlapping instructions found during decoder "
        + "generation: %s").formatted(insnNames), primary);

    for (DecodeEntry e : decodeEntries) {
      var others = insnNames.stream()
          .filter(n -> !n.equals(e.source().simpleName())).toList();

      diagnostic.locationDescription(e.source().encoding(),
          "Encoding definition overlaps with other instruction%s: %s",
          others.size() != 1 ? "s" : "",
          others.size() == 1 ? others.getFirst() : others);
    }

    return diagnostic.build();
  }

  private boolean match(BitPattern p1, BitPattern p2) {
    return IntStream.range(0, p1.width())
        .allMatch(
            i -> p1.get(i).equals(p2.get(i)) || p1.get(i).getValue() == PBit.Value.DONT_CARE
                || p2.get(i).getValue() == PBit.Value.DONT_CARE);
  }

  private boolean contain(BitPattern p1, BitPattern p2) {
    return IntStream.range(0, p1.width())
        .allMatch(
            i -> p1.get(i).equals(p2.get(i)) || p2.get(i).getValue() == PBit.Value.DONT_CARE);
  }

  private BitPattern invalidate(BitPattern p, BitPattern inputPattern) {
    final PBit[] bits = new PBit[inputPattern.width()];
    for (int i = 0; i < inputPattern.width(); i++) {
      bits[i] = inputPattern.get(i).getValue() == PBit.Value.DONT_CARE ? p.get(i) :
          new PBit(PBit.Value.DONT_CARE);
    }
    return new BitPattern(bits);
  }

  /**
   * Prepare the input decode entries for the generator. This includes padding the patterns to the
   * same width and truncating them to the overall relevant region.
   *
   * @param checkedBits   The bits already checked by the algorithm (i.e. fixed for the entry set)
   * @param decodeEntries The entry set
   * @return The relevant region to truncate to and the prepared entry set
   */
  private DecodeEntries toRelevantBits(BitPattern checkedBits,
                                       Collection<DecodeEntry> decodeEntries) {

    // Pad all patterns to the maximum occurring width
    final int maxWidth = decodeEntries.stream()
        .mapToInt(DecodeEntry::width)
        .max()
        .orElseThrow(() -> new IllegalArgumentException("Empty entry set"));

    // Pad all patterns to the same width
    List<DecodeEntry> entries = decodeEntries.stream()
        .map(e -> transform(e, p -> p.rightPad(maxWidth - p.width())))
        .toList();

    // Collect all involved patterns, including the condition patterns
    final Set<BitPattern> allPatterns = entries.stream()
        .flatMap(e -> Stream.concat(
            Stream.of(e.pattern()),
            e.exclusionConditions().stream()
                .flatMap(c -> Stream.concat(Stream.of(c.matching()),
                    c.unmatching().stream()))))
        .collect(Collectors.toSet());

    // Determine the relevant region for decoding
    final int fromIdx = allPatterns.stream()
        .mapToInt(this::getFirstRelevantIdx)
        .min()
        .orElse(0);

    final int toIdx = allPatterns.stream()
        .mapToInt(this::getLastRelevantIdx)
        .max()
        .orElse(fromIdx);

    if (toIdx < fromIdx) {
      throw new IllegalArgumentException("Invalid entry set: " + entries);
    }

    // Slice all patterns to the region relevant for decoding
    entries = entries.stream()
        .map(e -> transform(e, p -> slice(p, fromIdx, toIdx)))
        .collect(Collectors.toList());

    return new DecodeEntries(fromIdx, (toIdx + 1) - fromIdx, checkedBits, entries);
  }

  private DecodeEntries toRelevantBits(int parentOffset, BitPattern checkedBits,
                                       Collection<DecodeEntry> decodeEntries) {

    // Determine and transform the input patterns to only consider decoding relevant bits
    final var res = toRelevantBits(checkedBits, decodeEntries);

    // Convert the relative offset to an absolute one
    return new DecodeEntries(parentOffset + res.offset(), res.length(), checkedBits, res.entries());
  }

  private int getFirstRelevantIdx(BitPattern pattern) {
    for (int i = 0; i < pattern.width(); i++) {
      if (pattern.get(i).getValue() != PBit.Value.DONT_CARE) {
        return i;
      }
    }
    return pattern.width();
  }

  private int getLastRelevantIdx(BitPattern pattern) {
    for (int i = pattern.width() - 1; i >= 0; i--) {
      if (pattern.get(i).getValue() != PBit.Value.DONT_CARE) {
        return i;
      }
    }
    return 0;
  }

  private DecodeEntry transform(DecodeEntry entry, Function<BitPattern, BitPattern> transformer) {

    final BitPattern pattern = entry.pattern();
    final BitPattern transformedPattern = transformer.apply(pattern);

    final Set<ExclusionCondition> transformedExclusions = entry.exclusionConditions().stream()
        .map(c -> new ExclusionCondition(transformer.apply(c.matching()),
            c.unmatching().stream()
                .map(transformer)
                .collect(Collectors.toSet())))
        .collect(Collectors.toSet());

    return new DecodeEntry(entry.source(), transformedPattern.width(), transformedPattern,
        transformedExclusions);
  }

  private BitPattern slice(BitPattern pattern, int from, int to) {
    final PBit[] bits = new PBit[to - from + 1];
    for (int i = from; i <= to; i++) {
      bits[i - from] = pattern.get(i);
    }
    return new BitPattern(bits);
  }

  private BitPattern getUncheckedBits(BitPattern checkedBits, int offset, BitPattern insn) {

    final BitPattern aligned = insn.leftPad(offset)
        .rightPad(checkedBits.width() - offset - insn.width());

    // Allow to check more bits than required by the instruction pattern, which may be required for
    // checking constraints.
    final BitVector mask = aligned.toMaskVector().xor(checkedBits.toMaskVector())
        .and(aligned.toMaskVector());
    if (mask.toValue().compareTo(BigInteger.ZERO) == 0) {
      return BitPattern.empty(insn.width());
    }

    return fromBitVector(mask, aligned.toBitVector());
  }

  /**
   * Encapsulate the decode entry set, only considering the region specified by offset and length.
   *
   * @param offset      The offset in bits given the instruction word to decode
   * @param length      The length of the relevant bit-region considered for decoding
   * @param checkedBits The bits already checked by the algorithm (not sliced to the offset/width)
   * @param entries     The decode entries and patterns, truncated to the specified offset and width
   */
  private record DecodeEntries(int offset, int length, BitPattern checkedBits,
                               List<DecodeEntry> entries) {

  }

  private record MultiPatterns(BitVector mask, Set<BitPattern> patterns) {

    boolean hasDecision() {
      return patterns.size() > 1;
    }
  }
}
