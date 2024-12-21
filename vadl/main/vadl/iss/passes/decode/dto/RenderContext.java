package vadl.iss.passes.decode.dto;

public record RenderContext(int maxPatternNameLength,
                            int maxPatternBitLength,
                            int maxFormatNameLength,
                            int maxFormatBitLength,
                            int maxArgSetNameLength,
                            int maxFieldNameLength) {
}
