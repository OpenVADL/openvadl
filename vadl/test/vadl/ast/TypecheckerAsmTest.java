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

  private String inputWrappedByValidAsmDescription(String input) {
    return """
          instruction set architecture ISA = {}
          application binary interface ABI for ISA = {}
        
          assembly description AD for ABI = {
            %s
          }
        """.formatted(input);
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

  @Test
  void addInstruction() {
    var prog = """
          grammar = {
            AddInstruction :
              (
              mnemonic = 'ADD' @operand
              rd   = Register  @operand
              rs1  = Register  @operand
                 ( rs2 = Register   @operand
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
              (
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
              (mod=Identifier@modifier val=Expression) @operand
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
              (Register | Integer @register) @operand
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

  @Test
  void debug() {
    var prog = """
          grammar = {
            A : "a" C;
            B : A "x";
            C : [Register];
          }
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAsmDescription(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    typechecker.verify(ast);

//    var ll1checker = new AsmLL1Checker();
//    ll1checker.verify(ast);
//    Assertions.assertDoesNotThrow(() -> ll1checker.verify(ast), "Program isn't LL(1)");
    var followComp = new FollowSetSetComputer();
    followComp.computeFollowSets(ast);
    var x = 1;
  }
}
