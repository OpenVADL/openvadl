package vadl.viam.passes.htmlDump;

import java.util.Map;
import javax.annotation.Nullable;
import vadl.pass.PassKey;
import vadl.viam.Definition;
import vadl.viam.DefinitionVisitor;

public interface InfoSupplier {

  @Nullable
  Info produce(Definition def, Map<PassKey, Object> passResults);
  
}
