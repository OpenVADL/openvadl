package vadl.lcb.codegen;

import vadl.viam.Encoding;
import vadl.viam.graph.Graph;

/**
 * Generates Cpp Code given a behavior {@link Graph}.
 * This class will be used to generate the {@link Encoding}.
 */
public class PredicateCodeGenerator extends EncoderDecoderCodeGenerator {

  public static String generateFunctionName(String rawName) {
    return rawName.trim() + "_" + "predicate";
  }

  @Override
  protected String getFunctionName(String rawName) {
    return generateFunctionName(rawName);
  }
}
