package vadl.vdt.utils;

public interface BitWise<T> {

  T and(T other);

  T or(T other);

  T xor(T other);

  T not();

}
