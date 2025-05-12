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

package vadl.ast;


import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.error.Diagnostic;
import vadl.utils.ViamUtils;
import vadl.viam.Function;
import vadl.viam.passes.verification.ViamVerifier;

public class CallIndexExprTest {

  private static String wrapProg(String defs) {
    return """
        instruction set architecture TEST = {
          register A: Bits<32>
          register X: Bits<5> -> Bits<32>
        
          alias register B: Format = A
          alias register Z: Format = X(1)
        
          memory MEM: Bits<64> -> Bits<8>
        
          function D -> Bits<32> = 32
          function E(a: Bits<32>, b: Bits<32>) -> Bits<32> = a + b
        
          format Format: Bits<32> =
          { one   [30..16]
          , two   [31, 6..2, 15..10]
          , three [9]
          , four  [8..7, 1..0]
          }
        
          %s
        }
        processor CPU implements TEST = {}
        """.formatted(defs);
  }

  static Stream<String> validReadInputs() {
    return Stream.of(
        "function T -> Bits<32> = D",
        "function T -> Bits<1> = D(2)",
        "function T -> Bits<10> = D(10..1)",
        "function T -> Bits<2> = D(4, 2)",
        "function T -> Bits<2> = D(10..1)(4, 2)",
        "function T -> Bits<2> = E(2, 3)(4, 2)",
        "function T -> Bits<1> = E(2, 3)(4, 2)(1)",
        "function T -> Bits<1> = A(1)",
        "function T -> Bits<10> = A(11..2)",
        "function T  -> Bits<32> = X(2)",
        "function T  -> Bits<1> = X(2)(1)",
        "function T  -> Bits<10> = X(2)(11..2)",

        "function T  -> Bits<15> = B.one",
        "function T  -> Bits<6> = B.one(10..5)",
        "function T  -> Bits<6> = B(10..5)",
        "function T  -> Bits<6> = Z(10..5)",
        "function T  -> Bits<1> = Z.three",

        "function T  -> Bits<24> = MEM<3>(2)",
        "function T  -> Bits<6> = MEM<3>(2)(10..5)",
        "function T  -> Bits<4> = MEM(2)(5..2)",
        "function T  -> Bits<4> = MEM(2)(4, 2, 3, 4)",

        "function T  -> Bits<10> = X(2)(11..2)"
    );
  }

  @MethodSource("validReadInputs")
  @ParameterizedTest
  void validReads(String input) {
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(wrapProg(input)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var lowering = new ViamLowering();
    var spec = Assertions.assertDoesNotThrow(() -> lowering.generate(ast), "Cannot generate VIAM");
    ViamVerifier.verifyAllIn(spec);
    var testFuncs = ViamUtils.findDefinitionsByFilter(spec,
            d -> d instanceof Function && d.simpleName().startsWith("T"))
        .stream().toList();
    assertThat(testFuncs).size().isEqualTo(1);
  }


  static Stream<String> validWriteInputs() {
    return Stream.of(
        "A(10) := 1",
        "A(10, 12) := 3",
        "A(10..5) := 63",
        "B.one := 63",
        "Z.one := 63",
        "MEM(2) := 63",
        "MEM(2)(7..5) := 3",
        "MEM<2>(2) := 4",
        "MEM<2>(2)(11..6) := 4",
        ""
    );
  }

  @MethodSource("validWriteInputs")
  @ParameterizedTest
  void validWrites(String input) {
    var prog = """
        instruction T: Format = {
        %s
        }
        encoding T = {two = 3}
        assembly T = ""
        """.formatted(input);

    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(wrapProg(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var lowering = new ViamLowering();
    var spec = Assertions.assertDoesNotThrow(() -> lowering.generate(ast), "Cannot generate VIAM");
    ViamVerifier.verifyAllIn(spec);
    var testFuncs = ViamUtils.findDefinitionsByFilter(spec,
            d -> d instanceof Function && d.simpleName().startsWith("T"))
        .stream().toList();
    assertThat(testFuncs).size().isEqualTo(1);
  }

  static Stream<Arguments> invalidReadInputs() {
    return Stream.of(
        Arguments.of("function T -> Bits<32> = D(1)", "Expected `Bits<32>` but got `Bits<1>`"),
        Arguments.of("function T -> Bits<32> = E(1)(2)", "Expected 2 arguments but got 1."),
        Arguments.of("function T -> Bits<32> = E(1,2,3)", "Expected 2 arguments but got 3."),
        Arguments.of("function T -> Bits<32> = E(1,2)(32..1)",
            "Range start 32 out of bounds for `Bits<32>`"),
        Arguments.of("function T -> Bits<32> = B.one", "Expected `Bits<32>` but got `Bits<15>`"),
        Arguments.of("function T -> Bits<1> = MEM<2>(2, 1)",
            "Expected 1 arguments but got 2.")
    );
  }

  @MethodSource("invalidReadInputs")
  @ParameterizedTest
  void invalidReads(String input, String error) {
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(wrapProg(input)), "Cannot parse input");
    var typechecker = new TypeChecker();
    var diag = Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    assertThat(diag).hasMessageContaining(error);
  }
}
