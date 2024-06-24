package vadl.test.viam;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static vadl.test.TestUtils.findDefinitionByNameIn;

import java.util.List;
import org.junit.jupiter.api.Test;
import vadl.test.AbstractTest;
import vadl.viam.Format;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Register;

public class ViamGraphTest extends AbstractTest {

  @Test
  void basicMultiScopeDefinition() {
    var spec = runAndGetViamSpecification("spec/valid_multiscope_definition.vadl");

    var base_isa = findDefinitionByNameIn("Base", spec, InstructionSetArchitecture.class);
    var baseA_reg = findDefinitionByNameIn("Base.A", base_isa, Register.class);
    var baseX_format = findDefinitionByNameIn("Base.X", base_isa, Format.class);

    var sub_isa = findDefinitionByNameIn("Sub", spec, InstructionSetArchitecture.class);
    var subA_reg = findDefinitionByNameIn("Sub.A", sub_isa, Register.class);
    var subB_reg = findDefinitionByNameIn("Sub.B", sub_isa, Register.class);
    var subX_format = findDefinitionByNameIn("Sub.X", sub_isa, Format.class);
    var subY_format = findDefinitionByNameIn("Sub.Y", sub_isa, Format.class);

    // check 1
    assertEquals(subX_format, baseA_reg.refFormat());
    assertEquals(1, baseA_reg.subRegisters().length);

  }

}
