package vadl.test.lcb.passes;

import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import vadl.lcb.passes.llvmLowering.GenerateRegisterClassesPass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.lcb.AbstractLcbTest;

public class GenerateRegisterClassesPassTest extends AbstractLcbTest {

  @ParameterizedTest
  @CsvSource({"processorNameValue,X,32,i64",
      "processorNameValue,X0Class,32,i64",
  "processorNameValue,X1Class,32,i64",
      "processorNameValue,X2Class,32,i64",
      "processorNameValue,X3Class,32,i64",
      "processorNameValue,X4Class,32,i64",
      "processorNameValue,X5Class,32,i64",
      "processorNameValue,X6Class,32,i64",
      "processorNameValue,X7Class,32,i64",
      "processorNameValue,X8Class,32,i64",
      "processorNameValue,X9Class,32,i64",
      "processorNameValue,X10Class,32,i64",
      "processorNameValue,X11Class,32,i64",
      "processorNameValue,X12Class,32,i64",
      "processorNameValue,X13Class,32,i64",
      "processorNameValue,X14Class,32,i64",
      "processorNameValue,X15Class,32,i64",
      "processorNameValue,X16Class,32,i64",
      "processorNameValue,X17Class,32,i64",
      "processorNameValue,X18Class,32,i64",
      "processorNameValue,X19Class,32,i64",
      "processorNameValue,X20Class,32,i64",
      "processorNameValue,X21Class,32,i64",
      "processorNameValue,X22Class,32,i64",
      "processorNameValue,X23Class,32,i64",
      "processorNameValue,X24Class,32,i64",
      "processorNameValue,X25Class,32,i64",
      "processorNameValue,X26Class,32,i64",
      "processorNameValue,X27Class,32,i64",
      "processorNameValue,X28Class,32,i64",
      "processorNameValue,X29Class,32,i64",
      "processorNameValue,X30Class,32,i64",
      "processorNameValue,X31Class,32,i64"
  })
  void shouldHaveMainRegisterClasses(String namespace, String name, int alignment, String type)
      throws IOException, DuplicatedPassKeyException {
    // Given
    var setup = runLcb(getConfiguration(false), "sys/risc-v/rv64im.vadl",
        new PassKey(GenerateRegisterClassesPass.class.getName()));
    var passManager = setup.passManager();
    var spec = setup.specification();

    // When
    var generatedRegisterClasses = (GenerateRegisterClassesPass.Output) passManager.getPassResults()
        .lastResultOf(GenerateRegisterClassesPass.class);

    // Then
    var rg = generatedRegisterClasses.registerClasses().stream().filter(x -> x.name().equals(name))
        .findFirst();
    Assertions.assertTrue(rg.isPresent());
    Assertions.assertEquals(namespace, rg.get().namespace().value());
    Assertions.assertEquals(name, rg.get().name());
    Assertions.assertEquals(alignment, rg.get().alignment());
    Assertions.assertEquals(type, rg.get().regTypes().get(0).getLlvmType());
  }
}
