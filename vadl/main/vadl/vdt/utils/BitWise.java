package vadl.vdt.utils;

/**
 * Interface for bitwise operations.
 *
 * @param <T> the type of the elements
 */
public interface BitWise<T> {

  T and(T other);

  T or(T other);

  T xor(T other);

  T not();

}
