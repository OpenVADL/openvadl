package vadl.viam.passes.constant_propagation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static vadl.viam.helper.TestGraphUtils.binaryOp;
import static vadl.viam.helper.TestGraphUtils.cast;
import static vadl.viam.helper.TestGraphUtils.intSNode;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
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
import vadl.viam.graph.dependency.TypeCastNode;
import vadl.viam.passes.canonicalization.CanoicalizationPass;
import vadl.viam.passes.canonicalization.Canonicalizer;

class CanoicalizationPassTest {
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
        List.of(new Instruction(
            Identifier.noLocation("instructionValue"),
            behavior,
            assembly,
            encoding)),
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList(),
        null,
        Collections.emptyList());

    viam.add(isa);

    // When
    var pass = new CanoicalizationPass();
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
        cast(
            Type.signedInt(4),
            binaryOp(
                BuiltInTable.ADD,
                Type.bits(4),
                cast(intSNode(2, 4), Type.bits(4)),
                cast(intSNode(2, 4), Type.bits(4))
            )
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