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

package vadl.ast;


import static vadl.error.Diagnostic.error;
import static vadl.utils.GraphUtils.ifElseSideEffect;
import static vadl.utils.GraphUtils.intU;
import static vadl.utils.GraphUtils.neq;
import static vadl.utils.GraphUtils.or;
import static vadl.utils.GraphUtils.select;

import com.google.common.collect.Lists;
import com.google.errorprone.annotations.concurrent.LazyInit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import vadl.types.BitsType;
import vadl.types.BuiltInTable;
import vadl.types.ConcreteRelationType;
import vadl.types.DataType;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.types.UIntType;
import vadl.utils.Either;
import vadl.utils.Pair;
import vadl.utils.WithLocation;
import vadl.viam.ArtificialResource;
import vadl.viam.Constant;
import vadl.viam.Counter;
import vadl.viam.Definition;
import vadl.viam.ExceptionDef;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Instruction;
import vadl.viam.Memory;
import vadl.viam.Procedure;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.Graph;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.BeginNode;
import vadl.viam.graph.control.BranchEndNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.MergeNode;
import vadl.viam.graph.control.ProcEndNode;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.AsmBuiltInCall;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldAccessRefNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.FuncCallNode;
import vadl.viam.graph.dependency.FuncParamNode;
import vadl.viam.graph.dependency.LetNode;
import vadl.viam.graph.dependency.ProcCallNode;
import vadl.viam.graph.dependency.ReadArtificialResNode;
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.ReadResourceNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.TupleGetFieldNode;
import vadl.viam.graph.dependency.WriteArtificialResNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;
import vadl.viam.graph.dependency.WriteResourceNode;
import vadl.viam.graph.dependency.ZeroExtendNode;


/**
 * Lowers statements and expressions into viam behaivor graph.
 *
 * <p>Because the caches this class holds are delicate, create a new instance for every graph you
 * generate.
 */
@SuppressWarnings("OverloadMethodsDeclarationOrder")
class BehaviorLowering implements StatementVisitor<SubgraphContext>, ExprVisitor<ExpressionNode> {
  private final ViamLowering viamLowering;
  private final ConstantEvaluator constantEvaluator = new ConstantEvaluator();

  private final IdentityHashMap<Expr, ExpressionNode> expressionCache = new IdentityHashMap<>();
  //private IdentityHashMap<Statement, SubgraphContext> statementCache = new IdentityHashMap<>();

  @LazyInit
  private Graph currentGraph;

  BehaviorLowering(ViamLowering generator) {
    this.viamLowering = generator;
  }

  Graph getFunctionGraph(Expr expr, String name) {
    var exprNode = fetch(expr);

    var graph = new Graph(name);
    graph.setSourceLocation(expr.location());
    currentGraph = graph;

    ControlNode endNode = graph.addWithInputs(new ReturnNode(exprNode));
    endNode.setSourceLocation(expr.location());
    ControlNode startNode = graph.add(new StartNode(endNode));
    startNode.setSourceLocation(expr.location());
    return graph;
  }

  Graph getProcedureGraph(Statement stmt, String name) {
    var graph = new Graph(name);
    graph.setSourceLocation(stmt.location());
    currentGraph = graph;

    var stmtCtx = stmt.accept(this);
    var sideEffects = stmtCtx.sideEffectsOrEmptyList();

    var end = graph.addWithInputs(new ProcEndNode(sideEffects));
    end.setSourceLocation(stmt.location());

    ControlNode startSuccessor = end;
    if (stmtCtx.hasControlBlock()) {
      var controlBlock = Objects.requireNonNull(stmtCtx.controlBlock());
      controlBlock.lastNode().setNext(end);
      startSuccessor = controlBlock.firstNode();
    }
    var start = new StartNode(startSuccessor);
    start.setSourceLocation(stmt.location());
    graph.addWithInputs(start);

    return graph;
  }

  Graph getInstructionGraph(InstructionDefinition definition) {
    var graph = new Graph("%s Behavior".formatted(definition.identifier().name));
    graph.setSourceLocation(definition.location());
    currentGraph = graph;

    var stmtCtx = definition.behavior.accept(this);
    var sideEffects = stmtCtx.sideEffectsOrEmptyList();

    var end = graph.addWithInputs(new InstrEndNode(sideEffects));
    end.setSourceLocation(definition.location());

    ControlNode startSuccessor = end;
    if (stmtCtx.hasControlBlock()) {
      var controlBlock = Objects.requireNonNull(stmtCtx.controlBlock());
      controlBlock.lastNode().setNext(end);
      startSuccessor = controlBlock.firstNode();
    }
    var start = new StartNode(startSuccessor);
    start.setSourceLocation(definition.location());
    graph.addWithInputs(start);

    return graph;
  }

  Graph getInstructionSequenceGraph(Identifier identifier,
                                    InstructionSequenceDefinition definition) {
    var graph = new Graph("%s Behavior".formatted(identifier.name));
    graph.setSourceLocation(definition.location());
    currentGraph = graph;

    var end = graph.addWithInputs(new InstrEndNode(new NodeList<>()));
    end.setSourceLocation(definition.location());

    var calls = definition.statements.stream()
        .map(s -> (InstrCallNode) Objects.requireNonNull(s.accept(this).controlBlock()).firstNode())
        .toList();

    ControlNode curr = end;
    for (int i = calls.size() - 1; i >= 0; i--) {
      var call = calls.get(i);
      call.setNext(curr);
      curr = call;
    }

    var start = new StartNode(curr);
    start.setSourceLocation(definition.location());
    graph.addWithInputs(start);

    return graph;
  }


  private static Type getViamType(Type astType) {
    return ViamLowering.getViamType(astType);
  }

  /**
   * Produces a boolean expression that returns whether the given constraint values
   * are different from the given indices expressions.
   * It is assumed that {@code constraints.size() <= indices.size()}.
   *
   * <p>Consider the following VADL spec <pre>{@code
   *  register A: Bits<4><5><32>
   *  [ zero : Y(1)(2) ]
   *  alias register Y = A
   * }</pre>
   * The check for access of {@code Y(a)(b)} looks like
   * <pre>{@code
   *  (a != 1) || (b != 2)
   * }</pre>
   */
  private ExpressionNode buildConstraintDontMatchCheck(List<ExpressionNode> indices,
                                                       List<ConstantValue> constraints) {
    var constChecks = new ExpressionNode[constraints.size()];
    for (int i = 0; i < constraints.size(); i++) {
      // compare each constraint index value with the given one
      var idxExpr = indices.get(i);
      var idxConst = constraints.get(i).toViamConstant()
          .zeroExtend(idxExpr.type().asDataType())  // cast constant to same type as expr
          .toNode();
      // check if constraint and expression are different
      constChecks[i] = neq(idxExpr, idxConst);
    }
    return constChecks.length == 1
        ? constChecks[0]
        // build conjunction of all comparisons
        : or(constChecks);
  }

