package vadl.iss.passes.decode.dto;

public record RenderContext(int maxPatternNameLength,
                            int maxPatternBitLength,
                            int maxFormatNameLength,
                            int maxFormatBitLength,
                            int maxArgSetNameLength,
                            int maxFieldNameLength,
                            int maxFieldBitLength) {

  public static final RenderContext EMPTY = new RenderContext(0, 0, 0, 0, 0, 0, 0);
}
