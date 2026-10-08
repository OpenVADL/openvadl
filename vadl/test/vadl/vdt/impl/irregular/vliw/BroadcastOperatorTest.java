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

package vadl.vdt.impl.irregular.vliw;

import static vadl.vdt.impl.vliw.PointedRegExpBroadcastOperator.broadcast;
import static vadl.vdt.impl.vliw.PointedRegExpConverter.initialize;

import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.TestUtils;

public class BroadcastOperatorTest {

  static Stream<Arguments> vliwGroupSource() {
    return Stream.of(
        Arguments.of("O.P", "<*O.P, false>"),
        Arguments.of("O.P.Q", "<*O.P.Q, false>"),
        Arguments.of("(O|P)", "<*O|*P, false>"),
        Arguments.of("O<1..2>", "<*(O)<1..2>, false>"),
        Arguments.of("O<0..1>", "<*(O)<0..1>, true>"),
        Arguments.of("O<0..1>.P", "<*(O)<0..1>.*P, false>"),
        Arguments.of("O<0..1>.P<0..2>.Q<1..2>", "<*(O)<0..1>.*(P)<0..2>.*(Q)<1..2>, false>"),
        Arguments.of("(O<0..1>|P)", "<*(O)<0..1>|*P, true>")
    );
  }

  @Test
    //@Disabled("For manual use")
  void singleReproducer() {
    testBroadcastOperator("O<1..2>", "<*(O)<1..2>, false>");
  }

  @ParameterizedTest
  @MethodSource("vliwGroupSource")
  void testBroadcastOperator(String group, String expected) {

    /* GIVEN */
    var spec = TestUtils.compileToViam(wrap(group));
    Assertions.assertTrue(spec.isa().isPresent());

    var groupExpr = spec.isa().get().group();
    Assertions.assertNotNull(groupExpr);
    Assertions.assertNotNull(groupExpr.getExpression());

    /* WHEN */
    var pre = initialize(groupExpr.getExpression());
    var result = broadcast(pre);

    /* THEN */
    Assertions.assertNotNull(result);
    Assertions.assertEquals(expected, result.toString());
  }

  private static String wrap(String formulas) {
    return """
        instruction set architecture TEST = {
        
          register X: Bits<8>
        
          format Format: Bits<8> =
          { two   [7..4]
          , one   [3..0]
          }
        
          instruction A: Format = { }
          encoding A = { one = 0b01 }
          assembly A = ( mnemonic )
        
          instruction B: Format = { }
          encoding B = { one = 0b10 }
          assembly B = ( mnemonic )
        
          instruction C: Format = { }
          encoding C = { one = 0b11 }
          assembly C = ( mnemonic )
        
          operation O = {A}
          operation P = {B}
          operation Q = {C}
        
          group VLIW = %s
        }
        """.formatted(formulas);
  }

}
