package vadl.viam.graph.dependency;

import java.util.List;
import vadl.javaannotations.viam.DataValue;
import vadl.types.BuiltInTable.BuiltIn;
import vadl.types.Type;
import vadl.viam.graph.NodeList;

public class BuiltInCall extends AbstractFunctionCall {

  @DataValue
  protected BuiltIn builtIn;

  public BuiltInCall(BuiltIn builtIn, NodeList<ExpressionNode> args, Type type) {
    super(args, type);
    this.builtIn = builtIn;
  }

//  @Override
//  public Type type() {
//    var argTypes = args.toTypeList();
//    ensureNonNull(argTypes, "Type args cannot be null");
//    return builtIn.returnType(argTypes);
//  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(builtIn);
  }
}
