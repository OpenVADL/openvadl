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
import vadl.types.DataType;
import vadl.types.SIntType;
import vadl.types.Type;
import vadl.types.UIntType;
import vadl.utils.Pair;
import vadl.viam.Constant;
import vadl.viam.Counter;
import vadl.viam.Definition;
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Instruction;
import vadl.viam.Memory;
import vadl.viam.Register;
import vadl.viam.RegisterFile;
import vadl.viam.Relocation;
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
import vadl.viam.graph.dependency.ReadMemNode;
import vadl.viam.graph.dependency.ReadRegFileNode;
import vadl.viam.graph.dependency.ReadRegNode;
import vadl.viam.graph.dependency.SelectNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.SignExtendNode;
import vadl.viam.graph.dependency.SliceNode;
import vadl.viam.graph.dependency.TruncateNode;
import vadl.viam.graph.dependency.WriteMemNode;
import vadl.viam.graph.dependency.WriteRegFileNode;
import vadl.viam.graph.dependency.WriteRegNode;
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

  Graph getGraph(Expr expr, String name) {
    var exprNode = fetch(expr);

    var graph = new Graph(name);
    ControlNode endNode = graph.addWithInputs(new ReturnNode(exprNode));
    endNode.setSourceLocation(expr.sourceLocation());
    ControlNode startNode = graph.add(new StartNode(endNode));
    startNode.setSourceLocation(expr.sourceLocation());
    return graph;
  }

  Graph getInstructionGraph(InstructionDefinition definition) {
    var graph = new Graph("%s Behavior".formatted(definition.identifier().name));
    currentGraph = graph;

    var stmtCtx = definition.behavior.accept(this);
    var sideEffects = stmtCtx.sideEffectsOrEmptyList();

    var end = graph.addWithInputs(new InstrEndNode(sideEffects));
    end.setSourceLocation(definition.sourceLocation());

    ControlNode startSuccessor = end;
    if (stmtCtx.hasControlBlock()) {
      var controlBlock = Objects.requireNonNull(stmtCtx.controlBlock());
      controlBlock.lastNode().setNext(end);
      startSuccessor = controlBlock.firstNode();
    }
    var start = new StartNode(startSuccessor);
    start.setSourceLocation(definition.sourceLocation());
    graph.addWithInputs(start);

    return graph;
  }

  Graph getInstructionSequenceGraph(Identifier identifier,
                                    InstructionSequenceDefinition definition) {
    var graph = new Graph("%s Behavior".formatted(identifier.name));
    currentGraph = graph;

    var end = graph.addWithInputs(new InstrEndNode(new NodeList<>()));
    end.setSourceLocation(definition.sourceLocation());

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
    start.setSourceLocation(definition.sourceLocation());
    graph.addWithInputs(start);

    return graph;
  }


  private <T extends vadl.viam.graph.Node> T addToGraph(T node) {
    if (!node.isActive()) {
      return Objects.requireNonNull(currentGraph).addWithInputs(node);
    }
    return node;
  }

  private Pair<BeginNode, BranchEndNode> buildBranch(@Nullable Statement stmt) {
    if (stmt == null) {
      var endNode = addToGraph(new BranchEndNode(new NodeList<>()));
      var beginNode = addToGraph(new BeginNode(endNode));
      return new Pair<>(beginNode, endNode);
    }

    var branchCtx = stmt.accept(this);

    var endNode = addToGraph(new BranchEndNode(branchCtx.sideEffectsOrEmptyList()));

    BeginNode beginNode;
    if (branchCtx.controlBlock() != null) {
      beginNode = new BeginNode(branchCtx.controlBlock().firstNode());
      branchCtx.controlBlock().lastNode().setNext(endNode);
    } else {
      beginNode = new BeginNode(endNode);
    }
    beginNode = addToGraph(beginNode);

    endNode.setSourceLocation(stmt.sourceLocation());
    beginNode.setSourceLocation(stmt.sourceLocation());
    return new Pair<>(beginNode, endNode);
  }

  private static BuiltInCall produceNeqToZero(ExpressionNode node) {
    var constNode = new ConstantNode(Constant.Value.of(0, (DataType) node.type()));
    constNode.setSourceLocation(node.sourceLocation());
    return BuiltInCall.of(BuiltInTable.NEQ, node, constNode);
  }


  private ExpressionNode fetch(Expr expr) {
    if (expressionCache.containsKey(expr)) {
      return expressionCache.get(expr);
    }

    var result = expr.accept(this);
    result.setSourceLocationIfNotSet(expr.sourceLocation());
    expressionCache.put(expr, result);
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
      computedTarget = Objects.requireNonNull(expr.symbolTable).requireAs(identifier, Node.class);
      innerName = identifier.name;
      fullName = identifier.name;
    } else if (expr instanceof IdentifierPath path) {
      computedTarget = Objects.requireNonNull(expr.symbolTable).findAs(path, Node.class);
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
    if (computedTarget instanceof FormatDefinition.TypedFormatField typedFormatField) {
      return new FieldRefNode(
          (Format.Field) viamLowering.fetch(typedFormatField).orElseThrow(),
          (DataType) Objects.requireNonNull(expr.type));
    }
    if (computedTarget instanceof FormatDefinition.RangeFormatField rangeFormatField) {
      return new FieldRefNode(
          (Format.Field) viamLowering.fetch(rangeFormatField).orElseThrow(),
          (DataType) Objects.requireNonNull(expr.type));
    }
    if (computedTarget instanceof FormatDefinition.DerivedFormatField derivedFormatField) {
      return new FieldAccessRefNode(
          (Format.FieldAccess) viamLowering.fetch(derivedFormatField).orElseThrow(),
          (DataType) Objects.requireNonNull(expr.type));
    }

    // Register
    if (computedTarget instanceof RegisterDefinition registerDefinition) {
      var register = (Register) viamLowering.fetch(registerDefinition).orElseThrow();
      return new ReadRegNode(
          register,
          (DataType) Objects.requireNonNull(expr.type),
          null);
    }

    // Counters
    if (computedTarget instanceof CounterDefinition counterDefinition) {
      if (counterDefinition.kind == CounterDefinition.CounterKind.PROGRAM) {
        var counter = (Counter.RegisterCounter) viamLowering.fetch(counterDefinition).orElseThrow();

        return new ReadRegNode(counter.registerRef(), (DataType) Objects.requireNonNull(expr.type),
            null);
      }
      throw new IllegalStateException("Unsupported counter kind: " + counterDefinition.kind);
    }

    // Let statement and expression
    if (computedTarget instanceof LetStatement letStatement) {
      return new LetNode(new LetNode.Name(innerName, letStatement.sourceLocation()),
          fetch(letStatement.valueExpr));
    }
    if (computedTarget instanceof LetExpr letExpr) {
      return new LetNode(new LetNode.Name(innerName, letExpr.sourceLocation()),
          fetch(letExpr.valueExpr));
    }

    // Parameter of a function
    if (computedTarget instanceof Parameter parameter) {
      var param = viamLowering.fetch(parameter).orElseThrow();
      return new FuncParamNode(param);
    }

    // Function call without arguments (and no parenthesis)
    if (computedTarget instanceof FunctionDefinition functionDefinition) {
      var function = (Function) viamLowering.fetch(functionDefinition).orElseThrow();
      return new FuncCallNode(function, new NodeList<>(), Objects.requireNonNull(expr.type));
    }

    // Builtin Call
    var matchingBuiltins = BuiltInTable.builtIns()
        .filter(b -> b.signature().argTypeClasses().isEmpty())
        .filter(b -> b.name().toLowerCase().equals(innerName))
        .toList();

    if (matchingBuiltins.size() == 1) {
      var builtin = matchingBuiltins.get(0);
      return new BuiltInCall(builtin, new NodeList<ExpressionNode>(),
          Objects.requireNonNull(expr.type));
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
    return new BuiltInCall(builtin, new NodeList<>(left, right), Objects.requireNonNull(expr.type));
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
        type);

    for (int i = 2; i < expr.expressions.size(); i++) {
      type = expr.type().equals(Type.string()) ? expr.type() :
          Type.bits(type.asDataType().bitWidth()
              + expr.expressions.get(i).type().asDataType().bitWidth());
      call = new BuiltInCall(concatBuiltin,
          new NodeList<>(call,
              expr.expressions.get(i).accept(this)),
          type);
    }

    return call;
  }

  @Override
  public ExpressionNode visit(IntegerLiteral expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(BinaryLiteral expr) {
    return new ConstantNode(Constant.Value.fromInteger(expr.number,
        (DataType) Objects.requireNonNull(expr.type)));
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
        Objects.requireNonNull(expr.type));
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
      var bitRange = Objects.requireNonNull(subCall.computedFormatFieldBitRange);
      var bitSlice =
          new Constant.BitSlice(new Constant.BitSlice.Part(bitRange.from(), bitRange.to()));
      var slice =
          new SliceNode(resultExpr, bitSlice,
              (DataType) Objects.requireNonNull(subCall.formatFieldType));
      resultExpr = visitSliceIndexCall(expr, slice, subCall.argsIndices);
    }

    return resultExpr;
  }

  private ExpressionNode visitSliceIndexCall(CallIndexExpr expr, ExpressionNode exprBeforeSlice,
                                             List<CallIndexExpr.Arguments> argumentsList) {
    if (argumentsList.isEmpty()) {
      return exprBeforeSlice;
    }

    var args = argumentsList.get(0);

    // A range slice
    if (!args.values.isEmpty() && args.values.get(0) instanceof RangeExpr rangeExpr) {
      var from = constantEvaluator.eval(rangeExpr.from).value().intValueExact();
      var to = constantEvaluator.eval(rangeExpr.to).value().intValueExact();
      var bitSlice = new Constant.BitSlice(new Constant.BitSlice.Part(from, to));
      var slice =
          new SliceNode(exprBeforeSlice, bitSlice, Type.bits(from - to + 1));
      return visitSliceIndexCall(expr, slice, argumentsList.subList(1, argumentsList.size()));
    }

    // A index (slice)
    var fromTo = constantEvaluator.eval(args.values.get(0)).value().intValueExact();
    var bitSlice = new Constant.BitSlice(new Constant.BitSlice.Part(fromTo, fromTo));
    var slice =
        new SliceNode(exprBeforeSlice, bitSlice, (DataType) expr.type());
    return visitSliceIndexCall(expr, slice, argumentsList.subList(1, argumentsList.size()));
  }

  @Override
  public ExpressionNode visit(CallIndexExpr expr) {

    List<Expr> firstArgs =
        !expr.argsIndices.isEmpty() ? expr.argsIndices.get(0).values : new ArrayList<>();

    // Builtin Call
    if (expr.computedBuiltIn != null) {
      var args = firstArgs.stream().map(this::fetch).toList();
      if (BuiltInTable.ASM_PARSER_BUILT_INS.contains(expr.computedBuiltIn)) {
        return new AsmBuiltInCall(expr.computedBuiltIn, new NodeList<>(args),
            Objects.requireNonNull(expr.type));
      }
      return new BuiltInCall(expr.computedBuiltIn, new NodeList<>(args),
          Objects.requireNonNull(expr.type));
    }

    // Function Call
    if (expr.computedTarget instanceof FunctionDefinition functionDefinition) {
      var args = firstArgs.stream().map(this::fetch).toList();
      var function = (Function) viamLowering.fetch(functionDefinition).orElseThrow();
      var funcCall =
          new FuncCallNode(function, new NodeList<>(args), Objects.requireNonNull(expr.type));
      var slicedNode = visitSliceIndexCall(expr, funcCall,
          expr.argsIndices.subList(1, expr.argsIndices.size()));
      return visitSubCall(expr, slicedNode);
    }

    // Relocation Call (similar to function call)
    if (expr.computedTarget instanceof RelocationDefinition relocationDefinition) {
      var args = firstArgs.stream().map(this::fetch).toList();
      var relocation = (Relocation) viamLowering.fetch(relocationDefinition).orElseThrow();
      var funcCall =
          new FuncCallNode(relocation, new NodeList<>(args), Objects.requireNonNull(expr.type));
      var slicedNode = visitSliceIndexCall(expr, funcCall,
          expr.argsIndices.subList(1, expr.argsIndices.size()));
      return visitSubCall(expr, slicedNode);
    }

    // Register file read
    if (expr.computedTarget instanceof RegisterFileDefinition) {
      var args = firstArgs.stream().map(this::fetch).toList();
      var regFile = (RegisterFile) viamLowering.fetch(expr.computedTarget).orElseThrow();
      var type = (DataType) Objects.requireNonNull(expr.type);
      var readRegFile = new ReadRegFileNode(regFile, args.get(0), type, null);
      var slicedNode = visitSliceIndexCall(expr, readRegFile,
          expr.argsIndices.subList(1, expr.argsIndices.size()));
      return visitSubCall(expr, slicedNode);
    }

    // Memory read
    if (expr.computedTarget instanceof MemoryDefinition memoryDefinition) {
      var args = firstArgs.stream().map(this::fetch).toList();
      var words = 1;
      if (expr.target instanceof SymbolExpr targetSymbol) {
        words = constantEvaluator.eval(targetSymbol.size).value().intValueExact();
      }
      var memory = (Memory) viamLowering.fetch(memoryDefinition).orElseThrow();
      var readMem = new ReadMemNode(memory, words, args.get(0),
          (DataType) Objects.requireNonNull(expr.type));
      var slicedNode = visitSliceIndexCall(expr, readMem,
          expr.argsIndices.subList(1, expr.argsIndices.size()));
      return visitSubCall(expr, slicedNode);
    }

    // Program counter read
    if (expr.computedTarget instanceof CounterDefinition counterDefinition) {
      // Calls like PC.next are translated to PC + 8 (if address is 8)
      var counter = (Counter.RegisterCounter) viamLowering.fetch(counterDefinition).orElseThrow();
      var counterType = (DataType) Objects.requireNonNull(counterDefinition.typeLiteral.type);

      var regRead = new ReadRegNode(counter.registerRef(),
          (DataType) Objects.requireNonNull(expr.type), null);

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

      var constant = new ConstantNode(Constant.Value.of(offset, counterType));
      return
          new BuiltInCall(BuiltInTable.ADD, new NodeList<>(constant, regRead), counterType);
    }

    // If nothing else, assume slicing and subcall
    var exprBeforeSubCall = fetch((Expr) expr.target);
    var result = visitSubCall(expr, exprBeforeSubCall);
    result = visitSliceIndexCall(expr, result, expr.argsIndices);
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
              (DataType) Objects.requireNonNull(expr.type)));
    }

    // check the different rules and apply them accordingly
    var source = fetch(expr.value);
    var sourceType = (DataType) Objects.requireNonNull(expr.value.type);
    var targetType = (DataType) Objects.requireNonNull(expr.type);
    if (sourceType.isTrivialCastTo(targetType)) {
      // match 1. rule: same bit representation
      // -> no casting needs to be applied
      return source;
    }
    if (targetType.getClass() == vadl.types.BoolType.class) {
      // match 2. rule: target type is bool
      // -> produce != 0 call
      //return new BuiltInCall
      return produceNeqToZero(source);
    }
    if (targetType.bitWidth() < sourceType.bitWidth()) {
      // match 3. rule: cast type bit-width is smaller than source type
      // -> create TruncateNode
      return new TruncateNode(source, targetType);
    }
    if (sourceType.getClass() == SIntType.class) {
      // match 4.
      // rule: source type is a signed integer
      // -> create sign extend node
      return new SignExtendNode(source, targetType);
    }
    if (sourceType.getClass() == BitsType.class
        && targetType.getClass() == SIntType.class) {
      // match 5.
      // rule: source type is a bits type and target type is SInt
      // -> create sign extend node
      return new SignExtendNode(source, targetType);
    }
    if (targetType.getClass() == UIntType.class
        || targetType.getClass() == BitsType.class
        || targetType.getClass() == SIntType.class
    ) {
      // match 5. rule: cast type is one of sint, uint, or bits
      return new ZeroExtendNode(source, targetType);
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
  public ExpressionNode visit(ExtendIdExpr expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(IdToStrExpr expr) {
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
  public SubgraphContext visit(AssignmentStatement statement) {
    var value = fetch(statement.valueExpression);

    if (statement.target instanceof CallIndexExpr callTarget) {
      // Register File Write
      if (callTarget.computedTarget instanceof RegisterFileDefinition regFileTarget) {
        var regFile = viamLowering.fetch(regFileTarget).orElseThrow();
        var address = callTarget.argsIndices.get(0).values.get(0).accept(this);
        var write = new WriteRegFileNode(
            (RegisterFile) regFile,
            address,
            value,
            null,
            null);
        write.setSourceLocation(statement.sourceLocation());
        write = Objects.requireNonNull(currentGraph).addWithInputs(write);
        return SubgraphContext.of(statement, write);
      }

      // Memory Write
      if (callTarget.computedTarget instanceof MemoryDefinition memoryTarget) {
        var memory = (Memory) viamLowering.fetch(memoryTarget).orElseThrow();
        var words = 1;
        if (callTarget.target instanceof SymbolExpr targetSymbol) {
          words = constantEvaluator.eval(targetSymbol.size).value().intValueExact();
        }
        var address = callTarget.argsIndices.get(0).values.get(0).accept(this);
        var write = new WriteMemNode(memory, words, address, value);
        write = Objects.requireNonNull(currentGraph).addWithInputs(write);
        return SubgraphContext.of(statement, write);
      }

      throw new IllegalStateException(
          "Call target not yet implemented " + callTarget.computedTarget);
    } else if (statement.target instanceof Identifier identifierExpr) {
      var computedTarget = Objects.requireNonNull(Objects.requireNonNull(identifierExpr.symbolTable)
          .requireAs(identifierExpr, vadl.ast.Node.class));

      // Register Write
      if (computedTarget instanceof RegisterDefinition registerDefinition) {
        var register = (Register) viamLowering.fetch(registerDefinition).orElseThrow();
        var write = new WriteRegNode(register, value, null);
        return SubgraphContext.of(statement, write);
      }

      // Counter (also register) Write
      if (computedTarget instanceof CounterDefinition counterDefinition) {
        var counter = (Counter.RegisterCounter) viamLowering.fetch(counterDefinition).orElseThrow();
        var write = new WriteRegNode(counter.registerRef(), value, null);
        return SubgraphContext.of(statement, write);
      }

      throw new IllegalStateException("Identifier targeting %s not yet implemented".formatted(
          computedTarget.getClass().getSimpleName()));
    }

    throw new IllegalStateException("unknown target expression: " + statement.target);
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
      throw new IllegalStateException("The behavior generator doesn't implement yet");
    }

    var target =
        (Instruction) viamLowering.fetch(Objects.requireNonNull(statement.instrDef)).orElseThrow();
    var fieldMap = Arrays.stream(target.encoding().nonEncodedFormatFields())
        .collect(Collectors.toMap(Definition::simpleName, f -> f));

    var argExprs = new NodeList<ExpressionNode>();
    var fields = new ArrayList<Format.Field>();

    for (var arg : statement.namedArguments) {
      fields.add(fieldMap.get(arg.name.name));
      argExprs.add(fetch(arg.value));
    }
    var call = new InstrCallNode(target, fields, argExprs);
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
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + statement.getClass().getSimpleName());
  }

  @Override
  public SubgraphContext visit(PlaceholderStatement statement) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + statement.getClass().getSimpleName());
  }

  @Override
  public SubgraphContext visit(RaiseStatement statement) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + statement.getClass().getSimpleName());
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
                    root.sourceLocation()));
          }
          blockStart = controlNode;
        }

        if ((node instanceof DirectionalNode directionalNode)
            && directionalNode.successors().count() == 0) {
          if (blockEnd != null && directionalNode.successors().count() == 0) {
            throw new IllegalStateException(
                "tried to add %s, but blockEnd already set: %s @%s".formatted(node, blockEnd,
                    root.sourceLocation()));
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
          "blockStart and blockEnd must be both set or not set @ " + root.sourceLocation());
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
              + root.sourceLocation());
    }
    return this;
  }

  SubgraphContext ensureNoSideEffects() {
    if (sideEffects != null) {
      throw new IllegalStateException(
          "expected sideEffects to be null but was " + sideEffects + " @ " + root.sourceLocation());
    }
    return this;
  }

  SubgraphContext ensureSideEffects() {
    if (sideEffects == null || sideEffects.isEmpty()) {
      throw new IllegalStateException(
          "expected sideEffects to exist, but it was " + sideEffects + " @ "
              + root.sourceLocation());
    }
    return this;
  }
}
