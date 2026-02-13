// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

import static vadl.vdt.utils.PBit.Value.DONT_CARE;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.function.Function;
import javax.annotation.Nonnull;
import vadl.utils.Pair;
import vadl.vdt.impl.irregular.model.DecodeEntries;
import vadl.vdt.impl.irregular.model.DecodeEntry;
import vadl.vdt.model.Node;
import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.BitVector;

/**
 * Decode tree generator largely based on the Qin et al. decode tree construction algorithm using
 * cost minimization on instruction occurrence probabilities.
 *
 * <p>See: <a
 * href="https://dl.acm.org/doi/pdf/10.1145/775832.776027">Automated synthesis of efficient binary
 * decoders for retargetable software toolkits (Wei Qin, Sharad Malik)</a>
 */
public class OccurrenceAwareDecodeTreeGenerator extends IrregularDecodeTreeGenerator {

  @SuppressWarnings("UnusedVariable")
  private final double memoryPenalty;

  public OccurrenceAwareDecodeTreeGenerator(double memoryPenalty) {
    this.memoryPenalty = memoryPenalty;
  }

  @Override
  protected Node makeNode(DecodeEntries decodeEntries) {

    final var singleDecision = bestSingleDecision(decodeEntries).orElse(null);
    final var multiDecision = bestMultiDecision(decodeEntries).orElse(null);

    if (singleDecision == null && multiDecision == null) {
      throw new IllegalStateException("Unable to split entry set");
    }

    if (multiDecision == null
        || (singleDecision != null && singleDecision.left() < multiDecision.left())) {

      return makeConditionNode(decodeEntries,
          Objects.requireNonNull(singleDecision).right().pattern());
    }

    return makeMultiDecisionNode(decodeEntries, multiDecision.right());
  }

  private Pair<Double, SingleSplitEntrySet> calculateSingleCost(DecodeEntries entries,
                                                                BitPattern p) {
    final SingleSplitEntrySet split = split(entries, p);

    final double cost = calculateCost(List.of(split.matching(), split.others()));

    // Add a memory penalty
    final int s = split.matching().size() + split.others().size() - 1;
    final double meRatio = s / (entries.entries().size() - 1.0);
    final double penalty = memoryPenalty * (Math.log(meRatio) / Math.log(2));

    return new Pair<>(cost + penalty, split);
  }

  private Pair<Double, MultiSplitEntrySet> calculateMultiCost(DecodeEntries entries,
                                                              BitVector mask) {
    final MultiSplitEntrySet split = split(entries, mask);

    final double cost = calculateCost(split.entries().values());

    // Add a memory penalty
    final int m = mask.toValue().bitCount();
    final double s = split.entries().values().stream()
        .filter(e -> !e.isEmpty())
        .reduce(0, (acc, e) -> acc + (e.size() - 1), Integer::sum)
        + 1 + Math.pow(2, m);

    final double meRatio = s / (entries.entries().size() - 1.0);
    final double penalty = memoryPenalty * (Math.log(meRatio) / Math.log(2));

    return new Pair<>(cost + penalty, split);
  }

  private double calculateCost(Collection<List<DecodeEntry>> splits) {
    return 1 + splits.stream()
        .reduce(0.0, (acc, e) -> {
          double prob = e.stream()
              .map(DecodeEntry::occurrenceProbability).reduce(0.0, Double::sum);
          return acc + prob * huffmanTreeHeight(e);
        }, Double::sum);
  }

