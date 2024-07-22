package vadl.viam.graph;

import vadl.viam.helper.TestNodes;

public interface TestGraphVisitor extends GraphNodeVisitor {
  void visit(TestNodes.PlainUnique plainUnique);

  void visit(TestNodes.Plain plain);

  void visit(TestNodes.WithInput withInput);

  void visit(TestNodes.WithSuccessor withSuccessor);

  void visit(TestNodes.WithTwoInputs withTwoInputs);

  void visit(TestNodes.WithNodeListInput withNodeListInput);

  void visit(TestNodes.WithData withData);
}
