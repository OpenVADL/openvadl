package vadl.iss.passes.decode.dto;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import vadl.viam.Format;

public class ArgumentSet implements Renderable, SourceMapping {

  private final Format source;
  private final List<Field> fields;

  @Nullable
  private String name;

  public ArgumentSet(Format source, List<Field> fields) {
    this.source = source;
    this.fields = fields;
  }

  @Override
  public String render(RenderContext context) {
    final var sb = new StringBuilder();
    sb.append("&").append(name);

    // Insert padding to align the argument set patterns
    if (name != null) {
      sb.append(" ".repeat(context.maxArgSetNameLength() - name.length()));
    }

    for (var field : fields) {
      sb.append(" ").append(field.getSource().simpleName());
    }
    return sb.toString();
  }

  @Override
  public Format getSource() {
    return source;
  }

  public List<Field> getFields() {
    return fields;
  }

  @Nullable
  public String getName() {
    return name;
  }

  public void setName(@Nonnull String name) {
    this.name = name;
  }
}