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

public class TypecheckerAbiTest {

  private final String base = """
       instruction set architecture ISA = {
        register X : Bits<5> -> Bits<32>
      
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
      
      """;

  private String inputWrappedByValidAbi(String input) {
    return """
          %s
        
          application binary interface ABI for ISA = {
            %s
          }
        """.formatted(base, input);
  }

  @Test
  void valid() {
    var prog = """
          special return instruction = NOP
          special call instruction = NOP
          special local address load instruction = NOP
          special absolute address load instruction = NOP
          alias register zero = X(0)
          stack pointer = zero
          return address = zero
          global pointer = zero
          frame pointer = zero
          thread pointer = zero
          return value = zero
          function argument = zero
          caller saved = zero
          callee saved = zero
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAbi(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
  }

  @Test
  void valid_whenThreadPointerMissing() {
    var prog = """
          special return instruction = NOP
          special call instruction = NOP
          special local address load instruction = NOP
          special absolute address load instruction = NOP
          alias register zero = X(0)
          stack pointer = zero
          return address = zero
          global pointer = zero
          frame pointer = zero
          // thread pointer = zero
          return value = zero
          function argument = zero
          caller saved = zero
          callee saved = zero
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAbi(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    Assertions.assertDoesNotThrow(() -> typechecker.verify(ast), "Program isn't typesafe");
  }

  @Test
  void shouldThrow_whenPseudoReturnInstructionMissing() {
    var prog = """
          // special return instruction = NOP
          special call instruction = NOP
          special local address load instruction = NOP
          special absolute address load instruction = NOP
          alias register zero = X(0)
          stack pointer = zero
          return address = zero
          global pointer = zero
          frame pointer = zero
          thread pointer = zero
          return value = zero
          function argument = zero
          caller saved = zero
          callee saved = zero
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAbi(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    var throwable = Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    Assertions.assertEquals(Diagnostic.Level.ERROR, throwable.level);
    Assertions.assertEquals("No RETURN was declared but one was expected", throwable.reason);
  }

  @Test
  void shouldThrow_whenPseudoCallInstructionMissing() {
    var prog = """
          special return instruction = NOP
          // special call instruction = NOP
          special local address load instruction = NOP
          special absolute address load instruction = NOP
          alias register zero = X(0)
          stack pointer = zero
          return address = zero
          global pointer = zero
          frame pointer = zero
          thread pointer = zero
          return value = zero
          function argument = zero
          caller saved = zero
          callee saved = zero
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAbi(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    var throwable = Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    Assertions.assertEquals(Diagnostic.Level.ERROR, throwable.level);
    Assertions.assertEquals("No CALL was declared but one was expected", throwable.reason);
  }

  @Test
  void shouldThrow_whenPseudoNonPicAddressLoadInstructionMissing() {
    var prog = """
          special return instruction = NOP
          special call instruction = NOP
          special local address load instruction = NOP
          // special absolute address load instruction = NOP
          alias register zero = X(0)
          stack pointer = zero
          return address = zero
          global pointer = zero
          frame pointer = zero
          thread pointer = zero
          return value = zero
          function argument = zero
          caller saved = zero
          callee saved = zero
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAbi(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    var throwable = Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    Assertions.assertEquals(Diagnostic.Level.ERROR, throwable.level);
    Assertions.assertEquals("No ABSOLUTE_ADDRESS_LOAD was declared but one was expected",
        throwable.reason);
  }

  @Test
  void shouldThrow_whenStackPointerMissing() {
    var prog = """
          special return instruction = NOP
          special call instruction = NOP
          special local address load instruction = NOP
          special absolute address load instruction = NOP
          alias register zero = X(0)
          // stack pointer = zero
          return address = zero
          global pointer = zero
          frame pointer = zero
          thread pointer = zero
          return value = zero
          function argument = zero
          caller saved = zero
          callee saved = zero
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAbi(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    var throwable = Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    Assertions.assertEquals(Diagnostic.Level.ERROR, throwable.level);
    Assertions.assertEquals("No STACK_POINTER registers were declared but one was expected",
        throwable.reason);
  }

  @Test
  void shouldThrow_whenStackPointerHasMultipleRegisters() {
    var prog = """
          special return instruction = NOP
          special call instruction = NOP
          special local address load instruction = NOP
          special absolute address load instruction = NOP
          alias register zero = X(0)
          stack pointer = [ zero, zero]
          return address = zero
          global pointer = zero
          frame pointer = zero
          thread pointer = zero
          return value = zero
          function argument = zero
          caller saved = zero
          callee saved = zero
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAbi(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    var throwable = Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    Assertions.assertEquals(Diagnostic.Level.ERROR, throwable.level);
    Assertions.assertEquals("Number of registers is incorrect. This definition expects only one",
        throwable.reason);
  }

  @Test
  void shouldThrow_whenReturnAddressHasMultipleRegisters() {
    var prog = """
          special return instruction = NOP
          special call instruction = NOP
          special local address load instruction = NOP
          special absolute address load instruction = NOP
          alias register zero = X(0)
          stack pointer = zero
          return address = [ zero, zero]
          global pointer = zero
          frame pointer = zero
          thread pointer = zero
          return value = zero
          function argument = zero
          caller saved = zero
          callee saved = zero
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAbi(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    var throwable = Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    Assertions.assertEquals(Diagnostic.Level.ERROR, throwable.level);
    Assertions.assertEquals("Number of registers is incorrect. This definition expects only one",
        throwable.reason);
  }

  @Test
  void shouldThrow_whenGlobalPointerHasMultipleRegisters() {
    var prog = """
          special return instruction = NOP
          special call instruction = NOP
          special local address load instruction = NOP
          special absolute address load instruction = NOP
          alias register zero = X(0)
          stack pointer = zero
          return address = zero
          global pointer = [ zero, zero]
          frame pointer = zero
          thread pointer = zero
          return value = zero
          function argument = zero
          caller saved = zero
          callee saved = zero
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAbi(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    var throwable = Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    Assertions.assertEquals(Diagnostic.Level.ERROR, throwable.level);
    Assertions.assertEquals("Number of registers is incorrect. This definition expects only one",
        throwable.reason);
  }

  @Test
  void shouldThrow_whenFramePointerHasMultipleRegisters() {
    var prog = """
          special return instruction = NOP
          special call instruction = NOP
          special local address load instruction = NOP
          special absolute address load instruction = NOP
          alias register zero = X(0)
          stack pointer = zero
          return address = zero
          global pointer = zero
          frame pointer = [ zero, zero]
          thread pointer = zero
          return value = zero
          function argument = zero
          caller saved = zero
          callee saved = zero
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAbi(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    var throwable = Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    Assertions.assertEquals(Diagnostic.Level.ERROR, throwable.level);
    Assertions.assertEquals("Number of registers is incorrect. This definition expects only one",
        throwable.reason);
  }

  @Test
  void shouldThrow_whenThreadPointerHasMultipleRegisters() {
    var prog = """
          special return instruction = NOP
          special call instruction = NOP
          special local address load instruction = NOP
          special absolute address load instruction = NOP
          alias register zero = X(0)
          stack pointer = zero
          return address = zero
          global pointer = zero
          frame pointer = zero
          thread pointer = [ zero, zero]
          return value = zero
          function argument = zero
          caller saved = zero
          callee saved = zero
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAbi(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    var throwable = Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    Assertions.assertEquals(Diagnostic.Level.ERROR, throwable.level);
    Assertions.assertEquals("Number of registers is incorrect. This definition expects only one",
        throwable.reason);
  }

  @Test
  void shouldThrow_whenFramePointerMissing() {
    var prog = """
          special return instruction = NOP
          special call instruction = NOP
          special local address load instruction = NOP
          special absolute address load instruction = NOP
          alias register zero = X(0)
          stack pointer = zero
          return address = zero
          global pointer = zero
          // frame pointer = zero
          thread pointer = zero
          return value = zero
          function argument = zero
          caller saved = zero
          callee saved = zero
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAbi(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    var throwable = Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    Assertions.assertEquals(Diagnostic.Level.ERROR, throwable.level);
    Assertions.assertEquals("No FRAME_POINTER registers were declared but one was expected",
        throwable.reason);
  }

  @Test
  void shouldThrow_whenReturnValueMissing() {
    var prog = """
          special return instruction = NOP
          special call instruction = NOP
          special local address load instruction = NOP
          special absolute address load instruction = NOP
          alias register zero = X(0)
          stack pointer = zero
          return address = zero
          global pointer = zero
          frame pointer = zero
          thread pointer = zero
          //return value = zero
          function argument = zero
          caller saved = zero
          callee saved = zero
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAbi(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    var throwable = Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    Assertions.assertEquals(Diagnostic.Level.ERROR, throwable.level);
    Assertions.assertEquals("No RETURN_VALUE registers were declared but one was expected",
        throwable.reason);
  }

  @Test
  void shouldThrow_whenFunctionArgumentMissing() {
    var prog = """
          special return instruction = NOP
          special call instruction = NOP
          special local address load instruction = NOP
          special absolute address load instruction = NOP
          alias register zero = X(0)
          stack pointer = zero
          return address = zero
          global pointer = zero
          frame pointer = zero
          thread pointer = zero
          return value = zero
          //function argument = zero
          caller saved = zero
          callee saved = zero
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAbi(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    var throwable = Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    Assertions.assertEquals(Diagnostic.Level.ERROR, throwable.level);
    Assertions.assertEquals("No FUNCTION_ARGUMENT registers were declared but one was expected",
        throwable.reason);
  }

  @Test
  void shouldThrow_whenCallerSavedMissing() {
    var prog = """
          special return instruction = NOP
          special call instruction = NOP
          special local address load instruction = NOP
          special absolute address load instruction = NOP
          alias register zero = X(0)
          stack pointer = zero
          return address = zero
          global pointer = zero
          frame pointer = zero
          thread pointer = zero
          return value = zero
          function argument = zero
          //caller saved = zero
          callee saved = zero
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAbi(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    var throwable = Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    Assertions.assertEquals(Diagnostic.Level.ERROR, throwable.level);
    Assertions.assertEquals("No CALLER_SAVED registers were declared but one was expected",
        throwable.reason);
  }

  @Test
  void shouldThrow_whenCalleeSavedMissing() {
    var prog = """
          special return instruction = NOP
          special call instruction = NOP
          special local address load instruction = NOP
          special absolute address load instruction = NOP
          alias register zero = X(0)
          stack pointer = zero
          return address = zero
          global pointer = zero
          frame pointer = zero
          thread pointer = zero
          return value = zero
          function argument = zero
          caller saved = zero
          //callee saved = zero
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAbi(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    var throwable = Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    Assertions.assertEquals(Diagnostic.Level.ERROR, throwable.level);
    Assertions.assertEquals("No CALLEE_SAVED registers were declared but one was expected",
        throwable.reason);
  }

  @Test
  void shouldThrow_wheMultipleFramePointerDefined() {
    var prog = """
          special return instruction = NOP
          special call instruction = NOP
          special local address load instruction = NOP
          special absolute address load instruction = NOP
          alias register zero = X(0)
          stack pointer = zero
          return address = zero
          global pointer = zero
          frame pointer = zero
          frame pointer = zero
          thread pointer = zero
          return value = zero
          function argument = zero
          caller saved = zero
          callee saved = zero
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAbi(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    var throwable = Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    Assertions.assertEquals(Diagnostic.Level.ERROR, throwable.level);
    Assertions.assertEquals(
        "Multiple FRAME_POINTER registers were declared but only one was expected",
        throwable.reason);
  }

  @Test
  void shouldThrow_whenMultiplePseudoLocalAddressLoadInstructionDefined() {
    var prog = """
          special return instruction = NOP
          special call instruction = NOP
          special local address load instruction = NOP
          special local address load instruction = NOP
          special absolute address load instruction = NOP
          alias register zero = X(0)
          stack pointer = zero
          return address = zero
          global pointer = zero
          frame pointer = zero
          thread pointer = zero
          return value = zero
          function argument = zero
          caller saved = zero
          callee saved = zero
        """;
    var ast = Assertions.assertDoesNotThrow(
        () -> VadlParser.parse(inputWrappedByValidAbi(prog)), "Cannot parse input");
    var typechecker = new TypeChecker();
    var throwable = Assertions.assertThrows(Diagnostic.class, () -> typechecker.verify(ast));
    Assertions.assertEquals(Diagnostic.Level.ERROR, throwable.level);
    Assertions.assertEquals(
        "Multiple LOCAL_ADDRESS_LOAD were declared but one was expected",
        throwable.reason);
  }

}