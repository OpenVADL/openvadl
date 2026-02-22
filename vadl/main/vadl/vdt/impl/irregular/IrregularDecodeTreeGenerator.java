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
import static vadl.vdt.utils.PatternUtils.compatible;
import static vadl.vdt.utils.PatternUtils.contain;
import static vadl.vdt.utils.PatternUtils.invalidate;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import vadl.error.Diagnostic;
import vadl.vdt.impl.irregular.model.DecodeEntries;
import vadl.vdt.impl.irregular.model.DecodeEntry;
import vadl.vdt.impl.irregular.model.ExclusionCondition;
import vadl.vdt.impl.irregular.model.ExclusionConditions;
import vadl.vdt.impl.irregular.model.MultiPatterns;
import vadl.vdt.impl.irregular.tree.MultiDecisionNode;
import vadl.vdt.impl.irregular.tree.SingleDecisionNode;
import vadl.vdt.model.DecodeTreeGenerator;
import vadl.vdt.model.Node;
import vadl.vdt.model.impl.LeafNodeImpl;
import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.BitVector;
import vadl.vdt.utils.PatternUtils;
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
   * @param input The entry set
   * @return The generated decode tree
   */
  @Override
  public Node generate(Collection<DecodeEntry> input) {

    if (input.isEmpty()) {
      throw new IllegalArgumentException("Entry set must not be empty");
    }

    var entries = prepareEntries(input);

    return generateInternal(entries);
  }

  /**
   * Top level method to generate the decode tree. This method will recursively build the tree
   * based on the input entries.
   *
   * @param decodeEntries The entry set
   * @return The generated decode (sub-) tree
   */
  protected Node generateInternal(DecodeEntries decodeEntries) {

    if (decodeEntries.hasMultiple()) {
      // Split the entry set
      return makeNode(decodeEntries);
    }

    if (decodeEntries.isEmpty()) {
      throw new IllegalStateException("Entry set must not be empty");
    }

    // It's possible to have multiple decode entries pointing to the same instruction
    var entry = combineEntries(decodeEntries);

    // Determine any unchecked bits in the instruction pattern
    var remainingFixedBitPattern = getUncheckedBits(decodeEntries.checkedBits(), entry.pattern());

    // Determine any unchecked constraints
    var checkedBits = combinePatterns(decodeEntries.checkedBits(), remainingFixedBitPattern);
    var remainingConstraintsDecision = checkRemainingConstraints(checkedBits, entry);

    // The remaining constraints decision node (or leaf node, if no constraints left)
    var innerNode = Objects.requireNonNullElseGet(remainingConstraintsDecision,
        () -> new LeafNodeImpl(entry));

    if (remainingFixedBitPattern.doesMatchAll()) {
      return innerNode;
    }

    return new SingleDecisionNode(remainingFixedBitPattern, innerNode, null);
  }

  protected Node makeNode(DecodeEntries decodeEntries) {

    final MultiPatterns patterns = makePatterns(decodeEntries);

    if (!patterns.hasDecision()) {
      // Select best splitting pattern based on exclusion conditions
      final BitPattern pattern = selectPattern(decodeEntries);
      return makeConditionNode(decodeEntries, pattern);
    }

    final MultiSplitEntrySet splitEntries = split(decodeEntries, patterns.mask());

    return makeMultiDecisionNode(decodeEntries, splitEntries);
  }

  protected MultiDecisionNode makeMultiDecisionNode(DecodeEntries decodeEntries,
                                                    MultiSplitEntrySet splitEntries) {

    final Map<BitPattern, Node> children = new LinkedHashMap<>();
    for (var branches : splitEntries.entries().entrySet()) {

      final BitPattern pattern = branches.getKey();
      final List<DecodeEntry> matchingEntries = branches.getValue();

      if (matchingEntries.isEmpty()) {
        continue;
      }

      final BitPattern checked = combinePatterns(decodeEntries.checkedBits(), pattern);

      final DecodeEntries entries = new DecodeEntries(checked, matchingEntries);
      final Node childNode = generateInternal(entries);
      children.put(pattern, childNode);
    }

    return new MultiDecisionNode(splitEntries.mask(), children);
  }

  protected Node makeConditionNode(DecodeEntries decodeEntries, BitPattern pattern) {

    // Split the entry set
    final SingleSplitEntrySet splitEntries = split(decodeEntries, pattern);

    // We can only consider the splitting pattern as 'checked' for the matching entries
    final BitPattern parentChecked = decodeEntries.checkedBits();
    final BitPattern checked = combinePatterns(parentChecked, pattern);

    // Recursively build child-trees
    final DecodeEntries me = new DecodeEntries(checked, splitEntries.matching());
    final Node matchingChild = generateInternal(me);

    final DecodeEntries oe = new DecodeEntries(parentChecked, splitEntries.others());
    final Node otherChild = generateInternal(oe);

    return new SingleDecisionNode(pattern, matchingChild, otherChild);
  }

  private MultiPatterns makePatterns(DecodeEntries decodeEntries) {

    List<DecodeEntry> entries = decodeEntries.entries();

    // Construct the bit mask to select the significant bits
    BitVector mask = entries.getFirst().pattern().toMaskVector();
    for (DecodeEntry e : entries) {
      mask = mask.and(e.pattern().toMaskVector());
    }

    return makePatterns(decodeEntries, mask);
  }

  private MultiPatterns makePatterns(DecodeEntries decodeEntries, BitVector mask) {

    // We don't need to check bits more than once
    BitVector checked = decodeEntries.checkedBits().toMaskVector();
    mask = mask.and(checked.not());

    final Set<BitPattern> options = new LinkedHashSet<>();
    for (DecodeEntry e : decodeEntries.entries()) {
      final BitVector b = e.pattern().toBitVector().and(mask);
      final BitPattern p = BitPattern.fromBitVector(mask, b);
      options.add(p);
    }

    return new MultiPatterns(mask, options);
  }

  protected record SingleSplitEntrySet(BitPattern pattern, List<DecodeEntry> matching,
                                       List<DecodeEntry> others) {
  }

  protected record MultiSplitEntrySet(BitVector mask, Map<BitPattern, List<DecodeEntry>> entries) {

  }

  /**
   * Split the entry set by the given pattern, distributing the occurrence probabilities
   * accordingly, and adapting the exclusion conditions.
   *
   * @param decodeEntries the decode entries.
   * @param pattern       the splitting pattern.
   * @return the split entry set.
   */
  protected SingleSplitEntrySet split(DecodeEntries decodeEntries, BitPattern pattern) {

    final List<DecodeEntry> matchingEntries = new ArrayList<>();
    final List<DecodeEntry> otherEntries = new ArrayList<>();

    for (DecodeEntry e : decodeEntries.entries()) {

      // See if there is an encoding that matches the base pattern and is not excluded by the
      // exclusion conditions
      var mThen = compatible(e.pattern(), pattern) && e.exclusionConditions().stream()
          .allMatch(ex -> {
            var combined = combinePatterns(e.pattern(), pattern);
            return !contain(combined, ex.matching()) || ex.unmatching().stream()
                .anyMatch(u -> compatible(combined, u));
          });

      // See if there is an encoding that either does not match the base pattern or is excluded by
      // the exclusion conditions (and not re-included by the unmatching patterns)
      var mElse = !compatible(e.pattern(), pattern) || e.exclusionConditions().stream()
          .anyMatch(ex -> {
            var combined = combinePatterns(e.pattern(), pattern);
            return compatible(combined, ex.matching()) && ex.unmatching().stream()
                .noneMatch(u -> compatible(combined, u));
          });

      if (mThen ^ mElse) {
        // The entry occurs only on one side of the branch, we can keep the current probability.
        (mThen ? matchingEntries : otherEntries).add(e);
        continue;
      }

      if (!mThen) {
        // Must not happen
        throw toConstructionDiagnostic(decodeEntries);
      }

      // Distribute the occurrence probability
      // TODO: Distribute proportionally
      matchingEntries.add(
          new DecodeEntry(e.source(), e.width(), e.pattern(), e.exclusionConditions(),
              e.occurrenceProbability() / 2));
      otherEntries.add(new DecodeEntry(e.source(), e.width(), e.pattern(), e.exclusionConditions(),
          e.occurrenceProbability() / 2));
    }

    final var matching = makeMatchingEntries(matchingEntries, pattern);
    final var others = makeOtherEntries(otherEntries, pattern);

    return new SingleSplitEntrySet(pattern, matching, others);
  }

  /**
   * Split the entry set by the given mask, distributing the occurrence probabilities accordingly
   * across all children, and adapting the exclusion conditions.
   *
   * @param decodeEntries the decode entries.
   * @param mask          the splitting mask.
   * @return the split entry set.
   */
  protected MultiSplitEntrySet split(DecodeEntries decodeEntries, BitVector mask) {

    final MultiPatterns patterns = makePatterns(decodeEntries, mask);
    final Map<BitPattern, List<DecodeEntry>> entries = new LinkedHashMap<>();

    for (DecodeEntry e : decodeEntries.entries()) {

      final Map<BitPattern, DecodeEntry> branches = new LinkedHashMap<>();
      for (BitPattern pattern : patterns.patterns()) {

        var match = compatible(e.pattern(), pattern) && e.exclusionConditions().stream()
            .noneMatch(c -> contain(pattern, c.matching())
                && c.unmatching().stream().noneMatch(p -> compatible(pattern, p)));

        if (match) {
          branches.put(pattern, e);
        }
      }

      // TODO: Distribute proportionally
      final var occurrence = e.occurrenceProbability() / branches.size();
      for (BitPattern pattern : branches.keySet()) {
        entries
            .computeIfAbsent(pattern, _ -> new ArrayList<>())
            .add(new DecodeEntry(e.source(), e.width(), e.pattern(), e.exclusionConditions(),
                occurrence));
      }
    }

    final Map<BitPattern, List<DecodeEntry>> result = new LinkedHashMap<>();

    for (final var branch : entries.entrySet()) {
      final var pattern = branch.getKey();
      result.put(pattern, makeMatchingEntries(branch.getValue(), pattern));
    }

    return new MultiSplitEntrySet(mask, result);
  }

  protected List<DecodeEntry> makeMatchingEntries(List<DecodeEntry> decodeEntries,
                                                  BitPattern pattern) {

    // Step 1
    final List<DecodeEntry> matchingEntries = decodeEntries.stream()
        .filter(d -> compatible(d.pattern(), pattern))
        .filter(d -> d.exclusionConditions().stream()
            .noneMatch(c -> contain(pattern, c.matching())
                && c.unmatching().stream().noneMatch(p -> compatible(pattern, p))))
        .toList();

    // Step 2
    final List<DecodeEntry> matchingEntries2 = new ArrayList<>();
    for (DecodeEntry e : matchingEntries) {

      final Set<ExclusionCondition> ex = e.exclusionConditions().stream()
          .filter(c -> compatible(pattern, c.matching()) && c.unmatching().stream()
              .noneMatch(pu -> contain(pattern, pu)))
          .map(c -> {
            final Set<BitPattern> newUnmatching = c.unmatching().stream()
                .filter(pu -> compatible(pattern, pu))
                .collect(Collectors.toSet());
            return new ExclusionCondition(c.matching(), newUnmatching);
          })
          .collect(Collectors.toSet());

      matchingEntries2.add(DecodeEntry.withExclusions(e, ex));
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
      matchingEntries3.add(DecodeEntry.withExclusions(e, newExclusions));
    }

    // Step 4
    return expandUnmatchingConditions(matchingEntries3);
  }

  protected List<DecodeEntry> makeOtherEntries(List<DecodeEntry> decodeEntries,
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

      otherEntries2.add(DecodeEntry.withExclusions(e, newExclusions));
    }

    return otherEntries2;
  }

  private BitPattern selectPattern(DecodeEntries decodeEntries) {

    final Set<BitPattern> patternCandidates = decodeEntries.entries().stream()
        .flatMap(e -> e.exclusionConditions().stream())
        .map(ExclusionCondition::matching)
        .collect(Collectors.toSet());

    if (patternCandidates.isEmpty()) {
      throw toConstructionDiagnostic(decodeEntries);
    }

    int bestSplit = Integer.MAX_VALUE;
    BitPattern minimizingPattern = null;

    for (BitPattern p : patternCandidates) {

      var split = split(decodeEntries, p);

      // If the candidate pattern does not split the entry set at all, skip it
      if (split.matching().isEmpty() || split.others().isEmpty()) {
        continue;
      }

      var splitSize = split.matching().size() + split.others().size();

      if (bestSplit <= splitSize) {
        continue;
      }

      bestSplit = splitSize;
      minimizingPattern = p;
    }

    if (minimizingPattern == null) {
      throw toConstructionDiagnostic(decodeEntries);
    }

    return minimizingPattern;
  }

  /**
   * Prepare the input decode entries for the generator. This includes padding the patterns to the
   * same width.
   *
   * @param decodeEntries The entry set
   * @return The padded entry set
   */
  private DecodeEntries prepareEntries(Collection<DecodeEntry> decodeEntries) {

    // Determine the maximum width to pad encoding to
    final int maxWidth = decodeEntries.stream()
        .mapToInt(DecodeEntry::width)
        .max()
        .orElseThrow(() -> new IllegalArgumentException("Empty entry set"));

    // Pad all patterns to the same width
    final List<DecodeEntry> entries = decodeEntries.stream()
        .map(e -> transform(e, p -> p.rightPad(maxWidth - p.width())))
        .toList();

    // Resolve all-matching 'matching' patterns in exclusion conditions
    final List<DecodeEntry> expandedEntries = expandUnmatchingConditions(entries);

    // Initially none are checked
    final BitPattern checkedBits = BitPattern.empty(maxWidth);

    return new DecodeEntries(checkedBits, expandedEntries);
  }

  /**
   * In case of a tautological 'matching' pattern, expand the 'unmatching' patterns to their opcode
   * pattern.
   *
   * @param entries The entry set
   * @return The expanded entries
   */
  @Nonnull
  private List<DecodeEntry> expandUnmatchingConditions(List<DecodeEntry> entries) {

    final List<DecodeEntry> expandedEntries = new ArrayList<>();
    for (DecodeEntry e : entries) {

      if (e.exclusionConditions().isEmpty()) {
        expandedEntries.add(e);
        continue;
      }

      if (e.exclusionConditions().stream().noneMatch(c -> c.matching().doesMatchAll())) {
        expandedEntries.add(e);
        continue;
      }

      // Collect exclusions which do not become the match-all pattern
      final Set<ExclusionCondition> validExclusions = e.exclusionConditions().stream()
          .filter(c -> !c.matching().doesMatchAll())
          .collect(Collectors.toSet());

      // Expand unmatching conditions to their own decode entries
      final var unmatching = e.exclusionConditions().stream()
          .filter(c -> c.matching().doesMatchAll())
          .flatMap(c -> c.unmatching().stream())
          .toList();

      // TODO: Distributed probability proportionally
      final double occurrence = e.occurrenceProbability() / unmatching.size();

      for (BitPattern pu : unmatching) {
        final BitPattern op = combinePatterns(e.pattern(), pu);
        final var nd = new DecodeEntry(e.source(), e.width(), op, validExclusions, occurrence);

        expandedEntries.add(nd);
      }
    }

    return expandedEntries;
  }

  /**
   * Combine decode entries for the same instruction into a single decode entry.
   *
   * @param decodeEntries Decode entries which all have the same source instruction.
   * @return The combined entry
   */
  private DecodeEntry combineEntries(DecodeEntries decodeEntries) {

    if (decodeEntries.hasMultiple()) {
      // Should not happen
      throw new IllegalArgumentException("Multiple instructions not supported");
    }

    final List<DecodeEntry> entries = decodeEntries.entries();
    final DecodeEntry first = entries.getFirst();
    if (entries.size() == 1) {
      // Nothing to combine
      return first;
    }

    final BitPattern commonPattern = entries.stream()
        .map(DecodeEntry::pattern)
        .reduce(first.pattern(), PatternUtils::commonPattern);

    final ExclusionConditions conditions = new ExclusionConditions();

    double occurrence = 0;
    for (DecodeEntry e : entries) {
      occurrence += e.occurrenceProbability();

      final BitPattern diff = invalidate(e.pattern(), commonPattern);
      conditions.add(e.exclusionConditions());

      if (diff.doesMatchAll()) {
        continue;
      }

      final var condition = new ExclusionCondition(BitPattern.empty(diff.width()), Set.of(diff));
      conditions.add(Set.of(condition));
    }

    return new DecodeEntry(first.source(), first.width(), commonPattern, conditions.conditions(),
        occurrence);
  }

  /**
   * Apply a modification to all bit patterns of a decode entry, that is the instruction pattern
   * as well as the ex-/inclusion conditions.
   *
   * @param entry       The decode entry to apply the transformation to
   * @param transformer The transformation function
   * @return The modified entry
   */
  private DecodeEntry transform(DecodeEntry entry, UnaryOperator<BitPattern> transformer) {

    final BitPattern pattern = entry.pattern();
    final BitPattern transformedPattern = transformer.apply(pattern);

    final Set<ExclusionCondition> transformedExclusions = entry.exclusionConditions().stream()
        .map(c -> new ExclusionCondition(transformer.apply(c.matching()),
            c.unmatching().stream()
                .map(transformer)
                .collect(Collectors.toSet())))
        .collect(Collectors.toSet());

    return new DecodeEntry(entry.source(), transformedPattern.width(), transformedPattern,
        transformedExclusions, entry.occurrenceProbability());
  }

  /**
   * Resolve the yet unchecked bits for the given instruction pattern.
   *
   * @param checkedBits The bits already checked by the algorithm
   * @param insn        The instruction pattern to check
   * @return The yet unchecked bits.
   */
  private BitPattern getUncheckedBits(BitPattern checkedBits, BitPattern insn) {

    // Allow to check more bits than required by the instruction pattern, which may be required for
    // checking constraints.
    final BitVector mask = insn.toMaskVector().xor(checkedBits.toMaskVector())
        .and(insn.toMaskVector());

    if (mask.toValue().compareTo(BigInteger.ZERO) == 0) {
      return BitPattern.empty(checkedBits.width());
    }

    return fromBitVector(mask, insn.toBitVector());
  }

  @Nullable
  private Node checkRemainingConstraints(BitPattern checkedBits, DecodeEntry entry) {

    Node node = null;

    for (ExclusionCondition c : entry.exclusionConditions()) {

      if (!compatible(checkedBits, c.matching())) {
        // The exclusion condition cannot match, it collides with the already known bits
        continue;
      }

      BitPattern matchingPattern = null;
      if (!contain(checkedBits, c.matching())) {

        var mask = c.matching().toMaskVector().xor(checkedBits.toMaskVector())
            .and(c.matching().toMaskVector());
        matchingPattern = fromBitVector(mask, c.matching().toBitVector());
      }

      final Set<BitPattern> unmatchingConditions = new HashSet<>();
      for (BitPattern pu : c.unmatching()) {

        if (!compatible(checkedBits, pu) || contain(checkedBits, pu)) {
          // The unmatching condition cannot match, or it has already been checked
          continue;
        }

        var mask = pu.toMaskVector().xor(checkedBits.toMaskVector())
            .and(pu.toMaskVector());

        if (mask.toValue().compareTo(BigInteger.ZERO) == 0) {
          // No bits left to check, skip this condition
          continue;
        }

        unmatchingConditions.add(fromBitVector(mask, pu.toBitVector()));
      }

      if (matchingPattern == null && unmatchingConditions.isEmpty()) {
        // No bits left to check, skip this condition
        continue;
      }

      if (unmatchingConditions.isEmpty()) {
        // If there are no unmatching conditions, we can directly check that the exclusion condition
        // does not match.
        node =
            new SingleDecisionNode(Objects.requireNonNull(matchingPattern),
                false, node == null ? new LeafNodeImpl(entry) : node, null);
        continue;
      }

      Node unmatchingNode = null;
      for (BitPattern pu : unmatchingConditions) {
        unmatchingNode = new SingleDecisionNode(pu, new LeafNodeImpl(entry), unmatchingNode);
      }

      if (matchingPattern == null) {
        node = unmatchingNode;
        continue;
      }

      node = new SingleDecisionNode(matchingPattern,
          Objects.requireNonNull(unmatchingNode), node == null ? new LeafNodeImpl(entry) : node);
    }

    return node;
  }

  private Diagnostic toConstructionDiagnostic(DecodeEntries decodeEntries) {
    var primary = decodeEntries.entries().getFirst().source();
    var insnNames = decodeEntries.entries().stream()
        .map(DecodeEntry::source)
        .map(Definition::simpleName)
        .toList();

    var diagnostic = error("Unable to split instruction set during decoder generation: %s"
        .formatted(insnNames), primary);

    for (DecodeEntry e : decodeEntries.entries()) {
      var others = insnNames.stream()
          .filter(n -> !n.equals(e.source().simpleName())).toList();

      diagnostic.locationDescription(e.source().encoding(),
          "Unable to split encoding definition with other instruction%s: %s",
          others.size() != 1 ? "s" : "",
          others.size() == 1 ? others.getFirst() : others);
    }

    return diagnostic.build();
  }
}
