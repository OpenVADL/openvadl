package vadl.gcb.passes;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import org.jetbrains.annotations.Nullable;
import vadl.javaannotations.viam.Input;
import vadl.pass.Pass;
import vadl.pass.PassKey;
import vadl.pass.PassName;
import vadl.types.BuiltInTable;
import vadl.types.BuiltInTable.BuiltIn;
import vadl.types.DataType;
import vadl.types.UIntType;
import vadl.viam.Constant;
import vadl.viam.Encoding;
import vadl.viam.Format;
import vadl.viam.Format.Field;
import vadl.viam.Format.FieldAccess;
import vadl.viam.Function;
import vadl.viam.Instruction;
import vadl.viam.Parameter;
import vadl.viam.Specification;
import vadl.viam.ViamError;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.AbstractFunctionCallNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.UnaryNode;
import vadl.viam.matching.TreeMatcher;
import vadl.viam.matching.impl.BuiltInMatcher;

/**
 * This pass generate the behavior {@link Graph} of {@link FieldAccess} when the {@link Encoding}
 * is {@code null}.
 */
public class GenerateFieldAccessEncodingFunctionPass extends Pass {
  @Override
  public PassName getName() {
    return new PassName("GenerateFieldAccessEncodingFunctionPass");
  }

  /**
   * Represents an equation operator.
   * a = b + c
   */
  class EquationNode extends AbstractFunctionCallNode {

    public EquationNode(NodeList<ExpressionNode> args) {
      super(args, Type.dummy());
    }

    @Override
    public Node copy() {
      return new EquationNode(
          new NodeList<>(this.arguments().stream().map(x -> (ExpressionNode) x.copy()).toList()));
    }

    @Override
    public Node shallowCopy() {
      return new EquationNode(args);
    }
  }

  /**
   * Represents a negative value in the graph. This helps us to remove subtraction in the graph.
   */
  public class NegatedNode extends UnaryNode {

    public NegatedNode(ExpressionNode value, Type type) {
      super(value, type);
    }

    @Override
    public Node copy() {
      return new NegatedNode((ExpressionNode) value.copy(), type());
    }

    @Override
    public Node shallowCopy() {
      return new NegatedNode(value, type());
    }
  }

  @Nullable
  @Override
  public Object execute(Map<PassKey, Object> passResults, Specification viam) {
    viam.isas()
        .flatMap(x -> x.instructions().stream())
        .map(Instruction::format)
        .distinct()
        .flatMap(x -> Arrays.stream(x.fieldAccesses()))
        .filter(x -> x.encoding() == null)
        .forEach(fieldAccess -> {
          var behavior = fieldAccess.accessFunction().behavior();
          var ident = fieldAccess.identifier.append("encoding");
          var identParam = ident.append(fieldAccess.name());
          var param = new Parameter(identParam, fieldAccess.accessFunction().returnType());
          var function =
              new Function(ident, new Parameter[] {param}, fieldAccess.fieldRef().type());

          fieldAccess.setEncoding(function);

          // We need to compute multiple encoding functions based on the field access function.
          // Different field access functions require different heuristics for the encoding.
          if (trivialBehavior(behavior)) {
            // format Utype : Inst =
            // {     imm    : Bits<20>
            //     , rd     : Index
            //     , opcode : Bits7
            //     , immU = imm as UInt<32>
            // }
            //
            // This branch should compute the following encoding function automatically:
            //
            // encode {
            //  imm => immU(19..0)
            // }
            generateTrivialEncodingFunction(param, fieldAccess.fieldRef(), Objects.requireNonNull(
                fieldAccess.encoding()).behavior());
          } else if (hasOnlyShifts(behavior)) {
            // format Utype : Inst =
            // {     imm    : Bits<20>
            //     , rd     : Index
            //     , opcode : Bits7
            //     , ImmediateU = ( imm, 0 as Bits<12> ) as UInt
            // }
            //
            // This branch should compute the following encoding function automatically:
            //
            // encode {
            //  imm => ImmediateU(31..12)
            // }
            generateShiftEncodingFunction(param, fieldAccess.fieldRef(), Objects.requireNonNull(
                fieldAccess.encoding()).behavior(), fieldAccess.accessFunction());
          } else if (hasOnlyAddOrSub(behavior)) {
            // format Utype : Inst =
            //      { imm    : Bits<20>
            //      , rd     : Index
            //    , opcode : Bits7
            //    , immU = ((31) as UInt<20>) - imm
            // }
            //
            // This branch should compute the following encoding function automatically:
            //
            // encode {
            //  imm => ((31) as UInt<20>) - immU
            // }

            var encodingGraph = generateAddOrSubEncodingFunction(param,
                fieldAccess.accessFunction()
            );
            Objects.requireNonNull(fieldAccess.encoding()).setBehavior(encodingGraph);
          }
        });

    var haveEncoding = viam.isas()
        .flatMap(x -> x.formats().stream())
        .distinct()
        .flatMap(x -> Arrays.stream(x.fieldAccesses()))
        .allMatch(x -> x.encoding() != null);

    if (!haveEncoding) {
      throw new ViamError("Not all formats have an encoding");
    }


    return null;
  }

