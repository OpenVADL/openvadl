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
        .forEach(typeCastNode -> typeCastNode.replaceAndDelete(typeCastNode.value()));

    // After that we need to find the field and add it to the other side.
    var fieldRefs = copy.getNodes(FieldRefNode.class).toList();
    var fieldRef = fieldRefs.get(0);
    var fieldRefBits = (BitsType) fieldRef.type();

    // Example
    // f(x) = x + 6
    // Let y = f(x)
    // y = x + 6
    // y - 6 = x
    // and
    // f(x) = x - 6
    // Let y = f(x)
    // y = x - 6
    // y + 6 = x
    // The heuristic just swaps the operators.

    var funcParam = new FuncParamNode(parameter);
    fieldRef.replaceAndDelete(funcParam);

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

    // At the end of the encoding function, the type must be exactly as the field type
    var sliceNode =
        new SliceNode(returnNode.value(), new Constant.BitSlice(new Constant.BitSlice.Part[] {
            new Constant.BitSlice.Part(fieldRefBits.bitWidth() - 1, 0)
        }), (DataType) fieldRef.type());
    var addedSliceNode = copy.add(sliceNode);
    returnNode.replaceInput(returnNode.value(), addedSliceNode);

    var encoding = fieldAccess.encoding();
    if (encoding != null) {
      encoding.setBehavior(copy);
    } else {
      throw new ViamError("An encoding must already exist");
    }
  }
}
