package vadl.types;

import com.google.errorprone.annotations.FormatMethod;
import java.util.ArrayList;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import vadl.viam.ViamError;

/**
 * TypeList is a specialized ArrayList that only allows elements of a specific type.
 * It provides additional methods to ensure the type and length constraints of the list.
 *
 * @param <T> the type of elements in the list, must extend Type
 */
public class TypeList<T extends Type> extends ArrayList<T> {

  public TypeList(int initialCapacity) {
    super(initialCapacity);
  }

  public TypeList() {
  }

  public TypeList(@NotNull Collection<? extends T> c) {
    super(c);
  }

  public T first() {
    return get(0);
  }

  public T second() {
    return get(1);
  }

  /**
   * Ensures that all elements in the TypeList are of the specified types.
   *
   * @param types  the array of types that all elements should be of
   * @param format the format string to be used when creating the error message
   * @param args   the arguments to be used when formatting the error message
   * @throws ViamError if any element in the TypeList does not match any of the specified types
   */
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

  /**
   * Ensures that the length of the TypeList is equal to the specified length.
   *
   * @param len    the expected length of the list
   * @param format the format string to be used when creating the error message
   * @param args   the arguments to be used when formatting the error message
   * @throws ViamError if the length of the TypeList is not equal to the expected length
   */
  @FormatMethod
  public void ensureLength(int len, String format, Object... args) {
    if (len != this.size()) {
      throw new ViamError(
          format.formatted(args) + ": invalid length of list. Expected " + len + ", got "
              + this.size())
          .addContext("expectedLength", len)
          .addContext("list", this.toString())
          .shrinkStacktrace(1);

    }
  }

  @FormatMethod
  public void ensureAllOfLength(int len, String format, Object... args) {

  }


}
