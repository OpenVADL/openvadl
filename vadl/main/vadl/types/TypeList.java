package vadl.types;

import com.google.errorprone.annotations.FormatMethod;
import java.util.ArrayList;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import vadl.viam.ViamError;

public class TypeList<T extends Type> extends ArrayList<T> {

  public TypeList(int initialCapacity) {
    super(initialCapacity);
  }

  public TypeList() {
  }

  public TypeList(@NotNull Collection<? extends T> c) {
    super(c);
  }

  @FormatMethod
  public void ensureAllOfType(Type[] types, String format, Object... args) {
    for (var t : this) {
      boolean found = false;
      for (var type : types) {
        if (t.equals(type)) {
          found = true;
          break;
        }
      }
      if (!found) {
        throw new ViamError("invalid type in list: " + format.formatted(args))
            .addContext("type", t.toString())
            .addContext("list", this.toString())
            .shrinkStacktrace(1);
      }
    }
  }

  @FormatMethod
  public void ensureLength(int len, String format, Object... args) {
    if (len != this.size()) {
      throw new ViamError("invalid length of list: " + format.formatted(args))
          .addContext("expectedLength", len)
          .addContext("list", this.toString())
          .shrinkStacktrace(1);

    }
  }

  @FormatMethod
  public void ensureAllOfLength(int len, String format, Object... args) {

  }


}