  Function getRegisterAliasReadFunc(AliasDefinition definition) {
    var graph = new Graph("%s Read Behavior".formatted(definition.viamId));
    graph.setSourceLocation(definition.location());
    currentGraph = graph;

    final var identifier =
        viamLowering.generateIdentifier(definition.viamId + "::read", definition.loc);
    final var regFileDef = (RegisterDefinition) Objects.requireNonNull(definition.computedTarget);
    final var zeroConst = definition.getAnnotation("zero", ZeroConstraintAnnotation.class);

    DataType resultType;
    // Initially the indices are all fixed arguments specified in the alias definition.
    // E.g. in `register alias Z = X(1)` is `1` a fixed argument.
    var indices = Objects.requireNonNull(definition.computedFixedArgs).stream()
        .map(this::fetch).collect(Collectors.toCollection(NodeList::new));
    var params = new ArrayList<>();

    // FIXME: Support pre-indexed registers, for example:
    //  register X: Bits<3><4><32>
    //  register alias Z = X(1)(2)
    if (definition.type() instanceof ConcreteRelationType relType) {
      // FIXME: Wrap input and output in casts
      var param = new vadl.viam.Parameter(
          viamLowering.generateIdentifier(
              identifier.name() + "::index",
              identifier.location()),
          relType.argTypes().getFirst());
      params.add(param);
      indices.add(new FuncParamNode(param));
      resultType = relType.resultType().asDataType();
    } else {
      resultType = definition.type().asDataType();
    }

    var reg = (RegisterTensor) viamLowering.fetch(regFileDef).orElseThrow();
    var regReadType = regFileDef.type() instanceof ConcreteRelationType relType
        ? relType.resultType().asDataType() : resultType.asDataType();
    ExpressionNode regAccess = new ReadRegTensorNode(
        reg,
        indices,
        regReadType,
        null
    );

    if (zeroConst != null) {
      // Wrap the register read in a conditional read, depending on the indices values.
      // Compatibility was already checked by the annotation itself during type checking.
      var dontMatch = buildConstraintDontMatchCheck(indices, zeroConst.indices);
      // if the indices constraints don't match the arguments,
      // we return the register read, otherwise zero
      regAccess = select(
          dontMatch,
          regAccess,
          Constant.Value.of(0, regReadType).toNode()
      );
    }

    var returnNode = graph.addWithInputs(new ReturnNode(regAccess));
    graph.addWithInputs(new StartNode(returnNode));

    // FIXME: Modify based on annotations
    return new Function(
        identifier,
        params.toArray(vadl.viam.Parameter[]::new),
        getViamType(resultType),
        graph
    );
  }

  Procedure getRegisterAliasWriteProc(AliasDefinition definition) {
    final var graph = new Graph("%s Write Procedure".formatted(definition.viamId));
    graph.setSourceLocation(definition.location());
    currentGraph = graph;

    final var identifier =
        viamLowering.generateIdentifier(definition.viamId + "::write", definition.loc);
    final var regFileDef = (RegisterDefinition) Objects.requireNonNull(definition.computedTarget);
    final var zeroConst = definition.getAnnotation("zero", ZeroConstraintAnnotation.class);

    DataType resultType;
    // Initially the indices are all fixed arguments specified in the alias definition.
    // E.g. in `register alias Z = X(1)` is `1` a fixed argument.
    var indices = Objects.requireNonNull(definition.computedFixedArgs).stream()
        .map(this::fetch).collect(Collectors.toCollection(NodeList::new));
    var params = new ArrayList<>();
    // FIXME: Support pre-indexed registers, for example:
    //  register X = Bits<3><4><32>
    //  register alias Z = X(1, 2)
    if (definition.type() instanceof ConcreteRelationType relType) {
      // FIXME: Wrap input and output in casts
      // FIXME: Add conditions based on annotations
      var param = new vadl.viam.Parameter(
          viamLowering.generateIdentifier(
              identifier.name() + "::index",
              identifier.location()),
          relType.argTypes().getFirst());
      params.add(param);
      indices.add(new FuncParamNode(param));
      resultType = relType.resultType().asDataType();
    } else {
      resultType = definition.type().asDataType();
    }

    var valueParam = new vadl.viam.Parameter(
        viamLowering.generateIdentifier(
            identifier.name() + "::value",
            identifier.location()),
        getViamType(resultType));
    params.add(valueParam);

    // FIXME: Support pre-indexed registers, for example:
    //  register X = Bits<3><4><32>
    //  register alias Z = X(1, 2)
    // FIXME: Wrap input and output in casts
    // FIXME: Add conditions based on annotations
    var regFile = (RegisterTensor) viamLowering.fetch(regFileDef).orElseThrow();
    var regfileWrite = new WriteRegTensorNode(
        regFile,
        indices,
        new FuncParamNode(valueParam),
        null,
        null
    );

    ControlNode nextOfStart;
    if (zeroConst == null) {
      // If there is no zero constraint on the artifical resource
      // we attach the side effect to the proc end node
      nextOfStart = graph.addWithInputs(new ProcEndNode(new NodeList<>(regfileWrite)));
    } else {
      // If there is a zero constraint, we must build an if-else control flow
      // and apply the side effect on the true branch of the if
      // (so in the case that the indices don't match the constraint values).
      var dontMatch = buildConstraintDontMatchCheck(indices, zeroConst.indices);
      var end = graph.addWithInputs(new ProcEndNode(new NodeList<>()));
      nextOfStart =
          ifElseSideEffect(graph, dontMatch, List.of(regfileWrite), List.of(), end);
    }

    graph.addWithInputs(new StartNode(nextOfStart));

    return new Procedure(
        identifier,
        params.toArray(vadl.viam.Parameter[]::new),
        graph
    );
  }


