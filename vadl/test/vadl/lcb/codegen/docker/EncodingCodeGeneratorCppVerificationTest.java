package vadl.lcb.codegen.docker;

import java.io.IOException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.images.builder.ImageFromDockerfile;
import vadl.DockerExecutionTest;
import vadl.gcb.passes.encoding_generation.strategies.EncodingGenerationStrategy;
import vadl.lcb.codegen.EncodingCodeGenerator;
import vadl.oop.passes.field_node_replacement.FieldNodeReplacementPass;
import vadl.oop.passes.type_normalization.CppTypeNormalizationPass;
import vadl.viam.Format;
import vadl.viam.Function;

public class EncodingCodeGeneratorCppVerificationTest extends DockerExecutionTest {
  private static final String MOUNT_PATH = "/app/main.cpp";

  private static final Logger logger =
      LoggerFactory.getLogger(EncodingCodeGeneratorCppVerificationTest.class);

  private static final ImageFromDockerfile DOCKER_IMAGE = new ImageFromDockerfile()
      .withDockerfileFromBuilder(builder ->
          builder
              .from("gcc:12.4.0")
              .cmd("c++", "-Wall", MOUNT_PATH)
              .build());

  private static final String TEMPFILE_PREFIX = "encoding";
  private static final String TEMPFILE_SUFFIX = "cpp";

  @ParameterizedTest
  @MethodSource("vadl.lcb.codegen.docker.EncodingCodeGeneratorTestInputs#createFieldAccessFunctions")
  void verifyStrategies(Function decodingFunction, EncodingGenerationStrategy strategy)
      throws IOException {
    var fieldAccess = new Format.FieldAccess(createIdentifier("fieldAccessIdentifierValue"),
        decodingFunction, null, null);

    // Replace first field accesses
    FieldNodeReplacementPass.replaceFieldRefNodes(decodingFunction);
    // then upcast
    var normalizedDecodeFunction = CppTypeNormalizationPass.makeTypesCppConform(decodingFunction);

    var decodeCodeGenerator = new EncodingCodeGenerator();
    var encodeCodeGenerator = new EncodingCodeGenerator();

    // We are testing that the generation is correct.
    strategy.generateEncoding(fieldAccess);
    var normalizedEncodeFunction = CppTypeNormalizationPass.makeTypesCppConform(fieldAccess.encoding());

    var decodeFunction = decodeCodeGenerator.generateFunction(normalizedDecodeFunction);
    var encodeFunction = encodeCodeGenerator.generateFunction(normalizedEncodeFunction);

    String cppCode = String.format("""
        #include <cstdint>
                
        %s 
                
        %s
                
        int main() {
           
        }
        """, decodeFunction, encodeFunction);

    logger.atDebug().log(cppCode);
    runContainerWithContent(DOCKER_IMAGE, cppCode, MOUNT_PATH, TEMPFILE_PREFIX, TEMPFILE_SUFFIX);
  }
}
