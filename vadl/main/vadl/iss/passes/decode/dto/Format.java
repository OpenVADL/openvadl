package vadl.iss.passes.decode.dto;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import vadl.viam.ViamError;

public final class Format implements Renderable, SourceMapping {

  private final vadl.viam.Format source;
  private final ArgumentSet args;
  private final List<Field> fields;

  @Nullable
  private String name;

  public Format(vadl.viam.Format source, ArgumentSet args, List<Field> fields) {
    this.source = source;
    this.args = args;
    this.fields = fields;
  }

  @Override
  public String render(RenderContext context) {

    final var sb = new StringBuilder();
    sb.append("@");
    sb.append(name);
    sb.append(" ");

    // Insert padding to align the format patterns
    if (name != null) {
      sb.append(" ".repeat(context.maxFormatNameLength() - name.length()));
    }

    sb.append(toBitPattern());

    sb.append(" &").append(args.getName());

    // Insert padding to align the fields
    if (args.getName() != null) {
      sb.append(" ".repeat(context.maxArgSetNameLength() - args.getName().length()));
    }

    for (Field f : fields) {
      sb.append(" ");

      final Optional<Field> argSetField = args.getFields().stream()
          .filter(i -> i.getSource().identifier.equals(f.getSource().identifier))
          .findFirst();

      if (argSetField.isEmpty()) {
        // This should not happen, it would be caused by an error in the construction
        throw new ViamError("Failed to resolve field mapping");
      }

      final String argName = argSetField.get().getSource().simpleName();
      if (argName != null && !argName.equals(f.getName())) {
        sb.append(argName).append("=");
      }
      sb.append("%").append(f.getName());
    }

    return sb.toString();

  }

  public String toBitPattern() {
    StringBuilder sb = new StringBuilder();
    IntStream.range(0, source.type().bitWidth())
        .forEach(i -> sb.append("."));
    return sb.toString();
  }

  @Override
  public vadl.viam.Format getSource() {
    return source;
  }

  public ArgumentSet getArgs() {
    return args;
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
