package vadl.lcb.passes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vadl.lcb.AbstractLcbTest;
import vadl.lcb.passes.isaMatching.IsaMachineInstructionMatchingPass;
import vadl.lcb.passes.isaMatching.MachineInstructionLabel;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.viam.Definition;

public class IsaMachineInstructionMatchingPassTest extends AbstractLcbTest {

  private static Stream<Arguments> getExpectedMatchings() {
    return Stream.of(
        Arguments.of(List.of("ADD"), MachineInstructionLabel.ADD_64),
        Arguments.of(List.of("ADDI"), MachineInstructionLabel.ADDI_64),
        Arguments.of(List.of("BEQ"), MachineInstructionLabel.BEQ),
        Arguments.of(List.of("BNE"), MachineInstructionLabel.BNEQ),
        Arguments.of(List.of("BGE"), MachineInstructionLabel.BSGEQ),
        Arguments.of(List.of("BGEU"), MachineInstructionLabel.BUGEQ),
        Arguments.of(List.of("BLT"), MachineInstructionLabel.BSLTH),
        Arguments.of(List.of("BLTU"), MachineInstructionLabel.BULTH),
        Arguments.of(List.of("AND", "ANDI"), MachineInstructionLabel.AND),
        Arguments.of(List.of("SUB", "SUBW"), MachineInstructionLabel.SUB),
        Arguments.of(List.of("OR"), MachineInstructionLabel.OR),
        Arguments.of(List.of("ORI"), MachineInstructionLabel.ORI),
        Arguments.of(List.of("XOR"), MachineInstructionLabel.XOR),
        Arguments.of(List.of("XORI"), MachineInstructionLabel.XORI),
        Arguments.of(List.of("MUL", "MULW"), MachineInstructionLabel.MUL),
        Arguments.of(List.of("MULHU"), MachineInstructionLabel.MULHU),
        Arguments.of(List.of("MULH"), MachineInstructionLabel.MULHS),
        Arguments.of(List.of("DIV", "DIVW"), MachineInstructionLabel.SDIV),
        Arguments.of(List.of("DIVU", "DIVUW"), MachineInstructionLabel.UDIV),
        Arguments.of(List.of("REMU", "REMUW"), MachineInstructionLabel.UMOD),
        Arguments.of(List.of("REM", "REMW"), MachineInstructionLabel.SMOD),
        Arguments.of(List.of("SLL", "SLLW"), MachineInstructionLabel.SLL),
        Arguments.of(List.of("SLLI"), MachineInstructionLabel.SLLI),
        Arguments.of(List.of("SRL", "SRLW"), MachineInstructionLabel.SRL),
        Arguments.of(List.of("SLT"), MachineInstructionLabel.LTS),
        Arguments.of(List.of("SLTU"), MachineInstructionLabel.LTU),
        Arguments.of(List.of("SLTI"), MachineInstructionLabel.LTI),
        Arguments.of(List.of("SLTIU"), MachineInstructionLabel.LTIU),
        Arguments.of(List.of("LB", "LBU", "LD", "LH", "LHU", "LW", "LWU"),
            MachineInstructionLabel.LOAD_MEM),
        Arguments.of(List.of("SB", "SD", "SH", "SW"), MachineInstructionLabel.STORE_MEM),
        Arguments.of(List.of("JALR"), MachineInstructionLabel.JALR),
        Arguments.of(List.of("JAL"), MachineInstructionLabel.JAL)
    );
  }

  @ParameterizedTest
  @MethodSource("getExpectedMatchings")
  void shouldFindMatchings(List<String> expectedInstructionName, MachineInstructionLabel label)
      throws IOException, DuplicatedPassKeyException {
    // Given
    var config = getConfiguration(false);
    var setup = setupPassManagerAndRunSpec(
        "sys/risc-v/rv64im.vadl",
        PassOrders.lcb(config)
            .untilFirst(IsaMachineInstructionMatchingPass.class)
    );
    var passManager = setup.passManager();

    // When
    var matchings =
        ((IsaMachineInstructionMatchingPass.Result) passManager.getPassResults()
            .lastResultOf(IsaMachineInstructionMatchingPass.class)).labels();

    // Then
    Assertions.assertNotNull(matchings);
    Assertions.assertFalse(matchings.isEmpty());
    Assertions.assertNotNull(matchings.get(label));
    var result = matchings.get(label).stream().map(Definition::simpleName).sorted().toList();
    assertEquals(expectedInstructionName.stream().sorted().toList(), result);
  }
}