  private <T extends vadl.viam.graph.Node> T addToGraph(T node) {
    if (!node.isActive()) {
      return Objects.requireNonNull(currentGraph).addWithInputs(node);
    }
    return node;
  }


  private Pair<BeginNode, BranchEndNode> buildBranch(SubgraphContext branchCtx,
                                                     WithLocation locatable) {
    var endNode = addToGraph(new BranchEndNode(branchCtx.sideEffectsOrEmptyList()));

    BeginNode beginNode;
    if (branchCtx.controlBlock() != null) {
      beginNode = new BeginNode(branchCtx.controlBlock().firstNode());
      branchCtx.controlBlock().lastNode().setNext(endNode);
    } else {
      beginNode = new BeginNode(endNode);
    }
    beginNode = addToGraph(beginNode);

    endNode.setSourceLocation(locatable.location());
    beginNode.setSourceLocation(locatable.location());
    return new Pair<>(beginNode, endNode);
  }

  private Pair<BeginNode, BranchEndNode> buildBranch(@Nullable Statement stmt) {
    if (stmt == null) {
      var endNode = addToGraph(new BranchEndNode(new NodeList<>()));
      var beginNode = addToGraph(new BeginNode(endNode));
      return new Pair<>(beginNode, endNode);
    }

    var branchCtx = stmt.accept(this);
    return buildBranch(branchCtx, stmt);
  }

  private static BuiltInCall produceNeqToZero(ExpressionNode node) {
    var constNode = new ConstantNode(Constant.Value.of(0, (DataType) node.type()));
    constNode.setSourceLocation(node.location());
    return BuiltInCall.of(BuiltInTable.NEQ, node, constNode);
  }


  private ExpressionNode fetch(Expr expr) {
    if (expressionCache.containsKey(expr)) {
      return expressionCache.get(expr);
    }

    var result = expr.accept(this);
    result.setSourceLocationIfNotSet(expr.location());
    expressionCache.put(expr, result);
    result.ensure(!(result.type() instanceof ConstantType),
        "Constant types must not exist in the VIAM");
    return result;
  }


  /**
   * Identifier and IdentifierPath are quite similar in what they do, so let's resolve both here.
   */
  private ExpressionNode visitIdentifyable(Expr expr) {

    Node computedTarget;
    String innerName;
    String fullName;

    if (expr instanceof Identifier identifier) {
      computedTarget = identifier.target();
      innerName = identifier.name;
      fullName = identifier.name;
    } else if (expr instanceof IdentifierPath path) {
      computedTarget = path.target();
      var segments = path.pathToSegments();
      innerName = segments.get(segments.size() - 1);
      fullName = path.pathToString();
    } else {
      throw new IllegalStateException();
    }    // Constant

    if (computedTarget instanceof ConstantDefinition constant) {
      var value = constantEvaluator.eval(constant.value).toViamConstant();
      return new ConstantNode(value);
    }

    // Enum field
    if (computedTarget instanceof EnumerationDefinition.Entry enumField) {
      // Inline the value of the enum
      return fetch(Objects.requireNonNull(enumField.value));
    }

    // Format field
    if (computedTarget instanceof TypedFormatField typedFormatField) {
      return new FieldRefNode(
          (Format.Field) viamLowering.fetch(typedFormatField).orElseThrow(),
          (DataType) getViamType(Objects.requireNonNull(expr.type)));
    }
    if (computedTarget instanceof RangeFormatField rangeFormatField) {
      return new FieldRefNode(
          (Format.Field) viamLowering.fetch(rangeFormatField).orElseThrow(),
          (DataType) getViamType(Objects.requireNonNull(expr.type)));
    }
    if (computedTarget instanceof DerivedFormatField derivedFormatField) {
      return new FieldAccessRefNode(
          (Format.FieldAccess) viamLowering.fetch(derivedFormatField).orElseThrow(),
          (DataType) getViamType(Objects.requireNonNull(expr.type)));
    }

    // Register
    if (computedTarget instanceof RegisterDefinition registerDefinition) {
      var register = (RegisterTensor) viamLowering.fetch(registerDefinition).orElseThrow();
      return new ReadRegTensorNode(
          register,
          new NodeList<>(),
          (DataType) getViamType(Objects.requireNonNull(expr.type)),
          null);
    }

    // Register Alias
    if (computedTarget instanceof AliasDefinition aliasDefinition
        && aliasDefinition.kind.equals(AliasDefinition.AliasKind.REGISTER)) {
      // FIXME: Currently there are no artificial ressources for registers so just inline it here
      // but once there are uncomment the code below.
      // https://github.com/OpenVADL/open-vadl/issues/104

      // var alias = (ArtificialResource) viamLowering.fetch(aliasDefinition).orElseThrow();
      // return new ReadArtificialResNode(alias, null, (DataType) expr.type());

      // Don't call fetch on purpose cause the graph is the wrong one:
      var x = fetch(aliasDefinition.value);
      if (x.graph() != null && x.graph() != currentGraph) {
        System.out.println();
      }
      return fetch(aliasDefinition.value);
    }

    // Counters
    if (computedTarget instanceof CounterDefinition counterDefinition) {
      if (counterDefinition.kind == CounterDefinition.CounterKind.PROGRAM) {
        var counter = (Counter) viamLowering.fetch(counterDefinition).orElseThrow();

        if (!counter.registerTensor().isSingleRegister()) {
          throw new IllegalStateException(
              "Only one-dimensional counters are supported at the moment.");
        }

        return new ReadRegTensorNode((RegisterTensor) counter.registerTensor(),
            new NodeList<>(),
            (DataType) getViamType(Objects.requireNonNull(expr.type)),
            null);
      }
      throw new IllegalStateException("Unsupported counter kind: " + counterDefinition.kind);
    }

    // Let statement and expression
    if (computedTarget instanceof LetStatement letStatement) {
      var expression = fetch(letStatement.valueExpr);
      var index = letStatement.getIndexOf(innerName);
      if (letStatement.identifiers.size() > 1) {
        expression = new TupleGetFieldNode(index, expression,
            getViamType(letStatement.getTypeOf(innerName)));
      }
      return new LetNode(new LetNode.Name(innerName, letStatement.location()), expression);
    }
    if (computedTarget instanceof LetExpr letExpr) {
      var expression = fetch(letExpr.valueExpr);
      var index = letExpr.getIndexOf(innerName);
      if (letExpr.identifiers.size() > 1) {
        expression =
            new TupleGetFieldNode(index, expression, getViamType(letExpr.getTypeOf(innerName)));
      }
      return new LetNode(new LetNode.Name(innerName, letExpr.location()), expression);
    }

    // Parameter of a function
    if (computedTarget instanceof Parameter parameter) {
      var param = viamLowering.fetch(parameter).orElseThrow();
      return new FuncParamNode(param);
    }

    // Function call without arguments (and no parenthesis)
    if (computedTarget instanceof FunctionDefinition functionDefinition) {
      var function = (Function) viamLowering.fetch(functionDefinition).orElseThrow();
      return new FuncCallNode(function, new NodeList<>(),
          getViamType(Objects.requireNonNull(expr.type)));
    }

    // Builtin Call
    var matchingBuiltins = BuiltInTable.builtIns()
        .filter(b -> b.signature().argTypeClasses().isEmpty())
        .filter(b -> b.name().toLowerCase().equals(innerName))
        .toList();

    if (matchingBuiltins.size() == 1) {
      var builtin = matchingBuiltins.get(0);
      return new BuiltInCall(builtin, new NodeList<ExpressionNode>(),
          getViamType(Objects.requireNonNull(expr.type)));
    }

    throw new RuntimeException(
        "The behavior generator cannot resolve yet identifier '%s' which points to %s".formatted(
            fullName,
            computedTarget == null ? "null" : computedTarget.getClass().getSimpleName()));
  }

