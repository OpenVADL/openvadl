// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.iss.codegen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static vadl.TestUtils.findDefinitionByNameIn;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.configuration.DumpMode;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.common.planning.IssExecStrategyPass;
import vadl.iss.passes.tcg.lowering.TcgOpLoweringPass;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.viam.Instruction;
import vadl.viam.Specification;

public class IssTranslateCodeGeneratorTest extends AbstractTest {

  @Test
  void selectsSharedNonHelperGeneratorForRecognizedVectorPlan()
      throws IOException, DuplicatedPassKeyException {
    var viam = analyze("sys/risc-v/rv64v.vadl");
    var instr = findInstruction(viam, "RV64IMV::VADD_VV");

    var generator = IssTranslateCodeGenerator.translateGenerator(instr, config());

    assertInstanceOf(TcgTranslateGenerator.class, generator);
  }

  @Test
  void selectsHelperGeneratorForHelperPlan()
      throws IOException, DuplicatedPassKeyException {
    var viam = analyze("sys/risc-v/rv64v.vadl");
    var instr = findInstruction(viam, "RV64IMV::VDIV_VX");

    var generator = IssTranslateCodeGenerator.translateGenerator(instr, config());

    assertInstanceOf(HelperCallTranslateGenerator.class, generator);
  }

  @Test
  void selectsScalarGeneratorForScalarPlan()
      throws IOException, DuplicatedPassKeyException {
    var viam = analyze("sys/risc-v/rv64v.vadl");
    var instr = findInstruction(viam, "RV64IMV::VSETVLI");

    var generator = IssTranslateCodeGenerator.translateGenerator(instr, config());

    assertInstanceOf(TcgTranslateGenerator.class, generator);
  }

  @Test
  void emitsLoweredDirectGvecCallFromGraph()
      throws IOException, DuplicatedPassKeyException {
    var viam = analyzeWithLowering("sys/risc-v/rv64v.vadl");
    var instr = findInstruction(viam, "RV64IMV::VADD_VV");

    var code = IssTranslateCodeGenerator.fetch(instr, config());

    assertTrue(code.contains("tcg_gen_gvec_add("), code);
    assertTrue(code.contains("ofs_v(ctx, a->vd)"), code);
  }

  @Test
  void emitsLoweredDirectGvecCallForAliasDoVectorBenchInstruction()
      throws IOException, DuplicatedPassKeyException {
    var viam = analyzeWithLowering("sys/vectorbench/vectorbench64.vadl");
    var instr = findInstruction(viam, "VectorBench64::VADD_DO_VV");

    var code = IssTranslateCodeGenerator.fetch(instr, config());

    assertTrue(code.contains("tcg_gen_gvec_add("), code);
    assertTrue(code.contains("ofs_z(ctx, a->vd)"), code);
  }

  @Test
  void emitsLoweredDirectGvecScalarCallFromGraph()
      throws IOException, DuplicatedPassKeyException {
    var viam = analyzeWithLowering("sys/risc-v/rv64v.vadl");
    var instr = findInstruction(viam, "RV64IMV::VADD_VX");

    var code = IssTranslateCodeGenerator.fetch(instr, config());

    assertTrue(code.contains("tcg_gen_gvec_adds("), code);
    assertTrue(code.contains("tcg_gen_extract_i64("), code);
    assertTrue(code.contains("reg_x_rs1"), code);
  }

  @Test
  void emitsLoweredDirectGvecImmediateCallFromGraph()
      throws IOException, DuplicatedPassKeyException {
    var viam = analyzeWithLowering("sys/risc-v/rv64v.vadl");
    var instr = findInstruction(viam, "RV64IMV::VADD_VI");

    var code = IssTranslateCodeGenerator.fetch(instr, config());

    assertTrue(code.contains("tcg_gen_gvec_addi("), code);
    assertTrue(code.contains("a->imm"), code);
  }

  @Test
  void emitsAarch64SveUnpredicatedGvecCalls()
      throws IOException, DuplicatedPassKeyException {
    var viam = analyzeWithLowering("sys/aarch64/vprocessor.vadl");
    var cases = List.of(
        new SveCodegenCase("SVEunprADDB", "tcg_gen_gvec_add(", "MO_8"),
        new SveCodegenCase("SVEunprADDH", "tcg_gen_gvec_add(", "MO_16"),
        new SveCodegenCase("SVEunprADDS", "tcg_gen_gvec_add(", "MO_32"),
        new SveCodegenCase("SVEunprADDD", "tcg_gen_gvec_add(", "MO_64"),
        new SveCodegenCase("SVEunprSUBB", "tcg_gen_gvec_sub(", "MO_8"),
        new SveCodegenCase("SVEunprMULB", "tcg_gen_gvec_mul(", "MO_8")
    );

    for (var sveCase : cases) {
      var instr = findInstruction(
          viam,
          "AArch64SVEandSME::" + sveCase.instruction()
      );
      var code = IssTranslateCodeGenerator.fetch(instr, config());

      assertTrue(
          code.contains(sveCase.function()),
          code
      );
      assertTrue(code.contains(sveCase.memOp()), code);
      assertTrue(code.contains("ofs_z(ctx, a->zd)"), code);
      assertTrue(code.contains("ofs_z(ctx, a->zn)"), code);
      assertTrue(code.contains("ofs_z(ctx, a->zm)"), code);
      assertFalse(code.contains("gen_helper_"), code);
    }
  }

  private Specification analyze(String specPath) throws IOException, DuplicatedPassKeyException {
    return setupPassManagerAndRunSpec(specPath,
        PassOrders.iss(config()).untilFirst(IssExecStrategyPass.class)
    ).specification();
  }

  private Specification analyzeWithLowering(String specPath)
      throws IOException, DuplicatedPassKeyException {
    return setupPassManagerAndRunSpec(specPath,
        PassOrders.iss(config()).untilFirst(TcgOpLoweringPass.class)
    ).specification();
  }

  private IssConfiguration config() {
    return new IssConfiguration(
        new GeneralConfiguration(Path.of("build/test-output"), DumpMode.NONE)
    );
  }

  private Instruction findInstruction(Specification viam, String name) {
    return findDefinitionByNameIn(name, viam, Instruction.class);
  }

  private record SveCodegenCase(
      String instruction,
      String function,
      String memOp
  ) {
  }
}
