package vadl.lcb.codegen;

import vadl.viam.graph.Graph;

/**
 * Generates Cpp Code given a behavior {@link Graph}.
 * This class will be used to generate the {@code AccessFunction}.
 */
public class DecodingCodeGenerator extends EncoderDecoderCodeGenerator {
  @Override
  protected String getPrefix() {
    return getFunctionPrefix();
  }


  /**
   * The generator generates a function with a name. This function returns the prefix.
   *
   * @return a string for a function prefix.
   */
  public static String getFunctionPrefix() {
    return "decode";
  }
}