  @Override
  public ExpressionNode visit(Identifier expr) {
    return visitIdentifyable(expr);
  }

  @Override
  public ExpressionNode visit(BinaryExpr expr) {
    var builtin = AstUtils.getBinOpBuiltIn(expr);
    var left = fetch(expr.left);
    var right = fetch(expr.right);
    return new BuiltInCall(builtin, new NodeList<>(left, right),
        getViamType(Objects.requireNonNull(expr.type)));
  }

  @Override
  public ExpressionNode visit(GroupedExpr expr) {
    // Arithmetic grouping
    if (expr.expressions.size() == 1) {
      return expr.expressions.get(0).accept(this);
    }

    // String or Bits concatenation
    // This code looks so complicated because the concat function can only concat two arguments.
    // So the first two are directly concatenated, and all others are depend on the previous concat
    // node.

    var concatBuiltin = expr.type().equals(Type.string()) ? BuiltInTable.CONCATENATE_STRINGS :
        BuiltInTable.CONCATENATE_BITS;

    var type = expr.type().equals(Type.string()) ? expr.type() :
        Type.bits(expr.expressions.get(0).type().asDataType()
            .bitWidth() + expr.expressions.get(1).type().asDataType().bitWidth());

    var call = new BuiltInCall(concatBuiltin,
        new NodeList<>(expr.expressions.get(0).accept(this),
            expr.expressions.get(1).accept(this)),
        getViamType(type));

    for (int i = 2; i < expr.expressions.size(); i++) {
      type = expr.type().equals(Type.string()) ? expr.type() :
          Type.bits(type.asDataType().bitWidth()
              + expr.expressions.get(i).type().asDataType().bitWidth());
      call = new BuiltInCall(concatBuiltin,
          new NodeList<>(call,
              expr.expressions.get(i).accept(this)),
          getViamType(type));
    }

    return call;
  }

  @Override
  public ExpressionNode visit(IntegerLiteral expr) {
    // IntegerLiteral should never be reached as it should always be substituted by the typechecker.
    throw new IllegalStateException("IntegerLiteral should never be reached in the VIAM lowering.");
  }

  @Override
  public ExpressionNode visit(BinaryLiteral expr) {
    return new ConstantNode(
        Constant.Value.fromInteger(
            expr.number,
            (DataType) getViamType(Objects.requireNonNull(expr.type))));
  }

  @Override
  public ExpressionNode visit(BoolLiteral expr) {
    return new ConstantNode(Constant.Value.of(expr.value));
  }

  @Override
  public ExpressionNode visit(StringLiteral expr) {
    return new ConstantNode(
        new Constant.Str(expr.value));
  }

  @Override
  public ExpressionNode visit(TensorLiteral expr) {
    throw new IllegalStateException("Not yet implemented");
  }

