package vadl.oop.passes.type_normalization;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.types.BitsType;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.Function;
import vadl.viam.Parameter;
import vadl.viam.ViamError;
import vadl.viam.graph.Graph;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.TypeCastNode;

public class CppTypeNormalizer {
  private static final Logger logger = LoggerFactory.getLogger(CppTypeNormalizer.class);
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
  public Function makeTypesCppConform(Function function) {
    var liftedParameters = getParameters(function);
    var liftedResultTy = getResultTy(function);
    updateGraph(function.behavior());

    // We updated the old function's graph, so just take it for the new function.
    return new Function(function.identifier,
        liftedParameters.toArray(Parameter[]::new),
        liftedResultTy,
        function.behavior());
  }

  private List<Parameter> getParameters(Function function) {
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

  private Type getResultTy(Function function) {
    if (!cppSupportedTypes.contains(function.returnType())) {
      return upcast(function.returnType());
    } else {
      return function.returnType();
    }
  }

  /**
   * This method checks all the typecasts and if upcasts the type if necessary.
   * Additionally, it will also check the constant if the type is ok.
   */
  private void updateGraph(Graph graph) {
    // Updating typecasts
    var typeNodes = graph.getNodes(TypeCastNode.class).toList();
    typeNodes.forEach(typeCastNode -> {
      if (!cppSupportedTypes.contains(typeCastNode.castType()) &&
          typeCastNode.castType() instanceof BitsType bitsType) {
        var newSize = bitsType.withBitWidth(nextFittingType(
            bitsType.bitWidth()));
        var newTypeCastNode =
            new TypeCastNode(typeCastNode.value(), newSize);
        graph.replaceNode(typeCastNode, newTypeCastNode);
      }
    });

    // Updating constants
    var constantNodes = graph.getNodes(ConstantNode.class).toList();
    constantNodes
        .forEach(constantNode -> {
          if (constantNode.constant() instanceof Constant.Value constantValue
              && !cppSupportedTypes.contains(constantValue.type())) {
            constantNode.setConstant(new Constant.Value(
                constantValue.value(), upcast(constantValue.type())
            ));
          }
        });
  }


  private Parameter upcast(Parameter parameter) {
    return new Parameter(parameter.identifier,
        upcast(parameter.type()));
  }

  private BitsType upcast(Type type) {
    if (type instanceof BitsType cast) {
      return cast.withBitWidth(nextFittingType(cast.bitWidth()));
    } else {
      throw new ViamError("Non bits type are not supported");
    }
  }

  private int nextFittingType(int old) {
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
