package vadl.pass;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import vadl.configuration.GeneralConfiguration;

/**
 * A factory pattern to change the output of a file which will be emitted by a {@link Pass}.
 * This is useful for testing a generator. When rendering a file, we need to check the content
 * of the file. It is simpler to change the output to a {@link StringWriter}.
 */
public abstract class AbstractOutputFactory {
  /**
   * Create {@link Writer} based on the {@code outputPath}.
   */
  public abstract Writer createWriter(GeneralConfiguration configuration, String subDir,
                                      String outputPath) throws IOException;
}