  /**
   * Generate a simple encoding for {@code fieldAccess} and adds nodes to
   * {@code behavior} graph.
   *
   * @param parameter is the {@link Parameter} which is the input for the function so
   *                  it can be encoded as a field.
   * @param fieldRef  is the {@link Field} which should be encoded.
   * @param behavior  is the graph which contains new the encoding logic.
   */
  private void generateTrivialEncodingFunction(Parameter parameter,
                                               Field fieldRef,
                                               Graph behavior) {
    // The field takes up a certain slice.
    // But we need to take a slice of the immediate of the same size.
    var fieldAccessBitSlice = fieldRef.bitSlice();
    var invertedSlice = new Constant.BitSlice(new Constant.BitSlice.Part[] {
        Constant.BitSlice.Part.of(fieldAccessBitSlice.bitSize() - 1, 0)});
    var invertedSliceNode = new SliceNode(new FuncParamNode(
        parameter),
        invertedSlice,
        fieldRef.type());
    var returnNode = new ReturnNode(invertedSliceNode);
    var startNode = new StartNode(returnNode);

    behavior.addWithInputs(returnNode);
    behavior.add(startNode);
  }


  /**
   * Generate an encoding for {@code fieldAccess} and adds nodes to
   * {@code behavior} graph.
   *
   * @param parameter      is the {@link Parameter} which is the input for the function so
   *                       it can be encoded as a field.
   * @param fieldRef       is the {@link Field} which should be encoded.
   * @param behavior       is the graph which contains new the encoding logic.
   * @param accessFunction the access function for the encoding.
   */
  private void generateShiftEncodingFunction(Parameter parameter,
                                             Field fieldRef,
                                             Graph behavior,
                                             Function accessFunction) {
    var originalShift =
        (BuiltInCall) accessFunction.behavior().getNodes(BuiltInCall.class).findFirst().get();
    var shiftValue =
        ((Constant.Value) ((ConstantNode) originalShift.arguments()
            .get(1)).constant()).value();

    ExpressionNode invertedSliceNode;
    if (originalShift.builtIn() == BuiltInTable.LSL) {
      // If the decode function has a left shift,
      // then we need to extract the original shifted value.
      // We compute an upper bound which is the shift value plus the size of the field
      // and a lower bound which is the shifted value.
      var upperBound = shiftValue.intValue() + fieldRef.size() - 1;
      var lowerBound = shiftValue.intValue();
      var slice = new Constant.BitSlice(
          new Constant.BitSlice.Part[] {
              Constant.BitSlice.Part.of(upperBound, lowerBound)});
      invertedSliceNode = new SliceNode(new FuncParamNode(parameter), slice, fieldRef.type());
    } else if (originalShift.builtIn() == BuiltInTable.LSR
        || originalShift.builtIn() == BuiltInTable.ASR) {
      throw new ViamError("Not implemented now");
    } else {
      throw new ViamError("Inverting builtin is not supported");
    }

    var returnNode = new ReturnNode(invertedSliceNode);
    var startNode = new StartNode(returnNode);

    behavior.addWithInputs(returnNode);
    behavior.add(startNode);
  }

