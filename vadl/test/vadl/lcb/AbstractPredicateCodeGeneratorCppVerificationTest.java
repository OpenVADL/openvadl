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

package vadl.lcb;

import static java.util.stream.Collectors.toCollection;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.jqwik.api.Arbitrary;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.images.builder.ImageFromDockerfile;
import vadl.configuration.LcbConfiguration;
import vadl.cppCodeGen.common.GcbAccessOrPredicateFunctionCodeGenerator;
import vadl.cppCodeGen.model.GcbCppFunctionWithBody;
import vadl.gcb.valuetypes.TargetName;
import vadl.lcb.passes.llvmLowering.CreateFunctionsFromImmediatesPass;
import vadl.lcb.passes.llvmLowering.tablegen.model.TableGenImmediateRecord;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.utils.Pair;
import vadl.utils.VadlFileUtils;
import vadl.viam.Format;

public abstract class AbstractPredicateCodeGeneratorCppVerificationTest extends AbstractLcbTest {
  public record Test(String name, String fieldAccessFunction, Arbitrary<Integer> arbitrary) {

  }

  public abstract String specification();

  public abstract Stream<Test> inputs();

  public abstract String render(GcbCppFunctionWithBody record, Format.FieldAccess fieldAccess,
                                int sample);


  @TestFactory
  List<DynamicTest> cases() throws DuplicatedPassKeyException, IOException {
    var configuration =
        new LcbConfiguration(getConfiguration(false), new TargetName("processorNameValue"));
    var pair = setup(configuration, specification());
    var image = pair.left();
    var setup = pair.right();
    var predicates = predicates(setup)
        .entrySet()
        .stream()
        .collect(Collectors.toMap(
            x -> Pair.of(x.getKey().instructionRef().identifier().simpleName(),
                x.getKey().fieldAccessRef().identifier.simpleName()),
            x -> Pair.of(x.getKey().fieldAccessRef(), x.getValue())));

    var mappings = inputs()
        .flatMap(input -> {
          var value = predicates.get(Pair.of(input.name, input.fieldAccessFunction));
          var fieldAccess = value.left();
          var record = value.right();
          List<Pair<String, String>> copyMappings = new ArrayList<>();
          input.arbitrary.sampleStream().limit(1).forEach(sample -> {
            var fileName = record.header().functionName().lower() + "_sample_" + sample + ".cpp";
            var filePath = configuration.outputPath() + "/inputs/" + fileName;
            var code = render(record, fieldAccess, sample);
            copyMappings.add(Pair.of(filePath, "/inputs/" + fileName));

            try {
              var fs = new FileWriter(filePath);
              fs.write(code);
              fs.close();
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          });

          return copyMappings.stream();
        })
        .collect(toCollection(ArrayList::new));

    var path = configuration.outputPath();

    // Add vadl-builtin
    mappings.add(new Pair<>(path.toString() + "/vadl-builtins.h",
        "/vadl-builtins.h"));

    runContainerAndCopyDirectoryIntoContainerAndCopyOutputBack(image,
        mappings,
        path + "/result.csv",
        "/work/output.csv");

    try {
      return assertStatusCodes(path + "/result.csv");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static Map<TableGenImmediateRecord, GcbCppFunctionWithBody> predicates(TestSetup setup) {
    var passManager = setup.passManager();
    var output = (CreateFunctionsFromImmediatesPass.Output) passManager.getPassResults()
        .lastResultOf(CreateFunctionsFromImmediatesPass.class);
    return output.predicates();
  }

  private Pair<ImageFromDockerfile, TestSetup> setup(LcbConfiguration configuration,
                                                     String specification)
      throws IOException, DuplicatedPassKeyException {

    var setup = runLcb(configuration, specification);

    // Move files into Docker Context
    {
      VadlFileUtils.createDirectories(configuration, "encoding", "inputs");
      VadlFileUtils.copyFile(Path.of(
              "main/resources/templates/common/vadl-builtins.h"
          ),
          Path.of(configuration.outputPath() + "/vadl-builtins.h")
      );
      VadlFileUtils.copyDirectory(
          Path.of(
              "test/resources/images/encodingCodeGeneratorCppVerification/"),
          Path.of(configuration.outputPath() + "/encoding/"));
    }

    return Pair.of(new ImageFromDockerfile()
        .withDockerfile(Paths.get(configuration.outputPath() + "/encoding/Dockerfile")), setup);
  }
}
