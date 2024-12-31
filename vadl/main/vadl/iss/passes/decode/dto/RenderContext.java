package vadl.iss.passes.decode.dto;

/**
 * The rendering context contains information about the maximum lengths of the different elements
 * that are rendered in the decode tree, such that the output can be formatted in a readable way.
 *
 * @param maxPatternNameLength the maximum length of the pattern name
 * @param maxPatternBitLength  the maximum length of the pattern bit representation
 * @param maxFormatNameLength  the maximum length of the format name
 * @param maxFormatBitLength   the maximum length of the format bit representation
 * @param maxArgSetNameLength  the maximum length of the argument set name
 * @param maxFieldNameLength   the maximum length of the field name
 * @param maxFieldBitLength    the maximum length of the field bit representation
 */
public record RenderContext(int maxPatternNameLength,
                            int maxPatternBitLength,
                            int maxFormatNameLength,
                            int maxFormatBitLength,
                            int maxArgSetNameLength,
                            int maxFieldNameLength,
                            int maxFieldBitLength) {

  public static final RenderContext EMPTY = new RenderContext(0, 0, 0, 0, 0, 0, 0);
}
