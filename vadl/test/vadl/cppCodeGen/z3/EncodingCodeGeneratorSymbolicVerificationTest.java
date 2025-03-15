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

package vadl.cppCodeGen.z3;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.images.builder.ImageFromDockerfile;
import vadl.configuration.GcbConfiguration;
import vadl.gcb.AbstractGcbTest;
import vadl.gcb.passes.encodingGeneration.GenerateFieldAccessEncodingFunctionPass;
import vadl.gcb.valuetypes.TargetName;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.utils.Pair;
import vadl.utils.VadlFileUtils;
import vadl.viam.Format;
import vadl.viam.graph.control.ReturnNode;

/**
 * Testing the conversion between encoding and decoding symbolically with a theorem prover.
 * Note that only field accesses **without** an encoding function are generating encodings
 * automatically and testing it.
 */
public class EncodingCodeGeneratorSymbolicVerificationTest extends AbstractGcbTest {
  private static final String GENERIC_FIELD_NAME = "x";
  private static final String DECODING_FUNCTION_NAME = "f_x";

  @TestFactory
  Collection<DynamicTest> instructions() throws DuplicatedPassKeyException, IOException {
    var configuration =
        new GcbConfiguration(getConfiguration(false), new TargetName("processorNameValue"));
    var setup = runGcbAndCppCodeGen(configuration, "sys/risc-v/rv64im.vadl");

    // Move files into Docker Context
    {
      VadlFileUtils.createDirectories(configuration, "encoding", "inputs");
      VadlFileUtils.copyDirectory(
          Path.of(
              "test/resources/images/python_z3/"),
          Path.of(configuration.outputPath() + "/encoding/"));
    }

    var image = new ImageFromDockerfile()
        .withDockerfile(Paths.get(configuration.outputPath() + "/encoding/Dockerfile"));

    // Generate files and output them into the temporary directory.
    return generateInputs(setup, image, configuration.outputPath());
  }

  private Collection<DynamicTest> generateInputs(TestSetup setup,
                                                 ImageFromDockerfile image,
                                                 Path path) throws IOException {
    var spec = setup.specification();

    List<Pair<String, String>> copyMappings = new ArrayList<>();
    spec.findAllFormats().flatMap(f -> f.fieldAccesses().stream())
        .forEach(fieldAccess -> {
          var testCase = createTestCase(fieldAccess);
          copyMappings.add(Pair.of(path.toString() + "/inputs/" + testCase.testName() + ".py",
              "/inputs/" + testCase.testName() + ".py"));
        });

    runContainerAndCopyDirectoryIntoContainerAndCopyOutputBack(image,
        copyMappings,
        path + "/result.csv",
        "/work/output.csv");

    return assertStatusCodes(path + "/result.csv");
  }

  private TestCase createTestCase(Format.FieldAccess fieldAccess) {
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

    return new TestCase(fieldAccess.identifier.lower(), z3Code);
  }
}
