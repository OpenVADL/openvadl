package vadl.configuration;

/**
 * This record defines some lcb specific LCB configuration.
 */
public class LcbConfiguration extends GcbConfiguration {
  public LcbConfiguration(GcbConfiguration gcbConfiguration) {
    super(gcbConfiguration);
  }

  public static LcbConfiguration from(GcbConfiguration gcbConfiguration) {
    return new LcbConfiguration(gcbConfiguration);
  }
}
