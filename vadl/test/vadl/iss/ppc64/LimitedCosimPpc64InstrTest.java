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

package vadl.iss.ppc64;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import vadl.iss.CosimTestUtils;

/* Tests ppc64.vadl instructions against the QEMU ppc64 simulator.
 * Covers all instructions not tested in CosimPpc64InstrTest.java.
 * Instructions in this class are tested using only a subset of possible values and/or are
 * not tested in all available modes.
 */

public class LimitedCosimPpc64InstrTest extends AbstractCosimPpc64InstrTest {

  @Test
  void instructions() throws IOException {
    runQemuInstrTests(
        generateIssSimulator(getVadlSpec()),
        buildInstructionTests());
  }

  private List<CosimTestUtils.TestCase> buildInstructionTests() throws IOException {
    return concatInstructionTests(List.of(
        this::divbug,
        this::mtspr,
        this::mfspr,
        this::mtmsr,
        this::mfmsr,
        () -> testBranchInstruction_LA("b", "B"),
        () -> testBranchConditionalInstruction_LA("bc", "BC"),
        () -> testBranchConditionalToCTRInstruction_L("bcctr", "BCCTR"),
        () -> testBranchConditionalToLRInstruction_L("bclr", "BCLR"),
        () -> testBranchConditionalToTARInstruction_L("bctar", "BCTAR")));
  }

  private List<CosimTestUtils.TestCase> concatInstructionTests(List<InstructionTestFactory> testFactories)
      throws IOException {
    List<CosimTestUtils.TestCase> tests = new ArrayList<>();
    for (var testFactory : testFactories) {
      tests.addAll(testFactory.create());
    }
    return tests;
  }

  @FunctionalInterface
  private interface InstructionTestFactory {
    List<CosimTestUtils.TestCase> create() throws IOException;
  }

