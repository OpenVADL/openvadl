package vadl.lcb.passes.llvmLowering.tablegen.lowering;

import java.io.StringWriter;
import java.util.Objects;
import vadl.lcb.passes.llvmLowering.LlvmNodeLowerable;
import vadl.lcb.passes.llvmLowering.model.LlvmBrCcSD;
import vadl.lcb.passes.llvmLowering.model.LlvmBrCondSD;
import vadl.lcb.passes.llvmLowering.model.LlvmFieldAccessRefNode;
import vadl.lcb.passes.llvmLowering.model.LlvmFrameIndexSD;
import vadl.lcb.passes.llvmLowering.model.LlvmStore;
import vadl.lcb.passes.llvmLowering.model.LlvmTruncStore;
import vadl.lcb.passes.llvmLowering.model.LlvmTypeCastSD;
import vadl.lcb.passes.llvmLowering.strategies.LlvmLoweringStrategy;
import vadl.lcb.passes.llvmLowering.strategies.visitors.TableGenNodeVisitor;
import vadl.viam.Constant;
import vadl.viam.graph.Node;
import vadl.viam.graph.NodeList;
import vadl.viam.graph.control.AbstractBeginNode;
import vadl.viam.graph.control.EndNode;
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

/**
 * Visitor for generating TableGen patterns.
 * The goal is to generate S-expressions with the same the
 * variable naming.
 */
public class TableGenPatternPrinterVisitor
    implements TableGenNodeVisitor {
  protected final StringWriter writer = new StringWriter();

  public String getResult() {
    return writer.toString();
  }

  @Override
  public void visit(Node node) {
    node.accept(this);
  }

  @Override
  public void visit(ConstantNode node) {
    node.ensure(node.constant() instanceof Constant.Value
        || node.constant() instanceof Constant.Str, "constant must be value or string");
    if (node.constant() instanceof Constant.Value constant) {
      writer.write(constant.intValue());
    } else if (node.constant() instanceof Constant.Str str) {
      writer.write(str.value());
    }
  }

  @Override
  public void visit(BuiltInCall node) {
    writer.write("(");
    if (node instanceof LlvmNodeLowerable lowerable) {
      writer.write(lowerable.lower() + " ");
    } else {
      writer.write(node.builtIn().operator() + " ");
    }

    joinArgumentsWithComma(node.arguments());

    writer.write(")");
  }

  @Override
  public void visit(WriteRegNode writeRegNode) {

  }

  @Override
  public void visit(WriteRegFileNode writeRegFileNode) {

  }

  @Override
  public void visit(WriteMemNode writeMemNode) {

  }

  @Override
  public void visit(SliceNode sliceNode) {

  }

  @Override
  public void visit(SelectNode selectNode) {

  }

  @Override
  public void visit(ReadRegNode readRegNode) {

  }

  @Override
  public void visit(ReadRegFileNode readRegFileNode) {
    var operand = LlvmLoweringStrategy.generateTableGenInputOutput(readRegFileNode);
    writer.write(operand.render());
  }

  @Override
  public void visit(ReadMemNode readMemNode) {

  }

  @Override
  public void visit(LetNode letNode) {

  }

  @Override
  public void visit(FuncParamNode funcParamNode) {

  }

  @Override
  public void visit(FuncCallNode funcCallNode) {

  }

  @Override
  public void visit(FieldRefNode fieldRefNode) {

  }


  @Override
  public void visit(LlvmFieldAccessRefNode fieldAccessRefNode) {
    var operand = LlvmLoweringStrategy.generateTableGenInputOutput(fieldAccessRefNode);
    writer.write(operand.render());
  }

  @Override
  public void visit(LlvmBrCondSD node) {
    writer.write("(");
    writer.write(node.lower() + " ");
    visit(node.condition());
    writer.write(", ");
    visit(node.immOffset());
    writer.write(")");
  }

  @Override
  public void visit(LlvmTypeCastSD node) {
    writer.write("(" + node.lower() + " ");
    visit(node.value());
    writer.write(")");
  }

  @Override
  public void visit(LlvmTruncStore node) {
    writer.write("(" + node.lower() + " ");
    visit(node.value());
    writer.write(", ");
    visit(Objects.requireNonNull(node.address()));
    writer.write(")");
  }

  @Override
  public void visit(LlvmStore node) {
    writer.write("(" + node.lower() + " ");
    visit(node.value());
    writer.write(", ");
    visit(Objects.requireNonNull(node.address()));
    writer.write(")");
  }

  @Override
  public void visit(FieldAccessRefNode fieldAccessRefNode) {

  }

  @Override
  public void visit(AbstractBeginNode abstractBeginNode) {

  }

  @Override
  public void visit(InstrEndNode instrEndNode) {

  }

  @Override
  public void visit(ReturnNode returnNode) {

  }

  @Override
  public void visit(EndNode endNode) {

  }

  @Override
  public void visit(InstrCallNode instrCallNode) {

  }

  @Override
  public void visit(IfNode ifNode) {

  }

  @Override
  public void visit(ZeroExtendNode node) {

  }

  @Override
  public void visit(SignExtendNode node) {

  }

  @Override
  public void visit(TruncateNode node) {

  }

  @Override
  public void visit(LlvmBrCcSD node) {
    writer.write("(");
    writer.write(node.lower() + " (" + node.condition() + " ");
    visit(node.first());
    writer.write(", ");
    visit(node.second());
    writer.write("), ");
    visit(node.immOffset());
    writer.write(")");
  }

  @Override
  public void visit(ExpressionNode expressionNode) {
    expressionNode.accept(this);
  }

  @Override
  public void visit(SideEffectNode sideEffectNode) {
    sideEffectNode.accept(this);
  }

  protected void joinArgumentsWithComma(NodeList<ExpressionNode> args) {
    for (int i = 0; i < args.size(); i++) {
      visit(args.get(i));

      if (i < args.size() - 1) {
        writer.write(", ");
      }
    }
  }
}
