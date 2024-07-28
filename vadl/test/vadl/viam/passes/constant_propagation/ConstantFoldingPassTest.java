package vadl.viam.passes.constant_propagation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import vadl.types.BitsType;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.utils.SourceLocation;
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
import vadl.viam.passes.constant_folding.ConstantFoldingPass;

class ConstantFoldingPassTest {
  @Test
  void shouldReplaceAdditionWithConstant() {
    // Given
    var viam = new Specification(
        Identifier.noLocation("identifierValue"));

    var behavior = new Graph("graphNameValue");
    var p1 =
        behavior.add(new ConstantNode(Constant.Value.of(BigInteger.ONE, DataType.bits(32))));
    var p2 =
        behavior.add(new ConstantNode(Constant.Value.of(BigInteger.ONE, DataType.bits(32))));
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
        Collections.emptyList(),
        List.of(new Instruction(
            Identifier.noLocation("instructionValue"),
            behavior,
            assembly,
            encoding)),
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList());

    viam.add(isa);

    // When
    var pass = new ConstantFoldingPass();
    pass.execute(Collections.emptyMap(), viam);

    assertThat(behavior.getNodes().count(), equalTo(1L));
    assertThat(behavior.getNodes().findFirst().get().getClass(), equalTo(ConstantNode.class));
    assertThat(
        ((Constant.Value) ((ConstantNode) behavior.getNodes().findFirst()
            .get()).constant()).value(),
        equalTo(new BigInteger(String.valueOf(2))));
  }

}