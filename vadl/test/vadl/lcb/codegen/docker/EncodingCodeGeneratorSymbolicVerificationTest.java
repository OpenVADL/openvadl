package vadl.lcb.codegen.docker;

import java.io.IOException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.images.builder.ImageFromDockerfile;
import vadl.DockerExecutionTest;
import vadl.gcb.passes.encoding_generation.strategies.EncodingGenerationStrategy;
import vadl.lcb.codegen.Z3EncodingCodeGeneratorVisitor;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.graph.control.ReturnNode;

public class EncodingCodeGeneratorSymbolicVerificationTest extends DockerExecutionTest {
  private static final Logger logger =
      LoggerFactory.getLogger(EncodingCodeGeneratorSymbolicVerificationTest.class);

  private static final String GENERIC_FIELD_NAME = "x";
  private static final String ENCODING_FUNCTION_NAME = "f_x";
  private static final String MOUNT_PATH = "/app/main.py";

  private static final ImageFromDockerfile DOCKER_IMAGE = new ImageFromDockerfile()
      .withDockerfileFromBuilder(builder ->
          builder
              .from("python:3.8")
              .run("python3 -m pip install z3 z3-solver")
              .cmd("python3", MOUNT_PATH)
              .build());
  private static final String TEMPFILE_PREFIX = "encoding-z3";
  private static final String TEMPFILE_SUFFIX = "py";

  @ParameterizedTest
  @MethodSource("vadl.lcb.codegen.docker.EncodingCodeGeneratorTestInputs#createFieldAccessFunctions")
  void verifyStrategies(Function decodingFunction, EncodingGenerationStrategy strategy)
      throws IOException {
    // Setup decoding
    var fieldAccess = new Format.FieldAccess(createIdentifier("fieldAccessIdentifierValue"),
        decodingFunction, null, null);

    // Then generate the z3 code for the f_x
    var visitorDecode = new Z3EncodingCodeGeneratorVisitor(GENERIC_FIELD_NAME);
    visitorDecode.visit(decodingFunction.behavior().getNodes(ReturnNode.class).findFirst().get());

    // Generate encoding from decoding.
    // This is what we would like to test for.
    strategy.generateEncoding(fieldAccess);

    // Now the fieldAccess.encoding().behavior function is set with an inverted behavior graph.
    var visitorEncode = new Z3EncodingCodeGeneratorVisitor(ENCODING_FUNCTION_NAME);
    visitorEncode.visit(
        fieldAccess.encoding().behavior().getNodes(ReturnNode.class).findFirst().get());

    var generatedDecodeFunctionCode = visitorDecode.getResult();
    var generatedEncodeWithDecodeFunctionCode = visitorEncode.getResult();
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
    logger.atDebug().log(z3Code);
    runContainerWithContent(DOCKER_IMAGE, z3Code, MOUNT_PATH, TEMPFILE_PREFIX, TEMPFILE_SUFFIX);
  }
}
