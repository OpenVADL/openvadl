package vadl.vdt.impl.irregular;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import vadl.vdt.impl.irregular.model.DecodeEntry;
import vadl.vdt.impl.irregular.model.ExclusionCondition;
import vadl.vdt.impl.irregular.tree.MultiDecisionNode;
import vadl.vdt.impl.irregular.tree.SingleDecisionNode;
import vadl.vdt.model.DecodeTreeGenerator;
import vadl.vdt.model.Node;
import vadl.vdt.model.impl.LeafNodeImpl;
import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.PBit;

/**
 * Implements the Okuda et al. algorithm to generate a decode tree for irregular instruction sets.
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


  @Override
  public Node generate(Collection<DecodeEntry> decodeEntries) {

    // TODO: make sure the entries are properly padded

    final List<DecodeEntry> entries = new ArrayList<>(decodeEntries);
    if (entries.size() == 1) {
      return new LeafNodeImpl(entries.get(0));
    }

    return makeNode(entries);
  }

  private Node makeNode(List<DecodeEntry> decodeEntries) {
    final List<BitPattern> patterns = makePatterns(decodeEntries);

    if (patterns.isEmpty()) {
      // Split entry set by exclusion conditions instead
      return makeConditionNode(decodeEntries);
    }

    final Map<BitPattern, Node> children = new HashMap<>();
    for (BitPattern p : patterns) {
      final List<DecodeEntry> matchingEntries = makeMatchingEntries(decodeEntries, p);
      if (matchingEntries.isEmpty()) {
        continue;
      }
      final Node childNode = generate(matchingEntries);
      children.put(p, childNode);
    }

    return new MultiDecisionNode(children);
  }

  private Node makeConditionNode(List<DecodeEntry> decodeEntries) {

    // Select best splitting pattern based on exclusion conditions
    final BitPattern pattern = selectPattern(decodeEntries);

    // Split the entry set
    final List<DecodeEntry> matching = makeMatchingEntries(decodeEntries, pattern);
    final List<DecodeEntry> others = makeOtherEntries(decodeEntries, pattern);

    // Recursively build child-trees
    final Node matchingChild = generate(matching);
    final Node otherChild = generate(others);

    return new SingleDecisionNode(pattern, matchingChild, otherChild);
  }

  private List<BitPattern> makePatterns(List<DecodeEntry> decodeEntries) {
    // Determine the significant bits, i.e. ones that split the entry set
    final boolean[] significantBits = getSignificantBits(decodeEntries);
    // Enumerate all options
    return enumerate(significantBits)
        .stream()
        .map(sb -> BitPattern.fromString(sb.toString(), sb.length()))
        .filter(p -> !p.doesMatchAll())
        .toList();
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
      throw new IllegalArgumentException("Unable to split entry set");
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
      throw new IllegalArgumentException("Unable to find splitting pattern");
    }

    return minimizingPattern;
  }

  private boolean match(BitPattern p1, BitPattern p2) {
    return IntStream.range(0, p1.width())
        .allMatch(
            i -> p1.get(i).equals(p2.get(i)) || p1.get(i).getValue() == PBit.Value.DONT_CARE ||
                p2.get(i).getValue() == PBit.Value.DONT_CARE);
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
    final int width = decodeEntries.get(0).width();
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
}
