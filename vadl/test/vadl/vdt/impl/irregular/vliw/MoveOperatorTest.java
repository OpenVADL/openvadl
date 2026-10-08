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

import static vadl.vdt.impl.irregular.vliw.utils.assertions.PointedExpressionAssert.assertThat;
import static vadl.vdt.impl.irregular.vliw.utils.assertions.TransitionSetAssert.assertThat;
import static vadl.vdt.impl.irregular.vliw.utils.parser.PointedExpressionParser.parse;
import static vadl.vdt.impl.vliw.PointedRegExpBroadcastOperator.broadcast;
import static vadl.vdt.impl.vliw.PointedRegExpMoveOperator.move;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vadl.TestUtils;
import vadl.vdt.impl.irregular.vliw.utils.LiteralsCollector;
import vadl.vdt.impl.vliw.model.Counter;
import vadl.vdt.impl.vliw.model.Transition;
import vadl.viam.Group;

public class MoveOperatorTest {

  @BeforeEach
  void setUp() {
    Counter.ID_SEQ.set(0);
  }

  @Test
  void testMove() {

    String expression = "<*O|*(P.Q)<1..2>, false>";
    String token = "O";
    String expected = "<O|(P.Q)<1..2>, true>";

    /* GIVEN */
    final var literals = getLiterals();
    final var pre = parse(expression, literals.values());

    /* WHEN */
    Set<Transition> result = move(pre, literals.get(token));

    /* THEN */
    Assertions.assertEquals(1, result.size());

    var transition = result.stream().findFirst().orElse(null);
    Assertions.assertNotNull(transition);

    Assertions.assertEquals(expected, transition.nextState().toString());
  }

  @Test
  void testMoveRep1() {

    /* GIVEN */
    final var literals = getLiterals();

    final String e1 = "<(P.Q)<1..2>, false>";
    final var pre1 = parse(e1, literals.values());

    /* WHEN */

    // Check that it was parsed truthfully
    assertThat(pre1)
        .hasItem("(P.Q)[0]<1..2>")
        .notTrailing();

    // Broadcast stops in front of repetitions
    final var pre2 = broadcast(pre1);
    assertThat(pre2)
        .hasItem("*(P.Q)[0]<1..2>")
        .notTrailing();

    // Point should move inside the repetition, and over the consumed "P"
    final var t3 = move(pre2, literals.get("P"));

    // @formatter:off
    assertThat(t3)
        .transition()
          .hasState("<(P.*Q)[0]<1..2>, false>")
          .noActions()
        .done().noOthers();
    // @formatter:on

    final var pre3 = t3.stream().findFirst().get().nextState();

    // Step 4

    final var t4 = move(pre3, literals.get("Q"));

    // @formatter:off
    assertThat(t4)
        .transition()
          .hasState("<*(P.Q)[0]<1..2>, false>")
          .hasActions("[0]++")
        .next()
          .hasState("<(P.Q)[0]<1..2>, true>")
          .hasActions("[0]++")
        .done().noOthers();
    // @formatter:on
  }

  private Map<String, Group.Literal> getLiterals() {
    final String viam = """
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
        
          group VLIW = O.P.Q // We just need to mention all literals
        }
        """;

    final var spec = TestUtils.compileToViam(viam);
    Assertions.assertTrue(spec.isa().isPresent());

    final var groupExpr = spec.isa().get().group();
    Assertions.assertNotNull(groupExpr);
    Assertions.assertNotNull(groupExpr.getExpression());

    return LiteralsCollector.getLiterals(groupExpr).stream()
        .collect(Collectors.toMap(
            l -> l.op().simpleName(),
            Function.identity(),
            (a, b) -> a
        ));
  }
}
