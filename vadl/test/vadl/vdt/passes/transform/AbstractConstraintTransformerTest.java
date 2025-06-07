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

import org.assertj.core.api.Assertions;
import vadl.TestUtils;
import vadl.types.BuiltInTable;
import vadl.utils.GraphUtils;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldRefNode;

public abstract class AbstractConstraintTransformerTest {

  protected String toString(Graph graph) {
    final ExpressionNode root = GraphUtils.getSingleNode(graph, ReturnNode.class).value();
    return toString(root);
  }

  protected String toString(ExpressionNode node) {
    return switch (node) {
      case BuiltInCall bc when bc.builtIn() == BuiltInTable.OR ->
          String.format("(%s) || (%s)", toString(bc.arg(0)), toString(bc.arg(1)));
      case BuiltInCall bc when bc.builtIn() == BuiltInTable.AND ->
          String.format("(%s) && (%s)", toString(bc.arg(0)), toString(bc.arg(1)));
      case BuiltInCall bc when bc.builtIn() == BuiltInTable.EQU ->
          String.format("%s = %s", toString(bc.arg(0)), toString(bc.arg(1)));
      case BuiltInCall bc when bc.builtIn() == BuiltInTable.NEQ ->
          String.format("%s != %s", toString(bc.arg(0)), toString(bc.arg(1)));
      case FieldRefNode fr -> String.format("%s", fr.formatField().simpleName());
      case ConstantNode c -> c.constant().asVal().binary("0b");
      default -> node.toString();
    };
  }

  protected Graph parse(String constraint) {
    final String testSpec = """
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
          [ select when : %s ]
          encoding Instr = { three = 0b1 }
          assembly Instr = ( mnemonic )
        }
        """.formatted(constraint);

    final Specification specification = TestUtils.compileToViam(testSpec);

    Assertions.assertThat(specification).isNotNull();
    Assertions.assertThat(specification.isa()).isPresent();

    return specification.isa().get().ownInstructions().getFirst().encoding().constraint();
  }

}
