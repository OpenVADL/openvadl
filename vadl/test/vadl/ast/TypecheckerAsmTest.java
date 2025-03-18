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


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.error.Diagnostic;
import vadl.types.asmTypes.ConstantAsmType;
import vadl.types.asmTypes.InstructionAsmType;
import vadl.types.asmTypes.ModifierAsmType;
import vadl.types.asmTypes.OperandAsmType;
import vadl.types.asmTypes.RegisterAsmType;
import vadl.types.asmTypes.StatementsAsmType;
import vadl.types.asmTypes.SymbolAsmType;
import vadl.types.asmTypes.VoidAsmType;

public class TypecheckerAsmTest {

  private final String base = """
       instruction set architecture ISA = {
        register file X : Bits<5> -> Bits<32>
       
        format Rtype : Bits<1> =
        { funct7 : Bits<1> }
       
        instruction DO : Rtype =
        {
           X(0) := 1
        }
        encoding DO = { funct7 = 0b0 }
        assembly DO = (mnemonic)
       
        pseudo instruction NOP( symbol: Bits<5>) = {
        }
        assembly NOP = (mnemonic)
      }
      application binary interface ABI for ISA = {
        pseudo return instruction = NOP
        pseudo call instruction = NOP
        pseudo local address load instruction = NOP
        alias register zero = X(0)
        stack pointer = zero
        return address = zero
        global pointer = zero
        frame pointer = zer
        thread pointer = zero
      }
      """;

  private String inputWrappedByValidAsmDescription(String input) {
    return """
          %s
                
          assembly description AD for ABI = {
            %s
          }
        """.formatted(base, input);
  }

