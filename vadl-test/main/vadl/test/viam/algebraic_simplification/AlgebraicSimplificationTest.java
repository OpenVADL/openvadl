package vadl.test.viam.algebraic_simplification;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.shaded.com.google.common.collect.Streams;
import vadl.test.DockerExecutionTest;
import vadl.viam.Instruction;
import vadl.viam.passes.algebraic_simplication.AlgebraicSimplificationPass;
import vadl.viam.translation_validation.TranslationValidation;

public class AlgebraicSimplificationTest extends DockerExecutionTest {

  private static final String MOUNT_PATH = "/app/main.py";

  private static final ImageFromDockerfile DOCKER_IMAGE = new ImageFromDockerfile()
      .withDockerfileFromBuilder(builder ->
          builder
              .from("python:3.8")
              .run("python3 -m pip install z3 z3-solver")
              .cmd("python3", MOUNT_PATH)
              .build());

  @Test
  void testRv32im() {
    // Given
    var initialSpec = runAndGetViamSpecification("examples/rv3264im.vadl");
    var spec = runAndGetViamSpecification("examples/rv3264im.vadl");
    var pass = new AlgebraicSimplificationPass();

    // When
    pass.execute(Collections.emptyMap(), spec);

    // Then
    var allBeforeInstructions = initialSpec.isas().flatMap(x -> x.instructions().stream()).toList();
    var allAfterInstructions = spec.isas().flatMap(x -> x.instructions().stream()).collect(
        Collectors.toMap(Instruction::name, Function.identity()));

    for (Instruction left : allBeforeInstructions) {
      var right = allAfterInstructions.get(left.name());
      var translationValidation = new TranslationValidation();
      var code = translationValidation.lower(left, right);
      try {
        runContainerWithContent(DOCKER_IMAGE, code.value(), MOUNT_PATH, "algebraic",
            "simplification");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
