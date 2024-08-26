package vadl.viam.passes.sideeffect_condition;

import static vadl.utils.GraphUtils.getSingleNode;

import javax.annotation.Nullable;
import vadl.types.BuiltInTable;
import vadl.viam.Constant;
import vadl.viam.graph.Graph;
import vadl.viam.graph.Node;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.AbstractEndNode;
import vadl.viam.graph.control.BeginNode;
import vadl.viam.graph.control.ControlSplitNode;
import vadl.viam.graph.control.DirectionalNode;
import vadl.viam.graph.control.EndNode;
import vadl.viam.graph.control.IfNode;
import vadl.viam.graph.control.MergeNode;
import vadl.viam.graph.control.StartNode;
import vadl.viam.graph.dependency.BuiltInCall;
import vadl.viam.graph.dependency.ConstantNode;
import vadl.viam.graph.dependency.ExpressionNode;

public class SideEffectConditionResolver {

  public static void run(Graph behavior) {
    new SideEffectConditionResolver()
        .resolve(behavior);
  }

  private void resolve(Graph behavior) {
    var start = getSingleNode(behavior, StartNode.class);

    resolveBranch(
        start,
        new ConstantNode(Constant.Value.of(true))
    );
  }

  private @Nullable MergeNode resolveBranch(AbstractBeginNode beginNode,
                                            ExpressionNode branchCondition) {
    Node current = beginNode;

    while (true) {
      if (current instanceof AbstractEndNode endNode) {
        var graph = endNode.graph();
        endNode.ensure(graph != null,
            "Node is not active, but control flow must be stable for SideEffectConditionResolver");
        for (var sideEffect : endNode.sideEffects) {
          var cond = branchCondition.isActive() ? branchCondition :
              graph.addWithInputs(branchCondition);
          sideEffect.setCondition(cond);
        }

        return endNode.usages()
            .filter(user -> user instanceof MergeNode)
            .map(MergeNode.class::cast)
            .findAny()
            .orElse(null);

      } else if (current instanceof ControlSplitNode splitNode) {
        splitNode.ensure(splitNode instanceof IfNode,
            "SideEffectConditionResolver not implemented for ControlSplitNode %s",
            splitNode.getClass());
        var ifNode = (IfNode) current;

        // the condition for the true branch is the given branch condition and the
        // if condition
        var trueCondition = BuiltInCall.of(BuiltInTable.AND, branchCondition, ifNode.condition);
        var trueMergeNode = resolveBranch(beginNode, trueCondition);
        // the condition for the false branch is the given branch condition and the
        // negation of the if condition
        var falseCondition = BuiltInCall.of(BuiltInTable.AND,
            branchCondition,
            BuiltInCall.of(BuiltInTable.NEG, ifNode.condition)
        );
        var falseMergeNode = resolveBranch(beginNode, falseCondition);

        ifNode.ensure(trueMergeNode == falseMergeNode,
            "Branches of node don't result in the same merge node");
        ifNode.ensure(trueMergeNode != null,
            "Couldn't find merge node for true branch");
        current = trueMergeNode;
      } else if (current instanceof DirectionalNode directionalNode) {
        current = directionalNode.next();
      } else {
        current.ensure(false,
            "Not an expected node in the SideEffectConditionResolver. You want to implement it.");
      }
    }
  }


}
