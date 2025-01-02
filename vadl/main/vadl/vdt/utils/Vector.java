package vadl.vdt.utils;

/**
 * Represents a vector of elements of type T.
 *
 * @param <T> the type of the elements
 */
public interface Vector<T> {

  int width();

  T get(int i);

}
