// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static vadl.vdt.utils.PatternUtils.toFixedBitPattern;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.TestUtils;
import vadl.configuration.DumpMode;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.IssConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassManager;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.vdt.AbstractDecisionTreeTest;
import vadl.vdt.impl.irregular.model.DecodeEntry;
import vadl.vdt.impl.irregular.model.ExclusionCondition;
import vadl.vdt.impl.irregular.tree.MultiDecisionNode;
import vadl.vdt.model.Node;
import vadl.vdt.passes.VdtConstraintSynthesisPass;
import vadl.vdt.passes.VdtEncodingConstraintValidationPass;
import vadl.vdt.passes.VdtEncodingSemanticVerificationPass;
import vadl.vdt.passes.VdtInputPreparationPass;
import vadl.vdt.passes.VdtLoweringPass;
import vadl.vdt.passes.VdtVerificationPass;
import vadl.vdt.target.common.CheckedBitsCollector;
import vadl.vdt.target.common.DecisionTreeDecoder;
import vadl.vdt.target.common.DecisionTreeStatsCalculator;
import vadl.vdt.target.dump.TextGraphGenerator;
import vadl.vdt.utils.BitPattern;
import vadl.vdt.utils.BitVector;
import vadl.vdt.utils.Instruction;
import vadl.vdt.utils.PBit;

class IrregularDecodeTreeGeneratorTest extends AbstractDecisionTreeTest {

  private static final Logger log = LoggerFactory.getLogger(IrregularDecodeTreeGeneratorTest.class);

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
    final Node dt = new IrregularDecodeTreeGenerator().generate(decodeEntries);

    /* THEN */
    final DecisionTreeDecoder decoder = new DecisionTreeDecoder(dt);

    assertDecision(decoder, "00000000", "insn_0");
    assertDecision(decoder, "01000000", "insn_1");
    assertDecision(decoder, "10000000", "insn_2");
    assertDecision(decoder, "10000001", "insn_3");
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
    final Node dt = new IrregularDecodeTreeGenerator().generate(decodeEntries);

    /* THEN */
    final DecisionTreeDecoder decoder = new DecisionTreeDecoder(dt);