  private List<CosimTestUtils.TestCase> divbug() throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder("DIV BUG", id);
      b.add("lis 23, -32768                           # X(23) := 0xFFFFFFFF80000000 (-2147483648)\n"
          + "ori 23, 23, 0                            # ↑\n"
          + "lis 5, -1                                # X( 5) := 0xFFFFFFFFFFFFFFFF (-1)\n"
          + "ori 5, 5, 65535                          # ↑\n"
          + "divweo. 3, 23, 5");
      return b.toTestCase();
    });
  }

  private List<CosimTestUtils.TestCase> mtspr() throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder("MTSPR", id);
      var regSrc = b.anyReg().sample();
      b.fillReg(regSrc);
      b.add("mtspr %s, %s", b.anyImplementedSpecialReg().sample(), regSrc);
      return b.toTestCase();
    });
  }

  private List<CosimTestUtils.TestCase> mfspr() throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder("MFSPR", id);
      var regSrc = b.anyReg().sample();
      var spr = b.anyImplementedSpecialReg().sample();
      b.fillReg(regSrc);
      b.add("mtspr %s, %s", spr, regSrc);
      b.add("mfspr %s, %s", b.anyReg().sample(), spr);
      return b.toTestCase();
    });
  }

  private List<CosimTestUtils.TestCase> mtmsr() throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder("MTMSR", id);
      var regSrc = b.anyReg().sample();
      var val = b.getImmS(32)
          .clearBit(5)                 // disable IR
          .clearBit(9).clearBit(10) // set TE to 0b00
          .clearBit(14)                // disable PR
          .clearBit(15);               // disable EE
      b.fillReg(regSrc, val);
      b.add("mtmsr %s, %s", regSrc, b.getImmU(1));
      return b.toTestCase();
    });
  }

  private List<CosimTestUtils.TestCase> mfmsr() throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder("MFMSR", id);
      var regSrc = b.anyReg().sample();
      var val = b.getImmS(32)
          .clearBit(5)                 // disable IR
          .clearBit(9).clearBit(10) // set TE to 0b00
          .clearBit(14)                // disable PR
          .clearBit(15);               // disable EE
      b.fillReg(regSrc, val);
      b.add("mtmsr %s, %s", regSrc, 0);
      b.add("mfmsr %s", b.anyReg().sample());
      return b.toTestCase();
    });
  }

  private List<CosimTestUtils.TestCase> testBranchInstruction_LA(String instruction, String name)
      throws IOException {
    return List.of(
        testBranchInstruction(instruction, name),
        testBranchInstruction(instruction + "a", name + "A"),
        testBranchInstruction(instruction + "l", name + "L"),
        testBranchInstruction(instruction + "la", name + "LA"))
        .stream()
        .flatMap(List::stream)
        .toList();
  }

  private List<CosimTestUtils.TestCase> testBranchConditionalInstruction_LA(String instruction, String name)
      throws IOException {
    return List.of(
        testBranchConditionalInstruction(instruction, name),
        testBranchConditionalInstruction(instruction + "a", name + "A"),
        testBranchConditionalInstruction(instruction + "l", name + "L"),
        testBranchConditionalInstruction(instruction + "la", name + "LA"))
        .stream()
        .flatMap(List::stream)
        .toList();
  }

  private List<CosimTestUtils.TestCase> testBranchConditionalToCTRInstruction_L(String instruction, String name)
      throws IOException {
    return List.of(
        testBranchConditionalToCTRInstruction(instruction, name),
        testBranchConditionalToCTRInstruction(instruction + "l", name + "L"))
        .stream()
        .flatMap(List::stream)
        .toList();
  }

  private List<CosimTestUtils.TestCase> testBranchConditionalToLRInstruction_L(String instruction, String name)
      throws IOException {
    return List.of(
        testBranchConditionalToLRInstruction(instruction, name),
        testBranchConditionalToLRInstruction(instruction + "l", name + "L"))
        .stream()
        .flatMap(List::stream)
        .toList();
  }

  private List<CosimTestUtils.TestCase> testBranchConditionalToTARInstruction_L(String instruction, String name)
      throws IOException {
    return List.of(
        testBranchConditionalToTARInstruction(instruction, name),
        testBranchConditionalToTARInstruction(instruction + "l", name + "L"))
        .stream()
        .flatMap(List::stream)
        .toList();
  }

  private List<CosimTestUtils.TestCase> testBranchInstruction(String instruction, String name)
      throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder(name, id);
      var target = instruction.contains("a") ? "252" : "-4";
      b.add("%s %s", instruction, target);
      b.add("li 0, 1"); // shouldn't be reached
      return b.toTestCase();
    });
  }

  private List<CosimTestUtils.TestCase> testBranchConditionalInstruction(String instruction, String name)
      throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder(name, id);
      var target = instruction.contains("a") ? "252" : "-28";
      b.fillCR();
      b.fillReg("0");
      b.add("mtspr 9, 0");
      b.add("%s %s, %s, %s", instruction, b.getBOField(), b.anyCRBit().sample(), target);
      b.add("li 0, 1");
      return b.toTestCase();
    });
  }

  private List<CosimTestUtils.TestCase> testBranchConditionalToCTRInstruction(String instruction, String name)
      throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder(name, id);
      b.fillCR();
      b.fillReg("0", BigInteger.valueOf(252));
      b.add("mtspr 9, 0");
      b.add("%s %s, %s, 0", instruction, b.getLimitedBOField(), b.anyCRBit().sample());
      b.add("li 0, 1");
      return b.toTestCase();
    });
  }

  private List<CosimTestUtils.TestCase> testBranchConditionalToLRInstruction(String instruction, String name)
      throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder(name, id);
      b.fillCR();
      b.fillReg("0", BigInteger.valueOf(252));
      b.add("mtspr 8, 0");
      b.fillReg("0");
      b.add("mtspr 9, 0");
      b.add("%s %s, %s, 0", instruction, b.getBOField(), b.anyCRBit().sample());
      b.add("li 0, 1");
      return b.toTestCase();
    });
  }

  private List<CosimTestUtils.TestCase> testBranchConditionalToTARInstruction(String instruction, String name)
      throws IOException {
    return buildTests3264With(id -> {
      var b = getBuilder(name, id);
      b.fillCR();
      b.fillReg("0", BigInteger.valueOf(252));
      b.add("mtspr 815, 0");
      b.fillReg("0");
      b.add("mtspr 9, 0");
      b.add("%s %s, %s, 0", instruction, b.getBOField(), b.anyCRBit().sample());
      b.add("li 0, 1");
      return b.toTestCase();
    });
  }

}
