package vadl.cppCodeGen.passes.type_normalization;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.pass.Pass;
import vadl.pass.PassKey;
import vadl.types.BitsType;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.Function;
import vadl.viam.Parameter;
import vadl.viam.Specification;
import vadl.viam.ViamError;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.TypeCastNode;
import vadl.viam.graph.dependency.UnaryNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * When transforming a graph into a CPP code, we have to take care of unsupported types.
 * For example, VADL allows arbitrary bit sizes, however CPP has only fixed size types.
 * This pass inserts bit mask to ensure that the code generation works.
 */
public abstract class CppTypeNormalizationPass extends Pass {
  private static final Logger logger = LoggerFactory.getLogger(CppTypeNormalizationPass.class);

  /**
   * Get a list of functions on which the pass should be applied on.
   */
  protected abstract Stream<Function> getApplicable(Specification viam);

  @Nullable
  @Override
  public Object execute(Map<PassKey, Object> passResults, Specification viam)
      throws IOException {
    IdentityHashMap<Function, Function> results = new IdentityHashMap<>();

    getApplicable(viam).forEach(function -> {
      var cppFunction = makeTypesCppConform(function);
      results.put(function, cppFunction);
    });

    return results;
  }

  private static final HashSet<Type> cppSupportedTypes = new HashSet<>(List.of(
      DataType.bool(),
      DataType.unsignedInt(1),
      DataType.unsignedInt(8),
      DataType.unsignedInt(16),
      DataType.unsignedInt(32),
      DataType.unsignedInt(64),
      DataType.unsignedInt(128),
      DataType.signedInt(1),
      DataType.signedInt(8),
      DataType.signedInt(16),
      DataType.signedInt(32),
      DataType.signedInt(64),
      DataType.signedInt(128),
      DataType.bits(1),
      DataType.bits(8),
      DataType.bits(16),
      DataType.bits(32),
      DataType.bits(64),
      DataType.bits(128)
  ));

  /**
   * Changes the function so that all vadl types conform to CPP types
   * which simplifies the code generation.
   */
  public static Function makeTypesCppConform(Function function) {
    var liftedParameters = getParameters(function);
    var liftedResultTy = getResultTy(function);
    updateGraph(function.behavior());

    // We updated the old function's graph, so just take it for the new function.
    return new Function(function.identifier,
        liftedParameters.toArray(Parameter[]::new),
        liftedResultTy,
        function.behavior());
  }

  private static List<Parameter> getParameters(Function function) {
    return Arrays.stream(function.parameters())
        .map(parameter -> {
          if (!cppSupportedTypes.contains(parameter.type())) {
            logger.atDebug()
                .log("Parameter '{}' of type '{}' is not supported. Uplifting type.",
                    parameter.name(),
                    parameter.type());
            return upcast(parameter);
          } else {
            // do not modify existing types when they are ok.
            return parameter;
          }
        }).toList();
  }

  private static Type getResultTy(Function function) {
    if (!cppSupportedTypes.contains(function.returnType())) {
      return upcast(function.returnType());
    } else {
      return function.returnType();
    }
  }

  private static void cast(UnaryNode node, java.util.function.Function<BitsType, Node> buildNode) {
    var bitsType = (BitsType) node.type();
    var newBitSizeType = bitsType.withBitWidth(nextFittingType(
        bitsType.bitWidth()));
    var newNode = buildNode.apply(newBitSizeType);
    node.replaceAndDelete(newNode);
  }

  /**
   * This method checks all the typecasts and upcasts the type if necessary.
   * Additionally, it will also check the constant if the type is ok.
   */
  private static void updateGraph(Graph graph) {
    // Updating typecasts
    graph.getNodes(SignExtendNode.class)
        .filter(signExtendNode -> !cppSupportedTypes.contains(signExtendNode.type()))
        .forEach((signExtendNode -> cast(signExtendNode,
            (newType) -> new CppSignExtendNode(signExtendNode.value(), newType,
                signExtendNode.type()))));

    graph.getNodes(ZeroExtendNode.class)
        .filter(zeroExtendNode -> !cppSupportedTypes.contains(zeroExtendNode.type()))
        .forEach((zeroExtendNode -> cast(zeroExtendNode,
            (newType) -> new CppZeroExtendNode(zeroExtendNode.value(), newType,
                zeroExtendNode.type()))));

    graph.getNodes(TruncateNode.class)
        .filter(truncateNode -> !cppSupportedTypes.contains(truncateNode.type()))
        .forEach((truncateNode -> cast(truncateNode,
            (newType) -> new CppTruncateNode(truncateNode.value(), newType,
                truncateNode.type()))));


    // Updating constants
    var constantNodes = graph.getNodes(ConstantNode.class).toList();
    constantNodes
        .forEach(constantNode -> {
          if (constantNode.constant() instanceof Constant.Value constantValue
              && !cppSupportedTypes.contains(constantValue.type())) {
            constantNode.setConstant(constantValue.castTo(upcast(constantValue.type())));
          }
        });
  }

  private static Parameter upcast(Parameter parameter) {
    return new Parameter(parameter.identifier,
        upcast(parameter.type()));
  }

  private static BitsType upcast(Type type) {
    if (type instanceof BitsType cast) {
      return cast.withBitWidth(nextFittingType(cast.bitWidth()));
    } else {
      throw new ViamError("Non bits type are not supported");
    }
  }

  private static int nextFittingType(int old) {
    if (old == 1) {
      return 1;
    } else if (old > 1 && old <= 8) {
      return 8;
    } else if (old > 8 && old <= 16) {
      return 16;
    } else if (old > 16 && old <= 32) {
      return 32;
    } else if (old > 32 && old <= 64) {
      return 64;
    } else if (old > 64 && old <= 128) {
      return 128;
    }

    throw new ViamError("Types with more than 128 bits are not supported");
  }
}
