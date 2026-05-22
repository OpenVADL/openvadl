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

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static vadl.TestUtils.findDefinitionByNameIn;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.configuration.DumpMode;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.common.planning.IssExecStrategyPass;
import vadl.iss.passes.vector.IssDirectGvecLoweringPass;
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

    assertInstanceOf(ScalarTcgTranslateGenerator.class, generator);
  }

  @Test
  void selectsHelperGeneratorForHelperPlan()
      throws IOException, DuplicatedPassKeyException {
    var viam = analyze("sys/risc-v/rv64v.vadl");
    var instr = findInstruction(viam, "RV64IMV::VADD_VX");

    var generator = IssTranslateCodeGenerator.translateGenerator(instr, config());

    assertInstanceOf(HelperCallTranslateGenerator.class, generator);
  }

  @Test
  void selectsScalarGeneratorForScalarPlan()
      throws IOException, DuplicatedPassKeyException {
    var viam = analyze("sys/risc-v/rv64v.vadl");
    var instr = findInstruction(viam, "RV64IMV::VSETVLI");

    var generator = IssTranslateCodeGenerator.translateGenerator(instr, config());

    assertInstanceOf(ScalarTcgTranslateGenerator.class, generator);
  }

  @Test
  void emitsLoweredDirectGvecCallFromGraph()
      throws IOException, DuplicatedPassKeyException {
    var viam = analyzeWithLowering("sys/risc-v/rv64v.vadl");
    var instr = findInstruction(viam, "RV64IMV::VADD_VV");

    var code = IssTranslateCodeGenerator.fetch(instr, config());

    org.junit.jupiter.api.Assertions.assertTrue(code.contains("tcg_gen_gvec_add("), code);
    org.junit.jupiter.api.Assertions.assertTrue(code.contains("ofs_v(ctx, a->vd)"), code);
  }

  private Specification analyze(String specPath) throws IOException, DuplicatedPassKeyException {
    return setupPassManagerAndRunSpec(specPath,
        PassOrders.iss(config()).untilFirst(IssExecStrategyPass.class)
    ).specification();
  }

  private Specification analyzeWithLowering(String specPath)
      throws IOException, DuplicatedPassKeyException {
    return setupPassManagerAndRunSpec(specPath,
        PassOrders.iss(config()).untilFirst(IssDirectGvecLoweringPass.class)
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
}
