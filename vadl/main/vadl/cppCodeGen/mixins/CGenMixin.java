package vadl.cppCodeGen.mixins;

import java.io.StringWriter;
import vadl.viam.graph.Node;

public interface CGenMixin {

  void gen(Node node);

  StringWriter writer();
}