  private Optional<Pair<Double, SingleSplitEntrySet>> bestSingleDecision(
      DecodeEntries decodeEntries) {
    return findBestPattern(decodeEntries.checkedBits(), p -> {
      var split = calculateSingleCost(decodeEntries, p);
      if (split.right().matching().isEmpty() || split.right().others().isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(split.left());
    })
        .map(decision -> calculateSingleCost(decodeEntries, decision));
  }

  private Optional<Pair<Double, MultiSplitEntrySet>> bestMultiDecision(
      DecodeEntries decodeEntries) {

    final var candidates = baseMaskCandidates(decodeEntries.checkedBits());

    if (candidates.isEmpty()) {
      return Optional.empty();
    }

    final Pair<Double, MultiSplitEntrySet> base = candidates.stream()
        .map(m -> {
          var split = calculateMultiCost(decodeEntries, m);
          var hasDecision = split.right().entries().values().stream()
              .filter(es -> !es.isEmpty())
              .count() > 1;
          return hasDecision ? split : null;
        })
        .filter(Objects::nonNull)
        .min(Comparator.comparing(Pair::left)).orElse(null);

    final var checked = decodeEntries.checkedBits().toMaskVector();

    Pair<Double, MultiSplitEntrySet> prev = Objects.requireNonNull(base);
    Pair<Double, MultiSplitEntrySet> next = prev;
    do {

      prev = next;

      // candidate bits to try next
      var currentBase = prev.right().mask();
      var bits = checked.not().and(currentBase.not()).toValue();

      while (bits.getLowestSetBit() >= 0) {
        int i = bits.getLowestSetBit();

        var candidate = BitVector.fromValue(currentBase.toValue().setBit(i), currentBase.width());
        var cost = calculateMultiCost(decodeEntries, candidate);

        if (cost.left() < prev.left()) {
          next = cost;
        }

        bits = bits.clearBit(i);
      }

    } while (next.left() < prev.left());

    return Optional.of(prev);
  }

  /**
   * Search for a minimal cost pattern. To not enumerate all possibilities (3^n) we grow the
   * pattern bit by bit as long as the cost improves.
   *
   * @param base         the bit pattern to start with / or grow from.
   * @param costFunction the function calculating the cost of choosing a pattern.
   * @return all candidates of the base pattern extended with an additional decision bit.
   */
  private Optional<BitPattern> findBestPattern(final BitPattern base,
                                               final Function<BitPattern,
                                                   Optional<Double>> costFunction) {

    boolean initialized = false;

    BitPattern prev;
    double prevCost;

    BitPattern next = base;
    double nextCost = Double.POSITIVE_INFINITY;

    do {

      // Move current best to the 'previous' values
      prev = next;
      prevCost = nextCost;

      // Find next best
      var candidates = patternCandidates(prev);

      if (!initialized && !candidates.hasNext()) {
        return Optional.empty();
      }
      initialized = true;

      while (candidates.hasNext()) {
        var candidate = candidates.next();
        var cost = costFunction.apply(candidate);

        if (cost.isPresent() && cost.get() < nextCost) {
          next = candidate;
          nextCost = cost.get();
        }
      }

    } while (nextCost < prevCost);

    return Optional.of(prev);
  }

  /**
   * Generator for splitting pattern candidates by growing the base pattern with an additional bit.
   *
   * @param base the pattern from which to grow the splitting patterns.
   * @return all candidates of the base pattern extended with an additional decision bit.
   */
  private Iterator<BitPattern> patternCandidates(BitPattern base) {

    return new Iterator<>() {

      private BigInteger candidates = base.toMaskVector().not().toValue();
      private boolean first = true;

      @Override
      public boolean hasNext() {
        return !BigInteger.ZERO.equals(candidates) || !first;
      }

      @Override
      public BitPattern next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }

        final int decisionBit = candidates.getLowestSetBit();

        final var mask = base.toMaskVector().toValue().setBit(decisionBit);
        final var mVector = BitVector.fromValue(mask, base.width());

        if (first) {
          // Flip the lowest candidate bit to 'one' only in the bitmask
          first = false;
          return BitPattern.fromBitVector(mVector, base.toBitVector());
        }

        // Flip the lowest candidate bit to 'one' in the base pattern
        var value = base.toBitVector().toValue().setBit(decisionBit);
        var vVector = BitVector.fromValue(value, base.width());

        // Clear the candidate bit
        candidates = candidates.clearBit(decisionBit);
        first = true;

        return BitPattern.fromBitVector(mVector, vVector);
      }
    };
  }

  /**
   * Generator for all relevant 2-bit mask candidates, given the already known bits in the base
   * pattern.
   *
   * @param base The base pattern, specifying known bits.
   * @return all 2-bit mask candidates
   */
  private List<BitVector> baseMaskCandidates(final BitPattern base) {
    final int w = base.width();
    final List<BitVector> result = new ArrayList<>();
    for (int i = w - 1; i > 0; i--) {
      if (base.get(i).getValue() != DONT_CARE || base.get(i - 1).getValue() != DONT_CARE) {
        continue;
      }
      var maskValue = BigInteger.ZERO.setBit(w - i).setBit(w - i - 1);
      result.add(BitVector.fromValue(maskValue, w));
    }
    return result;
  }

  private record HuffmanNode(double weight, int height)
      implements Comparable<HuffmanNode> {

    static HuffmanNode of(DecodeEntry entry) {
      return new HuffmanNode(entry.occurrenceProbability(), 0);
    }

    @Override
    public int compareTo(@Nonnull HuffmanNode o) {
      return Double.compare(weight, o.weight);
    }

    public HuffmanNode merge(HuffmanNode node) {
      final var w = weight + node.weight;
      final var h = 1 + Math.max(height, node.height);
      return new HuffmanNode(w, h);
    }
  }

  private static int huffmanTreeHeight(Collection<DecodeEntry> entries) {
    if (entries.isEmpty()) {
      return 0;
    }

    final PriorityQueue<HuffmanNode> priorityQueue = new PriorityQueue<>(entries.size());
    for (var e : entries) {
      priorityQueue.add(HuffmanNode.of(e));
    }

    while (priorityQueue.size() > 1) {
      var a = priorityQueue.poll();
      var b = priorityQueue.poll();
      priorityQueue.add(a.merge(Objects.requireNonNull(b)));
    }
    return Objects.requireNonNull(priorityQueue.peek()).height();
  }

}
