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

package vadl.vdt.passes;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.TestUtils;
import vadl.error.Diagnostic;
import vadl.viam.Encoding;

@SuppressWarnings("OverloadMethodsDeclarationOrder")
public class VdtEncodingConstraintTest {

  private static String wrapProg(String formulas) {
    return """
        instruction set architecture TEST = {
        
          register X: Bits<5>
        
          format Format: Bits<32> =
          { one   [31..15]
          , two   [14..10]
          , three [9]
          , four  [8..0]
          , accFunc = one as SInt
          }
        
          instruction Instr: Format = { }
          [ unmatch when : %s ]
          encoding Instr = { three = 0b1 }
          assembly Instr = ( mnemonic )
        }
        """.formatted(formulas);
  }

  static Stream<String> validFormulas() {
    return Stream.of(
        "0b1 = one",
        "0b1 != one(0)",
        "one(4..2) = 0b10 && four = 0xF",
        "one(0) = 1 || (two(0) = 1 && one = 1)"
    );
  }

  @MethodSource("validFormulas")
  @ParameterizedTest
  void validFormulas(String formula) {
    assertThatNoException().isThrownBy(() -> {
      var spec = TestUtils.compileToViam(wrapProg(formula));
      var encoding =
          TestUtils.findDefinitionByNameIn("TEST::Instr::encoding", spec, Encoding.class);
      var validator = new ConstraintValidator(encoding);
      validator.check();
    });
  }

  // TODO: There are more checks to be made such as (0b10 = 0b01) but they currently crash in the
  //   frontend.
  static Stream<Arguments> invalidFormulas() {
    return Stream.of(
        Arguments.of("0b01", "Expression must be a Bool"),
        Arguments.of("0b1 as Bool", "Expected a comparison or logical operation"),
        Arguments.of("accFunc = 0b10",
            "Field access calls are disallowed in encoding constraints."),
        Arguments.of("three = 0b1",
            "The field is set by the encoding and can therefore not be used for encoding constraints."),
        Arguments.of("four(4..0) = two",
            "Exactly one side must be a constant, the other a format field or a slice."),
        Arguments.of("one(0) = 1 && (two(0) = 1 || one = 1)",
            "Expected a comparison using `=` or `!=` between a format field and a constant."),
        Arguments.of("one as Bits<1> = 1",
            "Only format fields, constant values, and slices on format fields are allowed as terms."),
        Arguments.of("one <= 1",
            "Expected a comparison using `=` or `!=` between a format field and a constant."),
        Arguments.of("X = 0b10",
            "Resource read operations (e.g., memory or I/O accesses) are not permitted in encoding constraints.")
    );
  }

  @MethodSource("invalidFormulas")
  @ParameterizedTest
  void validFormulas(String formula, String error) {
    assertThatThrownBy(() -> {
      var spec = TestUtils.compileToViam(wrapProg(formula));
      var encoding =
          TestUtils.findDefinitionByNameIn("TEST::Instr::encoding", spec, Encoding.class);

      var validator = new ConstraintValidator(encoding);
      validator.check();
    })
        .isExactlyInstanceOf(Diagnostic.class)
        .hasMessageContaining(error);
  }

}
