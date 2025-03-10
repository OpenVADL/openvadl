// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.viam.algebraic_simplification;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.images.builder.ImageFromDockerfile;
import vadl.DockerExecutionTest;
import vadl.pass.PassResults;
import vadl.viam.Instruction;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Specification;
import vadl.viam.passes.algebraic_simplication.AlgebraicSimplificationPass;
import vadl.viam.passes.functionInliner.FunctionInlinerPass;
import vadl.viam.passes.translation_validation.ExplicitBitSizesInTypingPass;
import vadl.viam.passes.translation_validation.ExtendMultiplicationPass;
import vadl.viam.passes.translation_validation.TranslationValidation;

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
  @Execution(ExecutionMode.CONCURRENT)
  Collection<DynamicTest> instructions() throws IOException {
    var configuration = getConfiguration(false);
    var initialSpec = runAndGetViamSpecification("sys/risc-v/rv64im.vadl");
    var spec = runAndGetViamSpecification("sys/risc-v/rv64im.vadl");

    new FunctionInlinerPass(configuration).execute(PassResults.empty(), initialSpec);
    new FunctionInlinerPass(configuration).execute(PassResults.empty(), spec);

    new ExtendMultiplicationPass(configuration).execute(PassResults.empty(), initialSpec);
    new ExtendMultiplicationPass(configuration).execute(PassResults.empty(), spec);

    // Add explicit bit sizes
    new ExplicitBitSizesInTypingPass(configuration).execute(PassResults.empty(), initialSpec);
    new ExplicitBitSizesInTypingPass(configuration).execute(PassResults.empty(), spec);

    var allBeforeInstructions =
        initialSpec.isa().map(InstructionSetArchitecture::ownInstructions).orElseGet(List::of);
    var allAfterInstructions =
        spec.isa().map(x -> x.ownInstructions().stream())
            .orElse(Stream.empty())
            .collect(Collectors.toMap(Instruction::simpleName, Function.identity()));

    // When
    var pass = new AlgebraicSimplificationPass(configuration);
    pass.execute(PassResults.empty(), spec);

    ArrayList<DynamicTest> tests = new ArrayList<>();
    for (Instruction left : allBeforeInstructions) {
      var right = allAfterInstructions.get(left.simpleName());
      tests.add(DynamicTest.dynamicTest(left.simpleName(), () -> {
        testInstruction(spec, left, right);
      }));
    }

    return tests;
  }

  void testInstruction(Specification specification, Instruction before, Instruction after) {
    var translationValidation = new TranslationValidation();
    var code = translationValidation.lower(specification, before, after);
    if (code.isPresent()) {
      logger.info(code.get().value());
      try {
        runContainerAndCopyInputIntoContainer(DOCKER_IMAGE, code.get().value(), MOUNT_PATH);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      logger.warn("Skipping because all side effects have the same behavior.");
    }
  }
}