  @Test
  void useTerminalRule() {
    var prog = """
          grammar = {
            A : Integer;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new AstFinder();
    Assertions.assertEquals(ConstantAsmType.instance(), typeFinder.getAsmRuleType(ast, "A"));
  }

  @Test
  void luiInstruction() {
    var prog = """
          grammar = {
            LuiInstruction @instruction :
              mnemonic = 'LUI' @operand
              rd = Register<> @operand
              imm = Expression<> @operand
            ;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new AstFinder();
    Assertions.assertEquals(InstructionAsmType.instance(),
        typeFinder.getAsmRuleType(ast, "LuiInstruction"));
  }

  // FIXME: re-enable when parameters of asm built-in functions are correctly casted
  // @Test
  void addInstruction() {
    var prog = """
          grammar = {
            AddInstruction :
              inst = (
              mnemonic = 'ADD' @operand
              rd   = Register  @operand
              rs1  = Register  @operand
                 ( ?(laidin(0,"r1","r2")) rs2 = Register   @operand
                 | imm = Expression @operand
                 )
              ) @instruction
            ;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new AstFinder();
    Assertions.assertEquals(InstructionAsmType.instance(),
        typeFinder.getAsmRuleType(ast, "AddInstruction"));
  }

  @Test
  void jalrInstruction() {
    var prog = """
          grammar = {
            JalrInstruction : var tmp = null @operand
              inst = (
              mnemonic = 'JALR' @operand
              tmp = Register @operand
              [ COMMA rs1 = tmp
                tmp = Register @operand ]
              rd = tmp
              ) @instruction
            ;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new AstFinder();
    Assertions.assertEquals(InstructionAsmType.instance(),
        typeFinder.getAsmRuleType(ast, "JalrInstruction"));
  }

  @Test
  void nestedModifiedExpression() {
    var prog = """
          grammar = {
            A @instruction:
              Integer @operand
              op2 = (mod=Identifier@modifier val=Expression) @operand
            ;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new AstFinder();
    Assertions.assertEquals(InstructionAsmType.instance(), typeFinder.getAsmRuleType(ast, "A"));
  }

  @Test
  void nestedAlternative() {
    var prog = """
          grammar = {
            A @instruction:
              Integer @operand
              op2 = (Register | Integer @register) @operand
            ;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new AstFinder();
    Assertions.assertEquals(InstructionAsmType.instance(), typeFinder.getAsmRuleType(ast, "A"));
  }

  @Test
  void nestedGroupToBeFlattened() {
    var prog = """
          grammar = {
            A @instruction:
              attr1 = Integer @operand
              (
                attr2 = Register @operand
                attr3 = Integer @operand
              )
            ;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new AstFinder();
    Assertions.assertEquals(InstructionAsmType.instance(), typeFinder.getAsmRuleType(ast, "A"));
  }

  @Test
  void discardTypeOfRepetitionBlock() {
    var prog = """
          grammar = {
            A @operand:
              attr1 = Integer @operand
              {
                (attr2 = Register)
              }
            ;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new AstFinder();
    Assertions.assertEquals(OperandAsmType.instance(), typeFinder.getAsmRuleType(ast, "A"));
  }

  @Test
  void invalidAttributeAssignInGroup() {
    var prog = """
          grammar = {
            A @operand:
              attr1 += Integer @operand
            ;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  @Test
  void invalidAttributeAssignInRepetition() {
    var prog = """
          grammar = {
            A @operand:
              {
                attr1 = Integer @operand
              }
            ;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  @Test
  void repetitionBlock() {
    var prog = """
          grammar = {
            Statements @statements:
              stmts = Statement @statements
              { stmts += Statement }
            ;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new AstFinder();
    Assertions.assertEquals(StatementsAsmType.instance(),
        typeFinder.getAsmRuleType(ast, "Statements"));
  }

  @Test
  void repetitionWithDoubleAssignment() {
    var prog = """
          grammar = {
            Statements @statements:
              stmts = Statement @statements
              {
                stmts += Statement
                stmts += Statement
              }
            ;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new AstFinder();
    Assertions.assertEquals(StatementsAsmType.instance(),
        typeFinder.getAsmRuleType(ast, "Statements"));
  }

  @Test
  void repetitionWithNestedGroup() {
    var prog = """
          grammar = {
            Statements @statements:
              stmts = Statement @statements
              {
                stmts += Statement
                (rd = "A")
              }
            ;
            Op : Integer @operand;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new AstFinder();
    Assertions.assertEquals(StatementsAsmType.instance(),
        typeFinder.getAsmRuleType(ast, "Statements"));
  }

  @Test
  void repetitionAssignToUnknownAttribute() {
    var prog = """
          grammar = {
            Statements @statements:
              {
                stmts += Statement
              }
            ;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  @Test
  void assignToLocalVarInRepetition() {
    var prog = """
          grammar = {
            Statements:
              var tmp = Statement
              stmts = tmp @statements
              { tmp = Statement
                stmts += tmp }
              lastStmt = tmp
            ;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
  }

  @Test
  void alternativeOfDifferingAsmTypes() {
    var prog = """
          grammar = {
            A : Integer | Identifier;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  @Test
  void invalidLocalVarDeclaration() {
    var prog = """
          grammar = {
            A :
              Integer
              var tmp = null @operand
            ;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  @Test
  void multipleLocalVarDeclarations() {
    var prog = """
          grammar = {
            A :
              var tmp = null @operand
              var tmp2 = Identifier @symbol
              Integer
            ;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new AstFinder();
    Assertions.assertEquals(ConstantAsmType.instance(), typeFinder.getAsmRuleType(ast, "A"));
  }

  @Test
  void invalidSymbolInRule() {
    var prog = """
          constant a = 5
          grammar = {
            A : a;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  @Test
  void invalidDoubleAssignToAttribute() {
    var prog = """
          grammar = {
            A : a1 = Integer a1 = Identifier;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  @Test
  void invalidCycleInRules() {
    var prog = """
          grammar = {
            A : B;
            B : C;
            C : D;
            D : A;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  @Test
  void invalidCycleWithBuiltinRules() {
    var prog = """
          grammar = {
            Expression : ImmediateOperand;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  @Test
  void validTypesFunctionInvocation() {
    var prog = """
          function minusOne(x: SInt<64>) -> SInt<64> = x - 1
          grammar = {
            A : attr = minusOne<Integer>;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new AstFinder();
    Assertions.assertEquals(ConstantAsmType.instance(), typeFinder.getAsmRuleType(ast, "A"));
  }

  @Test
  void invalidArgumentTypeFunctionInvocation() {
    var prog = """
            function minusOne(x: SInt<64>) -> SInt<64> = x - 1
            grammar = {
              A : attr = minusOne<Identifier>;
            }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  @Test
  void tooManyArgumentsFunctionInvocation() {
    var prog = """
            function minusOne(x: SInt<64>) -> SInt<64> = x - 1
            grammar = {
              A : attr = minusOne<Integer,Integer>;
            }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  @Test
  void functionInvocationInParameters() {
    var prog = """
            function one -> SInt<64> = 1
            function minusOne(x: SInt<64>) -> SInt<64> = x - 1
            grammar = {
              A : minusOne<one>;
            }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new AstFinder();
    Assertions.assertEquals(ConstantAsmType.instance(), typeFinder.getAsmRuleType(ast, "A"));
  }

  @Test
  void validSemanticPredicateType() {
    var prog = """
          grammar = {
            A :
              ?(VADL::equ(2 as Bits<2>,1 as Bits<2>)) Integer
              | Integer
            ;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new AstFinder();
    Assertions.assertEquals(ConstantAsmType.instance(), typeFinder.getAsmRuleType(ast, "A"));
  }

  @Test
  void invalidSemanticPredicateType() {
    var prog = """
          grammar = {
            A :
              ?(VADL::add(2,1)) Integer
              | Integer
            ;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
  }

  //region casts
  @Test
  void castOperandsToInstruction() {
    var prog = """
          grammar = {
            A @instruction :
              attr1 = Integer@operand
              attr2 = Integer@operand
            ;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new AstFinder();
    Assertions.assertEquals(InstructionAsmType.instance(), typeFinder.getAsmRuleType(ast, "A"));
  }

  @Test
  void castRegisterToOperand() {
    var prog = """
          grammar = {
            A@operand : Integer@register;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new AstFinder();
    Assertions.assertEquals(OperandAsmType.instance(), typeFinder.getAsmRuleType(ast, "A"));
  }

  @Test
  void castConstantToOperand() {
    var prog = """
          grammar = {
            A@operand : Integer;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new AstFinder();
    Assertions.assertEquals(OperandAsmType.instance(), typeFinder.getAsmRuleType(ast, "A"));
  }

  @Test
  void castStringToOperand() {
    var prog = """
          grammar = {
            A@operand : Identifier;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new AstFinder();
    Assertions.assertEquals(OperandAsmType.instance(), typeFinder.getAsmRuleType(ast, "A"));
  }

  @Test
  void castExpressionToOperand() {
    var prog = """
          grammar = {
            A@operand : Expression;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new AstFinder();
    Assertions.assertEquals(OperandAsmType.instance(), typeFinder.getAsmRuleType(ast, "A"));
  }

  @Test
  void castSymbolToOperand() {
    var prog = """
          grammar = {
            A@operand : Identifier@symbol;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new AstFinder();
    Assertions.assertEquals(OperandAsmType.instance(), typeFinder.getAsmRuleType(ast, "A"));
  }

  @Test
  void castModifiedExpressionToOperand() {
    var prog = """
          grammar = {
            A@operand : mod=Identifier@modifier val=Expression;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new AstFinder();
    Assertions.assertEquals(OperandAsmType.instance(), typeFinder.getAsmRuleType(ast, "A"));
  }

  @Test
  void castStringToRegister() {
    var prog = """
          grammar = {
            A @register : Identifier;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new AstFinder();
    Assertions.assertEquals(RegisterAsmType.instance(), typeFinder.getAsmRuleType(ast, "A"));
  }

  @Test
  void castConstantToRegister() {
    var prog = """
          grammar = {
            A @register : Integer;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new AstFinder();
    Assertions.assertEquals(RegisterAsmType.instance(), typeFinder.getAsmRuleType(ast, "A"));
  }

  @Test
  void castStringToModifier() {
    var prog = """
          grammar = {
            A @modifier : Identifier;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new AstFinder();
    Assertions.assertEquals(ModifierAsmType.instance(), typeFinder.getAsmRuleType(ast, "A"));
  }

  @Test
  void castStringToSymbol() {
    var prog = """
          grammar = {
            A @symbol : Identifier;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new AstFinder();
    Assertions.assertEquals(SymbolAsmType.instance(), typeFinder.getAsmRuleType(ast, "A"));
  }

  @Test
  void castToVoid() {
    var prog = """
          grammar = {
            A @void : Identifier;
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
    var typeFinder = new AstFinder();
    Assertions.assertEquals(VoidAsmType.instance(), typeFinder.getAsmRuleType(ast, "A"));
  }
  //endregion
}
