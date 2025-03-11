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

package vadl.viam.passes.constant_propagation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static vadl.utils.GraphUtils.signExtend;
import static vadl.viam.helper.TestGraphUtils.binaryOp;
import static vadl.viam.helper.TestGraphUtils.intSNode;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.pass.PassResults;
import vadl.types.BitsType;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.Assembly;
import vadl.viam.Constant;
import vadl.viam.Encoding;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Identifier;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Parameter;
import vadl.viam.Specification;
import vadl.viam.graph.Graph;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.passes.canonicalization.CanonicalizationPass;
import vadl.viam.passes.canonicalization.Canonicalizer;

class CanonicalizationPassTest extends AbstractTest {
  @Test
  void shouldReplaceAdditionWithConstant() {
    // Given
    var viam = new Specification(
        Identifier.noLocation("identifierValue"));

    var behavior = new Graph("graphNameValue");
    var p1 =
        behavior.add(new ConstantNode(Constant.Value.of(1, DataType.bits(32))));
    var p2 =
        behavior.add(new ConstantNode(Constant.Value.of(1, DataType.bits(32))));
    behavior.add(new BuiltInCall(BuiltInTable.ADD, new NodeList<>(p1, p2), Type.bits(32)));

    var assembly = new Assembly(
        Identifier.noLocation("assemblyIdentifierValue"),
        new Function(
            Identifier.noLocation("functionIdentifierValue"),
            new Parameter[] {},
            Type.string()));
    var encoding = new Encoding(
        Identifier.noLocation("encodingIdentifierValue"),
        new Format(Identifier.noLocation("formatIdentifierValue"),
            BitsType.bits(32)),
        new Encoding.Field[] {});

    var isa = new InstructionSetArchitecture(
        Identifier.noLocation("isaIdentifierValue"),
        viam,
        List.of(),
        List.of(),
        Collections.emptyList(),
        Collections.emptyList(),
        List.of(new Instruction(
            Identifier.noLocation("instructionValue"),
            behavior,
            assembly,
            encoding)),
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList(),
        null,
        Collections.emptyList(),
        Collections.emptyList()
    );

    viam.add(isa);

    // When
    var pass = new CanonicalizationPass(getConfiguration(false));
    pass.execute(new PassResults(), viam);

    assertThat(behavior.getNodes().count(), equalTo(1L));
    assertThat(behavior.getNodes().findFirst().get().getClass(), equalTo(ConstantNode.class));
    assertThat(
        ((Constant.Value) ((ConstantNode) behavior.getNodes().findFirst()
            .get()).constant()).integer(),
        equalTo(new BigInteger(String.valueOf(2))));
  }


  @Test
  void shouldEvaluateConstant_withUniqueNodeReplacement() {
    var graph = new Graph("test graph");

    graph.addWithInputs(
        signExtend(
            binaryOp(
                BuiltInTable.ADD,
                Type.bits(4),
                signExtend(intSNode(2, 4), Type.bits(4)),
                signExtend(intSNode(2, 4), Type.bits(4))
            ),
            Type.signedInt(4)
        )
    );

    Canonicalizer.canonicalize(graph);

    var nodes = graph.getNodes().toList();
    assertThat(nodes, hasSize(1));
    assertThat(nodes.get(0), instanceOf(ConstantNode.class));
    var constant = ((ConstantNode) nodes.get(0)).constant();
    assertThat(constant.asVal().intValue(), equalTo(4));

  }


}