  @Override
  public ExpressionNode visit(PlaceholderExpr expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(MacroInstanceExpr expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(RangeExpr expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(TypeLiteral expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(IdentifierPath expr) {
    return visitIdentifyable(expr);
  }

  @Override
  public ExpressionNode visit(UnaryExpr expr) {
    var value = fetch(expr.operand);
    return new BuiltInCall(
        Objects.requireNonNull(expr.computedTarget),
        new NodeList<>(value),
        getViamType(Objects.requireNonNull(expr.type)));
  }

  /**
   * Subcalls for format fields introduce slicing, which is handled here.
   *
   * @param expr              with the potential subcalls
   * @param exprBeforeSubcall to be sliced
   * @return the original expr or wrapped in a slice.
   */
  private ExpressionNode visitSubCall(CallIndexExpr expr, ExpressionNode exprBeforeSubcall) {
    if (expr.subCalls.isEmpty()) {
      return exprBeforeSubcall;
    }

    var resultExpr = exprBeforeSubcall;
    for (var subCall : expr.subCalls) {
      if (subCall.computedBitSlice != null) {
        var bitSlice = subCall.computedBitSlice;
        var slice =
            new SliceNode(resultExpr, bitSlice,
                (DataType) getViamType(Objects.requireNonNull(subCall.formatFieldType)));
        resultExpr = visitSliceIndexCall(slice, subCall.argsIndices);
      } else if (subCall.computedStatusIndex != null) {
        var indexing = new TupleGetFieldNode(subCall.computedStatusIndex, resultExpr, Type.bool());
        resultExpr = visitSliceIndexCall(indexing, subCall.argsIndices);
      } else if (exprBeforeSubcall instanceof ReadResourceNode resRead) {
        var computedTarget = expr.target.path().target();
        if (computedTarget instanceof CounterDefinition) {
          // FIXME: @ffreitag this is currently hardcoded as was wrong before.
          //  It must add the instruction width in bytes.
          // This width is obtained by the format type of the current instruction
          var instrWidth = 32;
          // The byte is defined by the "word" that is returned by the main memory definition.
          // So essentially the return type in the relation type of the memory definition.
          var byteWidth = 8;
          var instrWidthInByte = instrWidth / byteWidth;

          // FIXME: Handle slicing and format subcall propperly
          int offset = 0;
          for (var subcall : expr.subCalls) {
            var subcallName = subcall.id.name;
            if (subcallName.equals("next")) {
              offset += instrWidthInByte;
            } else {
              throw new IllegalStateException("unknown subcall: " + subcallName);
            }
          }

          resultExpr = BuiltInCall.of(BuiltInTable.ADD,
              resRead,
              intU(offset, resRead.type().bitWidth()).toNode()
          );
        }
      } else {
        throw new IllegalStateException();
      }
    }

    return resultExpr;
  }

  private ExpressionNode visitSliceIndexCall(ExpressionNode exprBeforeSlice,
                                             List<CallIndexExpr.Arguments> slices) {
    if (slices.isEmpty()) {
      return exprBeforeSlice;
    }

    var result = exprBeforeSlice;
    for (var slice : slices) {
      var bitSlice = Objects.requireNonNull(slice.computedBitSlice);
      var type = Type.bits(bitSlice.bitSize());
      result = new SliceNode(exprBeforeSlice, slice.computedBitSlice, type);
    }

    return result;
  }

  @Override
  public ExpressionNode visit(CallIndexExpr expr) {

    List<Expr> argExprs = AstUtils.flatArguments(expr.args());
    var args = argExprs.stream().map(this::fetch).toList();
    var typeBeforeSlice = getViamType(expr.typeBeforeSlice());

    ExpressionNode exprBeforeSlice;

    // Builtin Call
    if (expr.computedBuiltIn != null) {
      if (BuiltInTable.ASM_PARSER_BUILT_INS.contains(expr.computedBuiltIn)) {
        exprBeforeSlice = new AsmBuiltInCall(expr.computedBuiltIn, new NodeList<>(args),
            expr.typeBeforeSlice());
      } else {
        exprBeforeSlice = new BuiltInCall(expr.computedBuiltIn, new NodeList<>(args),
            expr.typeBeforeSlice());
      }
    } else {
      exprBeforeSlice = switch (expr.computedTarget()) {
        case FunctionDefinition funcDef -> new FuncCallNode(
            (Function) viamLowering.fetch(funcDef).orElseThrow(),
            new NodeList<>(args), typeBeforeSlice);

        case RelocationDefinition funcDef -> new FuncCallNode(
            (Function) viamLowering.fetch(funcDef).orElseThrow(),
            new NodeList<>(args), typeBeforeSlice);

        case RegisterDefinition regDef -> new ReadRegTensorNode(
            (RegisterTensor) viamLowering.fetch(regDef).orElseThrow(),
            new NodeList<>(args), typeBeforeSlice.asDataType(), null);

        case AliasDefinition aliasDef -> new ReadArtificialResNode(
            (ArtificialResource) viamLowering.fetch(aliasDef).orElseThrow(),
            new NodeList<>(args), typeBeforeSlice.asDataType());

        case MemoryDefinition memDef -> {
          var sizeExpr = expr.target.size();
          var words = sizeExpr != null
              ? constantEvaluator.eval(sizeExpr).value().intValueExact()
              : 1;
          yield new ReadMemNode((Memory) viamLowering.fetch(memDef).orElseThrow(),
              words, args.getFirst(), typeBeforeSlice.asDataType());
        }

        case CounterDefinition counterDef -> new ReadRegTensorNode(
            ((Counter) viamLowering.fetch(counterDef).orElseThrow()).registerTensor(),
            new NodeList<>(), expr.typeBeforeSlice().asDataType(), null);

        default -> fetch((Expr) expr.target);
      };
    }

    var result = visitSliceIndexCall(exprBeforeSlice, expr.slices());
    result = visitSubCall(expr, result);
    return result;
  }

  @Override
  public ExpressionNode visit(IfExpr expr) {
    var condition = fetch(expr.condition);
    var consequence = fetch(expr.thenExpr);
    var contradiction = fetch(expr.elseExpr);
    return new SelectNode(condition, consequence, contradiction);
  }

  @Override
  public ExpressionNode visit(LetExpr expr) {
    // The bounded variable is already resolved and it's usages will be turned into a let-node.
    // So just return the expr.
    return fetch(expr.body);
  }

  @Override
  public ExpressionNode visit(CastExpr expr) {
    // Shortcut for constant types
    if (expr.value.type instanceof ConstantType constType) {
      return new ConstantNode(
          Constant.Value.of(constType.getValue().longValueExact(),
                  (DataType) constType.closestTo(expr.type()))
              .castTo((DataType) expr.type()));
    }

    // check the different rules and apply them accordingly
    var source = fetch(expr.value);
    var sourceType = getViamType(Objects.requireNonNull(expr.value.type));
    var targetType = getViamType(Objects.requireNonNull(expr.type));
    if (sourceType.isTrivialCastTo(targetType)) {
      // match 1. rule: same bit representation
      // -> no casting needs to be applied
      source.setType(targetType);
      return source;
    }

    var sourceDataType = (DataType) sourceType;
    var targetDataType = (DataType) targetType;

    if (targetType.getClass() == vadl.types.BoolType.class) {
      // match 2. rule: target type is bool
      // -> produce != 0 call
      //return new BuiltInCall
      return produceNeqToZero(source);
    }
    if (targetDataType.bitWidth() < sourceDataType.bitWidth()) {
      // match 3. rule: cast type bit-width is smaller than source type
      // -> create TruncateNode
      return new TruncateNode(source, targetDataType);
    }
    if (sourceType.getClass() == SIntType.class) {
      // match 4.
      // rule: source type is a signed integer
      // -> create sign extend node
      return new SignExtendNode(source, targetDataType);
    }
    if (sourceType.getClass() == BitsType.class
        && targetType.getClass() == SIntType.class) {
      // match 5.
      // rule: source type is a bits type and target type is SInt
      // -> create sign extend node
      return new SignExtendNode(source, targetDataType);
    }
    if (targetType.getClass() == UIntType.class
        || targetType.getClass() == BitsType.class
        || targetType.getClass() == SIntType.class
    ) {
      // match 5. rule: cast type is one of sint, uint, or bits
      return new ZeroExtendNode(source, targetDataType);
    }

    throw new IllegalArgumentException(
        "The behavior generator doesn't implement real casting yet.");
  }

  @Override
  public ExpressionNode visit(SymbolExpr expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(MacroMatchExpr expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(MatchExpr expr) {
    ExpressionNode node = fetch(expr.defaultResult);
    ExpressionNode candidate = fetch(expr.candidate);

    // In reverse order to keep the execution order
    for (int i = expr.cases.size() - 1; i >= 0; i--) {
      var caseExpr = expr.cases.get(i);

      // Logical or join of all patterns
      var condition = new BuiltInCall(BuiltInTable.EQU,
          new NodeList<>(candidate, fetch(caseExpr.patterns.get(0))), Type.bool());
      for (int j = 1; j < caseExpr.patterns.size(); j++) {
        var patternCond = new BuiltInCall(BuiltInTable.EQU,
            new NodeList<>(candidate, fetch(caseExpr.patterns.get(0))), Type.bool());
        condition =
            new BuiltInCall(BuiltInTable.OR, new NodeList<>(condition, patternCond), Type.bool());
      }

      var consequence = fetch(caseExpr.result);

      node = new SelectNode(condition, consequence, node);
    }

    return node;
  }

  @Override
  public ExpressionNode visit(AsIdExpr expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(AsStrExpr expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(ExistsInExpr expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(ExistsInThenExpr expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(ForallThenExpr expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(ForallExpr expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(SequenceCallExpr expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(ExpandedSequenceCallExpr expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(ExpandedAliasDefSequenceCallExpr expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }


  @Override
  public SubgraphContext visit(AssignmentStatement statement) {
    var value = fetch(statement.valueExpression);

    vadl.ast.Definition targetDef;
    List<CallIndexExpr.Arguments> argGroups = List.of();
    List<Constant.BitSlice> slices = new ArrayList<>();

    // the MEM<xyz>(...) value
    @Nullable Integer callSize = null;

    if (statement.target instanceof CallIndexExpr callTarget) {
      targetDef = (vadl.ast.Definition) callTarget.computedTarget();
      argGroups = callTarget.args();
      callTarget.slices().forEach(s -> {
        slices.add(Objects.requireNonNull(s.computedBitSlice));
      });
      // add all slices that come from format field accesses
      callTarget.subCalls.forEach(s -> {
        if (s.computedBitSlice != null) {
          slices.add(s.computedBitSlice);
        }
      });

      var sizeExpr = callTarget.target.size();
      callSize = sizeExpr != null
          ? constantEvaluator.eval(sizeExpr).value().intValueExact()
          : null;
    } else if (statement.target instanceof Identifier identTarget) {
      targetDef = (vadl.ast.Definition) Objects.requireNonNull(identTarget.target());
    } else {
      throw new IllegalStateException("Unexpected target: " + statement);
    }

    var argExprs = AstUtils.flatArguments(argGroups).stream().map(this::fetch)
        .collect(Collectors.toCollection(NodeList::new));
    var viamDef = viamLowering.fetch(targetDef).orElseThrow();

    WriteResourceNode writeNode = switch (viamDef) {

      case RegisterTensor regDef -> new WriteRegTensorNode(regDef, argExprs,
          // slice the written value before writing it
          sliceWriteValue(value,
              new ReadRegTensorNode(regDef, argExprs, regDef.resultType(), null), slices),
          null, null);

      case ArtificialResource aliasDef -> new WriteArtificialResNode(aliasDef, argExprs,
          // slice the written value before writing it
          sliceWriteValue(value,
              new ReadArtificialResNode(aliasDef, argExprs, aliasDef.resultType()), slices)
      );

      case Memory memDef -> {
        var words = callSize != null ? callSize : 1;
        // slice the written value before writing it
        var slicedValue = sliceWriteValue(value,
            new ReadMemNode(memDef, words, argExprs.getFirst(),
                ((BitsType) memDef.resultType()).scaleBy(words)), slices);
        yield new WriteMemNode(
            memDef, callSize != null ? callSize : 1,
            argExprs.getFirst(), slicedValue
        );
      }

      // FIXME: Adjust value based on counter position
      case Counter counterDef -> new WriteRegTensorNode(counterDef.registerTensor(), argExprs,
          // slice the written value before writing it
          sliceWriteValue(value,
              new ReadRegTensorNode(counterDef.registerTensor(), argExprs,
                  counterDef.registerTensor().resultType(), null), slices),
          null, null);

      default -> throw new IllegalStateException("Unexpected target: " + viamDef);
    };

    return SubgraphContext.of(statement, writeNode);
  }

  /**
   * Method that prepares the value so it can be written to a subset region of a resource.
   * The entire resource before writing the value is given by the entireRead node.
   * The subset region of the resource is given by the slices list, that
   * holds a list of {@link vadl.viam.Constant.BitSlice}.
   * E.g. {@code A(3, 15..11) := 0b101111} writes the value's msb `1` at position 3 in the
   * resource,
   * and the rest (0b01111) is written to position 15 to 11 (inclusive) in the resource.
   *
   * @param value      value that is being written (right side of assignment)
   * @param entireRead resource value before value is written
   * @param slices     the slices where each entry represents a group.
   *                   The example above has one bit-slice with two parts
   * @return expression that incorporates the written value into the resource.
   */
  private ExpressionNode sliceWriteValue(ExpressionNode value,
                                         ReadResourceNode entireRead,
                                         List<Constant.BitSlice> slices) {
    if (slices.isEmpty()) {
      return value;
    }
    if (slices.size() != 1) {
      // this requires to merge all slices into a single one before applying adjustment
      throw new IllegalStateException("Nested slices are not yet supported");
    }

    var slice = slices.getFirst();

    // the value bits all shifted in place of the position in final results
    ExpressionNode injected = null;
    // how many bits taken from <value>
    int consumed = 0;

    // parts from lsb to msb
    var parts = Lists.reverse(slice.parts().toList());
    for (var part : parts) {
      // shift the next lsb part of the write value
      value = consumed == 0 ? value :
          BuiltInCall.of(BuiltInTable.LSR, value, intU(consumed, 32).toNode());
      // extracted value of this part
      ExpressionNode partValue = new TruncateNode(value, Type.bits(part.size()));
      // zero extend part value to correct size
      partValue = new ZeroExtendNode(partValue, entireRead.type());

      var placed = part.lsb() == 0 ? partValue :
          BuiltInCall.of(BuiltInTable.LSL, partValue, intU(part.lsb(), 32).toNode());

      injected = injected == null ? placed : BuiltInCall.of(BuiltInTable.OR, injected, placed);
      consumed += part.size();
    }

    var mask = slice.mask().castTo(Type.bits(entireRead.type().bitWidth())).toNode();
    var clearedResource = BuiltInCall.of(BuiltInTable.AND, entireRead, mask);
    return BuiltInCall.of(BuiltInTable.OR, clearedResource, Objects.requireNonNull(injected));
  }


  @Override
  public SubgraphContext visit(BlockStatement statement) {
    List<vadl.viam.graph.Node> nodes = new ArrayList<>();
    @Nullable ControlNode firstNode = null;
    @Nullable DirectionalNode lastNode = null;

    for (var stmt : statement.statements) {
      var stmtCtx = stmt.accept(this);

      if (stmtCtx.hasControlBlock()) {
        if (firstNode == null) {
          firstNode = Objects.requireNonNull(stmtCtx.controlBlock()).firstNode();
        }

        if (lastNode != null) {
          // link previous stmt with current stmt
          lastNode.setNext(Objects.requireNonNull(stmtCtx.controlBlock()).firstNode());
        }
        lastNode = Objects.requireNonNull(stmtCtx.controlBlock()).lastNode();
      }
      nodes.addAll(stmtCtx.sideEffectsOrEmptyList());
    }

    if ((firstNode == null) != (lastNode == null)) {
      throw new IllegalStateException(
          "first and last node must be both null or not null @ " + statement);
    }

    if (firstNode != null) {
      nodes.add(firstNode);
      nodes.add(lastNode);
    }

    return SubgraphContext.of(statement, nodes);
  }

  @Override
  public SubgraphContext visit(CallStatement statement) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + statement.getClass().getSimpleName());
  }

  @Override
  public SubgraphContext visit(ForallStatement statement) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + statement.getClass().getSimpleName());
  }

  @Override
  public SubgraphContext visit(IfStatement statement) {
    var condition = fetch(statement.condition);

    var ifPair = buildBranch(statement.thenStmt);
    var elsePair = buildBranch(statement.elseStmt);
    var ifStart = ifPair.left();
    var ifEnd = ifPair.right();
    var elseStart = elsePair.left();
    var elseEnd = elsePair.right();

    var mergeNode = addToGraph(new MergeNode(new NodeList<>(ifEnd, elseEnd)));
    var ifNode = addToGraph(new IfNode(condition, ifStart, elseStart));
    return SubgraphContext.of(statement, ifNode, mergeNode);
  }

  @Override
  public SubgraphContext visit(InstructionCallStatement statement) {
    if (statement.instrDef instanceof PseudoInstructionDefinition) {
      // FIXME: Implement flattening as described in
      // https://github.com/OpenVADL/openvadl/issues/312
      // This will require a lot of special handling as we have to create datastrucutres for a
      // "stack" that holds the arguments, and implementing recursive calls.
      throw error("Not yet supported", statement)
          .locationDescription(statement, "Calling pseudo instructions isn't supported yet.")
          .build();
    }

    var target =
        (Instruction) viamLowering.fetch(Objects.requireNonNull(statement.instrDef)).orElseThrow();
    var fieldMap = Arrays.stream(target.encoding().nonEncodedFormatFields())
        .collect(Collectors.toMap(Definition::simpleName, f -> f));

    var argExprs = new NodeList<ExpressionNode>();
    var fieldsOrAccesses = new ArrayList<Either<Format.Field, Format.FieldAccess>>();

    for (var arg : statement.namedArguments) {
      var field = fieldMap.get(arg.name.name);
      var fieldAccess = target.encoding().format().fieldAccesses().stream()
          .filter(access -> access.simpleName().equals(arg.name.name))
          .findFirst().orElse(null);

      fieldsOrAccesses.add(new Either<>(field, fieldAccess));
      argExprs.add(fetch(arg.value));
    }
    var call = new InstrCallNode(target, fieldsOrAccesses, argExprs);
    call.setSourceLocation(statement.location());
    call = addToGraph(call);
    return SubgraphContext.of(statement, call);
  }

  @Override
  public SubgraphContext visit(LetStatement statement) {
    // The bounded variable is already resolved and it's usages will be turned into a let-node.
    // So just return the body.
    return statement.body.accept(this);
  }

  @Override
  public SubgraphContext visit(LockStatement statement) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + statement.getClass().getSimpleName());
  }

  @Override
  public SubgraphContext visit(MacroInstanceStatement statement) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + statement.getClass().getSimpleName());
  }

  @Override
  public SubgraphContext visit(MacroMatchStatement statement) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + statement.getClass().getSimpleName());
  }

  @Override
  public SubgraphContext visit(MatchStatement statement) {
    var defaultPair = buildBranch(statement.defaultResult);
    //BeginNode beginnNode = defaultPair.left();
    IfNode start = null;
    MergeNode end = null;
    var candidate = fetch(statement.candidate);


    // In reverse order to keep the execution order
    for (int i = statement.cases.size() - 1; i >= 0; i--) {
      var kase = statement.cases.get(i);

      // Logical or join of all patterns
      var condition = new BuiltInCall(BuiltInTable.EQU,
          new NodeList<>(candidate, fetch(kase.patterns.get(0))), Type.bool());
      for (int j = 1; j < kase.patterns.size(); j++) {
        var patternCond = new BuiltInCall(BuiltInTable.EQU,
            new NodeList<>(candidate, fetch(kase.patterns.get(0))), Type.bool());
        condition =
            new BuiltInCall(BuiltInTable.OR, new NodeList<>(condition, patternCond), Type.bool());
      }

      var consequencePair = buildBranch(kase.result);

      Pair<BeginNode, BranchEndNode> contradictionPair;
      if (start == null) {
        contradictionPair = defaultPair;
      } else {
        contradictionPair =
            buildBranch(SubgraphContext.of(statement, start, Objects.requireNonNull(end)), kase);
      }

      end = addToGraph(
          new MergeNode(new NodeList<>(consequencePair.right(), contradictionPair.right())));
      start = addToGraph(new IfNode(condition, consequencePair.left(), contradictionPair.left()));
    }


    return SubgraphContext.of(statement, Objects.requireNonNull(start),
        Objects.requireNonNull(end));
  }

  @Override
  public SubgraphContext visit(PlaceholderStatement statement) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + statement.getClass().getSimpleName());
  }

  @Override
  public SubgraphContext visit(RaiseStatement statement) {
    ExceptionDef exception;
    NodeList<ExpressionNode> args = new NodeList<>();

    if (statement.statement instanceof CallStatement callStatement) {
      var expr = callStatement.expr;

      if (expr instanceof Identifier ident
          && ident.target instanceof ExceptionDefinition exceptionDef) {
        exception = (ExceptionDef) viamLowering.fetch(exceptionDef).get();
      } else if (expr instanceof CallIndexExpr call
          && call.computedTarget() instanceof ExceptionDefinition exceptionDef) {
        exception = (ExceptionDef) viamLowering.fetch(exceptionDef).get();
        args = call.argsIndices.get(0).values.stream()
            .map(this::fetch)
            .collect(Collectors.toCollection(NodeList::new));
      } else {
        throw error("Invalid Raise Call", expr)
            .locationDescription(expr, "Expected a call to an Exception.")
            .build();
      }
    } else {
      // FIXME: Add to a global store so that ISA can get a list of all exceptions
      var name = statement.viamId + "::anonymousException";
      exception = new ExceptionDef(
          viamLowering.generateIdentifier(name, statement.statement),
          new vadl.viam.Parameter[] {},
          new BehaviorLowering(this.viamLowering).getProcedureGraph(statement.statement, name),
          ExceptionDef.Kind.ANONYMOUS
      );
    }

    var raise = new ProcCallNode(exception, args, null);
    raise.setSourceLocation(statement.location());
    return SubgraphContext.of(statement, raise);
  }

  @Override
  public SubgraphContext visit(StatementList statement) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + statement.getClass().getSimpleName());
  }
}

