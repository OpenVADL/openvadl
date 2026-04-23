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
import vadl.cppCodeGen.model.GcbCppFunctionWithBody;
import vadl.lcb.AbstractPredicateCodeGeneratorCppVerificationTest;

/**
 * This test classes tests the instruction immediates where the predicate should not match.
 */
public class PredicateCodeGeneratorCppNegativeCasesVerificationTest extends
    AbstractPredicateCodeGeneratorCppVerificationTest {

  @Override
  public String specification() {
    return "sys/risc-v/rv64im.vadl";
  }

  @Override
  public Stream<Test> inputs() {
    return Stream.of(new Test(
            "ADDI",
            "immS",
            allIntegersExcept(-2048, 2047)
        ),
        new Test(
            "SW",
            "immS",
            allIntegersExcept(-2048, 2047)
        ),
        new Test(
            "BEQ",
            "immS",
            allIntegersExcept(-4096, 4094)
        ),
        new Test(
            "JAL",
            "immS",
            allIntegersExcept(-1048576, 1048574)
        )
    );
  }

  @Override
  public String render(GcbCppFunctionWithBody record, int sample) {
    return renderNegative(record, sample);
  }
}