  /**
   * Generate an encoding for {@code fieldAccess} and adds nodes to
   * {@code behavior} graph.
   *
   * @param parameter      is the {@link Parameter} which is the input for the function so
   *                       it can be encoded as a field.
   * @param accessFunction of the {@link Format}.
   */
  private Graph generateAddOrSubEncodingFunction(Parameter parameter,
                                                 Function accessFunction) {
    // We know that the access function only contains add or sub.
    // Imagine the following function f(x):
    // f(x) = 31 - x
    // Let y = f(x)
    // y = 31 - x
    // then x = 31 - y
    //
    // and
    //
    // f(x) = 31 + x
    // Let y = f(x)
    // y = 31 + x
    // then x = -31 + y

    // So, let's replace the field ref by a function param
    // and revert all operations and invert every constant.
    var copy = accessFunction.behavior().copy();
    var returnNode = copy.getNodes(ReturnNode.class).findFirst().get();

    // First, remove all usages of subtraction.
    // We can replace them by addition with a NegatedNode
    var subtractions = TreeMatcher.matches(copy.getNodes(),
        new BuiltInMatcher(BuiltInTable.SUB, Collections.emptyList()));

    subtractions.forEach(subtraction -> {
      var cast = (BuiltInCall) subtraction;
      cast.setBuiltIn(BuiltInTable.ADD);

      // a - b will be changed to a + (-b)
      var value = (ExpressionNode) cast.inputs().toList().get(1);
      var negation = copy.add(new NegatedNode(value, value.type()));
      cast.replaceInput(value, negation);
    });

    var equation = new EquationNode(new NodeList<>(
        // left
        copy.add(new ConstantNode(new Constant.Value(BigInteger.ZERO, (DataType) returnNode.returnType()))),
        // right
        returnNode.value
    ));
    var addedEquationNode = copy.add(equation);
    returnNode.replaceInput(returnNode.value, addedEquationNode);


    /*
    ensure(copy.getNodes(FieldRefNode.class).count() == 1,
        "Only one field reference is allowed");
    var fieldRefNode = copy.getNodes(FieldRefNode.class).findFirst().get();

    // We have to check how the field is used.
    // If the parent is a subtraction then we need to subtract the function parameter.
    // If the parent is an addition then we need to invert every operation and add
    // the function parameter.
    var isSub = fieldRefNode.usages()
        .anyMatch(x -> x instanceof BuiltInCall && ((BuiltInCall) x).builtIn() == BuiltInTable.SUB);

    if (isSub) {
      copy.replaceNode(fieldRefNode, new FuncParamNode(parameter));
    } else {
      returnNode.applyOnInputs(new GraphVisitor.Applier<>() {
        @Nullable
        @Override
        public Node applyNullable(Node from, @Nullable Node to) {
          if (to != null) {
            to.applyOnInputs(this);

            // Revert operations
            if (to instanceof BuiltInCall) {
              var cast = (BuiltInCall) to;
              if (cast.builtIn() == BuiltInTable.ADD) {
                cast.setBuiltIn(BuiltInTable.SUB);
              } else if (cast.builtIn() == BuiltInTable.SUB) {
                cast.setBuiltIn(BuiltInTable.ADD);
              }
            } else if (to instanceof ConstantNode) {
              var cast = (ConstantNode) to;
              if (cast.constant() instanceof Constant.Value) {
                var negated = ((Constant.Value) cast.constant()).value().negate();
                // If the type was unsigned then it has to be signed now.
                var ty = cast.type() instanceof UIntType ? ((UIntType) cast.type()).makeSigned() :
                    cast.type();

                cast.setConstant(new Constant.Value(negated, (DataType) ty));
                ((ConstantNode) to).setType(ty);
              }
            }
          }

          return to;
        }
      });

      // We need to invert the function parameter because we have inverted the operation with
      // the visitor.
      copy.replaceNode(fieldRefNode, new BuiltInCall(BuiltInTable.SUB, new NodeList<>(
          new ConstantNode(Constant.Value.of(0, (DataType) parameter.type())),
          new FuncParamNode(parameter)
      ), parameter.type()));
    }
     */

    return copy;
  }

  /**
   * Checks whether the behavior only contains (logical or arithmetic) left or right shift.
   * But only one logical operation is allowed.
   */
  private boolean hasOnlyShifts(Graph behavior) {
    return behavior.getNodes(BuiltInCall.class)
        .allMatch(x -> {
          var cast = (BuiltInCall) x;

          if (cast.builtIn() == BuiltInTable.LSL
              || cast.builtIn() == BuiltInTable.LSR
              || cast.builtIn() == BuiltInTable.ASR) {
            return true;
          }

          return false;
        }) && behavior.getNodes(BuiltInCall.class).count() == 1;
  }


  /**
   * Checks whether the behavior only contains addition or subtraction.
   */
  private boolean hasOnlyAddOrSub(Graph behavior) {
    return behavior.getNodes(BuiltInCall.class)
        .allMatch(x -> {
          var cast = (BuiltInCall) x;

          if (cast.builtIn() == BuiltInTable.ADD
              || cast.builtIn() == BuiltInTable.SUB) {
            return true;
          }

          return false;
        }) && behavior.getNodes(SliceNode.class).findAny().isEmpty();
  }

  /**
   * Checks whether the behavior does not contain any {@link BuiltIn} or {@link SliceNode}.
   */
  private boolean trivialBehavior(Graph behavior) {
    return behavior.getNodes()
        .filter(x -> x instanceof BuiltInCall || x instanceof SliceNode)
        .findAny().isEmpty();
  }
}
