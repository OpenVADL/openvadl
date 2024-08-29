package vadl.test.viam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static vadl.test.TestUtils.findDefinitionByNameIn;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.pass.PassOrder;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.AbstractTest;
import vadl.types.Type;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Specification;

/**
 * Checks if the risc-v specifications are translated and optimized without errors.
 */
public class Rv3264imTest extends AbstractTest {

  void testRv32i(Specification spec) {
    var rv32i = findDefinitionByNameIn("RV32I", spec, InstructionSetArchitecture.class);
    var rv3264i = findDefinitionByNameIn("RV3264I", spec, InstructionSetArchitecture.class);

    assertEquals(0, rv32i.instructions().size());
    assertEquals(2, spec.isas().count());
    assertEquals(rv3264i, rv32i.dependencyRef());
    assertEquals(Type.bits(32), Objects.requireNonNull(rv32i.pc()).type());
  }

  void testRv32im(Specification spec) {
    var rv32im = findDefinitionByNameIn("RV32IM", spec, InstructionSetArchitecture.class);
    var rv3264im = findDefinitionByNameIn("RV3264IM", spec, InstructionSetArchitecture.class);
    var rv3264i = findDefinitionByNameIn("RV3264I", spec, InstructionSetArchitecture.class);

    assertEquals(0, rv32im.instructions().size());
    assertEquals(3, spec.isas().count());
    assertEquals(rv3264i, rv3264im.dependencyRef());
    assertEquals(rv3264im, rv32im.dependencyRef());
    assertEquals(Type.bits(32), Objects.requireNonNull(rv32im.pc()).type());
  }

  void testRv64i(Specification spec) {
    var rv64i = findDefinitionByNameIn("RV64I", spec, InstructionSetArchitecture.class);
    var rv3264i = findDefinitionByNameIn("RV3264I", spec, InstructionSetArchitecture.class);

    assertEquals(0, rv64i.instructions().size());
    assertEquals(2, spec.isas().count());
    assertEquals(rv3264i, rv64i.dependencyRef());
    assertEquals(Type.bits(64), Objects.requireNonNull(rv64i.pc()).type());
  }

  void testRv64im(Specification spec) {
    var rv64im = findDefinitionByNameIn("RV64IM", spec, InstructionSetArchitecture.class);
    var rv3264im = findDefinitionByNameIn("RV3264IM", spec, InstructionSetArchitecture.class);
    var rv3264i = findDefinitionByNameIn("RV3264I", spec, InstructionSetArchitecture.class);

    assertEquals(0, rv64im.instructions().size());
    assertEquals(3, spec.isas().count());
    assertEquals(rv3264i, rv3264im.dependencyRef());
    assertEquals(rv3264im, rv64im.dependencyRef());
    assertEquals(Type.bits(64), Objects.requireNonNull(rv64im.pc()).type());
  }

  void testRv3264im(Specification spec) {
    var rv3264im = findDefinitionByNameIn("RV3264IM", spec, InstructionSetArchitecture.class);
    var rv3264i = findDefinitionByNameIn("RV3264I", spec, InstructionSetArchitecture.class);

    assertEquals(2, spec.isas().count());
    assertEquals(rv3264i, rv3264im.dependencyRef());
    assertEquals(Type.bits(32), Objects.requireNonNull(rv3264im.pc()).type());
  }


  Specification getSpecification(String rvFile) throws IOException, DuplicatedPassKeyException {
    // runs the general viam optimizations on the rv3264im impl
    //    var config = new GeneralConfiguration("build/test-out/rv3264im/", true);
    var config = getConfiguration(false);
    var setup = setupPassManagerAndRunSpec(
        "examples/riscv/" + rvFile,
        PassOrder.viam(config)
    );
    return setup.specification();
  }

  @TestFactory
  Stream<DynamicTest> test_all_riscv() {
    return Stream.of(
        dynamicTest("RV32I", () -> testRv32i(getSpecification("rv32i.vadl"))),
        dynamicTest("RV32IM", () -> testRv32im(getSpecification("rv32im.vadl"))),
        dynamicTest("RV64I", () -> testRv64i(getSpecification("rv64i.vadl"))),
        dynamicTest("RV64IM", () -> testRv64im(getSpecification("rv64im.vadl"))),
        dynamicTest("RV3264IM", () -> testRv3264im(getSpecification("rv3264im.vadl")))
    );
  }

}
