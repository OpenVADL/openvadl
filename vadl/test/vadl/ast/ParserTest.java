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

import static org.assertj.core.api.Assertions.assertThat;
import static vadl.ast.AstTestUtils.verifyPrettifiedAst;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.Streams;
import org.testcontainers.shaded.org.checkerframework.checker.nullness.qual.Nullable;
import vadl.error.DiagnosticList;

/**
 * A class to test the parser. Since the parser does  also name resolution and macro expansion this
 * class only focuses on correct parsing and the two other tasks are tested in different classes.
 */
public class ParserTest {

  @Test
  void parseEmpty() {
    var prog = "";
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }

  @Test
  void commonConstantDefinition() {
    var prog = "constant a = 13";
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }

  @Test
  void emptyIsa() {
    var prog = """
        instruction set architecture imaginaryIsa = {
        }
        """;

    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }

  @Test
  void minimalIsa() {
    var prog = """
        instruction set architecture Flo = {
        constant jojo = 42
        constant paul = 40 + 4 * 8
        }
        """;

    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }

  @Test
  void comments() {
    var prog = """
        // Some invalid code here 
        /* also here */
        """;

    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }

  @Test
  void contantsWithTypeAnnotation() {
    var prog = """
        constant a: Bool = 1
        constant b: Bits<12> = 12
        constant c: SInt<64> = 42
        constant d: UInt<32> = 13
        """;

    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }

  @Test
  void contantsWithAdvancedTypeAnnotation() {
    // FIXME: we will need to adapt this test once we check if certain types exist
    // FIXME: Reenable the last line
    var prog = """
        constant size = 64
        constant a: customBoolean = 1
        constant b: Bits<size> = 1
        //constant c: SInt<1+2> = 1
        """;

    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }

  @Test
  void memoryDefinition() {
    var prog = """
        instruction set architecture FLO = {
          memory mem: Bits<32> -> Bits<8>
        }
        """;

    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }

  @Test
  void programCounterDefinition() {
    var prog = """
        instruction set architecture FLO = {
          program counter PC : Bits<32>   // PC = program counter
        }
        """;

    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }

  @Test
  void groupCounterDefinition() {
    var prog = """
        instruction set architecture FLO = {
          group counter PFC : Bits<32>    // PFC = program fetch counter
        }
        """;

    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }

  @Test
  void registerDefinition() {
    var prog = """
        instruction set architecture FLO = {
          register Y : Bits<32>
        }
        """;

    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }

  @Test
  void registerFileDefinition() {
    var prog = """
        instruction set architecture FLO = {
          register X : Bits<5> -> Bits<32>
        }
        """;

    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }

  @Test
  void formatDefinition() {
    var prog = """
        format I_TYPE : Bits<32> =
        { funct6 [31..26]
        , shamt  [25..20]
        , rs1    [19..15]
        , funct3 [14..12]
        , rd     [11..7]
        , opcode [6..0]
        }
        """;

    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }

  @Test
  void relocationDefinition() {
    var prog = """
        instruction set architecture ISA = {
          relocation HI ( symbol : Bits <32> ) -> Bits <16> = ( symbol >> 16 ) & 0xFFFF
        }
        """;
    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }

  @Test
  void unaryOperators() {
    var prog = """
        constant a = -9
        constant b = !(a = 3)
        constant c = ~a
        constant d = -4 as Bits<32>
        constant e = -(4 as Bits<32>)
        constant f = ~(3 as Bits<32>)
        constant g = !(3 as Bool)
        """;

    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog), "Cannot parse input");
    verifyPrettifiedAst(ast);
  }

  @Test
  void symbolConflictIsaInheritance() {
    var prog = """
        instruction set architecture Base0 = {
            program counter PC: Bits<32>
        }
        instruction set architecture Base1 extending Base0 = { }
        instruction set architecture Sub extending Base1 = {
            constant PC = 4
        }
        """;

    var diags = Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog));
    assertThat(diags.items).anySatisfy(diag -> {
      assertThat(diag).hasMessageContaining("Symbol name already used: PC");
    });
  }

  @Test
  void symbolConflictIsaMultiInheritance() {
    var prog = """
        instruction set architecture Base0 = {
            program counter PC: Bits<32>
        }
        instruction set architecture Base1 extending Base0 = { }
        instruction set architecture Base2 = {
            constant PC = 4
        }
        instruction set architecture Sub extending Base1, Base2 = { }
        """;

    var diags = Assertions.assertThrows(DiagnosticList.class, () -> VadlParser.parse(prog));
    assertThat(diags.items).anySatisfy(diag -> {
      assertThat(diag).hasMessageContaining("Symbol name already used: PC");
    });
  }

  @Test
  void symbolNoConflictOnDiamondPattern() {
    // NOTE: This is not ISA multi-inheritance exclusive, but can also happen in
    // other cases, such as a ISA - ABI - ISA diamond pattern.
    var prog = """
        instruction set architecture Base = {
          constant X = 3
        }
        instruction set architecture Base0 extending Base = { }
        instruction set architecture Base1 extending Base = { }
        instruction set architecture Sub extending Base0, Base1 = { }
        """;

    Assertions.assertDoesNotThrow(() -> VadlParser.parse(prog));
  }


  @Test
  void testRv32im() throws IOException {
    testPrettyPrintSpec(Path.of("../sys/risc-v"), "rv3264im.vadl", "rv32im.vadl");
  }

  private void testPrettyPrintSpec(Path rootDir, String... vadlSpecs) throws IOException {
    var absRoot = rootDir.toAbsolutePath();
    var sysName = rootDir.getFileName();
    var prettyPath = Path.of("build/test/pretty-print/" + sysName);
    if (prettyPath.toFile().exists()) {
      FileUtils.forceDelete(prettyPath.toFile());
    }

    var specFiles = Arrays.stream(vadlSpecs).map(s -> absRoot.resolve(s).toFile())
        .toList();
    var prettyFiles = Arrays.stream(vadlSpecs)
        .map(s -> prettyPath.resolve(s).toFile())
        .toList();

    // first check all handwirtten specs
    var originalAsts = checkAll(specFiles, prettyPath);
    var prettyPrintAsts = checkAll(prettyFiles, null);

    Streams.forEachPair(prettyPrintAsts.stream(), originalAsts.stream(), (actual, expected) ->
        assertThat(actual.prettyPrintToString()).isEqualTo(expected.prettyPrintToString()));
  }

  private List<Ast> checkAll(List<File> specs, @Nullable Path prettyPrintPath) {
    return specs.stream().map(s -> check(s, prettyPrintPath)).toList();
  }

  private Ast check(File vadlFile, @Nullable Path prettyPrintPath) {
    System.out.println("Checking " + vadlFile);
    AssertionsForClassTypes.assertThat(vadlFile).exists();

    var ast = Assertions.assertDoesNotThrow(() -> VadlParser.parse(vadlFile.toPath()),
        "Cannot parse input");
    new Ungrouper().ungroup(ast);
    new ModelRemover().removeModels(ast);
    if (prettyPrintPath != null) {
      var progPretty = ast.prettyPrintToString();
      var resFile = prettyPrintPath.resolve(vadlFile.getName()).toFile();
      try {
        FileUtils.createParentDirectories(resFile);
        FileUtils.writeStringToFile(resFile, progPretty, Charset.defaultCharset());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return ast;
  }
}
