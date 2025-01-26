package vadl.ast;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import vadl.types.BuiltInTable;
import vadl.types.DataType;
import vadl.types.Type;
import vadl.viam.Constant;
import vadl.viam.RegisterFile;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.ControlNode;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.MergeNode;
import vadl.viam.graph.control.ReturnNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;
import vadl.viam.graph.dependency.FieldRefNode;
import vadl.viam.graph.dependency.SideEffectNode;
import vadl.viam.graph.dependency.WriteRegFileNode;


class BehaviorLowering implements StatementVisitor<SubgraphContext>, ExprVisitor<ExpressionNode> {
  private final ViamLowering viamLowering;

  @Nullable
  private Graph currentGraph;

  BehaviorLowering(ViamLowering generator) {
    this.viamLowering = generator;
  }

  Graph getGraph(Expr expr, String name) {
    var exprNode = expr.accept(this);

    // FIXME: Should we set the parent here too?
    var graph = new Graph(name);
    ControlNode endNode = graph.addWithInputs(new ReturnNode(exprNode));
    graph.add(new StartNode(endNode));

    return graph;
  }

  Graph getInstructionGraph(InstructionDefinition definition) {
    // FIXME: there should be a link to the generated definition but it's not ready yet so we cannot
    //  fetch it.
    var graph = new Graph("%s Behavior".formatted(definition.identifier().name));
    currentGraph = graph;

    var stmtCtx = definition.behavior.accept(this);
    var sideEffects = stmtCtx.sideEffects();

    var end = graph.add(new InstrEndNode(sideEffects));

    ControlNode startSuccessor = end;
    if (stmtCtx.hasControlBlock()) {
      var controlBlock = stmtCtx.controlBlock();
      controlBlock.lastNode().setNext(end);
      startSuccessor = controlBlock.firstNode();
    }
    graph.add(new StartNode(startSuccessor));

    return graph;
  }

  @Override
  public ExpressionNode visit(Identifier expr) {

    var computedTarget = Objects.requireNonNull(expr.symbolTable).resolveNode(expr.name);

    // Format field
    if (computedTarget instanceof FormatDefinition.TypedFormatField typedFormatField) {
      return new FieldRefNode(
          viamLowering.fetch(typedFormatField).orElseThrow(),
          (DataType) Objects.requireNonNull(expr.type));
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
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(BinaryExpr expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
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
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
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
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(CallExpr expr) {
    var args = expr.flatArgs().stream().map(a -> a.accept(this)).toList();

    if (expr.computedBuiltIn != null) {
      return new BuiltInCall(expr.computedBuiltIn, new NodeList<>(args),
          Objects.requireNonNull(expr.type));
    }

    throw new IllegalStateException("cannot handle many calls yet");
  }

  @Override
  public ExpressionNode visit(IfExpr expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(LetExpr expr) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + expr.getClass().getSimpleName());
  }

  @Override
  public ExpressionNode visit(CastExpr expr) {
    // Shortcut for constant types
    if (expr.value.type instanceof ConstantType constType) {
      return new ConstantNode(
          Constant.Value.of(constType.getValue().longValueExact(),
              (DataType) Objects.requireNonNull(expr.type)));
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
    var rightExpr = statement.valueExpression.accept(this);

    if (statement.target instanceof CallExpr callTarget) {
      if (callTarget.computedTarget instanceof RegisterFileDefinition regFileTarget) {
        var regFile = viamLowering.fetch(regFileTarget).orElseThrow();
        var address = callTarget.flatArgs().get(0).accept(this);
        var read = new WriteRegFileNode(
            (RegisterFile) regFile,
            address,
            rightExpr,
            null,
            null);
        read.setSourceLocation(statement.sourceLocation());
        Objects.requireNonNull(currentGraph).addWithInputs(read);
        return SubgraphContext.of(read, read);
      }

    } else if (statement.target instanceof Identifier identifierExpr) {
      throw new IllegalStateException("Identifier target not yet implemented" + statement.target);
    }

    throw new IllegalStateException("unknown target expression: " + statement.target);
  }

  @Override
  public SubgraphContext visit(BlockStatement statement) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + statement.getClass().getSimpleName());
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
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + statement.getClass().getSimpleName());
  }

  @Override
  public SubgraphContext visit(InstructionCallStatement statement) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + statement.getClass().getSimpleName());
  }

  @Override
  public SubgraphContext visit(LetStatement statement) {
    throw new RuntimeException(
        "The behavior generator doesn't implement yet: " + statement.getClass().getSimpleName());
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

  static SubgraphContext of(Node root, Node... nodes) {
    return SubgraphContext.of(root, List.of(nodes));
  }

  static SubgraphContext of(Node root, List<Node> nodes) {
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

  ControlBlock controlBlock() {
    return Objects.requireNonNull(controlBlock);
  }

  NodeList<SideEffectNode> sideEffects() {
    return Objects.requireNonNull(sideEffects);
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
