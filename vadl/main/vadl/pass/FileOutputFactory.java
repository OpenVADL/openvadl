package vadl.pass;

import static vadl.viam.ViamError.ensure;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import vadl.configuration.GeneralConfiguration;

/**
 * Creates a factory which creates a {@link FileWriter}.
 */
public class FileOutputFactory extends AbstractOutputFactory {
  @Override
  public Writer createWriter(GeneralConfiguration configuration, String subDir,
                             String outputPath) throws IOException {
    var file = new File(configuration.outputPath() + "/" + subDir + "/" + outputPath);

    if (!file.getParentFile().exists()) {
      ensure(file.getParentFile().mkdirs(), "Cannot create parent directories");
    }

    if (!file.exists()) {
      ensure(file.createNewFile(), "Cannot create new file");
    }

    return new FileWriter(file, Charset.defaultCharset());
  }
}