record ControlBlock(ControlNode firstNode, DirectionalNode lastNode) {
}

/**
 * Contains the nodes of a subgraph.
 * The root references the context causing AST Node.
 * The beginNode and endNode define the start and end node
 * of the subgraph. The sideEffects are all dependencies that may
 * cause side effects and those must be dependencies of the outer branch.
 * The result is the return expression node as depenency of the outer node.
 *
 * <p>All members are optional/nullable and must be checked before access.
 */
class SubgraphContext {
  private Node root;

  @Nullable
  private NodeList<SideEffectNode> sideEffects;

  @Nullable
  private ControlBlock controlBlock;

  private SubgraphContext(Node root, @Nullable NodeList<SideEffectNode> sideEffects,
                          @Nullable ControlBlock controlBlock) {
    this.root = root;
    this.sideEffects = sideEffects;
    this.controlBlock = controlBlock;
  }

  static SubgraphContext of(Node root, vadl.viam.graph.Node... nodes) {
    return SubgraphContext.of(root, List.of(nodes));
  }

  static SubgraphContext of(Node root, List<vadl.viam.graph.Node> nodes) {
    var sideEffects = new NodeList<SideEffectNode>();
    @Nullable ControlNode blockStart = null;
    @Nullable DirectionalNode blockEnd = null;
    SubgraphContext ctx = new SubgraphContext(root, null, null);

    for (var node : nodes) {
      if (node instanceof ControlNode controlNode) {
        if (node.predecessor() == null && !(node instanceof MergeNode)) {
          if (blockStart != null && blockStart != node) {
            throw new IllegalStateException(
                "tried to add %s, but blockStart already set: %s @%s".formatted(node, blockStart,
                    root.location()));
          }
          blockStart = controlNode;
        }

        if ((node instanceof DirectionalNode directionalNode)
            && directionalNode.successors().count() == 0) {
          if (blockEnd != null && directionalNode.successors().count() == 0) {
            throw new IllegalStateException(
                "tried to add %s, but blockEnd already set: %s @%s".formatted(node, blockEnd,
                    root.location()));
          }
          blockEnd = directionalNode;
        }

      } else if (node instanceof SideEffectNode sideEffect) {
        sideEffects.add(sideEffect);
      } else {
        throw new IllegalStateException(
            "Nodes of this class cannot be inserted into a subgraph context: %s"
                .formatted(node.getClass().getSimpleName()));
      }
    }

    if ((blockStart == null) != (blockEnd == null)) {
      throw new IllegalStateException(
          "blockStart and blockEnd must be both set or not set @ " + root.location());
    }
    if (blockStart != null) {
      ctx.controlBlock = new ControlBlock(blockStart, blockEnd);
    }

    if (!sideEffects.isEmpty()) {
      ctx.sideEffects = sideEffects;
    }
    return ctx;
  }

