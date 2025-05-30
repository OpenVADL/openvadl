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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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

    final DecodeEntries entries = toRelevantBits(decodeEntries);
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

    if (decodeEntries.entries().size() == 1) {
      return new LeafNodeImpl(decodeEntries.entries().getFirst());
    }

    return makeNode(decodeEntries);
  }

  private Node makeNode(DecodeEntries decodeEntries) {

    final MultiPatterns patterns = makePatterns(decodeEntries.entries());

    if (patterns.isEmpty()) {
      // Split entry set by exclusion conditions instead
      return makeConditionNode(decodeEntries);
    }

    final Map<BitPattern, Node> children = new HashMap<>();
    for (BitPattern p : patterns.patterns()) {
      final List<DecodeEntry> matchingEntries = makeMatchingEntries(decodeEntries.entries(), p);
      if (matchingEntries.isEmpty()) {
        continue;
      }

      final DecodeEntries entries = toRelevantBits(decodeEntries.offset(), matchingEntries);
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

    // Recursively build child-trees
    final DecodeEntries me = toRelevantBits(decodeEntries.offset(), matching);
    final Node matchingChild = generateInternal(me);

    final DecodeEntries oe = toRelevantBits(decodeEntries.offset(), others);
    final Node otherChild = generateInternal(oe);

    return new SingleDecisionNode(decodeEntries.offset(), decodeEntries.length(), pattern,
        matchingChild, otherChild);
  }

  private MultiPatterns makePatterns(List<DecodeEntry> decodeEntries) {
    // Determine the significant bits, i.e. ones that split the entry set
    final boolean[] significantBits = getSignificantBits(decodeEntries);

    // Return the bit mask to select the significant bits
    final BitVector mask = new BitVector(significantBits);

    // Enumerate all options
    var options = enumerate(significantBits)
        .stream()
        .map(sb -> BitPattern.fromString(sb.toString(), sb.length()))
        .filter(p -> !p.doesMatchAll())
        .toList();

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

  private static boolean[] getSignificantBits(List<DecodeEntry> decodeEntries) {
    final int width = decodeEntries.getFirst().width();
    final boolean[] significantBits = new boolean[width];

    for (int i = 0; i < width; i++) {
      boolean wildcard = false;
      boolean zero = false;
      boolean one = false;
      for (DecodeEntry d : decodeEntries) {
        PBit bit = d.pattern().get(i);
        switch (bit.getValue()) {
          case ZERO -> zero = true;
          case ONE -> one = true;
          case DONT_CARE -> wildcard = true;
        }
      }
      // no entry has a wildcard at this position and the values are distinct
      significantBits[i] = !wildcard && zero && one;
    }
    return significantBits;
  }


  private static List<StringBuilder> enumerate(boolean[] mask) {

    List<StringBuilder> patterns = new ArrayList<>();
    patterns.add(new StringBuilder());

    for (boolean b : mask) {
      if (!b) {
        patterns = patterns.stream().map(sb -> sb.append("-")).toList();
        continue;
      }

      patterns = patterns.stream()
          .flatMap(p -> Stream.of(
              new StringBuilder("0").insert(0, p),
              new StringBuilder("1").insert(0, p)))
          .toList();
    }

    return patterns;
  }

  /**
   * Prepare the input decode entries for the generator. This includes padding the patterns to the
   * same width and truncating them to the overall relevant region.
   *
   * @param decodeEntries The entry set
   * @return The relevant region to truncate to and the prepared entry set
   */
  private DecodeEntries toRelevantBits(Collection<DecodeEntry> decodeEntries) {

    // Pad all patterns to the maximum occurring width
    final int maxWidth = decodeEntries.stream()
        .mapToInt(DecodeEntry::width)
        .max()
        .orElseThrow(() -> new IllegalArgumentException("Empty entry set"));

    // Pad all patterns to the same width
    List<DecodeEntry> entries = decodeEntries.stream()
        .map(e -> transform(e, p -> pad(p, maxWidth)))
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

    return new DecodeEntries(fromIdx, (toIdx + 1) - fromIdx, entries);
  }

  private DecodeEntries toRelevantBits(int parentOffset, Collection<DecodeEntry> decodeEntries) {

    // Determine and transform the input patterns to only consider decoding relevant bits
    final var res = toRelevantBits(decodeEntries);

    // Convert the relative offset to an absolute one
    return new DecodeEntries(parentOffset + res.offset(), res.length(), res.entries());
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

  /**
   * Pad the given pattern to the target width. The padding is done by adding don't care bits to
   * the least significant bits.
   *
   * @param pattern     The pattern to pad
   * @param targetWidth The target width
   * @return The padded pattern
   */
  private BitPattern pad(BitPattern pattern, int targetWidth) {

    if (pattern.width() >= targetWidth) {
      return pattern;
    }

    final PBit[] bits = new PBit[targetWidth];
    for (int i = 0; i < targetWidth; i++) {
      if (i < pattern.width()) {
        bits[i] = pattern.get(i);
      } else {
        bits[i] = new PBit(PBit.Value.DONT_CARE);
      }
    }

    return new BitPattern(bits);
  }

  private BitPattern slice(BitPattern pattern, int from, int to) {
    final PBit[] bits = new PBit[to - from + 1];
    for (int i = from; i <= to; i++) {
      bits[i - from] = pattern.get(i);
    }
    return new BitPattern(bits);
  }

  /**
   * Encapsulate the decode entry set, only considering the region specified by offset and length.
   *
   * @param offset  The offset in bits given the instruction word to decode
   * @param length  The length of the relevant bit-region considered for decoding
   * @param entries The decode entries and patterns, truncated to the specified offset and width
   */
  private record DecodeEntries(int offset, int length, List<DecodeEntry> entries) {

  }

  private record MultiPatterns(BitVector mask, List<BitPattern> patterns) {
    boolean isEmpty() {
      return patterns.isEmpty();
    }
  }
}
