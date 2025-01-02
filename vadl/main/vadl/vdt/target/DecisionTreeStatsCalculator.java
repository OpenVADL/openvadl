package vadl.vdt.target;

import java.util.Objects;
import vadl.vdt.model.InnerNode;
import vadl.vdt.model.LeafNode;
import vadl.vdt.model.Node;
import vadl.vdt.model.Visitor;
import vadl.vdt.target.dto.DecisionTreeStatistics;

public class DecisionTreeStatsCalculator implements Visitor<DecisionTreeStatistics> {

  public DecisionTreeStatistics calculate(Node node) {
    return Objects.requireNonNull(node.accept(this));
  }

  @Override
  public DecisionTreeStatistics visit(InnerNode node) {

    var children = node.children();

    var stats = new DecisionTreeStatistics();

    stats.setNumberOfNodes(1);
    stats.setNumberOfLeafNodes(0);

    stats.setMaxDepth(0);
    stats.setMinDepth(Integer.MAX_VALUE);
    stats.setAvgDepth(0);

    for (Node child : children) {
      DecisionTreeStatistics childStats = Objects.requireNonNull(child.accept(this));

      stats.setNumberOfNodes(stats.getNumberOfNodes() + childStats.getNumberOfNodes());
      stats.setNumberOfLeafNodes(stats.getNumberOfLeafNodes() + childStats.getNumberOfLeafNodes());
      stats.setMaxDepth(Math.max(stats.getMaxDepth(), childStats.getMaxDepth()));
      stats.setMinDepth(Math.min(stats.getMinDepth(), childStats.getMinDepth()));

      double avgDepth = (childStats.getAvgDepth() + 1) * childStats.getNumberOfLeafNodes();
      stats.setAvgDepth(stats.getAvgDepth() + avgDepth);
    }

    stats.setMinDepth(stats.getMinDepth() + 1);
    stats.setMaxDepth(stats.getMaxDepth() + 1);
    stats.setAvgDepth(stats.getAvgDepth() / stats.getNumberOfLeafNodes());

    return stats;
  }

  @Override
  public DecisionTreeStatistics visit(LeafNode node) {
    var stats = new DecisionTreeStatistics();
    stats.setNumberOfNodes(1);
    stats.setNumberOfLeafNodes(1);
    stats.setMaxDepth(0);
    stats.setMinDepth(0);
    stats.setAvgDepth(0);
    return stats;
  }
}
