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

package vadl.viam.passes.algebraic_simplication.rules.impl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static vadl.TestUtils.createIdentifier;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.AbstractTest;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.Parameter;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.passes.algebraic_simplication.rules.AlgebraicSimplificationRule;

class DivisionWithOneSimplificationRuleTest extends AbstractTest {
  AlgebraicSimplificationRule rule =
      new DivisionWithOneSimplificationRule();

  private static Stream<Arguments> getSupportedBuiltins() {
    return Stream.of(Arguments.of(BuiltInTable.SDIV), Arguments.of(BuiltInTable.UDIV),
        Arguments.of(BuiltInTable.UDIVS), Arguments.of(BuiltInTable.SDIVS));
  }

  @ParameterizedTest
  @MethodSource("getSupportedBuiltins")
  void shouldReplaceNode(BuiltInTable.BuiltIn built) {
    var ty = Type.signedInt(32);
    var node = new BuiltInCall(built, new NodeList<>(List.of(
        new FuncParamNode(new Parameter(createIdentifier("parameterValue"), ty)),
        new ConstantNode(Constant.Value.of(1, ty)
        ))), ty);

    var res = rule.simplify(node);

    assertThat(res).isNotNull()
        .isPresent();
    assertThat(res.get()).isExactlyInstanceOf(FuncParamNode.class);
  }
}