package vadl.lcb.codegen;

import vadl.viam.graph.Graph;

/**
 * Generates Cpp Code given a behavior {@link Graph}.
 * This class will be used to generate the {@code AccessFunction}.
 */
public class DecodingCodeGenerator extends EncoderDecoderCodeGenerator {
  @Override
  protected String getSuffix() {
    return getFunctionSuffix();
  }


  /**
   * The generator generates a function with a name. This function returns the suffix.
   *
   * @return a string for a function suffix.
   */
  public static String getFunctionSuffix() {
    return "decode";
  }
}
