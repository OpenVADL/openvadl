// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.cppCodeGen.mixins;

import static java.util.Objects.requireNonNull;
import static vadl.cppCodeGen.CppTypeMap.getCppTypeNameByVadlType;
import static vadl.utils.GraphUtils.getSingleNode;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import vadl.cppCodeGen.CppTypeMap;
import vadl.cppCodeGen.context.CGenContext;
import vadl.cppCodeGen.context.CNodeContext;
import vadl.javaannotations.Handler;
import vadl.types.DataType;
import vadl.utils.Pair;
import vadl.viam.Function;
import vadl.viam.Parameter;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.BeginNode;
import vadl.viam.graph.control.BranchEndNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.MergeNode;
import vadl.viam.graph.control.NewLabelNode;
import vadl.viam.graph.control.ProcEndNode;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.ScheduledNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.LabelNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.TupleGetFieldNode;
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

  @SuppressWarnings("MissingJavadocType")
  interface Utils {
    /**
     * Get the context.
     */
    CNodeContext context();

    /**
     * Get the function. It is used to get the function for the default implementations.
     */
    Function function();

    /**
     * Get the string builder.
     */
    StringBuilder builder();

    /**
     * Generate the name for the function.
     */
    default String genFunctionName() {
      return function().simpleName();
    }

    /**
     * Generates and returns the C++ function signature for the function. Does not modify the
     * internal state of the code generator.
     *
     * @return the generated C++ function signature
     */
    default String genFunctionSignature() {
      var function = function();
      var returnType = function.returnType().asDataType().fittingCppType();

      function.ensure(returnType != null, "No fitting Cpp type found for return type %s",
          returnType);
      function.ensure(function.behavior().isPureFunction(), "Function is not pure.");

      return CppTypeMap.getCppTypeNameByVadlType(returnType)
          + " %s(%s)".formatted(genFunctionName(), genFunctionParameters(function.parameters()));
    }

    /**
     * Generate the parameters for the given {@code parameters}. It will change the type
     * if its type is not a valid CPP type.
     *
     * @return a comma seperated string with the parameter types and names.
     */
    default String genFunctionParameters(Parameter[] parameters) {
      var cppArgs = Stream.of(parameters)
          .map(p -> Pair.of(p.simpleName(), requireNonNull(p.type().asDataType().fittingCppType())))
          .toList();

      return cppArgs.stream().map(
          s -> CppTypeMap.getCppTypeNameByVadlType(s.right())
              + " " + s.left()
      ).collect(Collectors.joining(", "));
    }

    /**
     * Generates and returns the C++ code for the function, including its signature and body.
     *
     * @return the generated C++ function code
     */
    default String genFunctionDefinition() {
      var returnNode = getSingleNode(function().behavior(), ReturnNode.class);
      context().wr(genFunctionSignature())
          .wr(" {\n")
          .wr("   return ")
          .gen(returnNode.value())
          .wr(";\n}");
      return builder().toString();
    }
  }


  ///  CONTROL HANDLERS ///

  @SuppressWarnings("MissingJavadocType")
  interface AllControl
      extends Scheduled, InstrExit, IfElse, Begin, Start, Merge, BranchEnd, Return, InstrEnd,
      ProcEnd, NewLabel {

  }

  @SuppressWarnings("MissingJavadocType")
  interface Scheduled {
    @Handler
    default void handle(CGenContext<Node> ctx, ScheduledNode node) {
      ctx.gen(node.node()).ln(";");
      ctx.gen(node.next());
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface InstrExit {
    @Handler
    default void handle(CGenContext<Node> ctx, InstrExitNode node) {
      ctx.ln("return;");
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface IfElse {

    @Handler
    @SuppressWarnings("MissingJavadocMethod")
    default void handle(CGenContext<Node> ctx, IfNode node) {
      ctx.wr("if (")
          .gen(node.condition())
          .ln(") { ").spacedIn()
          .gen(node.trueBranch()).spaceOut()
          .ln("} else {").spacedIn()
          .gen(node.falseBranch()).spaceOut()
          .ln("}");
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface Begin {
    @Handler
    default void handle(CGenContext<Node> ctx, BeginNode node) {
      ctx.gen(node.next());
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface Start {
    @Handler
    default void handle(CGenContext<Node> ctx, StartNode node) {
      ctx.gen(node.next());
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface Merge {
    @Handler
    default void handle(CGenContext<Node> ctx, MergeNode node) {
      ctx.gen(node.next());
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface BranchEnd {
    @Handler
    default void handle(CGenContext<Node> ctx, BranchEndNode node) {
      // nothing
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface Return {
    @Handler
    default void handle(CGenContext<Node> ctx, ReturnNode node) {
      ctx.wr("return ").gen(node.value()).ln(";");
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface InstrEnd {
    @Handler
    default void handle(CGenContext<Node> ctx, InstrEndNode node) {
      // nothing
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface ProcEnd {
    @Handler
    default void handle(CGenContext<Node> ctx, ProcEndNode node) {
      // nothing
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface NewLabel {
    @Handler
    default void handle(CGenContext<Node> ctx, NewLabelNode node) {
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
      TupleAccess, Label {

  }

  @SuppressWarnings("MissingJavadocType")
  interface TypeCasts extends SignExtend, ZeroExtend, Truncate {

  }

  @SuppressWarnings("MissingJavadocType")
  interface SignExtend {
    @Handler
    @SuppressWarnings("MissingJavadocMethod")
    default void handle(CGenContext<Node> ctx, SignExtendNode node) {
      // Use the built-in VADL library functions.
      // Generators must include the vadl-builtins.h header file.
      var srcType = node.value().type().asDataType();
      ctx.wr("VADL_sextract(")
          .gen(node.value())
          .wr(", %s)", srcType.bitWidth());
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface ZeroExtend {
    @Handler
    @SuppressWarnings("MissingJavadocMethod")
    default void handle(CGenContext<Node> ctx, ZeroExtendNode node) {
      // Use the built-in VADL library functions.
      // Generators must include the vadl-builtins.h header file.
      var srcType = node.value().type().asDataType();
      ctx.wr("VADL_uextract(")
          .gen(node.value())
          .wr(", %s)", srcType.bitWidth());
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface Truncate {
    @Handler
    @SuppressWarnings("MissingJavadocMethod")
    default void handle(CGenContext<Node> ctx, TruncateNode node) {
      // Use the built-in VADL library functions.
      // Generators must include the vadl-builtins.h header file.

      // Datatype must not be boolean because when it should be already replaced by a
      // check.
      node.ensure(node.type() != DataType.bool(),
          "Truncation to boolean is not allowed");
      var bitWidth = node.type().bitWidth();
      ctx.wr("VADL_uextract(")
          .gen(node.value())
          .wr(", %s)", bitWidth);
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface Constant {
    @Handler
    @SuppressWarnings("MissingJavadocMethod")
    default void handle(CGenContext<Node> ctx, ConstantNode constant) {
      var fittingCppType = constant.type().asDataType().fittingCppType();
      constant.ensure(fittingCppType != null, "No fitting cpp type");
      var cppType = getCppTypeNameByVadlType(fittingCppType);
      ctx.wr("((" + cppType + ") " + constant.constant().asVal().asString("0x", 16, false) + " )");
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface Slice {
    @Handler
    default void handle(CGenContext<Node> ctx, SliceNode node) {
      ctx.wr("VADL_slice(")
          .gen(node.value())
          .wr(", %s", node.bitSlice().partSize());
      node.bitSlice().parts().forEach(p -> {
        ctx.wr(", " + p.msb() + ", " + p.lsb());
      });
      ctx.wr(")");
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface Select {

    @Handler
    @SuppressWarnings("MissingJavadocMethodCheck")
    default void handle(CGenContext<Node> ctx, SelectNode toHandle) {
      ctx.wr("(")
          .gen(toHandle.condition())
          .wr(" ? ")
          .gen(toHandle.trueCase())
          .wr(": ")
          .gen(toHandle.falseCase())
          .wr(")");
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface TupleAccess {
    @Handler
    default void handle(CGenContext<Node> ctx, TupleGetFieldNode toHandle) {
      throw new UnsupportedOperationException("Type TupleGetFieldNode not yet implemented");
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface LetNode {
    @Handler
    default void handle(CGenContext<Node> ctx, vadl.viam.graph.dependency.LetNode toHandle) {
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
    default void handle(CGenContext<Node> ctx, FuncParamNode toHandle) {
      ctx.wr(toHandle.parameter().simpleName());
    }
  }


  @SuppressWarnings("MissingJavadocType")
  interface BuiltIns {
    @Handler
    @SuppressWarnings("MissingJavadocMethod")
    default void handle(CGenContext<Node> ctx, BuiltInCall op) {
      // use the built-in VADL library functions.
      // generators must include the vadl-builtins.h header file.

      var name = op.builtIn().name().replace("::", "_");
      ctx.wr(name)
          .wr("(");

      var first = true;
      for (var arg : op.arguments()) {
        if (!first) {
          ctx.wr(", ");
        }

        var argWidth = arg.type()
            .asDataType().bitWidth();

        ctx.gen(arg)
            .wr(", " + argWidth);

        first = false;
      }

      ctx.wr(")");
    }
  }

  @SuppressWarnings("MissingJavadocType")
  interface Label {
    @Handler
    @SuppressWarnings("MissingJavadocMethod")
    default void handle(CGenContext<Node> ctx, LabelNode label) {
      // nothing
    }
  }
}
