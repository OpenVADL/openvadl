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
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import vadl.DockerImage;
import vadl.configuration.LcbConfiguration;
import vadl.cppCodeGen.common.PredicateFunctionCodeGenerator;
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

  protected Arbitrary<Integer> allIntegersExcept(int minExclusive, int maxExclusive) {
    return Arbitraries.oneOf(Arbitraries.integers().lessOrEqual(minExclusive - 1),
        Arbitraries.integers().greaterOrEqual(maxExclusive + 1)
    );
  }


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
          input.arbitrary.sampleStream().limit(5).forEach(sample -> {
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

  private Pair<DockerImage, TestSetup> setup(LcbConfiguration configuration,
                                             String specification)
      throws IOException, DuplicatedPassKeyException {

    var setup = runLcb(configuration, specification);

    // Move files into Docker Context
    {
      VadlFileUtils.createDirectories(configuration, "encoding", "inputs");
      VadlFileUtils.copyResource("templates/common/vadl-builtins.h",
          Path.of(configuration.outputPath() + "/vadl-builtins.h")
      );
      VadlFileUtils.copyDirectory(
          Path.of(
              "test/resources/images/encodingCodeGeneratorCppVerification/"),
          Path.of(configuration.outputPath() + "/encoding/"));
    }

    return Pair.of(new DockerImage()
        .withDockerfile(Paths.get(configuration.outputPath() + "/encoding/Dockerfile")), setup);
  }

  protected String renderPositive(GcbCppFunctionWithBody record, Format.FieldAccess fieldAccess,
                                  int sample) {
    var predicateFunctionGenerator =
        new PredicateFunctionCodeGenerator(record.header(), fieldAccess,
            record.header().functionName().lower());

    var predicateFunction = predicateFunctionGenerator.genFunctionDefinition();

    String cppCode = String.format("""
            #include <cstdint>
            #include <iostream>
            #include <bitset>
            #include <vector>
            
            // Imported by manual copy mapping
            #include "/vadl-builtins.h"
            
            template<int start, int end, std::size_t N>
            std::bitset<N> project_range(std::bitset<N> bits)
            {
                std::bitset<N> result;
                size_t result_index = 0; // Index for the new bitset
            
                // Extract bits from the range [start, end]
                for (size_t i = start; i <= end; ++i) {
                  result[result_index] = bits[i];
                  result_index++;
                }
            
                return result;
            }
            
            template<std::size_t N, std::size_t M>
            std::bitset<N> set_bits(std::bitset<N> dest, const std::bitset<M> source, std::vector<int> bits) {
                auto target = 0;
                for (int i = bits.size() - 1; i >= 0 ; --i) {
                    auto j = bits[target];
                    dest.set(j, source[i]);
                    target++;
                }
            
                return dest;
            }
            
            %s
            
            int main() {
              auto actual = %s(%d);
              if(actual) {
                std::cout << "ok" << std::endl;
                return 0;
              } else {
                std::cout << "not ok" << std::endl;
                return -1;
              }
            }
            """,
        predicateFunction,
        record.header().identifier.lower(),
        sample);

    return cppCode;
  }

  protected String renderNegative(GcbCppFunctionWithBody record, Format.FieldAccess fieldAccess,
                               int sample) {
    var predicateFunctionGenerator =
        new PredicateFunctionCodeGenerator(record.header(), fieldAccess,
            record.header().functionName().lower());

    var predicateFunction = predicateFunctionGenerator.genFunctionDefinition();

    String cppCode = String.format("""
            #include <cstdint>
            #include <iostream>
            #include <bitset>
            #include <vector>
            
            // Imported by manual copy mapping
            #include "/vadl-builtins.h"
            
            template<int start, int end, std::size_t N>
            std::bitset<N> project_range(std::bitset<N> bits)
            {
                std::bitset<N> result;
                size_t result_index = 0; // Index for the new bitset
            
                // Extract bits from the range [start, end]
                for (size_t i = start; i <= end; ++i) {
                  result[result_index] = bits[i];
                  result_index++;
                }
            
                return result;
            }
            
            template<std::size_t N, std::size_t M>
            std::bitset<N> set_bits(std::bitset<N> dest, const std::bitset<M> source, std::vector<int> bits) {
                auto target = 0;
                for (int i = bits.size() - 1; i >= 0 ; --i) {
                    auto j = bits[target];
                    dest.set(j, source[i]);
                    target++;
                }
            
                return dest;
            }
            
            %s
            
            int main() {
              auto actual = %s(%d);
              if(!actual) {
                std::cout << "ok" << std::endl;
                return 0;
              } else {
                std::cout << "not ok" << std::endl;
                return -1;
              }
            }
            """,
        predicateFunction,
        record.header().identifier.lower(),
        sample);

    return cppCode;
  }
}
