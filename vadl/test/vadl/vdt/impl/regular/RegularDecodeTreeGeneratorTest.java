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

package vadl.vdt.impl.regular;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.vdt.AbstractDecisionTreeTest;
import vadl.vdt.model.Node;
import vadl.vdt.target.common.DecisionTreeDecoder;
import vadl.vdt.utils.BitVector;
import vadl.vdt.utils.Instruction;

class RegularDecodeTreeGeneratorTest extends AbstractDecisionTreeTest {

  @Test
  void testGenerate_simpleInstructions_succeeds() {

    /* GIVEN */
    final var instructions = createInsns(List.of("1--", "01-", "00-"));

    /* WHEN */
    final Node dt = new RegularDecodeTreeGenerator().generate(instructions);

    /* THEN */
    final DecisionTreeDecoder decoder = new DecisionTreeDecoder(dt);

    assertDecision(decoder, "100", "1--");
    assertDecision(decoder, "110", "1--");
    assertDecision(decoder, "101", "1--");
    assertDecision(decoder, "111", "1--");

    assertDecision(decoder, "010", "01-");
    assertDecision(decoder, "011", "01-");

    assertDecision(decoder, "001", "00-");
    assertDecision(decoder, "000", "00-");
  }

  @Test
  void testGenerate_subsumedInstructions_succeeds() {

    /* GIVEN */
    final var instructions = createInsns(List.of("1--", "10-", "0--"));

    /* WHEN */
    final Node dt = new RegularDecodeTreeGenerator().generate(instructions);

    /* THEN */
    final DecisionTreeDecoder decoder = new DecisionTreeDecoder(dt);

    assertDecision(decoder, "100", "10-");
    assertDecision(decoder, "101", "10-");

    assertDecision(decoder, "110", "1--");
    assertDecision(decoder, "111", "1--");

    assertDecision(decoder, "000", "0--");
    assertDecision(decoder, "001", "0--");
    assertDecision(decoder, "010", "0--");
    assertDecision(decoder, "011", "0--");
  }

  private void assertDecision(DecisionTreeDecoder decoder, String insn, String expected) {
    Instruction decision = decoder.decide(BitVector.fromString(insn, insn.length()));
    Assertions.assertNotNull(decision);
    Assertions.assertEquals(expected, decision.pattern().toString());
  }
}