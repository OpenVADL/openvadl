package vadl.lcb.codegen;

import static org.awaitility.Awaitility.await;
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
import java.math.BigInteger;
import java.time.Duration;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.AbstractTest;
import vadl.gcb.passes.encoding.strategies.EncodingGenerationStrategy;
import vadl.gcb.passes.encoding.strategies.impl.ArithmeticImmediateStrategy;
import vadl.gcb.passes.encoding.strategies.impl.ShiftedImmediateStrategy;
import vadl.gcb.passes.encoding.strategies.impl.TrivialImmediateStrategy;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.graph.Graph;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.TypeCastNode;

public class EncodingCodeGeneratorVerificationTest extends AbstractTest {
  private static final Logger logger =
      LoggerFactory.getLogger(EncodingCodeGeneratorVerificationTest.class);

  private static final String GENERIC_FIELD_NAME = "x";
  private static final String ENCODING_FUNCTION_NAME = "f_x";
  private static final String IMAGE = "py-z3:latest";
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

  private static Stream<Arguments> createFieldAccessFunctions() {
    return Stream.of(
        Arguments.of(createUnsignedInt32DecodingFunction(), new TrivialImmediateStrategy()),
        Arguments.of(createSignedInt32DecodingFunction(), new TrivialImmediateStrategy()),
        Arguments.of(createUnsignedInt32ShiftDecodingFunction(), new ShiftedImmediateStrategy()),
        Arguments.of(createSignedInt32ShiftDecodingFunction(), new ShiftedImmediateStrategy()),
        Arguments.of(createUnsignedInt32WithAdditionDecodingFunction(),
            new ArithmeticImmediateStrategy()),
        Arguments.of(createSignedInt32WithAdditionDecodingFunction(),
            new ArithmeticImmediateStrategy()),
        Arguments.of(createUnsignedInt32WithSubtractionDecodingFunction(),
            new ArithmeticImmediateStrategy()),
        Arguments.of(createSignedInt32WithSubtractionDecodingFunction(),
            new ArithmeticImmediateStrategy())
    );
  }

