package vadl.ast;

import java.math.BigInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.error.Diagnostic;
import vadl.types.Type;

public class TypecheckerTest {
  @Test
  public void booleanConstant() {
    var prog = """
        constant b: Bool = true
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new TypeFinder();
    Assertions.assertEquals(Type.bool(), typeFinder.getConstantType(ast, "b"));
  }

  @Test
  public void booleanConstantWithoutType() {
    var prog = """
        constant b = true
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new TypeFinder();
    Assertions.assertEquals(Type.bool(), typeFinder.getConstantType(ast, "b"));
  }

  @Test
  public void invalidBooleanType() {
    var prog = """
        constant b: Bool<1> = true
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast),
        "Shouldn't accept the program");
  }

  @Test
  public void sintConstant() {
    var prog = """
        constant i: SInt<32> = 42
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new TypeFinder();
    Assertions.assertEquals(Type.signedInt(32), typeFinder.getConstantType(ast, "i"));
  }

  @Test
  public void invalidSintType() {
    var prog = """
        constant i: SInt = 42
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast),
        "Shouldn't accept the program");
  }


  @Test
  public void invalidConstantAssignementIntToBool() {
    var prog = """
        constant b: Bool = 42
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast),
        "Shouldn't accept the program");
  }

  @Test
  public void invalidDeclaredSIntTooNarrow() {
    var prog = """
        constant b: SInt<3> = 8
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast),
        "Shouldn't accept the program");
  }


  @Test
  public void unaryMinusIntExpression() {
    var prog = """
        constant i = -3
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new TypeFinder();
    Assertions.assertEquals(new ConstantType(BigInteger.valueOf(-3)),
        typeFinder.getConstantType(ast, "i"));
  }

  @Test
  public void unaryMinusMultipleIntExpression() {
    var prog = """
        constant i = -----3
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new TypeFinder();
    Assertions.assertEquals(new ConstantType(BigInteger.valueOf(-3)),
        typeFinder.getConstantType(ast, "i"));
  }

  @Test
  public void invalidMinusOnBoolean() {
    var prog = """
        constant i = -true
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast),
        "Shouldn't accept the program");
  }

  @Test
  public void unaryNegateBool() {
    var prog = """
        // It's funny because it's true.
        constant b = !false
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new TypeFinder();
    Assertions.assertEquals(Type.bool(),
        typeFinder.getConstantType(ast, "b"));
  }

  @Test
  public void unaryNegateMultipleBool() {
    var prog = """
        constant b =  !!!!!false
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new TypeFinder();
    Assertions.assertEquals(Type.bool(),
        typeFinder.getConstantType(ast, "b"));
  }

  @Test
  public void invalidNegateInt() {
    var prog = """
        constant b = !5
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast),
        "Shouldn't accept the program");
  }

  @Test
  public void typeFromVariable() {
    var prog = """
        constant a = 3
        constant b = a
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new TypeFinder();
    Assertions.assertEquals(new ConstantType(BigInteger.valueOf(3)),
        typeFinder.getConstantType(ast, "b"));
  }

  @Test
  public void typeSizeDependsOnConstant() {
    var prog = """
        constant a = 3
        constant b: SInt<a> = 1
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new TypeFinder();
    Assertions.assertEquals(Type.signedInt(3), typeFinder.getConstantType(ast, "b"));
  }

  @Test
  public void castConstants() {
    var prog = """
        constant a = 3 as UInt<8>
        constant b = 3 as SInt<8>
        constant c = 3 as Bits<8>
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new TypeFinder();
    Assertions.assertEquals(Type.unsignedInt(8), typeFinder.getConstantType(ast, "a"));
    Assertions.assertEquals(Type.signedInt(8), typeFinder.getConstantType(ast, "b"));
    Assertions.assertEquals(Type.bits(8), typeFinder.getConstantType(ast, "c"));
  }

  @Test
  public void castEvalTruncates() {
    var prog = """
        constant a = 9 as UInt<3>
        constant b: UInt<a> = 1
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new TypeFinder();
    Assertions.assertEquals(Type.unsignedInt(3), typeFinder.getConstantType(ast, "a"));
    Assertions.assertEquals(Type.unsignedInt(1), typeFinder.getConstantType(ast, "b"));
  }

  @Test
  public void invalidCastAfterEvalTruncates() {
    var prog = """
        constant a = 9 as UInt<3>
        constant b: UInt<a> = 3
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast),
        "Program isn't typesafe");
  }
}
