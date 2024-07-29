package vadl.gcb.passes.encoding_generation.strategies.impl;

import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.gcb.passes.encoding_generation.strategies.EncodingGenerationStrategy;
import vadl.types.BitsType;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.viam.Constant;
import vadl.viam.Format;
import vadl.viam.ViamError;
import vadl.viam.graph.GraphVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TypeCastNode;
import vadl.viam.matching.TreeMatcher;
import vadl.viam.matching.impl.AnyNodeMatcher;
import vadl.viam.matching.impl.BuiltInMatcher;
import vadl.viam.matching.impl.FieldRefNodeMatcher;

/**
 * This strategy will create an encoding when the access function only contains add or sub.
 * format Utype : Inst =
 * { imm    : Bits<20>
 * , rd     : Index
 * , opcode : Bits7
 * , immU = ((31) as UInt<20>) - imm
 * }
 * This class should compute the following encoding function automatically:
 * encode {
 * imm => ((31) as UInt<20>) - immU
 * }
 */
public class ArithmeticImmediateStrategy implements EncodingGenerationStrategy {
  @Override
  public boolean checkIfApplicable(Format.FieldAccess fieldAccess) {
    var behavior = fieldAccess.accessFunction().behavior();
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

  @Override
  public void generateEncoding(Format.FieldAccess fieldAccess) {
    var parameter = setupEncodingForFieldAccess(fieldAccess);
    var accessFunction = fieldAccess.accessFunction();
    var copy = accessFunction.behavior().copy();
    final var returnNode = copy.getNodes(ReturnNode.class).findFirst().get();

    // Optimistic assumption: Remove all typecasts because they are not correct anymore when
    // inverted.
    copy.getNodes(TypeCastNode.class)
        .forEach(typeCastNode -> copy.replaceNode(typeCastNode, typeCastNode.value()));

    // First, remove all usages of subtraction.
    // We can replace them by addition with a NegatedNode
    var subtractions = TreeMatcher.matches(copy.getNodes(),
        new BuiltInMatcher(BuiltInTable.SUB, Collections.emptyList()));

    subtractions.forEach(subtraction -> {
      var cast = (BuiltInCall) subtraction;
      cast.setBuiltIn(BuiltInTable.ADD);

      // a - b will be changed to a + (-b)
      var value = (ExpressionNode) cast.inputs().toList().get(1);
      var negation =
          copy.add(new BuiltInCall(BuiltInTable.NEG, new NodeList<>(List.of(value)), value.type()));
      cast.replaceInput(value, negation);
    });

    // After that we need to find the field and add it to the other side.
    var fieldRefs = copy.getNodes(FieldRefNode.class).toList();
    var fieldRef = fieldRefs.get(0);

    var hasFieldSubtractionOnRHS =
        TreeMatcher.matches(copy.getNodes(), new BuiltInMatcher(BuiltInTable.ADD, List.of(
            new AnyNodeMatcher(),
            new BuiltInMatcher(BuiltInTable.NEG, List.of(new FieldRefNodeMatcher()))
        )));

    var fieldRefBits = (BitsType) fieldRef.type();

    // The else branch is not required because the field is positive on the LHS.
    // Only when the field is subtracted on the LHS, we need to rewrite the equation.
    if (hasFieldSubtractionOnRHS.isEmpty()) {
      var funcParam = new FuncParamNode(parameter);
      copy.replaceNode(fieldRef, funcParam);

      // This case is more complicated because the LHS has f(x) - field = XXX
      // If we subtract the f(x) then: - field = XXX is left
      // Which means that we have to invert every operand.

      // We need to invert every operand of a builtin.
      // (1) We can create NegatedNodes and remove NegatedNodes
      // (2) Or we can just change the builtin.
      // I preferred (2) because it is easier.
      returnNode.applyOnInputs(new GraphVisitor.Applier<>() {
        @Nullable
        @Override
        public Node applyNullable(Node from, @Nullable Node to) {
          if (to != null) {
            to.applyOnInputs(this);
          }

          if (to instanceof BuiltInCall) {
            var cast = (BuiltInCall) to;
            if (cast.builtIn() == BuiltInTable.ADD) {
              cast.setBuiltIn(BuiltInTable.SUB);
            } else if (cast.builtIn() == BuiltInTable.SUB) {
              cast.setBuiltIn(BuiltInTable.ADD);
            }
          }

          return to;
        }
      });
    } else {
      var negated =
          new BuiltInCall(BuiltInTable.NEG, new NodeList<>(List.of(new FuncParamNode(
              parameter
          ))), parameter.type());
      copy.replaceNode(fieldRef, negated);


    }

    // At the end of the encoding function, the type must be exactly as the field type
    var sliceNode =
        new SliceNode(returnNode.value, new Constant.BitSlice(new Constant.BitSlice.Part[] {
            new Constant.BitSlice.Part(fieldRefBits.bitWidth() - 1, 0)
        }), (DataType) fieldRef.type());
    var addedSliceNode = copy.add(sliceNode);
    returnNode.replaceInput(returnNode.value, addedSliceNode);

    var encoding = fieldAccess.encoding();
    if (encoding != null) {
      encoding.setBehavior(copy);
    } else {
      throw new ViamError("An encoding must already exist");
    }
  }
}
