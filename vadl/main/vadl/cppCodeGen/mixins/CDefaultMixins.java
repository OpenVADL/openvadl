package vadl.cppCodeGen.mixins;

import static vadl.cppCodeGen.CppTypeMap.getCppTypeNameByVadlType;

import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.context.CNodeContext;
import vadl.javaannotations.Handler;
import vadl.types.BuiltInTable;
import vadl.viam.graph.Node;
import vadl.viam.graph.ViamGraphError;
import vadl.viam.graph.control.BeginNode;
import vadl.viam.graph.control.BranchEndNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.MergeNode;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.ScheduledNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.TupleGetFieldNode;
import vadl.viam.graph.dependency.TypeCastNode;
import vadl.viam.graph.dependency.WriteRegNode;
import vadl.viam.graph.dependency.ZeroExtendNode;
import vadl.viam.passes.sideEffectScheduling.nodes.InstrExitNode;

public interface CDefaultMixins {

  interface All extends AllDependencies, AllControl {

  }


  ///  CONTROL HANDLERS ///

  interface AllControl
      extends Scheduled, InstrExit, IfElse, Begin, Start, Merge, BranchEnd, Return, InstrEnd {

  }

  interface Scheduled {
    @Handler
    default void impl(CGenContext<Node> ctx, ScheduledNode node) {
      ctx.gen(node.node()).ln(";");
      ctx.gen(node.next());
    }
  }

  interface InstrExit {
    @Handler
    default void impl(CGenContext<Node> ctx, InstrExitNode node) {
      ctx.ln("return;");
    }
  }


  interface IfElse {
    @Handler
    default void impl(CGenContext<Node> ctx, IfNode node) {
      ctx.wr("if (")
          .gen(node.condition())
          .ln(") { ")
          .gen(node.trueBranch())
          .ln("} else {")
          .gen(node.falseBranch())
          .ln("}");
    }
  }

  interface Begin {
    @Handler
    default void impl(CGenContext<Node> ctx, BeginNode node) {
      ctx.gen(node.next());
    }
  }

  interface Start {
    @Handler
    default void impl(CGenContext<Node> ctx, StartNode node) {
      ctx.gen(node.next());
    }
  }

  interface Merge {
    @Handler
    default void impl(CGenContext<Node> ctx, MergeNode node) {
      ctx.gen(node.next());
    }
  }

  interface BranchEnd {
    @Handler
    default void impl(CGenContext<Node> ctx, BranchEndNode node) {
      // nothing
    }
  }

  interface Return {
    @Handler
    default void impl(CGenContext<Node> ctx, ReturnNode node) {
      ctx.wr("return ").gen(node.value()).ln(";");
    }
  }

  interface InstrEnd {
    @Handler
    default void impl(CGenContext<Node> ctx, InstrEndNode node) {
      // nothing
    }
  }

  ///  DEPENDENCY HANDLERS ///

  interface AllDependencies extends AllExpressions {
  }


  ///  EXPRESSION HANDLERS ///

  interface AllExpressions
      extends TypeCasts, Constant, FuncCall, BuiltIns, Slice, LetNode, Select, FuncParam,
      TupleAccess {

  }

  interface TypeCasts extends SignExtend, ZeroExtend, Truncate {
    @Handler
    default void handle(CNodeContext ctx, TypeCastNode toHandle) {
      throw new ViamGraphError("Type Cast node should not exist at this stage.")
          .addContext(toHandle);
    }
  }

  interface SignExtend {
    @Handler
    default void impl(CGenContext<Node> ctx, SignExtendNode node) {
      var srcType = node.value().type().asDataType();
      ctx.wr("sextract" + ctx.cTypeOf(node.type()) + "(")
          .gen(node.value())
          .wr(", 0, %s)", srcType.bitWidth());
    }
  }

  interface ZeroExtend {
    @Handler
    default void impl(CGenContext<Node> ctx, ZeroExtendNode node) {
      var type = node.type().fittingCppType();
      node.ensure(type != null, "Nodes type cannot fit in a c/c++ type.");
      ctx.wr("extract" + type.bitWidth() + "(");
      ctx.gen(node.value());
      ctx.wr(", 0, " + node.type().bitWidth() + ")");
    }
  }

  interface Truncate {
    @Handler
    default void impl(CGenContext<Node> ctx, TruncateNode node) {
      var type = node.type().fittingCppType();
      node.ensure(type != null, "Nodes type cannot fit in a c/c++ type.");
      ctx.wr("(("
          + getCppTypeNameByVadlType(type)
          + ") (");
      ctx.gen(node.value());
      ctx.wr("))");
    }
  }

  interface Constant {
    @Handler
    default void impl(CGenContext<Node> ctx, ConstantNode constant) {
      var fittingCppType = constant.type().asDataType().fittingCppType();
      constant.ensure(fittingCppType != null, "No fitting cpp type");
      var cppType = getCppTypeNameByVadlType(fittingCppType);
      ctx.wr("((" + cppType + ") " + constant.constant().asVal().decimal() + " )");
    }
  }

  interface Slice {
    @Handler
    default void handle(CNodeContext ctx, SliceNode toHandle) {
      throw new UnsupportedOperationException("Type SliceNode not yet implemented");
    }
  }

  interface Select {
    @Handler
    default void handle(CNodeContext ctx, SelectNode toHandle) {
      throw new UnsupportedOperationException("Type SelectNode not yet implemented");
    }
  }

  interface TupleAccess {
    @Handler
    default void handle(CNodeContext ctx, TupleGetFieldNode toHandle) {
      throw new UnsupportedOperationException("Type TupleGetFieldNode not yet implemented");
    }
  }

  interface LetNode {
    @Handler
    default void handle(CNodeContext ctx, vadl.viam.graph.dependency.LetNode toHandle) {
      ctx.gen(toHandle.expression());
    }
  }

  interface FuncCall {
    @Handler
    default void handle(CGenContext<Node> ctx, FuncCallNode toHandle) {
      ctx.wr(toHandle.function().simpleName())
          .wr("(");
      var first = true;
      for (var arg : toHandle.arguments()) {
        if (!first) {
          ctx.wr(", ");
        }
        ctx.gen(arg);
      }
      ctx.wr(")");
    }
  }

  interface FuncParam {
    @Handler
    default void handle(CNodeContext ctx, FuncParamNode toHandle) {
      ctx.wr(toHandle.parameter().simpleName());
    }
  }


  interface BuiltIns {
    @Handler
    default void handle(CGenContext<Node> ctx, BuiltInCall op) {
      // open scope
      ctx.wr("(");

      var a = op.arguments().get(0);

      if (op.arguments().size() == 2) {
        var b = op.arguments().get(1);
        ctx.gen(a);
        // TODO: Refactor this unreadable stuff
        if (op.builtIn() == BuiltInTable.LSL) {
          ctx.wr(" << ");
        } else if (op.builtIn() == BuiltInTable.ADD) {
          ctx.wr(" + ");
        } else if (op.builtIn() == BuiltInTable.AND) {
          ctx.wr(" & ");
        } else {
          throw new ViamGraphError("built-in to C of %s is not implemented", op.builtIn())
              .addContext(op);
        }
        ctx.gen(b);
      } else {
        throw new ViamGraphError("built-in to C of %s is not supported", op.builtIn())
            .addContext(op);
      }

      // close scope
      ctx.wr(")");
    }
  }

}
