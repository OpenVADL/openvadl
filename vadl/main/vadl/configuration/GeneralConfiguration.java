package vadl.configuration;

import vadl.pass.AbstractOutputFactory;
import vadl.pass.FileOutputFactory;

/**
 * This configuration holds information for all passes.
 */
public class GeneralConfiguration {
  private final String outputPath;
  private final boolean doDump;

  /**
   * Defines how a file should be outputted.
   */
  private final AbstractOutputFactory outputFactory;

  public GeneralConfiguration(String outputPath, boolean doDump) {
    this(outputPath, doDump, new FileOutputFactory());
  }

  public GeneralConfiguration(String outputPath, boolean doDump,
                              AbstractOutputFactory outputFactory) {
    this.outputPath = outputPath;
    this.doDump = doDump;
    this.outputFactory = outputFactory;
  }

  public String outputPath() {
    return outputPath;
  }

  public boolean doDump() {
    return doDump;
  }

  public AbstractOutputFactory outputFactory() {
    return outputFactory;
  }
}
