package vadl.pass;

import java.io.StringWriter;
import java.io.Writer;
import org.jetbrains.annotations.Nullable;
import vadl.configuration.GeneralConfiguration;

/**
 * Creates a factory which creates a {@link StringWriter}.
 */
public class StringOutputFactory extends AbstractOutputFactory {
  private StringWriter stringWriter = new StringWriter();

  @Override
  public Writer createWriter(GeneralConfiguration configuration, String subDir,
                             String outputPath) {
    // We have to reset the string writer because every pass writes to this,
    // but we only want the last execution for testing.
    stringWriter = new StringWriter();
    return stringWriter;
  }

  @Nullable
  public StringWriter getLastStringWriter() {
    return stringWriter;
  }
}
