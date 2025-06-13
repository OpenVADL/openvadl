// SPDX-FileCopyrightText : © 2025 TU Wien <vadl@tuwien.ac.at>
// SPDX-License-Identifier: GPL-3.0-or-later
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package vadl.ast;

import java.math.BigInteger;
import java.util.List;
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
  public void implicitTypeBitWidths() {
    var prog = """
        constant a: SInt<32> = 32
        constant b = a as Bits
        constant c: Bits = a
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new AstFinder();
    Assertions.assertEquals(Type.bits(32), typeFinder.getConstantType(ast, "b"));
    Assertions.assertEquals(Type.bits(32), typeFinder.getConstantType(ast, "c"));
  }

  @Test
  public void usingTypeDefinition() {
    var prog = """
        using Flo = SInt<32>
        constant a: Flo = 32
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new AstFinder();
    Assertions.assertEquals(Type.signedInt(32), typeFinder.getConstantType(ast, "a"));
  }

  @Test
  public void multipleUsingTypeDefinition() {
    var prog = """
        using Flo = SInt<32>
        using Johannes = Flo
        using Paul = Johannes
        constant a: Paul = 32
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new AstFinder();
    Assertions.assertEquals(Type.signedInt(32), typeFinder.getConstantType(ast, "a"));
  }

  @Test
  public void usingTypeDefinitionAfterUsage() {
    var prog = """
        constant a = 32 as Paul
        using Paul = Johannes
        using Johannes = Flo
        using Flo = SInt<32>
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new AstFinder();
    Assertions.assertEquals(Type.signedInt(32), typeFinder.getConstantType(ast, "a"));
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

  @Test
  public void builtinOnConstantTypes() {
    var prog = """
        constant a = 51                 // 0b110011
        constant b = 42                 // 0b101010
        
        //constant c = VADL::and(true, true)  // true
        //constant d = true || false          // true
        constant e = VADL::or(a,  b)          // 0b111011 = 59
        constant f = VADL::and(a, b)          // 0b100010 = 34
        constant g = VADL::xor(a, b)          // 0b011001 = 25
        constant h = VADL::equ(a, b)          // false
        constant i = VADL::neq(a, b)          // true
        constant j = VADL::sgeq(a, b)         // true
        constant k = VADL::sgth(a, b)         // true
        constant l = VADL::slth(a, b)         // false
        constant m = VADL::sleq(a, b)         // false
        constant n = VADL::lsl(b, 2)          // 0b10101000 = 168
        constant o = VADL::lsr(b, 2)          // 0b1010 = 10
        constant p = VADL::add(a, b)          // 93
        constant q = VADL::sub(a, b)          // 9
        constant r = VADL::mul(a, b)          // 2142
        constant s = VADL::div(a, 5)          // 5
        constant t = VADL::mod(b, 20)         // 2
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");

    var finder = new AstFinder();
//    Assertions.assertEquals(Type.bool(), finder.getConstantType(ast, "c"));
//    Assertions.assertEquals(BigInteger.ONE, finder.getConstantValue(ast, "c").value());
//    Assertions.assertEquals(Type.bool(), finder.getConstantType(ast, "d"));
//    Assertions.assertEquals(BigInteger.ONE, finder.getConstantValue(ast, "d").value());
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
  public void extendedLongMultiplyTest() {
    var prog = """
        instruction set architecture Test = {
          register X : Bits<5> -> Bits<64>       // general purpose register file with stack pointer
          format ThreeRegOpFormat: Bits<32> =    // three register operand format
            { sf       [31]                      // size field, if (sf = 0) 32 bit operation else 64 bit
            , op       [30..21,15..10]           // opcode
            , rm       [20..16]                  // 2nd source register
            , rn       [9..5]                    // 1st source register
            , rd       [4..0]                    // destination register
            }
        
          model ThreeRegOpEncAsm (i: Id, op: Lit): IsaDefs = {
            encoding $i = { op = $op, sf = 1 }
            assembly $i = (mnemonic, ' ', register(rd), ', ', register(rn), ', ', register(rm))
            }
        
          model ThreeRegOp (i: Id, op: Lit): IsaDefs = {
            instruction $i : ThreeRegOpFormat =
              let result = VADL::$i (X(rn), X(rm)) in
                X(rd) := result(127..64)
            $ThreeRegOpEncAsm ($i; $op)
            }
        
          model ThreeRegOpFlags (i: Id, op: Lit): IsaDefs = {
            instruction $i : ThreeRegOpFormat =
              let result, flags = VADL::$i (X(rn), X(rm)) in
                X(rd) := result(127..64)
            $ThreeRegOpEncAsm ($i; $op)
            }
        
          $ThreeRegOp (smull;  0)
          $ThreeRegOp (umull;  1)
          $ThreeRegOp (sumull; 2)
          $ThreeRegOpFlags (smulls;  10)
          $ThreeRegOpFlags (umulls;  11)
          $ThreeRegOpFlags (sumulls; 12)
        } 
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    new ModelRemover().removeModels(ast);
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
  }


  @Test
  public void functionDefinition() {
    var prog = """
        function addOne(n: SInt<8>) -> SInt<8> = n + 1 
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var finder = new AstFinder();
    Assertions.assertEquals(
        Type.concreteRelation(Type.signedInt(8), Type.signedInt(8)),
        finder.getFunctionType(ast, "addOne"));
  }

  @Test
  public void invalidFunctionDefinitionWrongReturnType() {
    var prog = """
        function addOne(n: SInt<8>) -> SInt<8> = false
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast),
        "Program isn't typesafe");
  }

  @Test
  public void formatWithTypes() {
    var prog = """
         format f : Bits<8> =
         { first: Bits<2>
         , second: Bits<6>
         }
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var finder = new AstFinder();
    var format = finder.findDefinition(ast, "f", FormatDefinition.class);
    Assertions.assertEquals(Type.bits(2), format.getFieldType("first"));
    Assertions.assertEquals(Type.bits(6), format.getFieldType("second"));
  }

  @Test
  public void formatWithRanges() {
    var prog = """
         format f : Bits<8> =
         { first   [7..3]
         , second  [2..0]
         }
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var finder = new AstFinder();
    var format = finder.findDefinition(ast, "f", FormatDefinition.class);
    Assertions.assertEquals(Type.bits(5), format.getFieldType("first"));
    Assertions.assertEquals(Type.bits(3), format.getFieldType("second"));
  }

  @Test
  public void formatComplexWithRanges() {
    var prog = """
           format x: Bits<6> = {
             a  [0, 5..4, 2],
             b  [1, 3]
           }
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var finder = new AstFinder();
    var format = finder.findDefinition(ast, "x", FormatDefinition.class);
    Assertions.assertEquals(Type.bits(4), format.getFieldType("a"));
    Assertions.assertEquals(Type.bits(2), format.getFieldType("b"));
  }


  @Test
  public void invalidFormatUnusedBitsWithTypes() {
    var prog = """
        format f : Bits<8> =
         { first: Bits<3>
         , second: Bits<4>
         }
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast),
        "Program isn't typesafe");
  }

  @Test
  public void invalidFormatOverusedBitsWithTypes() {
    var prog = """
        format f : Bits<8> =
         { first: Bits<6>
         , second: Bits<4>
         }
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast),
        "Program isn't typesafe");
  }

  @Test
  public void invalidFormatUnusedBitsWithRanges() {
    var prog = """
        format f : Bits<8> =
         { first   [7..6]
         , second  [4..0]
         }
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast),
        "Program isn't typesafe");
  }

  @Test
  public void invalidFormatOverusedBitsWithRanges() {
    var prog = """
        format f : Bits<8> =
         { first   [7..3]
         , second  [5..0]
         }
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast),
        "Program isn't typesafe");
  }

  @Test
  public void invalidFormatOutOfBoundsRange() {
    var prog = """
        instruction set architecture TEST = {
          format Format: Bits<32> =
          { field   [32..0]             // wrong higher offset
          , accFunc = field as SInt
          }
        }
        
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast),
        "Program isn't typesafe");
  }


  @Test
  public void enumWithTypes() {
    var prog = """
          enumeration ENUM: Bits<2> =
          { A = 0b00 + 0b01
          , B = 0b01
          , C = 0b10
          }
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var finder = new AstFinder();
    var enumeration = finder.findDefinition(ast, "ENUM", EnumerationDefinition.class);
    Assertions.assertEquals(Type.bits(2), enumeration.getEntryType("A"));
    Assertions.assertEquals(Type.bits(2), enumeration.getEntryType("B"));
    Assertions.assertEquals(Type.bits(2), enumeration.getEntryType("C"));
  }

  @Test
  public void invalidEnumWithTypes() {
    var prog = """
          enumeration ENUM: Bits<2> =
          { A = 0b00 + 0b01
          , B = 0b0111111
          , C = 0b10
          }
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast),
        "Program isn't typesafe");
  }

  @Test
  public void enumWithoutTypes() {
    var prog = """
          enumeration ENUM =
          { A = 1
          , B = 2
          , C = 3 + 4
          }
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var finder = new AstFinder();
    var enumeration = finder.findDefinition(ast, "ENUM", EnumerationDefinition.class);
    Assertions.assertEquals(new ConstantType(BigInteger.valueOf(1)), enumeration.getEntryType("A"));
    Assertions.assertEquals(new ConstantType(BigInteger.valueOf(2)), enumeration.getEntryType("B"));
    Assertions.assertEquals(new ConstantType(BigInteger.valueOf(7)), enumeration.getEntryType("C"));
  }

  @Test
  public void enumWithOutValues() {
    var prog = """
          enumeration ENUM =
          { A
          , B
          , C
          }
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var finder = new AstFinder();
    var enumeration = finder.findDefinition(ast, "ENUM", EnumerationDefinition.class);
    Assertions.assertEquals(new ConstantType(BigInteger.valueOf(0)), enumeration.getEntryType("A"));
    Assertions.assertEquals(new ConstantType(BigInteger.valueOf(1)), enumeration.getEntryType("B"));
    Assertions.assertEquals(new ConstantType(BigInteger.valueOf(2)), enumeration.getEntryType("C"));
  }

  @Test
  public void enumWithSomeValues() {
    var prog = """
          enumeration ENUM =
          { A
          , B
          , C = 6
          , D
          }
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var finder = new AstFinder();
    var enumeration = finder.findDefinition(ast, "ENUM", EnumerationDefinition.class);
    Assertions.assertEquals(new ConstantType(BigInteger.valueOf(0)), enumeration.getEntryType("A"));
    Assertions.assertEquals(new ConstantType(BigInteger.valueOf(1)), enumeration.getEntryType("B"));
    Assertions.assertEquals(new ConstantType(BigInteger.valueOf(6)), enumeration.getEntryType("C"));
    Assertions.assertEquals(new ConstantType(BigInteger.valueOf(7)), enumeration.getEntryType("D"));
  }

  @Test
  public void enumEntryReferenceBeforeDefinition() {
    // There once was a crash with code like that:
    // https://github.com/OpenVADL/openvadl/issues/190
    var prog = """
          constant friday:  Bits<Nums::second> = 5
        
          enumeration Nums : Bits<4> =
            { first = 10
            , second
            }
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var finder = new AstFinder();
    Assertions.assertEquals(Type.bits(11), finder.getConstantType(ast, "friday"));
  }

  @Test
  public void validIfExprTest() {
    var prog = """
          constant x = if 4 = 7 then 3 as Bits<32> else 4 as Bits<32>
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var finder = new AstFinder();
    Assertions.assertEquals(Type.bits(32), finder.getConstantType(ast, "x"));
  }

  @Test
  public void inValidIfExprConditionNoBoolTest() {
    var prog = """
          constant x = if 4 then 3 as Bits<32> else 4 as Bits<32>
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  @Test
  public void ifExprBranchesDifferentTypesInConstantTest() {
    var prog = """
          // In the "context" of an constant the branches can have different types.
          constant x = if 4 = 7 then 3 as Bits<32> else 4 as Bits<16>
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var finder = new AstFinder();
    Assertions.assertEquals(Type.bits(16), finder.getConstantType(ast, "x"));
  }

  @Test
  public void invalidIfExprBranchesDifferentTypesInFunctionTest() {
    // FIXME: In the future bidirectional typechecking should fix that.
    var prog = """
          function abc(n: SInt<8>) -> SInt<8> = if n = 7 then 3 else 4
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }


  @Test
  public void validMatchExprTest() {
    var prog = """
          constant x = match 4 as Bits<32> with 
          { 1 => 2 as Bits<16>
          , 2 => 4 as Bits<16>
          , 3, 9 => 6 as Bits<16>
          , _ => 42 as Bits<16>
          } 
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var finder = new AstFinder();
    Assertions.assertEquals(Type.bits(16), finder.getConstantType(ast, "x"));
  }

  @Test
  public void invalidMatchExprPatternDifferentTypeTest() {
    var prog = """
          constant x = match 4 as Bits<32> with 
          { 1 => 2 as Bits<16>
          , 2 as Bits<8> => 4 as Bits<16>
          , 3 => 6 as Bits<16>
          , _ => 42 as Bits<16>
          } 
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  @Test
  public void matchExprBranchesDifferentTypeInConstantTest() {
    var prog = """
          // In the "context" of an constant the branches can have different types.
          constant x = match 4 as Bits<32> with 
          { 1 => 2 as Bits<16>
          , 2 => 4 as Bits<32>
          , 3 => 6 as Bits<8>
          , _ => 42 as Bits<64>
          } 
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var finder = new AstFinder();
    Assertions.assertEquals(Type.bits(64), finder.getConstantType(ast, "x"));
  }

  @Test
  public void invalidMatchExprBranchesDifferentTypeInFunctionTest() {
    var prog = """
          function x(n: SInt<8>) -> Bits<64> = (match n with 
          { 1 => 2 as Bits<16>
          , 2 => 4 as Bits<32>
          , 3 => 6 as Bits<8>
          , _ => 42 as Bits<64>
          } ) as Bits<64>
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }


  @Test
  public void instructionEncodingAssembly() {
    var prog = """
        instruction set architecture Mini = {
        
          using Inst     = Bits<32>               // instruction word is 32 bit
          using Regs     = Bits<32>               // untyped register word type
        
          register    X : Bits<5>   -> Regs  // integer register with 32 registers of 32 bits
        
          format Rtype : Inst =                   // Rtype register 3 operand instruction format
            { funct7 : Bits<7>                    // [31..25] 7 bit function code
            , rs2    : Bits<5>                    // [24..20] 2nd source register index / shamt
            , rs1    : Bits<5>                    // [19..15] 1st source register index
            , funct3 : Bits<3>                    // [14..12] 3 bit function code
            , rd     : Bits<5>                    // [11..7]  destination register index
            , opcode : Bits<7>                    // [6..0]   7 bit operation code
            }
        
          instruction ADD : Rtype = X(rd) := (X(rs1) + X(rs2)) as Regs
          encoding ADD = {opcode = 0b011'0011, funct3 = 0b000, funct7 = 0b000'0000}
          assembly ADD = (mnemonic, " ", register(rd), ",", register(rs1), ",", register(rs2))
        }
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
  }

  @Test
  public void tupleUnpackingLetExprTest() {
    var prog = """
            instruction set architecture Mini = {
        
              using Inst     = Bits<32>               // instruction word is 32 bit
              using Regs     = Bits<32>               // untyped register word type
        
              register    X : Bits<5>   -> Regs  // integer register with 32 registers of 32 bits
        
              format Rtype : Inst =                   // Rtype register 3 operand instruction format
                { funct7 : Bits<7>                    // [31..25] 7 bit function code
                , rs2    : Bits<5>                    // [24..20] 2nd source register index / shamt
                , rs1    : Bits<5>                    // [19..15] 1st source register index
                , funct3 : Bits<3>                    // [14..12] 3 bit function code
                , rd     : Bits<5>                    // [11..7]  destination register index
                , opcode : Bits<7>                    // [6..0]   7 bit operation code
                }
        
              instruction ADD : Rtype = X(rd) :=
                let res, s = VADL::adds(X(rs1), X(rs2)) in
                  res as Regs
              encoding ADD = {opcode = 0b011'0011, funct3 = 0b000, funct7 = 0b000'0000}
              assembly ADD = (mnemonic, " ", register(rd), ",", register(rs1), ",", register(rs2))
            }
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
  }

  @Test
  public void tupleUnpackingLetStmtTest() {
    var prog = """
        instruction set architecture Mini = {
        
          using Inst     = Bits<32>               // instruction word is 32 bit
          using Regs     = Bits<32>               // untyped register word type
        
          register    X : Bits<5>   -> Regs  // integer register with 32 registers of 32 bits
        
          format Rtype : Inst =                   // Rtype register 3 operand instruction format
            { funct7 : Bits<7>                    // [31..25] 7 bit function code
            , rs2    : Bits<5>                    // [24..20] 2nd source register index / shamt
            , rs1    : Bits<5>                    // [19..15] 1st source register index
            , funct3 : Bits<3>                    // [14..12] 3 bit function code
            , rd     : Bits<5>                    // [11..7]  destination register index
            , opcode : Bits<7>                    // [6..0]   7 bit operation code
            }
        
          instruction ADD : Rtype =
            let res, s = VADL::adds(X(rs1), X(rs2)) in
              X(rd) := res as Regs
          encoding ADD = {opcode = 0b011'0011, funct3 = 0b000, funct7 = 0b000'0000}
          assembly ADD = (mnemonic, " ", register(rd), ",", register(rs1), ",", register(rs2))
        }
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
  }

  @Test
  public void tensorValuesTest() {
    var prog = """
        constant x = let a = 1 as Bits<6> in a + 2
        function flo -> Bits<6> = x as Bits<6>
        
        using Dim_1_a = Bits<16>
        using Dim_2_a = Dim_1_a<4>
        using Dim_3_a = Dim_2_a<2>
        using Dim_3_b = Bits<2><4><16>         // equivalent to Dim_3_a
        
        constant d2 = (3 as Dim_1_a, 2, 1, 0) as Dim_2_a                // specified with highest index first
        constant d3 = ((7 as Bits<16>, 6, 5, 4) as Dim_2_a,             // cast here is redundant but more readable
                       (3 as Bits<16>, 2, 1, 0) as Dim_2_a) as Dim_3_a 
        
        constant a = d2(3)                     // is 3 as Dim_1_a (Bits<16>)
        constant b = d2(3)(15)                 // is 0 as Bits<1>
        constant c = d3(0)                     // is (3, 2, 1, 0) as Dim_2_a
        constant d = d3(0)(3)                  // is 3 as Dim_1_a (Bits<16>)
        constant e = d3(0)(3)(15)              // is 0 as Bits<1>
        constant f = let x = d3(0) as Bits<64> in x(15..0)  // is 0, is d3(0)(0)
        constant g = let x = d3(0) as Bits<64> in x(63..48) // is 3, is d3(0)(3)
        constant h = let x = d3(1) as Bits<64> in x(63..48) // is 7, is d3(1)(3)
        
        constant i = 0xfedc'da98'7654'3210 as Bits<64>    // Bits<64> value
        constant j = i as Dim_2_a 
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");

    var finder = new AstFinder();
    Assertions.assertEquals(new TensorType(List.of(4), Type.bits(16)),
        finder.getConstantType(ast, "d2"));
    Assertions.assertEquals(new TensorType(List.of(2, 4), Type.bits(16)),
        finder.getConstantType(ast, "d3"));


    Assertions.assertEquals(Type.bits(16), finder.getConstantType(ast, "a"));
    Assertions.assertEquals(new ConstantValue(BigInteger.valueOf(3), Type.bits(16)),
        finder.getConstantValue(ast, "a"));

    Assertions.assertEquals(Type.bits(1), finder.getConstantType(ast, "b"));
    Assertions.assertEquals(new ConstantValue(BigInteger.ZERO, Type.bits(1)),
        finder.getConstantValue(ast, "b"));

    Assertions.assertEquals(new TensorType(List.of(4), Type.bits(16)),
        finder.getConstantType(ast, "c"));
    Assertions.assertEquals(
        new ConstantValue(BigInteger.valueOf(0x0003000200010000L),
            new TensorType(List.of(4), Type.bits(16))),
        finder.getConstantValue(ast, "c"));

    Assertions.assertEquals(Type.bits(16), finder.getConstantType(ast, "d"));
    Assertions.assertEquals(new ConstantValue(BigInteger.valueOf(3), Type.bits(16)),
        finder.getConstantValue(ast, "d"));

    Assertions.assertEquals(Type.bits(1), finder.getConstantType(ast, "e"));
    Assertions.assertEquals(new ConstantValue(BigInteger.ZERO, Type.bits(1)),
        finder.getConstantValue(ast, "e"));

    Assertions.assertEquals(Type.bits(16), finder.getConstantType(ast, "f"));
    Assertions.assertEquals(new ConstantValue(BigInteger.ZERO, Type.bits(16)),
        finder.getConstantValue(ast, "f"));

    Assertions.assertEquals(Type.bits(16), finder.getConstantType(ast, "g"));
    Assertions.assertEquals(new ConstantValue(BigInteger.valueOf(3), Type.bits(16)),
        finder.getConstantValue(ast, "g"));

    Assertions.assertEquals(Type.bits(16), finder.getConstantType(ast, "h"));
    Assertions.assertEquals(new ConstantValue(BigInteger.valueOf(7), Type.bits(16)),
        finder.getConstantValue(ast, "h"));

    Assertions.assertEquals(Type.bits(64), finder.getConstantType(ast, "i"));
    Assertions.assertEquals(
        new ConstantValue(new BigInteger("fedcda9876543210", 16), Type.bits(64)),
        finder.getConstantValue(ast, "i"));

    Assertions.assertEquals(new TensorType(List.of(4), Type.bits(16)),
        finder.getConstantType(ast, "j"));
    Assertions.assertEquals(new ConstantValue(new BigInteger("fedcda9876543210", 16),
            Type.bits(64)),
        finder.getConstantValue(ast, "j"));
  }

  @Test
  public void signedTensorValuesTest() {
    var prog = """
        constant a = (-3 as SInt<8>, 8, -9)  as SInt<3><8>
        constant b = a(0) 
        constant c = a(1) 
        constant d = a(2) 
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");

    var finder = new AstFinder();
    Assertions.assertEquals(new TensorType(List.of(3), Type.signedInt(8)),
        finder.getConstantType(ast, "a"));

    Assertions.assertEquals(Type.signedInt(8), finder.getConstantType(ast, "b"));
    Assertions.assertEquals(new ConstantValue(BigInteger.valueOf(-9), Type.signedInt(8)),
        finder.getConstantValue(ast, "b"));

    Assertions.assertEquals(Type.signedInt(8), finder.getConstantType(ast, "c"));
    Assertions.assertEquals(new ConstantValue(BigInteger.valueOf(8), Type.signedInt(8)),
        finder.getConstantValue(ast, "c"));

    Assertions.assertEquals(Type.signedInt(8), finder.getConstantType(ast, "d"));
    Assertions.assertEquals(new ConstantValue(BigInteger.valueOf(-3), Type.signedInt(8)),
        finder.getConstantValue(ast, "d"));
  }

}