    assertDecision(decoder, "00000000", "insn_0");
    assertDecision(decoder, "01000000", "insn_1");
    assertDecision(decoder, "10000000", "insn_2");
    assertDecision(decoder, "10000001", "insn_3");
    assertDecision(decoder, "00000001", "insn_4");
    assertDecision(decoder, "00000010", "insn_5");
    assertDecision(decoder, "00110000", "insn_6");
  }

  @Test
  void testGenerate_handleVariableLength_succeeds() {

    /* GIVEN */
    final List<Instruction> insns = createInsns(List.of(
        "-00--",
        "-01--",
        "-10----00--",
        "-10----01--",
        "-0000--01", // Overlapping entries
        "-0000--10",
        "-0-11----"
    ));

    final List<DecodeEntry> decodeEntries = new ArrayList<>();

    {
      var entry = toDecodeEntry(insns.get(0),
          exclude("---00----", "-------00", "-------11"),
          exclude("---11----"));
      decodeEntries.add(entry);
    }
    {
      var entry = toDecodeEntry(insns.get(1), "---11----");
      decodeEntries.add(entry);
    }

    // The rest do not have exclusion conditions
    for (int i = 2; i < insns.size(); i++) {
      decodeEntries.add(toDecodeEntry(insns.get(i)));
    }

    /* WHEN */
    final Node dt = new IrregularDecodeTreeGenerator().generate(decodeEntries);

    /* THEN */
    final DecisionTreeDecoder decoder = new DecisionTreeDecoder(dt);

    assertDecision(decoder, "00000", "insn_0");
    assertDecision(decoder, "00100", "insn_1");
    assertDecision(decoder, "01000000000", "insn_2");
    assertDecision(decoder, "01000000100", "insn_3");
    assertDecision(decoder, "000000001", "insn_4");
    assertDecision(decoder, "000000010", "insn_5");
    assertDecision(decoder, "000110000", "insn_6");
  }

  @Test
  void testGenerate_handleVariableLength_withAdditionalBits_succeeds() {

    /* GIVEN */
    final List<Instruction> insns = createInsns(List.of(
        "-00--",
        "-01--",
        "-10----00--",
        "-10----01--",
        "-0000--01", // Overlapping entries
        "-0000--10",
        "-0-11----"
    ));

    final List<DecodeEntry> decodeEntries = new ArrayList<>();

    {
      var entry = toDecodeEntry(insns.get(0),
          exclude("---00----", "-------00", "-------11"),
          exclude("---11----"));
      decodeEntries.add(entry);
    }
    {
      var entry = toDecodeEntry(insns.get(1), "---11----");
      decodeEntries.add(entry);
    }

    // The rest do not have exclusion conditions
    for (int i = 2; i < insns.size(); i++) {
      decodeEntries.add(toDecodeEntry(insns.get(i)));
    }

    /* WHEN */
    final Node dt = new IrregularDecodeTreeGenerator().generate(decodeEntries);

    /* THEN */
    final DecisionTreeDecoder decoder = new DecisionTreeDecoder(dt);

    // The input instructions to decode are of the same length, possibly containing additional bits
    // from the following instructions (Here padded with 0s)
    assertDecision(decoder, "00000000000", "insn_0");
    assertDecision(decoder, "00100000000", "insn_1");
    assertDecision(decoder, "01000000000", "insn_2");
    assertDecision(decoder, "01000000100", "insn_3");
    assertDecision(decoder, "00000000100", "insn_4");
    assertDecision(decoder, "00000001000", "insn_5");
    assertDecision(decoder, "00011000000", "insn_6");
  }

  @Test
  void testGenerate_amd64_add() {

    // AMD64 ADD immediate instruction encodings
    //  - 'REX.W' = 0x48 prefix for 64-bit ops
    //  - '66'    = Operand-size override (for 16-bit ops)
    //  - 'ModR/M' = byte selecting register/memory destination
    //  - '/0' in opcode means ModR/M.reg field must be 000 (the 'add' group)

    // | Form                  | Prefix(es)   | Opcode    | ModR/M | Immediate | Example                |
    // |-----------------------|--------------|-----------|--------|-----------|------------------------|
    // | add r/m64, imm32      | REX.W        | 81 /0     | yes    | imm32     | 48 81 C0 78 56 34 12   |
    // | add r/m32, imm32      | -            | 81 /0     | yes    | imm32     | 81 C0 78 56 34 12      |
    // | add r/m16, imm16      | 66           | 81 /0     | yes    | imm16     | 66 81 C0 34 12         |
    // | add r/m8, imm8        | -            | 80 /0     | yes    | imm8      | 80 C0 7F               |
    // | add r/m64, imm8       | REX.W        | 83 /0     | yes    | imm8      | 48 83 C0 01            |
    // | add r/m32, imm8       | -            | 83 /0     | yes    | imm8      | 83 C0 01               |

    // add r/m64, imm32 (general form)
    // e: add rax, 0x12345678
    // p: 01001000 10000001 --000--- -------- -------- -------- --------
    // e: 01001000 10000001 11000000 01111000 01010110 00110100 00010010

    // add r/m32, imm32
    // e: add eax, 0x12345678
    // p: 10000001 --000--- -------- -------- -------- --------
    // e: 10000001 11000000 01111000 01010110 00110100 00010010

    // add r/m16, imm16
    // e: add ax, 0x1234
    // p: 01100110 10000001 --000--- -------- --------
    // e: 01100110 10000001 11000000 00110100 00010010

    // add r/m8, imm8
    // e: add al, 0x7F
    // p: 10000000 --000--- --------
    // e: 10000000 11000000 01111111

    // add r/m64, imm8
    // e: add rax, 0x01
    // p: 01001000 10000011 --000--- --------
    // e: 01001000 10000011 11000000 00000001

    // add r/m32, imm8
    // e: add eax, 0x01
    // p: 10000011 --000--- --------
    // e: 10000011 11000000 00000001


    /* GIVEN */
    final List<Instruction> insns = createInsns(List.of(
        "01001000 10000001 --000--- -------- -------- -------- --------",
        "10000001 --000--- -------- -------- -------- --------",
        "01100110 10000001 --000--- -------- --------",
        "10000000 --000--- --------",
        "01001000 10000011 --000--- --------",
        "10000011 --000--- --------"
    ));

    // For this set we don't need exclusion conditions
    final List<DecodeEntry> decodeEntries = new ArrayList<>();
    for (Instruction insn : insns) {
      decodeEntries.add(toDecodeEntry(insn));
    }

    /* WHEN */
    final Node dt = new IrregularDecodeTreeGenerator().generate(decodeEntries);

    /* THEN */
    final DecisionTreeDecoder decoder = new DecisionTreeDecoder(dt);

    // Extended instruction encodings, padded with the beginning of the insn_0 instruction
    assertDecision(decoder,
        "01001000 10000001 11000000 01111000 01010110 00110100 00010010", "insn_0");
    assertDecision(decoder,
        "10000001 11000000 01111000 01010110 00110100 00010010 01001000", "insn_1");
    assertDecision(decoder,
        "01100110 10000001 11000000 00110100 00010010 01001000 10000001", "insn_2");
    assertDecision(decoder,
        "10000000 11000000 01111111 01001000 10000001 11000000 01111000", "insn_3");
    assertDecision(decoder,
        "01001000 10000011 11000000 00000001 01001000 10000001 11000000", "insn_4");
    assertDecision(decoder,
        "10000011 11000000 00000001 01001000 10000001 11000000 01111000", "insn_5");
  }

  private static final String TEST_ISA = """
      instruction set architecture TEST = {
      
        register X: Bits<5>
      
        format Format: Bits<8> =
        { a  [7]
        , b  [6]
        , c  [5..4]
        , d  [3..2]
        , e  [1..0]
        }
      
        instruction I1: Format = { }
        instruction I2: Format = { }
        instruction I3: Format = { }
        instruction I4: Format = { }
        instruction I5: Format = { }
        instruction I6: Format = { }
        instruction I7: Format = { }
      
        assembly I1 = ( mnemonic )
        assembly I2 = ( mnemonic )
        assembly I3 = ( mnemonic )
        assembly I4 = ( mnemonic )
        assembly I5 = ( mnemonic )
        assembly I6 = ( mnemonic )
        assembly I7 = ( mnemonic )
      
        [select when: c != 0b11 && (c != 0b00 || e = 0b00 || e = 0b11)]
        encoding I1 = { a = 0b0, b = 0b0 }
      
        [select when: c != 0b11]           // Subsumed I5 & I6, overlaps with I7
        encoding I2 = { a = 0b0, b = 0b1 } // Overlaps with I7
      
        encoding I3 = { a = 0b1, b = 0b0, e = 0b00 }
        encoding I4 = { a = 0b1, b = 0b0, e = 0b01 }
        encoding I5 = { a = 0b0, b = 0b0, c = 0b00, e = 0b01 }
        encoding I6 = { a = 0b0, b = 0b0, c = 0b00, e = 0b10 }
        encoding I7 = { a = 0b0, c = 0b11 }
      }
      """;

  @Test
  void testEncodingConstraints() throws DuplicatedPassKeyException, IOException {

    /* GIVEN */
    var spec = TestUtils.compileToViam(TEST_ISA);
    var config = new GeneralConfiguration(Path.of("build/test-output"), DumpMode.NONE);

    var passManager = new PassManager();
    passManager.add(new VdtInputPreparationPass(config));
    passManager.add(new VdtConstraintSynthesisPass(config));
    passManager.add(new VdtLoweringPass(config));
    passManager.add(new VdtVerificationPass(config));

    /* WHEN */
    passManager.run(spec);

    /* THEN */
    final Node dt = getResult(passManager, VdtLoweringPass.class);
    Assertions.assertNotNull(dt);

    final DecisionTreeDecoder decoder = new DecisionTreeDecoder(dt);

    assertDecision(decoder, "00000000", "I1");
    assertDecision(decoder, "00000001", "I5");
    assertDecision(decoder, "00000010", "I6");
    assertDecision(decoder, "00000011", "I1");
    assertDecision(decoder, "00110000", "I7");

    assertDecision(decoder, "01000000", "I2");
    assertDecision(decoder, "01010000", "I2");
    assertDecision(decoder, "01100000", "I2");
    assertDecision(decoder, "01110000", "I7");
  }

  @Test
  void testEncodingConstraints_checkRemainingConditions()
      throws DuplicatedPassKeyException, IOException {

    /* GIVEN */
    var spec = TestUtils.compileToViam("""
        instruction set architecture TEST = {
        
          register X: Bits<5>
        
          format Format: Bits<8> =
          { a  [7..4]
          , b  [3..2]
          , c  [1..0]
          }
        
          instruction I1: Format = { }
        
          assembly I1 = ( mnemonic )
        
          [select when: a = 0b0000 && (b != 0b01 || c = 0b00)]
          encoding I1 = { a = 0b0000 }
        }
        """);
    var config = new GeneralConfiguration(Path.of("build/test-output"), DumpMode.NONE);

    var passManager = new PassManager();
    passManager.add(new VdtInputPreparationPass(config));
    passManager.add(new VdtConstraintSynthesisPass(config));
    passManager.add(new VdtLoweringPass(config));
    passManager.add(new VdtVerificationPass(config));

    /* WHEN */
    passManager.run(spec);

    /* THEN */
    final Node dt = getResult(passManager, VdtLoweringPass.class);
    Assertions.assertNotNull(dt);

    final DecisionTreeDecoder decoder = new DecisionTreeDecoder(dt);

    assertDecision(decoder, "0000 0000", "I1");
    assertDecision(decoder, "0000 0100", "I1");
    assertDecision(decoder, "0000 1001", "I1");

    assertNoDecision(decoder, "0000 0101");
    assertNoDecision(decoder, "0000 0110");
  }

  @ParameterizedTest
  @MethodSource("checkRemainingConditions_testSource")
  void testEncodingConstraints_checkRemainingConditions_parameterized(String fixedEncoding,
                                                                      String constraint)
      throws DuplicatedPassKeyException, IOException {

    /* GIVEN */
    var spec = TestUtils.compileToViam("""
        instruction set architecture TEST = {
        
          register X: Bits<5>
        
          format Format: Bits<8> =
          { a  [7..4]
          , b  [3..2]
          , c  [1..0]
          }
        
          instruction I1: Format = { }
        
          assembly I1 = ( mnemonic )
        
          [select when: %s]
          encoding I1 = { %s }
        }
        """.formatted(constraint, fixedEncoding));
    var config = new GeneralConfiguration(Path.of("build/test-output"), DumpMode.NONE);

    var passManager = new PassManager();
    passManager.add(new VdtInputPreparationPass(config));
    passManager.add(new VdtConstraintSynthesisPass(config));
    passManager.add(new VdtLoweringPass(config));
    passManager.add(new VdtVerificationPass(config));

    /* WHEN */
    passManager.run(spec);

    /* THEN */
    final Node dt = getResult(passManager, VdtLoweringPass.class);
    Assertions.assertNotNull(dt);

    var decisionTable = new CheckedBitsCollector(dt).collect();

    for (var checkedBits : decisionTable.entrySet()) {

      final vadl.viam.Instruction i = checkedBits.getKey();
      final BitPattern iPattern = toFixedBitPattern(i, ByteOrder.LITTLE_ENDIAN);

      final BitPattern checkedPattern = checkedBits.getValue();

      Assertions.assertEquals(
          iPattern.width(), checkedPattern.width(),
          "Instruction '%s' has a different width than the checked pattern.".formatted(
              i.simpleName()));

      Assertions.assertTrue(contains(checkedPattern, iPattern),
          "Expected the checked pattern '%s' to be more specific than or equal to the instruction's fixed bits '%s'."
              .formatted(checkedPattern, iPattern));
    }
  }

  static Stream<Arguments> checkRemainingConditions_testSource() {
    return Stream.of(
        Arguments.of("b = 0b00", "c = 0b01"),
        Arguments.of("b = 0b00", "b = 0b00"),
        Arguments.of("b = 0b00", "c != 0b01"),
        Arguments.of("b = 0b00", "c != 0b01 || a = 0b0000")
    );
  }

  @Test
  void test_handleEmptyUnmatchingConditions_test1() throws DuplicatedPassKeyException, IOException {

    /* GIVEN */
    final String vadl = """
        instruction set architecture TEST = {
        
          register X: Bits<5>
        
          format Format: Bits<8> =
          { a   [7..4]
          , b   [3..0]
          }
        
          instruction I1: Format = { }
          [ select when : b = 0b0010 ]
          encoding I1 = { a = 0b0000 }
          assembly I1 = ( mnemonic )
        
          instruction I2: Format = { }
          [ select when : b = 0b0001 ]
          encoding I2 = { a = 0b0000 }
          assembly I2 = ( mnemonic )
        }
        """;

    var config =
        new IssConfiguration(new GeneralConfiguration(Path.of("build/test-output"), DumpMode.NONE));
    var spec = TestUtils.compileToViam(vadl);

    var manager = new PassManager();
    manager.add(new VdtEncodingConstraintValidationPass(config));
    manager.add(new VdtInputPreparationPass(config));
    manager.add(new VdtEncodingSemanticVerificationPass(config));
    manager.add(new VdtLoweringPass(config));
    manager.add(new VdtVerificationPass(config));

    /* WHEN */
    manager.run(spec);

    /* THEN */
    var decodeTree = manager.getPassResults().lastResultOf(VdtLoweringPass.class, Node.class);

    Assertions.assertNotNull(decodeTree);

    var stats = DecisionTreeStatsCalculator.statistics(decodeTree);

    log.info("VDT: {}", stats);
    log.info("Decoder: \n{}", new TextGraphGenerator(decodeTree).generate());

    Assertions.assertEquals(1, stats.getMaxDepth());
    Assertions.assertEquals(2, stats.getNumberOfLeafNodes());

    MultiDecisionNode decisionNode = assertInstanceOf(MultiDecisionNode.class, decodeTree);
    Assertions.assertEquals(
        BitVector.fromValue(new BigInteger("FF", 16), 8),
        decisionNode.getMask());
  }

  @Test
  void test_handleEmptyUnmatchingConditions_test2() throws DuplicatedPassKeyException, IOException {

    /* GIVEN */
    final String vadl = """
        instruction set architecture TEST = {
        
          register X: Bits<5>
        
          format Format: Bits<8> =
          { a   [7..4]
          , b   [3..0]
          }
        
          instruction I1: Format = { }
          [ select when : b = 0b0010 || b = 0b0100 ]
          encoding I1 = { a = 0b0000 }
          assembly I1 = ( mnemonic )
        
          instruction I2: Format = { }
          [ select when : b = 0b0001 ]
          encoding I2 = { a = 0b0000 }
          assembly I2 = ( mnemonic )
        }
        """;

    var config =
        new IssConfiguration(new GeneralConfiguration(Path.of("build/test-output"), DumpMode.NONE));
    var spec = TestUtils.compileToViam(vadl);

    var manager = new PassManager();
    manager.add(new VdtEncodingConstraintValidationPass(config));
    manager.add(new VdtInputPreparationPass(config));
    manager.add(new VdtEncodingSemanticVerificationPass(config));
    manager.add(new VdtLoweringPass(config));
    manager.add(new VdtVerificationPass(config));

    /* WHEN */
    manager.run(spec);

    /* THEN */
    var decodeTree = manager.getPassResults().lastResultOf(VdtLoweringPass.class, Node.class);

    Assertions.assertNotNull(decodeTree);

    var stats = DecisionTreeStatsCalculator.statistics(decodeTree);

    log.info("VDT: {}", stats);
    log.info("Decoder: \n{}", new TextGraphGenerator(decodeTree).generate());

    Assertions.assertEquals(1, stats.getMaxDepth());
    Assertions.assertEquals(3, stats.getNumberOfLeafNodes());

    MultiDecisionNode decisionNode = assertInstanceOf(MultiDecisionNode.class, decodeTree);
    Assertions.assertEquals(
        BitVector.fromValue(new BigInteger("FF", 16), 8),
        decisionNode.getMask());
  }

  @SuppressWarnings("unchecked")
  private <T, U extends Pass> T getResult(PassManager passManager, Class<U> passType) {
    return (T) passManager.getPassResults().lastResultOf(passType);
  }

  private DecodeEntry toDecodeEntry(Instruction insn) {
    return new DecodeEntry(insn.source(), insn.width(), insn.pattern(), Set.of());
  }

  private DecodeEntry toDecodeEntry(Instruction insn, String... exclusionPattern) {
    Set<ExclusionCondition> exclusions = Arrays.stream(exclusionPattern)
        .map(s -> {
          s = s.replace(" ", "");
          BitPattern matching = BitPattern.fromString(s, s.length());
          return new ExclusionCondition(matching, Set.of());
        })
        .collect(Collectors.toSet());
    return new DecodeEntry(insn.source(), insn.width(), insn.pattern(), exclusions);
  }

  private DecodeEntry toDecodeEntry(Instruction insn, ExclusionCondition... exclusions) {
    return new DecodeEntry(insn.source(), insn.width(), insn.pattern(), Set.of(exclusions));
  }

  private ExclusionCondition exclude(String matchingPattern, String... unmatchingPattern) {
    matchingPattern = matchingPattern.replace(" ", "");
    BitPattern matching = BitPattern.fromString(matchingPattern, matchingPattern.length());
    Set<BitPattern> unmatching = Arrays.stream(unmatchingPattern)
        .map(p -> {
          p = p.replace(" ", "");
          return BitPattern.fromString(p, p.length());
        })
        .collect(Collectors.toSet());
    return new ExclusionCondition(matching, unmatching);
  }

  private void assertDecision(DecisionTreeDecoder decoder, String insn, String expectedName) {
    insn = insn.replace(" ", "");
    Instruction decision = decoder.decide(BitVector.fromString(insn, insn.length()));
    Assertions.assertNotNull(decision);
    Assertions.assertEquals(expectedName, decision.source().simpleName());
  }

  private void assertNoDecision(DecisionTreeDecoder decoder, String insn) {
    insn = insn.replace(" ", "");
    String finalInsn = insn;
    RuntimeException e = Assertions.assertThrows(RuntimeException.class,
        () -> decoder.decide(BitVector.fromString(finalInsn, finalInsn.length())));
    Assertions.assertTrue(e.getMessage().startsWith("No decision found"));
  }

  /**
   * Checks whether the first pattern contains all fixed bits of the second pattern. I.e. whether
   * p1 is more (or equally) specific than p2.
   *
   * @param p1 The first pattern to check.
   * @param p2 The second pattern to check.
   * @return true if p1 contains all fixed bits of p2, false otherwise.
   */
  private boolean contains(BitPattern p1, BitPattern p2) {
    return IntStream.range(0, p1.width())
        .allMatch(
            i -> p1.get(i).equals(p2.get(i)) || p2.get(i).getValue() == PBit.Value.DONT_CARE);
  }
}
