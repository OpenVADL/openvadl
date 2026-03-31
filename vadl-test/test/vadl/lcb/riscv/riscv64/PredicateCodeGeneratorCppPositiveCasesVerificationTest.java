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

package vadl.lcb.riscv.riscv64;

import java.util.stream.Stream;
import net.jqwik.api.Arbitraries;
import vadl.cppCodeGen.model.GcbCppFunctionWithBody;
import vadl.lcb.AbstractPredicateCodeGeneratorCppVerificationTest;
import vadl.viam.Format;

/**
 * This test classes tests the instruction immediates where the predicate should match.
 */
public class PredicateCodeGeneratorCppPositiveCasesVerificationTest extends
    AbstractPredicateCodeGeneratorCppVerificationTest {

  @Override
  public String specification() {
    return "sys/risc-v/rv64im.vadl";
  }

  @Override
  public Stream<Test> inputs() {
    return Stream.of(
        new Test(
            "ADDI",
            "immS",
            Arbitraries.integers().greaterOrEqual(-2048).lessOrEqual(2047)),
        new Test(
            "SW",
            "immS",
            Arbitraries.integers().greaterOrEqual(-2048).lessOrEqual(2047)),
        new Test(
            "BEQ",
            "immS",
            Arbitraries.integers().greaterOrEqual(-4096).lessOrEqual(4094)
                .filter(x -> x % 2 == 0)),
        new Test(
            "JAL",
            "immS",
            Arbitraries.integers().greaterOrEqual(-1048576).lessOrEqual(1048574)
                .filter(x -> x % 2 == 0)
        )
    );
  }

  @Override
  public String render(GcbCppFunctionWithBody record, Format.FieldAccess fieldAccess, int sample) {
    return renderPositive(record, fieldAccess, sample);
  }
}
