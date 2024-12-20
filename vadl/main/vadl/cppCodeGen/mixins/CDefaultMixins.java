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
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.TupleGetFieldNode;
import vadl.viam.graph.dependency.TypeCastNode;
import vadl.viam.graph.dependency.ZeroExtendNode;
import vadl.viam.passes.sideEffectScheduling.nodes.InstrExitNode;

/**
 * A collection of default mixins for C code generation.
 * Some nodes don't have a default implementation as they don't have a basic C representation.
 * All generators must implement handlers for them or implement them via the {@link CInvalidMixins}
 * mixins.
 */
public interface CDefaultMixins {

  @SuppressWarnings("MissingJavadocType")
  interface All extends AllDependencies, AllControl {

  }


  ///  CONTROL HANDLERS ///

  @SuppressWarnings("MissingJavadocType")
  interface AllControl
      extends Scheduled, InstrExit, IfElse, Begin, Start, Merge, BranchEnd, Return, InstrEnd {

  }

  @SuppressWarnings("MissingJavadocType")
  interface Scheduled {
    @Handler
    default void impl(CGenContext<Node> ctx, ScheduledNode node) {
      ctx.gen(node.node()).ln(";");
      ctx.gen(node.next());
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface InstrExit {
    @Handler
    default void impl(CGenContext<Node> ctx, InstrExitNode node) {
      ctx.ln("return;");
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface IfElse {

    @Handler
    @SuppressWarnings("MissingJavadocMethod")
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

  @SuppressWarnings("MissingJavadocType")
  interface Begin {
    @Handler
    default void impl(CGenContext<Node> ctx, BeginNode node) {
      ctx.gen(node.next());
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface Start {
    @Handler
    default void impl(CGenContext<Node> ctx, StartNode node) {
      ctx.gen(node.next());
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface Merge {
    @Handler
    default void impl(CGenContext<Node> ctx, MergeNode node) {
      ctx.gen(node.next());
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface BranchEnd {
    @Handler
    default void impl(CGenContext<Node> ctx, BranchEndNode node) {
      // nothing
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface Return {
    @Handler
    default void impl(CGenContext<Node> ctx, ReturnNode node) {
      ctx.wr("return ").gen(node.value()).ln(";");
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface InstrEnd {
    @Handler
    default void impl(CGenContext<Node> ctx, InstrEndNode node) {
      // nothing
    }
  }

  ///  DEPENDENCY HANDLERS ///

  @SuppressWarnings("MissingJavadocType")
  interface AllDependencies extends AllExpressions {
  }


  ///  EXPRESSION HANDLERS ///

  @SuppressWarnings("MissingJavadocType")
  interface AllExpressions
      extends TypeCasts, Constant, FuncCall, BuiltIns, Slice, LetNode, Select, FuncParam,
      TupleAccess {

  }

  @SuppressWarnings("MissingJavadocType")
  interface TypeCasts extends SignExtend, ZeroExtend, Truncate {
    @Handler
    @SuppressWarnings("MissingJavadocMethod")
    default void handle(CNodeContext ctx, TypeCastNode toHandle) {
      throw new ViamGraphError("Type Cast node should not exist at this stage.")
          .addContext(toHandle);
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface SignExtend {
    @Handler
    @SuppressWarnings("MissingJavadocMethod")
    default void impl(CGenContext<Node> ctx, SignExtendNode node) {
      var srcType = node.value().type().asDataType();
      ctx.wr("sextract" + ctx.cTypeOf(node.type()) + "(")
          .gen(node.value())
          .wr(", 0, %s)", srcType.bitWidth());
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface ZeroExtend {
    @Handler
    @SuppressWarnings("MissingJavadocMethod")
    default void impl(CGenContext<Node> ctx, ZeroExtendNode node) {
      var type = node.type().fittingCppType();
      node.ensure(type != null, "Nodes type cannot fit in a c/c++ type.");
      ctx.wr("extract" + type.bitWidth() + "(");
      ctx.gen(node.value());
      ctx.wr(", 0, " + node.type().bitWidth() + ")");
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface Truncate {
    @Handler
    @SuppressWarnings("MissingJavadocMethod")
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

  @SuppressWarnings("MissingJavadocType")
  interface Constant {
    @Handler
    @SuppressWarnings("MissingJavadocMethod")
    default void impl(CGenContext<Node> ctx, ConstantNode constant) {
      var fittingCppType = constant.type().asDataType().fittingCppType();
      constant.ensure(fittingCppType != null, "No fitting cpp type");
      var cppType = getCppTypeNameByVadlType(fittingCppType);
      ctx.wr("((" + cppType + ") " + constant.constant().asVal().decimal() + " )");
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface Slice {
    @Handler
    default void handle(CNodeContext ctx, SliceNode toHandle) {
      throw new UnsupportedOperationException("Type SliceNode not yet implemented");
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface Select {
    @Handler
    default void handle(CNodeContext ctx, SelectNode toHandle) {
      throw new UnsupportedOperationException("Type SelectNode not yet implemented");
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface TupleAccess {
    @Handler
    default void handle(CNodeContext ctx, TupleGetFieldNode toHandle) {
      throw new UnsupportedOperationException("Type TupleGetFieldNode not yet implemented");
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface LetNode {
    @Handler
    default void handle(CNodeContext ctx, vadl.viam.graph.dependency.LetNode toHandle) {
      ctx.gen(toHandle.expression());
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface FuncCall {
    @Handler
    @SuppressWarnings("MissingJavadocMethod")
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

  @SuppressWarnings("MissingJavadocType")
  interface FuncParam {
    @Handler
    default void handle(CNodeContext ctx, FuncParamNode toHandle) {
      ctx.wr(toHandle.parameter().simpleName());
    }
  }


  @SuppressWarnings("MissingJavadocType")
  interface BuiltIns {
    @Handler
    @SuppressWarnings("MissingJavadocMethod")
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
