package vadl.lcb.codegen;

import vadl.viam.Encoding;
import vadl.viam.graph.Graph;

/**
 * Generates Cpp Code given a behavior {@link Graph}.
 * This class will be used to generate the {@link Encoding}.
 */
public class EncodingCodeGenerator extends EncoderDecoderCodeGenerator {


  @Override
  protected String getPrefix() {
    return "encode";
  }

}
