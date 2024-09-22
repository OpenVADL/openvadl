package vadl.configuration;

import java.nio.file.Path;
import vadl.pass.AbstractOutputFactory;
import vadl.pass.FileOutputFactory;

/**
 * This configuration holds information for all passes.
 */
public class GeneralConfiguration {
  private final Path outputPath;
  private final boolean doDump;

  /**
   * Defines how a file should be outputted.
   */
  private final AbstractOutputFactory outputFactory;

  public GeneralConfiguration(Path outputPath, boolean doDump) {
    this(outputPath, doDump, new FileOutputFactory());
  }

  public GeneralConfiguration(GeneralConfiguration generalConfig) {
    this(generalConfig.outputPath, generalConfig.doDump, generalConfig.outputFactory);
  }

  /**
   * Constructor.
   */
  public GeneralConfiguration(Path outputPath, boolean doDump,
                              AbstractOutputFactory outputFactory) {
    outputPath = outputPath.toAbsolutePath();
    this.outputPath = outputPath;
    this.doDump = doDump;
    this.outputFactory = outputFactory;
  }

  public Path outputPath() {
    return outputPath;
  }

  public boolean doDump() {
    return doDump;
  }

  public AbstractOutputFactory outputFactory() {
    return outputFactory;
  }
}