  @ParameterizedTest
  @MethodSource("createFieldAccessFunctions")
  void verifyStrategies(Function encodingFunction, EncodingGenerationStrategy strategy)
      throws IOException {
    // Setup decoding
    var fieldAccess = new Format.FieldAccess(createIdentifier("fieldAccessIdentifierValue"),
        encodingFunction, null, null);

    // Then generate the z3 code for the f_x
    var visitorDecode = new Z3EncodingCodeGeneratorVisitor(GENERIC_FIELD_NAME);
    visitorDecode.visit(encodingFunction.behavior().getNodes(ReturnNode.class).findFirst().get());

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
                    
            # Define the variables
            x = BitVec('x', %d) # field
                    
            f_x = %s
            f_z = %s
                    
            prove(x == f_z)
            """, fieldAccess.fieldRef().bitSlice().bitSize(),
        generatedDecodeFunctionCode,
        generatedEncodeWithDecodeFunctionCode);
    logger.atDebug().log(z3Code);

    var tempFile = writeCodeIntoTempFile(z3Code);
    runContainer(tempFile);

  }

  private void runContainer(File tempFile) {
    var createContainerCmd = dockerClient.createContainerCmd(IMAGE);
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

  private File writeCodeIntoTempFile(String z3Code) throws IOException {
    var tempFile = File.createTempFile("encoding-z3", "py");
    tempFile.deleteOnExit();
    var writer = new FileWriter(tempFile);
    writer.write(z3Code);
    writer.close();
    return tempFile;
  }

  private static Function createUnsignedInt32DecodingFunction() {
    var function = createFunction("functionNameValue", Type.unsignedInt(32));
    var field = createFieldWithParent("fieldNameIdentifierValue", DataType.bits(20),
        new Constant.BitSlice(new Constant.BitSlice.Part[] {new Constant.BitSlice.Part(19, 0)}),
        32);
    var returnNode = new ReturnNode(
        new TypeCastNode(new FieldRefNode(field, DataType.bits(20)), Type.unsignedInt(32)));
    var graph = new Graph("graphValue");
    graph.addWithInputs(returnNode);
    function.setBehavior(graph);
    return function;
  }

  private static Function createSignedInt32DecodingFunction() {
    var function = createFunction("functionNameValue", Type.signedInt(32));
    var field = createFieldWithParent("fieldNameIdentifierValue", DataType.bits(20),
        new Constant.BitSlice(new Constant.BitSlice.Part[] {new Constant.BitSlice.Part(19, 0)}),
        32);
    var returnNode = new ReturnNode(
        new TypeCastNode(new FieldRefNode(field, DataType.bits(20)), Type.signedInt(32)));
    var graph = new Graph("graphValue");
    graph.addWithInputs(returnNode);
    function.setBehavior(graph);
    return function;
  }

  private static Function createUnsignedInt32ShiftDecodingFunction() {
    var function = createFunction("functionNameValue", Type.unsignedInt(32));
    var field = createFieldWithParent("fieldNameIdentifierValue", DataType.bits(20),
        new Constant.BitSlice(new Constant.BitSlice.Part[] {new Constant.BitSlice.Part(19, 0)}),
        32);
    var returnNode = new ReturnNode(
        new BuiltInCall(BuiltInTable.LSL, new NodeList<>(
            new TypeCastNode(new FieldRefNode(field, DataType.bits(20)), Type.unsignedInt(32)),
            new ConstantNode(new Constant.Value(BigInteger.valueOf(6), DataType.unsignedInt(32)))),
            Type.unsignedInt(32))
    );
    var graph = new Graph("graphValue");
    graph.addWithInputs(returnNode);
    function.setBehavior(graph);
    return function;
  }

  private static Function createSignedInt32ShiftDecodingFunction() {
    var function = createFunction("functionNameValue", Type.signedInt(32));
    var field = createFieldWithParent("fieldNameIdentifierValue", DataType.bits(20),
        new Constant.BitSlice(new Constant.BitSlice.Part[] {new Constant.BitSlice.Part(19, 0)}),
        32);
    var returnNode = new ReturnNode(
        new BuiltInCall(BuiltInTable.LSL, new NodeList<>(
            new TypeCastNode(new FieldRefNode(field, DataType.bits(20)), Type.signedInt(32)),
            new ConstantNode(new Constant.Value(BigInteger.valueOf(6), DataType.unsignedInt(32)))),
            Type.signedInt(32))
    );
    var graph = new Graph("graphValue");
    graph.addWithInputs(returnNode);
    function.setBehavior(graph);
    return function;
  }

  private static Function createUnsignedInt32WithAdditionDecodingFunction() {
    var function = createFunction("functionNameValue", Type.unsignedInt(32));
    var field = createFieldWithParent("fieldNameIdentifierValue", DataType.bits(20),
        new Constant.BitSlice(new Constant.BitSlice.Part[] {new Constant.BitSlice.Part(19, 0)}),
        32);
    var returnNode = new ReturnNode(
        new BuiltInCall(BuiltInTable.ADD, new NodeList<>(
            new TypeCastNode(new FieldRefNode(field, DataType.bits(20)), Type.unsignedInt(32)),
            new ConstantNode(new Constant.Value(BigInteger.valueOf(6), DataType.unsignedInt(32)))),
            Type.unsignedInt(32))
    );
    var graph = new Graph("graphValue");
    graph.addWithInputs(returnNode);
    function.setBehavior(graph);
    return function;
  }

  private static Function createSignedInt32WithAdditionDecodingFunction() {
    var function = createFunction("functionNameValue", Type.signedInt(32));
    var field = createFieldWithParent("fieldNameIdentifierValue", DataType.bits(20),
        new Constant.BitSlice(new Constant.BitSlice.Part[] {new Constant.BitSlice.Part(19, 0)}),
        32);
    var returnNode = new ReturnNode(
        new BuiltInCall(BuiltInTable.ADD, new NodeList<>(
            new TypeCastNode(new FieldRefNode(field, DataType.bits(20)), Type.signedInt(32)),
            new ConstantNode(new Constant.Value(BigInteger.valueOf(6), DataType.signedInt(32)))),
            Type.signedInt(32))
    );
    var graph = new Graph("graphValue");
    graph.addWithInputs(returnNode);
    function.setBehavior(graph);
    return function;
  }

  private static Function createUnsignedInt32WithSubtractionDecodingFunction() {
    var function = createFunction("functionNameValue", Type.unsignedInt(32));
    var field = createFieldWithParent("fieldNameIdentifierValue", DataType.bits(20),
        new Constant.BitSlice(new Constant.BitSlice.Part[] {new Constant.BitSlice.Part(19, 0)}),
        32);
    var returnNode = new ReturnNode(
        new BuiltInCall(BuiltInTable.SUB, new NodeList<>(
            new TypeCastNode(new FieldRefNode(field, DataType.bits(20)), Type.unsignedInt(32)),
            new ConstantNode(new Constant.Value(BigInteger.valueOf(6), DataType.unsignedInt(32)))),
            Type.unsignedInt(32))
    );
    var graph = new Graph("graphValue");
    graph.addWithInputs(returnNode);
    function.setBehavior(graph);
    return function;
  }

  private static Function createSignedInt32WithSubtractionDecodingFunction() {
    var function = createFunction("functionNameValue", Type.signedInt(32));
    var field = createFieldWithParent("fieldNameIdentifierValue", DataType.bits(20),
        new Constant.BitSlice(new Constant.BitSlice.Part[] {new Constant.BitSlice.Part(19, 0)}),
        32);
    var returnNode = new ReturnNode(
        new BuiltInCall(BuiltInTable.SUB, new NodeList<>(
            new TypeCastNode(new FieldRefNode(field, DataType.bits(20)), Type.signedInt(32)),
            new ConstantNode(new Constant.Value(BigInteger.valueOf(6), DataType.signedInt(32)))),
            Type.signedInt(32))
    );
    var graph = new Graph("graphValue");
    graph.addWithInputs(returnNode);
    function.setBehavior(graph);
    return function;
  }


}
