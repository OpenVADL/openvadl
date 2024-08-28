package vadl.viam.matching.impl;

import vadl.viam.graph.GraphNodeVisitor;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.BranchEndNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.InstrCallNode;
import vadl.viam.graph.control.InstrEndNode;
import vadl.viam.graph.control.ReturnNode;
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
import vadl.viam.matching.Matcher;

/**
 * This class checks if it has any child matches the given {@link Matcher}.
 */
public class AnyChildMatcher implements Matcher {

  private final Matcher matcher;

  public AnyChildMatcher(Matcher matcher) {
    this.matcher = matcher;
  }

  @Override
  public boolean matches(Node node) {
    var visitor = new AnyChildVisitor(matcher);
    node.accept(visitor);

    return visitor.matched;
  }

  static class AnyChildVisitor implements GraphNodeVisitor {

    private final Matcher matcher;
    public boolean matched = false;

    AnyChildVisitor(Matcher matcher) {
      this.matcher = matcher;
    }

    @Override
    public void visit(ConstantNode node) {
      matched = matcher.matches(node);
    }

    @Override
    public void visit(BuiltInCall node) {
      for (var arg : node.arguments()) {
        var x = matcher.matches(arg);
        if (x) {
          matched = true;
          return;
        }
      }
    }

    @Override
    public void visit(WriteRegNode node) {
      matched |= matcher.matches(node);
    }

    @Override
    public void visit(WriteRegFileNode node) {
      matched |= matcher.matches(node);
    }

    @Override
    public void visit(WriteMemNode node) {
      matched |= matcher.matches(node);
    }

    @Override
    public void visit(SliceNode node) {
      matched |= matcher.matches(node.value());
      visit(node.value());
    }

    @Override
    public void visit(SelectNode node) {
      matched |= matcher.matches(node.condition());
      matched |= matcher.matches(node.falseCase());
      matched |= matcher.matches(node.trueCase());
      visit(node.condition());
      visit(node.falseCase());
      visit(node.trueCase());
    }

    @Override
    public void visit(ReadRegNode node) {
      matched |= matcher.matches(node);
    }

    @Override
    public void visit(ReadRegFileNode node) {
      matched |= matcher.matches(node);
    }

    @Override
    public void visit(ReadMemNode node) {
      matched |= matcher.matches(node);
    }

    @Override
    public void visit(LetNode node) {
      matched |= matcher.matches(node.expression());
      visit(node.expression());
    }

    @Override
    public void visit(FuncParamNode node) {
      matched |= matcher.matches(node);
    }

    @Override
    public void visit(FuncCallNode node) {
      matched |= matcher.matches(node);
    }

    @Override
    public void visit(FieldRefNode node) {
      matched |= matcher.matches(node);
    }

    @Override
    public void visit(FieldAccessRefNode node) {
      matched |= matcher.matches(node);
    }

    @Override
    public void visit(AbstractBeginNode node) {
      matched |= matcher.matches(node);
    }

    @Override
    public void visit(InstrEndNode node) {
      matched |= matcher.matches(node);
    }

    @Override
    public void visit(ReturnNode node) {
      matched |= matcher.matches(node);
    }

    @Override
    public void visit(BranchEndNode node) {
      matched |= matcher.matches(node);
    }

    @Override
    public void visit(InstrCallNode node) {
      matched |= matcher.matches(node);
    }

    @Override
    public void visit(IfNode node) {
      matched |= matcher.matches(node.condition());
      matched |= matcher.matches(node.falseBranch());
      matched |= matcher.matches(node.trueBranch());
      visit(node.condition());
      visit(node.falseBranch());
      visit(node.trueBranch());
    }

    @Override
    public void visit(ZeroExtendNode node) {
      matched |= matcher.matches(node);
      visit(node.value());
    }

    @Override
    public void visit(SignExtendNode node) {
      matched |= matcher.matches(node);
      visit(node.value());
    }

    @Override
    public void visit(TruncateNode node) {
      matched |= matcher.matches(node);
      visit(node.value());
    }

    @Override
    public void visit(ExpressionNode node) {
      node.accept(this);
    }

    @Override
    public void visit(SideEffectNode sideEffectNode) {
      sideEffectNode.accept(this);
    }

    public void visit(Node node) {
      node.accept(this);
    }
  }
}
