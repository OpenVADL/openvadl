package vadl.test.viam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static vadl.test.TestUtils.findDefinitionByNameIn;

import org.junit.jupiter.api.Test;
import vadl.test.AbstractTest;
import vadl.viam.Format;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Register;


public class ViamSpecTest extends AbstractTest {

  @Test
  void basicMultiScopeDefinition() {
    var spec = runAndGetViamSpecification("spec/valid_multiscope_definition.vadl");

    var base_isa = findDefinitionByNameIn("Base", spec, InstructionSetArchitecture.class);
    var baseA_reg = findDefinitionByNameIn("Base::A", base_isa, Register.class);
    var baseX_format = findDefinitionByNameIn("Base::X", base_isa, Format.class);

    var sub_isa = findDefinitionByNameIn("Sub", spec, InstructionSetArchitecture.class);
    var subA_reg = findDefinitionByNameIn("Sub::A", sub_isa, Register.class);
    var subB_reg = findDefinitionByNameIn("Sub::B", sub_isa, Register.class);
    var subX_format = findDefinitionByNameIn("Sub::X", sub_isa, Format.class);
    var subYA_field = findDefinitionByNameIn("Sub::Y::A", sub_isa, Format.Field.class);

    // check 1
    assertEquals(subX_format, baseA_reg.refFormat());
    assertEquals(1, baseA_reg.subRegisters().length);

    // check 2
    assertEquals(subX_format, subA_reg.refFormat());
    assertEquals(1, subA_reg.subRegisters().length);

    // check 3
    assertEquals(baseX_format, subB_reg.refFormat());
    assertEquals(2, subB_reg.subRegisters().length);

    // check 4
    assertEquals(baseX_format, subYA_field.refFormat());

  }

}
