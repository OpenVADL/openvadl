package vadl.test.viam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static vadl.test.TestUtils.findDefinitionByNameIn;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.opentest4j.AssertionFailedError;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.AbstractTest;
import vadl.types.Type;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Specification;

/**
 * Checks if the risc-v specifications are translated and optimized without errors.
 */
public class Rv3264imTest extends AbstractTest {

  void testRv32i(Specification spec) {
    var rv32i = findDefinitionByNameIn("RV32I", spec, InstructionSetArchitecture.class);
    findDefinitionByNameIn("RV32I::ADD", spec, Instruction.class);

    assertThrows(AssertionFailedError.class,
        () -> findDefinitionByNameIn("RV32I::DIVU", spec, Instruction.class));
    assertEquals(Type.bits(32), Objects.requireNonNull(rv32i.pc()).registerResource().type());
  }

  void testRv32im(Specification spec) {
    var rv32im = findDefinitionByNameIn("RV32IM", spec, InstructionSetArchitecture.class);

    findDefinitionByNameIn("RV32IM::ADD", spec, Instruction.class);
    findDefinitionByNameIn("RV32IM::DIVU", spec, Instruction.class);
    assertEquals(Type.bits(32), Objects.requireNonNull(rv32im.pc()).registerResource().type());
  }

  void testRv64i(Specification spec) {
    var rv64i = findDefinitionByNameIn("RV64I", spec, InstructionSetArchitecture.class);

    findDefinitionByNameIn("RV64I::ADD", spec, Instruction.class);
    assertThrows(AssertionFailedError.class,
        () -> findDefinitionByNameIn("RV64I::DIVU", spec, Instruction.class));
    assertEquals(Type.bits(64), Objects.requireNonNull(rv64i.pc()).registerResource().type());
  }

  void testRv64im(Specification spec) {
    var rv64im = findDefinitionByNameIn("RV64IM", spec, InstructionSetArchitecture.class);

    findDefinitionByNameIn("RV64IM::ADD", spec, Instruction.class);
    findDefinitionByNameIn("RV64IM::DIVU", spec, Instruction.class);
    assertEquals(Type.bits(64), Objects.requireNonNull(rv64im.pc()).registerResource().type());
  }


  Specification getSpecification(String rvFile) throws IOException, DuplicatedPassKeyException {
    // runs the general viam optimizations on the rv3264im impl
    //    var config = new GeneralConfiguration("build/test-out/rv3264im/", true);
    var config = getConfiguration(false);
    var setup = setupPassManagerAndRunSpec(
        "sys/risc-v/" + rvFile,
        PassOrders.viam(config)
    );
    return setup.specification();
  }

  @TestFactory
  Stream<DynamicTest> test_all_riscv() {
    return Stream.of(
        dynamicTest("RV32I", () -> testRv32i(getSpecification("rv32i.vadl"))),
        dynamicTest("RV32IM", () -> testRv32im(getSpecification("rv32im.vadl"))),
        dynamicTest("RV64I", () -> testRv64i(getSpecification("rv64i.vadl"))),
        dynamicTest("RV64IM", () -> testRv64im(getSpecification("rv64im.vadl")))
    );
  }

}
