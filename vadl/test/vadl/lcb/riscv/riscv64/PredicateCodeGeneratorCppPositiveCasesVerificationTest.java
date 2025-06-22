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
import vadl.cppCodeGen.common.GcbAccessOrPredicateFunctionCodeGenerator;
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
    return Stream.of(new Test(
        "ADDI",
        "immS",
        Arbitraries.integers().greaterOrEqual(-2048).lessOrEqual(2047)));
  }

  @Override
  public String render(GcbCppFunctionWithBody record, Format.FieldAccess fieldAccess, int sample) {
    var predicateFunctionGenerator =
        new GcbAccessOrPredicateFunctionCodeGenerator(record.header(), fieldAccess,
            record.header().functionName().lower());

    var predicateFunction = predicateFunctionGenerator.genFunctionDefinition();

    String cppCode = String.format("""
            #include <cstdint>
            #include <iostream>
            #include <bitset>
            #include <vector>
            
            // Imported by manual copy mapping
            #include "/vadl-builtins.h"
            
            template<int start, int end, std::size_t N>
            std::bitset<N> project_range(std::bitset<N> bits)
            {
                std::bitset<N> result;
                size_t result_index = 0; // Index for the new bitset
            
                // Extract bits from the range [start, end]
                for (size_t i = start; i <= end; ++i) {
                  result[result_index] = bits[i];
                  result_index++;
                }
            
                return result;
            }
            
            template<std::size_t N, std::size_t M>
            std::bitset<N> set_bits(std::bitset<N> dest, const std::bitset<M> source, std::vector<int> bits) {
                auto target = 0;
                for (int i = bits.size() - 1; i >= 0 ; --i) {
                    auto j = bits[target];
                    dest.set(j, source[i]);
                    target++;
                }
            
                return dest;
            }
            
            %s
            
            int main() {
              auto actual = %s(%d);
              if(actual) {
                std::cout << "ok" << std::endl;
                return 0;
              } else {
                std::cout << "not ok" << std::endl;
                return -1;
              }
            }
            """,
        predicateFunction,
        record.header().identifier.lower(),
        sample);

    return cppCode;
  }
}
