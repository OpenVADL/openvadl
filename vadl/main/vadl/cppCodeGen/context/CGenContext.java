package vadl.cppCodeGen.context;

import static java.util.Objects.requireNonNull;

import com.google.errorprone.annotations.FormatMethod;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import vadl.types.DataType;
import vadl.types.Type;

public abstract class CGenContext<T> {

  private final Consumer<String> writer;

  public CGenContext(
      Consumer<String> writer
  ) {
    this.writer = writer;
  }

  public abstract CGenContext<T> gen(T node);

  public abstract String genToString(T node);

  public CGenContext<T> wr(String str) {
    writer.accept(str);
    return this;
  }

  @FormatMethod
  public CGenContext<T> wr(String fmt, Object... args) {
    writer.accept(String.format(fmt, args));
    return this;
  }

  public CGenContext<T> ln(String str) {
    writer.accept(str + "\n");
    return this;
  }

  public CGenContext<T> ln() {
    writer.accept("\n");
    return this;
  }


  public DataType cTypeOf(Type type) {
    return requireNonNull(type.asDataType().fittingCppType());
  }

}
