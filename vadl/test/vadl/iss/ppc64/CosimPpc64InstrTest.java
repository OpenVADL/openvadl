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

package vadl.iss.ppc64;

import java.io.IOException;
import java.math.BigInteger;
import java.util.stream.Stream;
import net.jqwik.api.Arbitraries;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.iss.AsmTestBuilder;
import vadl.iss.CosimInstrTest;
import vadl.iss.CosimTestUtils;

public class CosimPpc64InstrTest extends CosimInstrTest {

  @Override
  public int getTestPerInstruction() {
    return 100;
  }

  @Override
  public String getVadlSpec() {
    return "sys/ppc64/ppc64.vadl";
  }

  @Override
  public AsmTestBuilder getBuilder(String testNamePrefix, int id) {
    return new Ppc64TestBuilder(testNamePrefix + id);
  }

  @Override
  public String compiler() {
    return "ppc64_compiler.py";
  }

  @Override
  public String cosimConfig() {
    return "ppc64_config.toml";
  }

  @Override
  public String withUpstreamTarget() {
    return "ppc64-softmmu";
  }

  // needed for all other tests
  @TestFactory
  Stream<DynamicTest> li() throws IOException {
    return testTRegImmS16Instruction("li", "LI");
  }

  @TestFactory
  Stream<DynamicTest> add() throws IOException {
    return testTSSRegInstruction("add", "ADD");
  }

  private Stream<DynamicTest> testTSSRegInstruction(String instruction, String testNamePrefix)
      throws IOException {
    return runTestsWith(id -> {
      var b = getBuilder(testNamePrefix, id);
      var regSrc1 = b.anyTempReg().sample();
      var regSrc2 = b.anyTempReg().sample();
      b.fillRegSigned(regSrc1, 16);
      b.fillRegSigned(regSrc2, 16);
      var regDest = b.anyTempReg().sample();
      b.add("%s %s, %s, %s", instruction, regDest, regSrc1, regSrc2);
      return new CosimTestUtils.TestCase(testNamePrefix + id, b.toAsmString());
    });
  }

  private Stream<DynamicTest> testTRegImmS16Instruction(String instruction, String testNamePrefix)
      throws IOException {
    return runTestsWith(id -> {
      var b = getBuilder(testNamePrefix, id);
      var regDest = b.anyTempReg().sample();
      b.add("%s %s, %s", instruction, regDest, arbitraryImmS(16));
      return new CosimTestUtils.TestCase(testNamePrefix + id, b.toAsmString());
    });
  }

  public static String arbitraryImmS(int bits) {
    var b = BigInteger.ONE.shiftLeft(bits - 1);
    return Arbitraries.bigIntegers()
        .greaterOrEqual(b.negate())
        .lessOrEqual(b.subtract(BigInteger.ONE))
        .sample()
        .toString();
  }

}
