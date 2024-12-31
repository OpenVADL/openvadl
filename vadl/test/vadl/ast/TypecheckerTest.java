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
    var typeFinder = new AstFinder();
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
    var typeFinder = new AstFinder();
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
    var typeFinder = new AstFinder();
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
    var typeFinder = new AstFinder();
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
    var typeFinder = new AstFinder();
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
    var typeFinder = new AstFinder();
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
    var typeFinder = new AstFinder();
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
    var typeFinder = new AstFinder();
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
    var typeFinder = new AstFinder();
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
    var typeFinder = new AstFinder();
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
    var typeFinder = new AstFinder();
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

  @Test
  public void unaryOperationsOnConcreteTypes() {
    var prog = """
        constant a = - (8 as SInt<8>)
        constant b = ~ (0b10101010 as UInt<8>)
        constant c = ! (true)
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var finder = new AstFinder();
    Assertions.assertEquals(BigInteger.valueOf(-8),
        finder.getConstantValue(ast, "a").value());
    Assertions.assertEquals(BigInteger.valueOf(0b01010101),
        finder.getConstantValue(ast, "b").value());
    Assertions.assertEquals(BigInteger.valueOf(0),
        finder.getConstantValue(ast, "c").value());
  }

  @Test
  public void binaryOperationsOnConstantTypes() {
    var prog = """
        constant a = 51                 // 0b110011
        constant b = 42                 // 0b101010
        
        constant c = true && true       // true
        constant d = true || false      // true
        constant e = a | b              // 0b111011 = 59
        constant f = a & b              // 0b100010 = 34
        constant g = a ^ b              // 0b011001 = 25
        constant h = a = b              // false
        constant i = a != b             // true
        constant j = a >= b             // true
        constant k = a > b              // true
        constant l = a < b              // false
        constant m = a <= b             // false
        constant n = b << 2             // 0b10101000 = 168
        constant o = b >> 2             // 0b1010 = 10
        constant p = a + b              // 93
        constant q = a - b              // 9
        constant r = a * b              // 2142
        constant s = a / 5              // 5
        constant t = b % 20             // 2
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");

    var finder = new AstFinder();
    Assertions.assertEquals(Type.bool(), finder.getConstantType(ast, "c"));
    Assertions.assertEquals(BigInteger.ONE, finder.getConstantValue(ast, "c").value());
    Assertions.assertEquals(Type.bool(), finder.getConstantType(ast, "d"));
    Assertions.assertEquals(BigInteger.ONE, finder.getConstantValue(ast, "d").value());
    Assertions.assertEquals(new ConstantType(BigInteger.valueOf(59)),
        finder.getConstantType(ast, "e"));
    Assertions.assertEquals(new ConstantType(BigInteger.valueOf(34)),
        finder.getConstantType(ast, "f"));
    Assertions.assertEquals(new ConstantType(BigInteger.valueOf(25)),
        finder.getConstantType(ast, "g"));
    Assertions.assertEquals(Type.bool(), finder.getConstantType(ast, "h"));
    Assertions.assertEquals(BigInteger.ZERO, finder.getConstantValue(ast, "h").value());
    Assertions.assertEquals(Type.bool(), finder.getConstantType(ast, "i"));
    Assertions.assertEquals(BigInteger.ONE, finder.getConstantValue(ast, "i").value());
    Assertions.assertEquals(Type.bool(), finder.getConstantType(ast, "j"));
    Assertions.assertEquals(BigInteger.ONE, finder.getConstantValue(ast, "j").value());
    Assertions.assertEquals(Type.bool(), finder.getConstantType(ast, "k"));
    Assertions.assertEquals(BigInteger.ONE, finder.getConstantValue(ast, "k").value());
    Assertions.assertEquals(Type.bool(), finder.getConstantType(ast, "l"));
    Assertions.assertEquals(BigInteger.ZERO, finder.getConstantValue(ast, "l").value());
    Assertions.assertEquals(Type.bool(), finder.getConstantType(ast, "m"));
    Assertions.assertEquals(BigInteger.ZERO, finder.getConstantValue(ast, "m").value());
    Assertions.assertEquals(new ConstantType(BigInteger.valueOf(168)),
        finder.getConstantType(ast, "n"));
    Assertions.assertEquals(new ConstantType(BigInteger.valueOf(10)),
        finder.getConstantType(ast, "o"));
    Assertions.assertEquals(new ConstantType(BigInteger.valueOf(93)),
        finder.getConstantType(ast, "p"));
    Assertions.assertEquals(new ConstantType(BigInteger.valueOf(9)),
        finder.getConstantType(ast, "q"));
    Assertions.assertEquals(new ConstantType(BigInteger.valueOf(2142)),
        finder.getConstantType(ast, "r"));
    Assertions.assertEquals(new ConstantType(BigInteger.valueOf(10)),
        finder.getConstantType(ast, "s"));
    Assertions.assertEquals(new ConstantType(BigInteger.valueOf(2)),
        finder.getConstantType(ast, "t"));
  }

  @Test
  public void binaryOperationsOnDataTypes() {
    var prog = """
        constant a: Bits<32> = 51         // 0b110011
        constant b: Bits<32> = 42         // 0b101010
        
        constant c = true && true         // true
        constant d = true || false        // true
        constant e = a | b                // 0b111011 = 59
        constant f = a & b                // 0b100010 = 34
        constant g = a ^ b                // 0b011001 = 25
        constant h = a = b                // false
        constant i = a != b               // true
        constant j = a >= b               // true
        constant k = a > b                // true
        constant l = a < b                // false
        constant m = a <= b               // false
        constant n = b << 2               // 0b10101000 = 168
        constant o = b >> 2               // 0b1010 = 10
        constant p = a + b                // 93
        constant q = a - b                // 9
        constant r = a * b                // 2142
        constant s = a / 5                // 5
        constant t = b % 20               // 2
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");

    var finder = new AstFinder();
    Assertions.assertEquals(Type.bool(), finder.getConstantType(ast, "c"));
    Assertions.assertEquals(BigInteger.ONE, finder.getConstantValue(ast, "c").value());
    Assertions.assertEquals(Type.bool(), finder.getConstantType(ast, "d"));
    Assertions.assertEquals(BigInteger.ONE, finder.getConstantValue(ast, "d").value());
    Assertions.assertEquals(Type.bits(32), finder.getConstantType(ast, "e"));
    Assertions.assertEquals(BigInteger.valueOf(59), finder.getConstantValue(ast, "e").value());
    Assertions.assertEquals(Type.bits(32), finder.getConstantType(ast, "f"));
    Assertions.assertEquals(BigInteger.valueOf(34), finder.getConstantValue(ast, "f").value());
    Assertions.assertEquals(Type.bits(32), finder.getConstantType(ast, "g"));
    Assertions.assertEquals(BigInteger.valueOf(25), finder.getConstantValue(ast, "g").value());
    Assertions.assertEquals(Type.bool(), finder.getConstantType(ast, "h"));
    Assertions.assertEquals(BigInteger.ZERO, finder.getConstantValue(ast, "h").value());
    Assertions.assertEquals(Type.bool(), finder.getConstantType(ast, "i"));
    Assertions.assertEquals(BigInteger.ONE, finder.getConstantValue(ast, "i").value());
    Assertions.assertEquals(Type.bool(), finder.getConstantType(ast, "j"));
    Assertions.assertEquals(BigInteger.ONE, finder.getConstantValue(ast, "j").value());
    Assertions.assertEquals(Type.bool(), finder.getConstantType(ast, "k"));
    Assertions.assertEquals(BigInteger.ONE, finder.getConstantValue(ast, "k").value());
    Assertions.assertEquals(Type.bool(), finder.getConstantType(ast, "l"));
    Assertions.assertEquals(BigInteger.ZERO, finder.getConstantValue(ast, "l").value());
    Assertions.assertEquals(Type.bool(), finder.getConstantType(ast, "m"));
    Assertions.assertEquals(BigInteger.ZERO, finder.getConstantValue(ast, "m").value());
    Assertions.assertEquals(Type.bits(32), finder.getConstantType(ast, "n"));
    Assertions.assertEquals(BigInteger.valueOf(168), finder.getConstantValue(ast, "n").value());
    Assertions.assertEquals(Type.bits(32), finder.getConstantType(ast, "o"));
    Assertions.assertEquals(BigInteger.valueOf(10), finder.getConstantValue(ast, "o").value());
    Assertions.assertEquals(Type.bits(32), finder.getConstantType(ast, "p"));
    Assertions.assertEquals(BigInteger.valueOf(93), finder.getConstantValue(ast, "p").value());
    Assertions.assertEquals(Type.bits(32), finder.getConstantType(ast, "q"));
    Assertions.assertEquals(BigInteger.valueOf(9), finder.getConstantValue(ast, "q").value());
    Assertions.assertEquals(Type.bits(32), finder.getConstantType(ast, "r"));
    Assertions.assertEquals(BigInteger.valueOf(2142), finder.getConstantValue(ast, "r").value());
    Assertions.assertEquals(Type.bits(32), finder.getConstantType(ast, "s"));
    Assertions.assertEquals(BigInteger.valueOf(10), finder.getConstantValue(ast, "s").value());
    Assertions.assertEquals(Type.bits(32), finder.getConstantType(ast, "t"));
    Assertions.assertEquals(BigInteger.valueOf(2), finder.getConstantValue(ast, "t").value());
  }

  @Test
  public void binaryOperationsImplicitCasting() {
    var prog = """
        constant u32: UInt<32> = 2
        constant s32: SInt<32> = 4
        constant b32: Bits<32> = 8
        constant cp = 16
        constant cn = -32
        
        // UInt + UInt -> UInt
        constant a = u32 + u32
        constant b = u32 + cp
        constant c = cp + u32
        
        // SInt + SInt -> SInt
        constant d = s32 + s32
        constant e = cp + s32
        constant f = s32 + cn
        
        // UInt + Bits -> UInt
        constant g = u32 + b32
        
        // Bits + UInt -> UInt
        constant h = b32 + u32
        
        // SInt + Bits -> SInt
        constant i = s32 + b32
        constant j = cn + b32
        
        // Bits + SInt -> SInt
        constant k = b32 + s32
        constant l = b32 + cn
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");

    var finder = new AstFinder();
    Assertions.assertEquals(Type.unsignedInt(32), finder.getConstantType(ast, "a"));
    Assertions.assertEquals(Type.unsignedInt(32), finder.getConstantType(ast, "b"));
    Assertions.assertEquals(Type.unsignedInt(32), finder.getConstantType(ast, "c"));
    Assertions.assertEquals(Type.signedInt(32), finder.getConstantType(ast, "d"));
    Assertions.assertEquals(Type.signedInt(32), finder.getConstantType(ast, "e"));
    Assertions.assertEquals(Type.signedInt(32), finder.getConstantType(ast, "f"));
    Assertions.assertEquals(Type.unsignedInt(32), finder.getConstantType(ast, "g"));
    Assertions.assertEquals(Type.unsignedInt(32), finder.getConstantType(ast, "h"));
    Assertions.assertEquals(Type.signedInt(32), finder.getConstantType(ast, "i"));
    Assertions.assertEquals(Type.signedInt(32), finder.getConstantType(ast, "j"));
    Assertions.assertEquals(Type.signedInt(32), finder.getConstantType(ast, "k"));
    Assertions.assertEquals(Type.signedInt(32), finder.getConstantType(ast, "l"));
  }
}
