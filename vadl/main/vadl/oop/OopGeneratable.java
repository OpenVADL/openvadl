package vadl.oop;

public interface OopGeneratable {
  /**
   * Returns the C++ expression which will be used by {@link OopGenerator}.
   */
  String generateOopExpression(SymbolTable symbolTable);
}
