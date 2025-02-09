package vadl.ast;


import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
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
import vadl.viam.Format;
import vadl.viam.Function;
import vadl.viam.Memory;
import vadl.viam.RegisterFile;
import vadl.viam.graph.Graph;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.BeginNode;
import vadl.viam.graph.control.BranchEndNode;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.IfNode;
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


class BehaviorLowering implements StatementVisitor<SubgraphContext>, ExprVisitor<ExpressionNode> {
  private final ViamLowering viamLowering;
  private final ConstantEvaluator constantEvaluator = new ConstantEvaluator();

  private final IdentityHashMap<Expr, ExpressionNode> expressionCache = new IdentityHashMap<>();
  //private IdentityHashMap<Statement, SubgraphContext> statementCache = new IdentityHashMap<>();

  @Nullable
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

  @Override
  public ExpressionNode visit(Identifier expr) {

    var computedTarget = Objects.requireNonNull(expr.symbolTable).resolveNode(expr.name);

    // Constant
    if (computedTarget instanceof ConstantDefinition constant) {
      var value = constantEvaluator.eval(constant.value).toViamConstant();
      return new ConstantNode(value);
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

    // Registers and counters
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
      return new LetNode(new LetNode.Name(expr.name, letStatement.sourceLocation()),
          fetch(letStatement.valueExpr));
    }
    if (computedTarget instanceof LetExpr letExpr) {
      return new LetNode(new LetNode.Name(expr.name, letExpr.sourceLocation()),
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
        .filter(b -> b.name().toLowerCase().equals(expr.name))
        .toList();

    if (matchingBuiltins.size() == 1) {
      var builtin = matchingBuiltins.get(0);
      return new BuiltInCall(builtin, new NodeList<ExpressionNode>(),
          Objects.requireNonNull(expr.type));
    }

    throw new RuntimeException(
        "The behavior generator cannot resolve yet identifier '%s' which points to %s".formatted(
            expr.name,
            computedTarget == null ? "null" : computedTarget.getClass().getSimpleName()));
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

    // String concatination
    // This code looks so complicated because the concat function can only concat two arguments.
    // So the first two are directly concatinaed and all others are depend on the previous concat
    // node.
    var call = new BuiltInCall(BuiltInTable.CONCATENATE_STRINGS,
        new NodeList<ExpressionNode>(expr.expressions.get(0).accept(this),
            expr.expressions.get(1).accept(this)),
        Type.string());

    for (int i = 2; i < expr.expressions.size(); i++) {
      call = new BuiltInCall(BuiltInTable.CONCATENATE_STRINGS,
          new NodeList<ExpressionNode>(call,
              expr.expressions.get(i).accept(this)),
          Type.string());
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
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(BoolLiteral expr) {
    return new ConstantNode(Constant.Value.of(true));
  }

  @Override
  public ExpressionNode visit(StringLiteral expr) {
    return new ConstantNode(
        //Constant.Value.of(expr.value, (DataType) Objects.requireNonNull(expr.type)));
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
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(UnaryExpr expr) {
    var value = fetch(expr.operand);
    return new BuiltInCall(
        Objects.requireNonNull(expr.computedTarget),
        new NodeList<>(value),
        Objects.requireNonNull(expr.type));
  }

  @Override
  public ExpressionNode visit(CallExpr expr) {

    // Builtin Call
    if (expr.computedBuiltIn != null) {
      var args = expr.flatArgs().stream().map(this::fetch).toList();
      if (BuiltInTable.ASM_PARSER_BUILT_INS.contains(expr.computedBuiltIn)) {
        return new AsmBuiltInCall(expr.computedBuiltIn, new NodeList<>(args),
            Objects.requireNonNull(expr.type));
      }
      return new BuiltInCall(expr.computedBuiltIn, new NodeList<>(args),
          Objects.requireNonNull(expr.type));
    }

    // Function Call
    if (expr.computedTarget instanceof FunctionDefinition functionDefinition) {
      var args = expr.flatArgs().stream().map(this::fetch).toList();
      var function = (Function) viamLowering.fetch(functionDefinition).orElseThrow();
      return new FuncCallNode(new NodeList<>(args), function, Objects.requireNonNull(expr.type));
    }

    // Register file read
    if (expr.computedTarget instanceof RegisterFileDefinition) {
      var args = expr.flatArgs().stream().map(this::fetch).toList();
      var regFile = (RegisterFile) viamLowering.fetch(expr.computedTarget).orElseThrow();
      var type = (DataType) Objects.requireNonNull(expr.type);
      return new ReadRegFileNode(regFile, args.get(0), type, null);
    }

    // Memory read
    if (expr.computedTarget instanceof MemoryDefinition memoryDefinition) {
      var args = expr.flatArgs().stream().map(this::fetch).toList();
      var words = 1;
      if (expr.target instanceof SymbolExpr targetSymbol) {
        words = constantEvaluator.eval(targetSymbol.size).value().intValueExact();
      }
      var memory = (Memory) viamLowering.fetch(memoryDefinition).orElseThrow();
      return new ReadMemNode(memory, words, args.get(0),
          (DataType) Objects.requireNonNull(expr.type));
    }

    // Program counter read
    if (expr.computedTarget instanceof CounterDefinition counterDefinition) {
      // Calls like PC.next are translated to PC + 8 (if address is 8)
      var counter = (Counter.RegisterCounter) viamLowering.fetch(counterDefinition).orElseThrow();
      var counterType = (DataType) Objects.requireNonNull(counterDefinition.typeLiteral.type);

      var regRead = new ReadRegNode(counter.registerRef(),
          (DataType) Objects.requireNonNull(expr.type), null);

      // FIXME: Properly divide by memory bit width (8 bits for one byte is quite common)
      // What about bytes that aren't 8 bits?
      var scale =
          counterType.bitWidth() / 8;

      int offset = 0;
      for (var subcall : expr.subCalls) {
        var subcallName = subcall.id().name;
        if (subcallName.equals("next")) {
          offset += scale;
        } else {
          throw new IllegalStateException("unknown subcall: " + subcallName);
        }
      }

      var constant = new ConstantNode(Constant.Value.of(offset, counterType));
      return new BuiltInCall(BuiltInTable.ADD, new NodeList<>(constant, regRead), counterType);
    }

    // Slicing
    if (expr.flatArgs().size() == 1 && expr.flatArgs().get(0) instanceof RangeExpr rangeExpr) {
      var value = fetch((Expr) expr.target);
      var from = constantEvaluator.eval(rangeExpr.from).value().intValueExact();
      var to = constantEvaluator.eval(rangeExpr.to).value().intValueExact();
      var slice = new Constant.BitSlice(new Constant.BitSlice.Part(from, to));
      return new SliceNode(value, slice, (DataType) Objects.requireNonNull(expr.type));
    }

    throw new IllegalStateException("Cannot handle call to %s yet".formatted(expr.computedTarget));
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
    return fetch(expr);
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
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
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

    if (statement.target instanceof CallExpr callTarget) {
      // Register Write
      if (callTarget.computedTarget instanceof RegisterFileDefinition regFileTarget) {
        var regFile = viamLowering.fetch(regFileTarget).orElseThrow();
        var address = callTarget.flatArgs().get(0).accept(this);
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
        var address = callTarget.flatArgs().get(0).accept(this);
        var write = new WriteMemNode(memory, words, address, value);
        write = Objects.requireNonNull(currentGraph).addWithInputs(write);
        return SubgraphContext.of(statement, write);
      }

      throw new IllegalStateException(
          "Call target not yet implemented " + callTarget.computedTarget);
    } else if (statement.target instanceof Identifier identifierExpr) {
      var computedTarget = Objects.requireNonNull(identifierExpr.symbolTable)
          .requireAs(identifierExpr, vadl.ast.Node.class);

      if (computedTarget instanceof CounterDefinition counterDefinition) {
        var counter = (Counter.RegisterCounter) viamLowering.fetch(counterDefinition).orElseThrow();
        var write = new WriteRegNode(counter.registerRef(), value, null);
        return SubgraphContext.of(statement, write);
      }

      throw new IllegalStateException("Identifier target not yet implemented" + statement.target);
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
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + statement.getClass().getSimpleName());
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
    if (sideEffects == null || sideEffects.size() == 0) {
      throw new IllegalStateException(
          "expected sideEffects to exist, but it was " + sideEffects + " @ "
              + root.sourceLocation());
    }
    return this;
  }
}
