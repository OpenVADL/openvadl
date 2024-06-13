package vadl.viam.graph.dependency;

import java.util.List;
import org.jetbrains.annotations.Nullable;
import vadl.javaannotations.viam.DataValue;
import vadl.types.DataType;
import vadl.viam.RegisterFile;
import vadl.viam.Resource;

public class ReadRegFileNode extends ReadResourceNode {

  @DataValue
  protected RegisterFile registerFile;

  public ReadRegFileNode(RegisterFile registerFile, @Nullable ExpressionNode address,
                         DataType type) {
    super(address, type);
    this.registerFile = registerFile;
  }

  @Override
  protected Resource resourceDefinition() {
    return registerFile;
  }

  @Override
  protected void collectData(List<Object> collection) {
    super.collectData(collection);
    collection.add(registerFile);
  }

}
