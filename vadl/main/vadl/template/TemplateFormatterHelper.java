package vadl.template;

/**
 * This class contains helper function which can be used in the templates.
 */
public class TemplateFormatterHelper {
  public String mapBeOrLeFromIsBigEndian(boolean isBigEndian) {
    return isBigEndian ? "be" : "le";
  }
}