  SubgraphContext setSideEffects(NodeList<SideEffectNode> sideEffects) {
    if (this.sideEffects != null) {
      throw new IllegalStateException("SideEffects already set to: %s".formatted(this.sideEffects));
    }
    this.sideEffects = sideEffects;
    return this;
  }

  @Nullable
  ControlBlock controlBlock() {
    return controlBlock;
  }

  @Nullable
  NodeList<SideEffectNode> sideEffects() {
    return sideEffects;
  }

  NodeList<SideEffectNode> sideEffectsOrEmptyList() {
    return sideEffects == null ? new NodeList<SideEffectNode>() : sideEffects;
  }

  boolean hasControlBlock() {
    return controlBlock != null;
  }

  boolean hasSideEffects() {
    return !sideEffectsOrEmptyList().isEmpty();
  }

  SubgraphContext ensureNoControlBlock() {
    if (hasControlBlock()) {
      throw new IllegalStateException(
          "expected control block to be null but was " + controlBlock + " @ "
              + root.location());
    }
    return this;
  }

  SubgraphContext ensureNoSideEffects() {
    if (sideEffects != null) {
      throw new IllegalStateException(
          "expected sideEffects to be null but was " + sideEffects + " @ " + root.location());
    }
    return this;
  }

  SubgraphContext ensureSideEffects() {
    if (sideEffects == null || sideEffects.isEmpty()) {
      throw new IllegalStateException(
          "expected sideEffects to exist, but it was " + sideEffects + " @ "
              + root.location());
    }
    return this;
  }
}
