package vadl.oop;

/**
 * This interfaces indicates that a node can generate cpp code.
 */
public interface OopGeneratable {
  /**
   * Returns the C++ expression which will be used by {@link OopGenerator}.
   */
  String generateOopExpression();
}
