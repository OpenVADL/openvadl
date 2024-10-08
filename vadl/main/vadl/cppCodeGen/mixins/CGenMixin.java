package vadl.cppCodeGen.mixins;

import java.io.StringWriter;
import vadl.viam.graph.Node;

/**
 * The CGenMixin is the interface that is extended by every other C generation mixin.
 *
 * <p>It defines methods that can be used by the mixin to interact with the
 * C generator.
 */
public interface CGenMixin {

  /**
   * The generation method that takes a node and writes generated code to the
   * writer.
   * This must be implemented by the specific {@link vadl.cppCodeGen.CodeGenerator}
   */
  void gen(Node node);

  /**
   * The writer to write the generated C source code.
   * This is provided by the {@link vadl.cppCodeGen.CodeGenerator}.
   */
  StringWriter writer();
}
