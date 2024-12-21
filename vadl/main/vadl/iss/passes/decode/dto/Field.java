package vadl.iss.passes.decode.dto;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import vadl.viam.Format;

public final class Field
    implements Renderable, SourceMapping {

  private final Format.Field source;
  private final List<FieldSlice> slices;

  @Nullable
  private final String decodeFunction;

  @Nullable
  private String name;

  public Field(Format.Field source, List<FieldSlice> slices,
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
      sb.append(" !function=").append(decodeFunction);
    }
    return sb.toString();
  }

  @Override
  public Format.Field getSource() {
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