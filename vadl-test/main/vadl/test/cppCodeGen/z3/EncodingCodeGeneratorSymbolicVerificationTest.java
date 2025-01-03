package vadl.test.cppCodeGen.z3;

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
import vadl.gcb.passes.encodingGeneration.GenerateFieldAccessEncodingFunctionPass;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.test.gcb.AbstractGcbTest;
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
    var configuration = new GcbConfiguration(getConfiguration(false));
    var setup = runGcbAndCppCodeGen(configuration, "sys/risc-v/rv64im.vadl");

    // Move files into Docker Context
    {
      VadlFileUtils.createDirectories(configuration, "encoding", "inputs");
      VadlFileUtils.copyDirectory(
          Path.of(
              "../../open-vadl/vadl-test/main/resources/images/encodingCodeGeneratorSymbolicVerification/"),
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
