package vadl.vdt.impl.katsumi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.vdt.AbstractDecisionTreeTest;
import vadl.vdt.impl.katsumi.model.DecodeEntry;
import vadl.vdt.impl.katsumi.model.ExclusionCondition;
import vadl.vdt.model.Node;
import vadl.vdt.target.common.DecisionTreeDecoder;
import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.BitVector;
import vadl.vdt.utils.Instruction;

class KatsumiDecodeTreeGeneratorTest extends AbstractDecisionTreeTest {

  @Test
  void testGenerate_simpleInstructions_succeeds() {

    /* GIVEN */
    final List<Instruction> instructions = createInsns(List.of(
        "00------",
        "01------",
        "10----00",
        "10----01"
    ));

    // Create decode entries with empty exclusion conditions
    final List<DecodeEntry> decodeEntries = instructions.stream()
        .map(i -> new DecodeEntry(i.source(), i.width(), i.pattern(), Set.of()))
        .toList();

    /* WHEN */
    final Node dt = new KatsumiDecodeTreeGenerator().generate(decodeEntries);

    /* THEN */
    final DecisionTreeDecoder decoder = new DecisionTreeDecoder(dt);

    assertDecision(decoder, "00000000", "00------");
    assertDecision(decoder, "01000000", "01------");
    assertDecision(decoder, "10000000", "10----00");
    assertDecision(decoder, "10000001", "10----01");
  }

  @Test
  void testGenerate_irregularInstructions_succeeds() {

    /* GIVEN */
    final List<Instruction> insns = createInsns(List.of(
        "00------",
        "01------",
        "10----00",
        "10----01",
        "0000--01", // New overlapping entries
        "0000--10",
        "0-11----"
    ));

    final List<DecodeEntry> decodeEntries = new ArrayList<>();

    {
      var entry = toDecodeEntry(insns.get(0),
          exclude("--00----", "------00", "------11"),
          exclude("--11----"));
      decodeEntries.add(entry);
    }
    {
      var entry = toDecodeEntry(insns.get(1), "--11----");
      decodeEntries.add(entry);
    }

    // The rest do not have exclusion conditions
    for (int i = 2; i < insns.size(); i++) {
      decodeEntries.add(toDecodeEntry(insns.get(i)));
    }

    /* WHEN */
    final Node dt = new KatsumiDecodeTreeGenerator().generate(decodeEntries);

    /* THEN */
    final DecisionTreeDecoder decoder = new DecisionTreeDecoder(dt);

    assertDecisionByName(decoder, "00000000", "insn_0");
    assertDecisionByName(decoder, "01000000", "insn_1");
    assertDecisionByName(decoder, "10000000", "insn_2");
    assertDecisionByName(decoder, "10000001", "insn_3");
    assertDecisionByName(decoder, "00000001", "insn_4");
    assertDecisionByName(decoder, "00000010", "insn_5");
    assertDecisionByName(decoder, "00110000", "insn_6");
  }

  private DecodeEntry toDecodeEntry(Instruction insn) {
    return new DecodeEntry(insn.source(), insn.width(), insn.pattern(), Set.of());
  }

  private DecodeEntry toDecodeEntry(Instruction insn, String... exclusionPattern) {
    Set<ExclusionCondition> exclusions = Arrays.stream(exclusionPattern)
        .map(s -> new ExclusionCondition(BitPattern.fromString(s, s.length()), Set.of()))
        .collect(Collectors.toSet());
    return new DecodeEntry(insn.source(), insn.width(), insn.pattern(), exclusions);
  }

  private DecodeEntry toDecodeEntry(Instruction insn, ExclusionCondition... exclusions) {
    return new DecodeEntry(insn.source(), insn.width(), insn.pattern(), Set.of(exclusions));
  }

  private ExclusionCondition exclude(String matchingPattern, String... unmatchingPattern) {
    BitPattern matching = BitPattern.fromString(matchingPattern, matchingPattern.length());
    Set<BitPattern> unmatching = Arrays.stream(unmatchingPattern)
        .map(p -> BitPattern.fromString(p, p.length()))
        .collect(Collectors.toSet());
    return new ExclusionCondition(matching, unmatching);
  }

  private void assertDecision(DecisionTreeDecoder decoder, String insn, String expected) {
    Instruction decision = decoder.decide(BitVector.fromString(insn, insn.length()));
    Assertions.assertNotNull(decision);
    Assertions.assertEquals(expected, decision.pattern().toString());
  }

  private void assertDecisionByName(DecisionTreeDecoder decoder, String insn, String expectedName) {
    Instruction decision = decoder.decide(BitVector.fromString(insn, insn.length()));
    Assertions.assertNotNull(decision);
    Assertions.assertEquals(expectedName, decision.source().simpleName());
  }
}