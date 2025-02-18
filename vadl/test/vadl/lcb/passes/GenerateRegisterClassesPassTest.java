package vadl.lcb.passes;

import java.io.IOException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import vadl.lcb.passes.llvmLowering.GenerateRegisterClassesPass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.lcb.AbstractLcbTest;

public class GenerateRegisterClassesPassTest extends AbstractLcbTest {

  @ParameterizedTest
  @CsvSource({"processorNameValue,X,32,i64"})
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
      "X,X31"
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
