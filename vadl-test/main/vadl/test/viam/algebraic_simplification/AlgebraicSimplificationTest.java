package vadl.test.viam.algebraic_simplification;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.images.builder.ImageFromDockerfile;
import vadl.test.DockerExecutionTest;
import vadl.viam.Instruction;
import vadl.viam.Specification;
import vadl.viam.passes.algebraic_simplication.AlgebraicSimplificationPass;
import vadl.viam.translation_validation.TranslationValidation;

public class AlgebraicSimplificationTest extends DockerExecutionTest {
  private static final Logger logger = LoggerFactory.getLogger(AlgebraicSimplificationTest.class);

  private static final String MOUNT_PATH = "/app/main.py";

  private static final ImageFromDockerfile DOCKER_IMAGE = new ImageFromDockerfile()
      .withDockerfileFromBuilder(builder ->
          builder
              .from("python:3.8")
              .run("python3 -m pip install z3 z3-solver")
              .cmd("python3", MOUNT_PATH)
              .build());

  @TestFactory
  Collection<DynamicTest> instructions() {
    var initialSpec = runAndGetViamSpecification("examples/rv3264im.vadl");
    var spec = runAndGetViamSpecification("examples/rv3264im.vadl");
    var allBeforeInstructions = initialSpec.isas().flatMap(x -> x.instructions().stream()).toList();
    var allAfterInstructions = spec.isas().flatMap(x -> x.instructions().stream()).collect(
        Collectors.toMap(Instruction::name, Function.identity()));

    // When
    var pass = new AlgebraicSimplificationPass();
    pass.execute(Collections.emptyMap(), spec);

    ArrayList<DynamicTest> tests = new ArrayList<>();
    for (Instruction left : allBeforeInstructions) {
      var right = allAfterInstructions.get(left.name());
      tests.add(DynamicTest.dynamicTest(left.name(), () -> {
        testInstruction(left, right);
      }));
    }

    return tests;
  }

  void testInstruction(Instruction before, Instruction after) {
    var translationValidation = new TranslationValidation();
    var code = translationValidation.lower(before, after);
    logger.info(code.value());
    try {
      runContainerWithContent(DOCKER_IMAGE, code.value(), MOUNT_PATH, "algebraic",
          "simplification");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
