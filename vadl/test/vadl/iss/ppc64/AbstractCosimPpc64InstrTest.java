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
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import vadl.iss.CosimInstrTest;
import vadl.iss.CosimTestUtils;

public class AbstractCosimPpc64InstrTest extends CosimInstrTest {

  public Ppc64TestBuilder getBuilder(String name, int id) {
    return new Ppc64TestBuilder(name, id);
  }

  @Override
  public int getTestPerInstruction() {
    return 1000;
  }

  @Override
  public String getVadlSpec() {
    return "sys/ppc64/ppc64.vadl";
  }

  @Override
  protected String getScriptFolder() {
    return "ppc64";
  }

  @Override
  public String getCosimConfigFileName() {
    return "ppc64_config.toml";
  }

  @Override
  public String withUpstreamTarget() {
    return "ppc64-softmmu";
  }

  // runs tests in 32- and 64-bit mode
  public Stream<DynamicTest> runTests3264With(
      Function<Integer, CosimTestUtils.TestCase> generators)
      throws IOException {
    Function<Integer, CosimTestUtils.TestCase> tests32 = id -> {
      CosimTestUtils.TestCase test = generators.apply(id);
      return new CosimTestUtils.TestCase(test.id() + " (32-bit)", false, test.asmCore());
    };
    Function<Integer, CosimTestUtils.TestCase> tests64 = id -> {
      CosimTestUtils.TestCase test = generators.apply(id);
      return new CosimTestUtils.TestCase(test.id() + " (64-bit)", false, "trap\n" + test.asmCore());
    };
    Stream<DynamicTest> dynamicTests32 = runTestsWith(tests32);
    Stream<DynamicTest> dynamicTests64 = runTestsWith(tests64);
    return Stream.concat(dynamicTests32, dynamicTests64);
  }

}
