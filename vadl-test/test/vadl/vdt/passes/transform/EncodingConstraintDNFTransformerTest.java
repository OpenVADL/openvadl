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

package vadl.vdt.passes.transform;

import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.utils.GraphUtils;
import vadl.viam.graph.control.ReturnNode;

class EncodingConstraintDNFTransformerTest extends AbstractConstraintTransformerTest {

  @ParameterizedTest
  @MethodSource("testCases")
  void testTransform(String constraint, String expected) {

    /* GIVEN */
    var parsed = parse(constraint);

    /* WHEN */
    var transformed = new EncodingConstraintDNFTransformer(parsed).transform();

    /* THEN */

    var root = GraphUtils.getSingleNode(transformed, ReturnNode.class).value();
    Assertions.assertThat(toString(root))
        .isEqualTo(expected);
  }

  static Stream<Arguments> testCases() {
    return Stream.of(
        Arguments.of(
            "(one = 0b0 && two = 0b1) || three = 0b1",
            "((one = 0b0) && (two = 0b1)) || (three = 0b1)"),
        Arguments.of(
            "(one = 0b0 || two = 0b1) && three = 0b1",
            "((one = 0b0) && (three = 0b1)) || ((two = 0b1) && (three = 0b1))"),
        Arguments.of(
            "three = 0b1 && (one = 0b0 || two = 0b1)",
            "((three = 0b1) && (one = 0b0)) || ((three = 0b1) && (two = 0b1))"),
        Arguments.of(
            "one = 0b0 && (two = 0b1 || (three = 0b1 || four = 0b1))",
            "((one = 0b0) && (two = 0b1)) || (((one = 0b0) && (three = 0b1)) || ((one = 0b0) && (four = 0b1)))")
    );
  }

}
