package vadl.lcb.passes.isa_matching;

import static java.util.Collections.emptyList;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.types.BuiltInTable;
import vadl.types.Type;
import vadl.viam.Encoding;
import vadl.viam.Format;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.RegisterFile;
import vadl.viam.graph.Graph;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.TypeCastNode;

class IsaMatchingPassTest extends AbstractTest {

  IsaMatchingPass pass;

  @BeforeEach
  void beforeEach() {
    pass = new IsaMatchingPass();
  }

  @Test
  void shouldFindUnsignedAdd32Bit() {
    // Given
    var spec = createSpecification("specificationNameValue");
    var instruction = createInstruction("ADD32U", Type.bits(32));
    instruction.behavior().addWithInputs(new ReturnNode(new BuiltInCall(BuiltInTable.ADD, new NodeList<>(
        new TypeCastNode(new ReadRegFileNode(new RegisterFile()))
    ))));

    var isa = new InstructionSetArchitecture(createIdentifier("isaNameValue"), spec, emptyList(),
        List.of(instruction), emptyList(), emptyList(), emptyList(), emptyList());

    // When

    // Then
  }
}