package vadl.lcb.codegen;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.gcb.passes.encoding.strategies.impl.TrivialImmediateStrategy;
import vadl.types.BitsType;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Parameter;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.TypeCastNode;

public class EncodingCodeGeneratorVerificationTest extends AbstractTest {

  private static final String GENERIC_FIELD_NAME = "x";
  private static final String ENCODING_FUNCTION_NAME = "f_x";
  private final DockerClientConfig config =
      DefaultDockerClientConfig.createDefaultConfigBuilder().build();
  private final DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
      .dockerHost(config.getDockerHost())
      .sslConfig(config.getSSLConfig())
      .maxConnections(100)
      .connectionTimeout(Duration.ofSeconds(30))
      .responseTimeout(Duration.ofSeconds(45))
      .build();
  private final DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);

  @Test
  void test() throws IOException {
    var strategy = new TrivialImmediateStrategy();

    // Setup decoding
    var function = new Function(createIdentifier("functionNameValue"),
        new Parameter[] {},
        Type.unsignedInt(32));
    var graph = new Graph("graphValue");
    var format = new Format(createIdentifier("formatIdentifierValue"), BitsType.bits(32));
    var field = new Format.Field(
        createIdentifier("fieldNameIdentifier"),
        DataType.bits(20),
        new Constant.BitSlice(new Constant.BitSlice.Part[] {new Constant.BitSlice.Part(19, 0)}),
        format
    );
    var returnNode = new ReturnNode(
        new TypeCastNode(new FieldRefNode(field, DataType.bits(20)), Type.unsignedInt(32)));
    var addedReturnNode = graph.addWithInputs(returnNode);
    function.setBehavior(graph);
    var fieldAccess = new Format.FieldAccess(createIdentifier("fieldAccessIdentifierValue"),
        function, null, null);

    var visitorDecode = new Z3EncodingCodeGeneratorVisitor(GENERIC_FIELD_NAME);
    visitorDecode.visit(addedReturnNode);

    // Generate encoding from decoding
    strategy.generateEncoding(fieldAccess);

    // Now the fieldAccess.decode function is set with an inverted behavior graph.
    var visitorEncode = new Z3EncodingCodeGeneratorVisitor(ENCODING_FUNCTION_NAME);
    visitorEncode.visit(
        fieldAccess.encoding().behavior().getNodes(ReturnNode.class).findFirst().get());

    var generatedDecodeFunctionCode = visitorDecode.getResult();
    var generatedEncodeWithDecodeFunctionCode = visitorEncode.getResult();
    String x = """
        from z3 import *
                
        # Define the variables
        x = BitVec('x', 20) # field
                
        f_x = """;
    x += generatedDecodeFunctionCode + "\n";
    x += """
        f_z = """;
    x += generatedEncodeWithDecodeFunctionCode + "\n";
    x += """
        prove(x == f_z)
        """;

    var tempFile = File.createTempFile("encoding-z3", "py");
    tempFile.deleteOnExit();
    var writer = new FileWriter(tempFile);
    writer.write(x);
    writer.close();

    var createContainerCmd = dockerClient.createContainerCmd("py-z3:latest");
    Objects.requireNonNull(createContainerCmd
            .getHostConfig())
        .withBinds(Bind.parse(tempFile.toPath() + ":/app/main.py"));
    var response = createContainerCmd.exec();
    dockerClient.startContainerCmd(response.getId()).exec();

    await()
        .atMost(Duration.ofSeconds(10))
        .until(() -> {
          var execution = dockerClient.inspectContainerCmd(response.getId()).exec();
          return execution.getState().getStatus().equals("exited");
        });

    var execution = dockerClient.inspectContainerCmd(response.getId()).exec();
    assertEquals(0, execution.getState().getExitCodeLong(),
        "Failed in container " + execution.getName());

  }
}
