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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.error.Diagnostic;

public class FormatFieldAccessTest {

  private String wrapSpec(String prog) {
    return """
        instruction set architecture TEST =
        {
          register X: Bits<32>
        
          format Format: Bits<32> =
          { one   [31..15]
          , two   [14..10]
          , three [9]
          , four  [8..0]
          , accOne = one as SInt
          %s
          }
        }
        """.formatted(prog);
  }


  static Stream<Arguments> invalidCases() {
    return Stream.of(
        Arguments.of("""
            , accTwo = X as SInt
            """, "Resource access is not allowed in field access function."),
        Arguments.of("""
            , accOne :- X as Bool
            """, "Resource access is not allowed in field access predicate."),
        Arguments.of("""
            , one := X as Bits<17>
            """, "Resource access is not allowed in field access encoding."),
        Arguments.of("""
            , accTwo = two as SInt<3>
            , two := accTwo
            """, "Expected `Bits<5>` but got `SInt<3>`."),
        Arguments.of("""
            , accOne :- 1 as Bits<1>
            """, "The predicate must be a `Bool` expression, but was `Bits<1>`."),
        Arguments.of("""
                , two := accOne as Bits<5>
                """,
            "At least one of the field accesses must use the target field `two` in its access functions."),
        Arguments.of("""
                , accTwoThree = three as Bits<5> + two
                """,
            "The encoding for this access function cannot be generated, as it uses multiple format fields."),
        Arguments.of("""
                , accTwoThree = three as Bits<5> + two
                , three := accTwoThree as Bits<1>
                """,
            "The encoding for this access function cannot be generated, as it uses multiple format fields. Each used field needs an encoding."),
        Arguments.of("""
                , accTwo = two
                , one := accOne
                , one := accTwo as Bits<17> + accOne
                """,
            "Field `one` is already target field for a subset of access functions.")
    );
  }

  @MethodSource("invalidCases")
  @ParameterizedTest
  void invalidCases(String prog, String expectedError) {
    assertThatThrownBy(() -> {
      var ast = VadlParser.parse(wrapSpec(prog));
      new Ungrouper().ungroup(ast);
      new ModelRemover().removeModels(ast);
      var typechecker = new TypeChecker();
      typechecker.verify(ast);
      var lowering = new ViamLowering();
      lowering.generate(ast);
    })
        .isExactlyInstanceOf(Diagnostic.class)
        .hasMessageContaining(expectedError);
  }

  @Test
  void validCase() {
    var spec = """
        instruction set architecture TEST = {
          format BitFieldMoveFormat: Bits<32> = 
          { sf         :  Bits<1>            
          , op         :  Bits<8>              
          , N          :  Bits<1>              
          , immr       :  Bits<6>              
          , imms       :  Bits<6>              
          , rn         :  Bits<5>              
          , rd         :  Bits<5>              
          , leftWSize  =  (-1 as Bits<5> - imms(4..0)) as Bits<64>
          , leftXSize  =  (-1 as Bits<6> - imms) as Bits<64>
          , rightWSize =  (immr(4..0) - imms(4..0) - 1) as Bits<64>
          , rightXSize =  (immr - imms - 1) as Bits<64>
          , imms       :=  -1 as Bits<6> - leftWSize as Bits<6>
          , imms       :=  -1 as Bits<6> - leftXSize as Bits<6>
          , immr       := (rightWSize - leftWSize) as Bits<6>
          , immr       := (rightXSize - leftXSize) as Bits<6>
          , leftWSize  :- leftWSize  as Bits<5> as Bits<64> = leftWSize
          , leftXSize  :- leftXSize  <= 63
          , rightWSize :- rightWSize <= 31
          , rightXSize :- rightXSize as Bits<6> as Bits<64> = rightXSize
          }
        }
        """;

    var ast = VadlParser.parse(spec);
    new Ungrouper().ungroup(ast);
    new ModelRemover().removeModels(ast);
    var typechecker = new TypeChecker();
    typechecker.verify(ast);
    var lowering = new ViamLowering();
    lowering.generate(ast);
  }

}
