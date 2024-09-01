package vadl.lcb.codegen.encoding;

import vadl.viam.graph.Graph;

/**
 * Generates Cpp Code given a behavior {@link Graph}.
 * This class will be used to generate the {@code AccessFunction}.
 */
public class DecodingCodeGenerator extends EncoderDecoderCodeGenerator {

  public static String generateFunctionName(String rawName) {
    return rawName.trim() + "_" + "decode";
  }


  @Override
  protected String getFunctionName(String rawName) {
    return generateFunctionName(rawName);
  }
}
