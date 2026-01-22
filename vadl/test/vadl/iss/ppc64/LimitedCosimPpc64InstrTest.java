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
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestMethodOrder;

/* Tests ppc64.vadl instructions against the QEMU ppc64 simulator.
 * Instructions in this class are tested using only a subset of possible values and/or are
 * not tested in all available modes.
 */

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LimitedCosimPpc64InstrTest extends AbstractCosimPpc64InstrTest {

  @TestFactory
  @Order(1)
  Stream<DynamicTest> mtspr() throws IOException {
    return runTests3264With(id -> {
      var b = getBuilder("MTSPR", id);
      var regSrc = b.anyReg().sample();
      b.fillReg(regSrc);
      b.add("mtspr %s, %s", b.anySpecialReg().sample(), regSrc);
      return b.toTestCase();
    });
  }

  @TestFactory
  @Order(2)
  Stream<DynamicTest> mfspr() throws IOException {
    return runTests3264With(id -> {
      var b = getBuilder("MFSPR", id);
      var regSrc = b.anyReg().sample();
      var spr = b.anySpecialReg().sample();
      b.fillReg(regSrc);
      b.add("mtspr %s, %s", spr, regSrc);
      b.add("mfspr %s, %s", regSrc, spr);
      return b.toTestCase();
    });
  }

  /* L = 0 doesn't work
  @TestFactory
  @Order(3)
  Stream<DynamicTest> mtmsr() throws IOException {
    return runTests3264With(id -> {
      var b = getBuilder("MTMSR", id);
      var regSrc = b.anyReg().sample();
      b.fillReg(regSrc);
      b.add("mtmsr %s, %s", regSrc, b.anyImmU(1));
      return b.toTestCase();
    });
  }

  @TestFactory
  @Order(4)
  Stream<DynamicTest> mfmsr() throws IOException {
    return runTests3264With(id -> {
      var b = getBuilder("MFMSR", id);
      var regSrc = b.anyReg().sample();
      b.fillReg(regSrc);
      b.add("mtmsr %s, %s", regSrc, b.anyImmU(1));
      b.add("mfmsr %s", b.anyReg().sample());
      return b.toTestCase();
    });
  }
  */

  @SuppressWarnings("MethodName")
  @TestFactory
  @Order(5)
  Stream<DynamicTest> b() throws IOException {
    return testIFormBranchInstruction_LA("b", "B");
  }

  @TestFactory
  @Order(6)
  Stream<DynamicTest> bc() throws IOException {
    return testBFormBranchInstruction_LA("bc", "BC");
  }

  private Stream<DynamicTest> testIFormBranchInstruction_LA(String instruction, String name)
      throws IOException {
    var s1 = testIFormBranchInstruction(instruction, name);
    var s2 = testIFormBranchInstruction(instruction + "a", name + "A");
    var s3 = testIFormBranchInstruction(instruction + "l", name + "L");
    var s4 = testIFormBranchInstruction(instruction + "la", name + "LA");
    return Stream.concat(Stream.concat(s1, s2), Stream.concat(s3, s4));
  }

  private Stream<DynamicTest> testBFormBranchInstruction_LA(String instruction, String name)
      throws IOException {
    var s1 = testBFormBranchInstruction(instruction, name);
    var s2 = testBFormBranchInstruction(instruction + "a", name + "A");
    var s3 = testBFormBranchInstruction(instruction + "l", name + "L");
    var s4 = testBFormBranchInstruction(instruction + "la", name + "LA");
    return Stream.concat(Stream.concat(s1, s2), Stream.concat(s3, s4));
  }

  private Stream<DynamicTest> testIFormBranchInstruction(String instruction, String name)
      throws IOException {
    return runTests3264With(id -> {
      var b = getBuilder(name, id);
      var target = instruction.contains("a") ? "0xFC" : "-4";
      b.add("%s %s", instruction, target);
      b.add("li 0, 1"); // shouldn't be reached
      return b.toTestCase();
    });
  }

  private Stream<DynamicTest> testBFormBranchInstruction(String instruction, String name)
      throws IOException {
    return runTests3264With(id -> {
      var b = getBuilder(name, id);
      var target = instruction.contains("a") ? "0xFC" : "-28";
      b.fillCR();
      b.fillReg("0");
      b.add("mtspr 9, 0");
      b.add("%s %s, %s, %s", instruction, b.getBOField(), b.anyCRBit().sample(), target);
      b.add("li 0, 1");
      return b.toTestCase();
    });
  }

}
