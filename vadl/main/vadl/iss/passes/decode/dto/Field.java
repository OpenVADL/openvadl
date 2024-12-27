package vadl.iss.passes.decode.dto;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import vadl.viam.Definition;

public final class Field
    implements Renderable, SourceMapping {

  private final Definition source;
  private final List<FieldSlice> slices;

  @Nullable
  private final String decodeFunction;

  @Nullable
  private String name;

  public Field(Definition source, List<FieldSlice> slices,
               @Nullable String decodeFunction) {
    this.source = source;
    this.slices = slices;
    this.decodeFunction = decodeFunction;
  }

  @Override
  public String render(RenderContext context) {
    final var sb = new StringBuilder();
    sb.append("%").append(name);

    // Insert padding to align the field slices
    if (name != null) {
      sb.append(" ".repeat(context.maxFieldNameLength() - name.length()));
    }

    for (var slice : slices) {
      sb.append(" ").append(slice.render(context));
    }

    if (decodeFunction != null) {
      int sWidth = slices.stream().mapToInt(f -> f.render(RenderContext.EMPTY).length()).sum() +
          slices.size() - 1;
      sb.append(" ".repeat(context.maxFieldBitLength() - sWidth));
      sb.append(" !function=").append(decodeFunction);
    }
    return sb.toString();
  }

  @Override
  public Definition getSource() {
    return source;
  }

  public List<FieldSlice> getSlices() {
    return slices;
  }

  @Nullable
  public String getDecodeFunction() {
    return decodeFunction;
  }

  @Nullable
  public String getName() {
    return name;
  }

  public void setName(@Nonnull String name) {
    this.name = name;
  }
}