package vadl.oop;

import vadl.viam.Encoding;
import vadl.viam.Function;
import vadl.viam.graph.Graph;
import vadl.viam.graph.control.ReturnNode;

/**
 * Generates Cpp Code given a behavior {@link Graph}.
 * This class will be used to generate the {@link Encoding}.
 */
public class OopGenerator {
  public String generateEncoding(Function encoding) {
    String code = "";
    var returnNode = encoding.behavior().getNodes(ReturnNode.class).findFirst().get();


    return code;
  }
}
