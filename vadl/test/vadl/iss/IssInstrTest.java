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

package vadl.iss;

import java.io.IOException;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;

public abstract class IssInstrTest extends QemuIssTest {

  abstract int getTestPerInstruction();

  abstract String getVadlSpec();

  abstract AsmTestBuilder getBuilder(String testNamePrefix, int id);

  @SafeVarargs
  protected final Stream<DynamicTest> runTestsWith(
      Function<Integer, IssTestUtils.TestSpec>... generators) throws IOException {
    return runTestsWith(getTestPerInstruction(), generators);
  }

  @SafeVarargs
  protected final Stream<DynamicTest> runTestsWith(
      int runs,
      Function<Integer, IssTestUtils.TestSpec>... generators) throws IOException {
    var image = generateIssSimulator(getVadlSpec());
    var testCases = Stream.of(generators)
        .flatMap(genFunc -> IntStream.range(0, runs)
            .mapToObj(genFunc::apply)
        )
        .toList();
    return runQemuInstrTests(image, testCases);
  }

}
