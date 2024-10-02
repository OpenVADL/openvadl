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
      "processorNameValue,X31Class,32,i64",
      "processorNameValue,PCClass,32,i64",
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
  @CsvSource({"X,X0",
      "X,X1",
      "X,X2",
      "X,X3",
      "X,X4",
      "X,X5",
      "X,X6",
      "X,X7",
      "X,X8",
      "X,X9",
      "X,X10",
      "X,X11",
      "X,X12",
      "X,X13",
      "X,X14",
      "X,X15",
      "X,X16",
      "X,X17",
      "X,X18",
      "X,X19",
      "X,X20",
      "X,X21",
      "X,X22",
      "X,X23",
      "X,X24",
      "X,X25",
      "X,X26",
      "X,X27",
      "X,X28",
      "X,X29",
      "X,X30",
      "X,X31",
      "X0Class,X0",
      "X1Class,X1",
      "X2Class,X2",
      "X3Class,X3",
      "X4Class,X4",
      "X5Class,X5",
      "X6Class,X6",
      "X7Class,X7",
      "X8Class,X8",
      "X9Class,X9",
      "X10Class,X10",
      "X11Class,X11",
      "X12Class,X12",
      "X13Class,X13",
      "X14Class,X14",
      "X15Class,X15",
      "X16Class,X16",
      "X17Class,X17",
      "X18Class,X18",
      "X19Class,X19",
      "X20Class,X20",
      "X21Class,X21",
      "X22Class,X22",
      "X23Class,X23",
      "X24Class,X24",
      "X25Class,X25",
      "X26Class,X26",
      "X27Class,X27",
      "X28Class,X28",
      "X29Class,X29",
      "X30Class,X30",
      "X31Class,X31",
      "PCClass,PC"
  })
  void shouldHaveCorrectRegisterAssignments(String name, String reg)
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
    var x = rg.get().registers().stream().filter(y -> y.name().equals(reg)).findFirst();
    Assertions.assertTrue(x.isPresent(), "Register was not found");
  }
}
