package vadl.ast;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import vadl.types.asmTypes.ConstantAsmType;
import vadl.types.asmTypes.InstructionAsmType;
import vadl.types.asmTypes.ModifierAsmType;
import vadl.types.asmTypes.OperandAsmType;
import vadl.types.asmTypes.RegisterAsmType;
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
    var typeFinder = new TypeFinder();
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
    var typeFinder = new TypeFinder();
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
    var typeFinder = new TypeFinder();
    Assertions.assertEquals(InstructionAsmType.instance(),
        typeFinder.getAsmRuleType(ast, "AddInstruction"));
  }

  @Test
  void JalrInstruction() {
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
    var typeFinder = new TypeFinder();
    Assertions.assertEquals(InstructionAsmType.instance(),
        typeFinder.getAsmRuleType(ast, "JalrInstruction"));
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
    var typeFinder = new TypeFinder();
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
    var typeFinder = new TypeFinder();
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
    var typeFinder = new TypeFinder();
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
    var typeFinder = new TypeFinder();
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
    var typeFinder = new TypeFinder();
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
    var typeFinder = new TypeFinder();
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
    var typeFinder = new TypeFinder();
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
    var typeFinder = new TypeFinder();
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
    var typeFinder = new TypeFinder();
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
    var typeFinder = new TypeFinder();
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
    var typeFinder = new TypeFinder();
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
    var typeFinder = new TypeFinder();
    Assertions.assertEquals(VoidAsmType.instance(), typeFinder.getAsmRuleType(ast, "A"));
  }
  //endregion
}
