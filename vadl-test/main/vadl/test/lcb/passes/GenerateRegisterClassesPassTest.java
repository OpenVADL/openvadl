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


  @ParameterizedTest
  @CsvSource({"processorNameValue,X,X0",
      "processorNameValue,X,X1",
      "processorNameValue,X,X2",
      "processorNameValue,X,X3",
      "processorNameValue,X,X4",
      "processorNameValue,X,X5",
      "processorNameValue,X,X6",
      "processorNameValue,X,X7",
      "processorNameValue,X,X8",
      "processorNameValue,X,X9",
      "processorNameValue,X,X10",
      "processorNameValue,X,X11",
      "processorNameValue,X,X12",
      "processorNameValue,X,X13",
      "processorNameValue,X,X14",
      "processorNameValue,X,X15",
      "processorNameValue,X,X16",
      "processorNameValue,X,X17",
      "processorNameValue,X,X18",
      "processorNameValue,X,X19",
      "processorNameValue,X,X20",
      "processorNameValue,X,X21",
      "processorNameValue,X,X22",
      "processorNameValue,X,X23",
      "processorNameValue,X,X24",
      "processorNameValue,X,X25",
      "processorNameValue,X,X26",
      "processorNameValue,X,X27",
      "processorNameValue,X,X28",
      "processorNameValue,X,X29",
      "processorNameValue,X,X30",
      "processorNameValue,X,X31",
      "processorNameValue,X0Class,X0",
      "processorNameValue,X1Class,X1",
      "processorNameValue,X2Class,X2",
      "processorNameValue,X3Class,X3",
      "processorNameValue,X4Class,X4",
      "processorNameValue,X5Class,X5",
      "processorNameValue,X6Class,X6",
      "processorNameValue,X7Class,X7",
      "processorNameValue,X8Class,X8",
      "processorNameValue,X9Class,X9",
      "processorNameValue,X10Class,X10",
      "processorNameValue,X11Class,X11",
      "processorNameValue,X12Class,X12",
      "processorNameValue,X13Class,X13",
      "processorNameValue,X14Class,X14",
      "processorNameValue,X15Class,X15",
      "processorNameValue,X16Class,X16",
      "processorNameValue,X17Class,X17",
      "processorNameValue,X18Class,X18",
      "processorNameValue,X19Class,X19",
      "processorNameValue,X20Class,X20",
      "processorNameValue,X21Class,X21",
      "processorNameValue,X22Class,X22",
      "processorNameValue,X23Class,X23",
      "processorNameValue,X24Class,X24",
      "processorNameValue,X25Class,X25",
      "processorNameValue,X26Class,X26",
      "processorNameValue,X27Class,X27",
      "processorNameValue,X28Class,X28",
      "processorNameValue,X29Class,X29",
      "processorNameValue,X30Class,X30",
      "processorNameValue,X31Class,X31"
  })
  void shouldHaveCorrectRegisterAssignments(String namespace, String name, String reg)
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
    var x = rg.get().registers().stream().filter(y -> y.registerName().equals(reg)).findFirst();
    Assertions.assertTrue(x.isPresent(), "Register was not found");
  }
}
