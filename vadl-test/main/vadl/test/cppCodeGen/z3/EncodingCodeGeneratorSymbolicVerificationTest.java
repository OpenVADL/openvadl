package vadl.test.cppCodeGen.z3;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.images.builder.ImageFromDockerfile;
import vadl.gcb.passes.encoding_generation.GenerateFieldAccessEncodingFunctionPass;
import vadl.pass.PassOrder;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.gcb.AbstractGcbTest;
import vadl.viam.Format;
import vadl.viam.graph.control.ReturnNode;

/**
 * Testing the conversion between encoding and decoding symbolically with a theorem prover.
 * Note that only field accesses **without** an encoding function are generating encodings
 * automatically and testing it.
 */
public class EncodingCodeGeneratorSymbolicVerificationTest extends AbstractGcbTest {
  private static final Logger logger =
      LoggerFactory.getLogger(EncodingCodeGeneratorSymbolicVerificationTest.class);

  private static final String GENERIC_FIELD_NAME = "x";
  private static final String DECODING_FUNCTION_NAME = "f_x";
  private static final String MOUNT_PATH = "/app/main.py";

  private static final ImageFromDockerfile DOCKER_IMAGE = new ImageFromDockerfile()
      .withDockerfileFromBuilder(builder ->
          builder
              .from("python:3.8")
              .run("python3 -m pip install z3 z3-solver")
              .cmd("python3", MOUNT_PATH)
              .build());

  @TestFactory
  @Execution(ExecutionMode.CONCURRENT)
  Collection<DynamicTest> instructions() throws IOException, DuplicatedPassKeyException {
    var setup = setupPassManagerAndRunSpec("sys/risc-v/rv64im.vadl",
        PassOrders.gcbAndCppCodeGen(getConfiguration(false)));
    var spec = setup.specification();

    return spec.findAllFormats().flatMap(f -> f.fieldAccesses().stream())
        .map(fieldAccess -> DynamicTest.dynamicTest(fieldAccess.identifier.lower(),
            () -> verify(fieldAccess)))
        .toList();
  }

  void verify(Format.FieldAccess fieldAccess) {
    var decodingFunction = fieldAccess.accessFunction();
    // Then generate the z3 code for the f_x
    var visitorDecode = new Z3EncodingCodeGeneratorVisitor(GENERIC_FIELD_NAME);
    visitorDecode.visit(decodingFunction.behavior().getNodes(ReturnNode.class).findFirst().get());

    // Generate encoding from decoding.
    // This is what we would like to test for.
    if (fieldAccess.encoding() == null) {
      for (var strategy : GenerateFieldAccessEncodingFunctionPass.strategies) {
        if (strategy.checkIfApplicable(fieldAccess)) {
          strategy.generateEncoding(fieldAccess);
          break;
        }
      }
    }

    // Now the fieldAccess.encoding().behavior function is set with an inverted behavior graph.
    var visitorEncode = new Z3EncodingCodeGeneratorVisitor(DECODING_FUNCTION_NAME);
    visitorEncode.visit(
        fieldAccess.encoding().behavior().getNodes(ReturnNode.class).findFirst().get());

    var generatedDecodeFunctionCode = visitorDecode.getResult();
    var generatedEncodeWithDecodeFunctionCode = visitorEncode.getResult();

    /*
     * How does this work?
     *
     * Well, you have an instruction word `x`, `f_x` is the decoding function
     * and `f_z` is the concatenation of the encoding function with the encoding function.
     * The visitor for encode is referencing the `DECODING_FUNCTION_NAME` when generating the
     * predicates.
     * The invocation of the `prove` function tries to verify that the original instruction word `x`
     * and the application of encode with decode is equivalent.
     */
    String z3Code = String.format("""
            from z3 import *
                        
            x = BitVec('x', %d) # field
                        
            f_x = %s
            f_z = %s
                        
            def prove(f):
                s = Solver()
                s.add(Not(f))
                if s.check() == unsat:
                    print("proved")
                    exit(0)
                else:
                    print("failed to prove")
                    exit(1)
                        
            prove(x == f_z)
            """, fieldAccess.fieldRef().bitSlice().bitSize(),
        generatedDecodeFunctionCode,
        generatedEncodeWithDecodeFunctionCode);
    logger.info(z3Code);

    try {
      runContainerWithContent(DOCKER_IMAGE, z3Code, MOUNT_PATH);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
