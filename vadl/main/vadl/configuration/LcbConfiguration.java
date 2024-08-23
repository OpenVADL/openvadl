package vadl.configuration;

import vadl.gcb.valuetypes.ProcessorName;

/**
 * This record defines some lcb specific LCB configuration.
 */
public class LcbConfiguration extends GcbConfiguration {
  private final ProcessorName processorName;

  public LcbConfiguration(GeneralConfiguration gcbConfiguration, ProcessorName processorName) {
    super(gcbConfiguration);
    this.processorName = processorName;
  }

  public static LcbConfiguration from(GcbConfiguration gcbConfiguration,
                                      ProcessorName processorName) {
    return new LcbConfiguration(gcbConfiguration, processorName);
  }

  public ProcessorName processorName() {
    return processorName;
  }
}
