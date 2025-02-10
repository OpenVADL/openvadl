package vadl.cppCodeGen.passes.typeNormalization;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.configuration.GcbConfiguration;
import vadl.cppCodeGen.model.GcbFieldAccessCppFunction;
import vadl.cppCodeGen.model.GcbImmediateExtractionCppFunction;
import vadl.pass.Pass;
import vadl.pass.PassResults;
import vadl.types.BitsType;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.utils.Pair;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Parameter;
import vadl.viam.Specification;
import vadl.viam.ViamError;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.UnaryNode;
import vadl.viam.graph.dependency.ZeroExtendNode;

/**
 * When transforming a graph into a CPP code, we have to take care of unsupported types.
 * For example, VADL allows arbitrary bit sizes, however CPP has only fixed size types.
 * This pass inserts bit mask to ensure that the code generation works.
 * This pass does mutate the {@link Specification} because it needs to update the {@link Function}
 * which is immutable.
 */
public abstract class CppTypeNormalizationPass extends Pass {
  private static final Logger logger = LoggerFactory.getLogger(CppTypeNormalizationPass.class);

  public CppTypeNormalizationPass(GcbConfiguration gcbConfiguration) {
    super(gcbConfiguration);
  }

  /**
   * A container for storing the result of {@link CppTypeNormalizationPass}.
   */
  public static class NormalisedTypeResult {
    private final IdentityHashMap<Function, GcbFieldAccessCppFunction> functions =
        new IdentityHashMap<>();
    private final IdentityHashMap<Format.Field, GcbFieldAccessCppFunction> fields =
        new IdentityHashMap<>();
    private final IdentityHashMap<Format.FieldAccess, GcbFieldAccessCppFunction> fieldAccesses =
        new IdentityHashMap<>();

    private void add(Function key, Format.FieldAccess key2, GcbFieldAccessCppFunction value) {
      functions.put(key, value);
      fields.put(key2.fieldRef(), value);
      fieldAccesses.put(key2, value);
    }

    @Nullable
    public GcbFieldAccessCppFunction byFunction(Function key) {
      return functions.get(key);
    }

    public Collection<Map.Entry<Function, GcbFieldAccessCppFunction>> functions() {
      return functions.entrySet();
    }

    public Collection<Map.Entry<Format.Field, GcbFieldAccessCppFunction>> fields() {
      return fields.entrySet();
    }
  }

  /**
   * Get a list of functions on which the pass should be applied on.
   */
  protected abstract Stream<Pair<Format.FieldAccess, Function>> getApplicable(Specification viam);

  /**
   * Converts a given {@code function} into a {@link GcbFieldAccessCppFunction} which has
   * cpp conforming types.
   */
  protected abstract GcbFieldAccessCppFunction liftFunction(Format.FieldAccess fieldAccess);

  @Nullable
  @Override
  public Object execute(PassResults passResults, Specification viam)
      throws IOException {
    var results = new NormalisedTypeResult();

    getApplicable(viam).forEach(pair -> {
      var field = pair.left();
      var function = pair.right();
      var cppFunction = liftFunction(field);
      results.add(function, field, cppFunction);
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
   * Changes the function so that all VADL types conform to CPP types
   * which simplifies the code generation.
   * All the non-conforming types will be upcasted to next higher bit size. The name of the
   * {@link GcbFieldAccessCppFunction} is taken from {@code function.identifier}.
   */
  public static GcbFieldAccessCppFunction createGcbFieldAccessCppFunction(
      Function function,
      Format.FieldAccess fieldAccess) {
    var liftedResultTy = getResultTy(function);
    updateGraph(function.behavior());
    var liftedParameters = getParameters(function);

    // We updated the old function's graph, so just take it for the new function.
    return new GcbFieldAccessCppFunction(function.identifier,
        liftedParameters.toArray(Parameter[]::new),
        liftedResultTy,
        function.behavior(),
        fieldAccess);
  }

  /**
   * Changes the function so that all VADL types conform to CPP types
   * which simplifies the code generation.
   * All the non-conforming types will be upcasted to next higher bit size. The name of the
   * {@link GcbImmediateExtractionCppFunction} is taken from {@code function.identifier}.
   */
  public static GcbImmediateExtractionCppFunction createGcbRelocationCppFunction(
      Function function) {
    var liftedResultTy = getResultTy(function);
    updateGraph(function.behavior());
    var liftedParameters = getParameters(function);

    // We updated the old function's graph, so just take it for the new function.
    return new GcbImmediateExtractionCppFunction(function.identifier,
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
                    parameter.simpleName(),
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
    var newBitSizeType = bitsType.withBitWidth(nextFittingBitSize(
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
        .forEach(signExtendNode -> cast(signExtendNode,
            (newType) -> new CppSignExtendNode(signExtendNode.value(), newType,
                signExtendNode.type())));

    graph.getNodes(ZeroExtendNode.class)
        .filter(zeroExtendNode -> !cppSupportedTypes.contains(zeroExtendNode.type()))
        .forEach(zeroExtendNode -> cast(zeroExtendNode,
            (newType) -> new CppZeroExtendNode(zeroExtendNode.value(), newType,
                zeroExtendNode.type())));

    graph.getNodes(TruncateNode.class)
        .filter(truncateNode -> !cppSupportedTypes.contains(truncateNode.type()))
        .forEach(truncateNode -> cast(truncateNode,
            (newType) -> new CppTruncateNode(truncateNode.value(), newType,
                truncateNode.type())));


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

  /**
   * Upcast the given type to the next fitting bit size.
   */
  public static BitsType upcast(Type type) {
    if (type instanceof BitsType cast) {
      return cast.withBitWidth(nextFittingBitSize(cast.bitWidth()));
    } else {
      throw new ViamError("Non bits type are not supported");
    }
  }

  private static int nextFittingBitSize(int old) {
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
