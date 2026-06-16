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

package vadl.iss.passes;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.configuration.DumpMode;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.common.planning.IssExecStrategyPass;
import vadl.pass.PassName;
import vadl.pass.PassOrders;
import vadl.pass.PassResults;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.viam.Instruction;
import vadl.viam.Specification;

public class IssExecutionStrategyRoutingTest extends AbstractTest {

  @Test
  void separatesNormalTcgDirectGvecCandidateAndWholeHelperInstructionRoutes()
      throws IOException, DuplicatedPassKeyException {
    var viam = analyze("sys/risc-v/rv64v.vadl");
    var routeProbe = new RouteProbePass(config());

    var normalTcgNames = simpleNames(routeProbe.normalTcgInstrs(viam));
    var directGvecNames = simpleNames(routeProbe.directGvecCandidateInstrs(viam));
    var helperNames = simpleNames(routeProbe.wholeHelperInstrs(viam));

    assertTrue(normalTcgNames.contains("VSETVLI"));
    assertTrue(normalTcgNames.contains("VADD_VV"));
    assertTrue(normalTcgNames.contains("VADD_VX"));
    assertTrue(normalTcgNames.contains("VADD_VI"));

    assertTrue(directGvecNames.contains("VADD_VV"));
    assertTrue(directGvecNames.contains("VSUB_VV"));
    assertTrue(directGvecNames.contains("VADD_VX"));
    assertTrue(directGvecNames.contains("VADD_VI"));
    assertFalse(directGvecNames.contains("VSETVLI"));

    assertFalse(helperNames.contains("VADD_VX"));
    assertFalse(helperNames.contains("VADD_VI"));
    assertFalse(helperNames.contains("VADD_VV"));
  }

  @Test
  void keepsVectorBenchCompositeScalarSideEffectOnNormalTcgRoute()
      throws IOException, DuplicatedPassKeyException {
    var viam = analyze("sys/vectorbench/vectorbench64.vadl");
    var routeProbe = new RouteProbePass(config());

    var normalTcgNames = simpleNames(routeProbe.normalTcgInstrs(viam));
    var directGvecNames = simpleNames(routeProbe.directGvecCandidateInstrs(viam));
    var helperNames = simpleNames(routeProbe.wholeHelperInstrs(viam));

    assertTrue(normalTcgNames.contains("VADD_XINC"));
    assertTrue(directGvecNames.contains("VADD_XINC"));
    assertFalse(helperNames.contains("VADD_XINC"));
  }

  private Specification analyze(String specPath) throws IOException, DuplicatedPassKeyException {
    return setupPassManagerAndRunSpec(specPath,
        PassOrders.iss(config()).untilFirst(IssExecStrategyPass.class)
    ).specification();
  }

  private IssConfiguration config() {
    return new IssConfiguration(
        new GeneralConfiguration(Path.of("build/test-output"), DumpMode.NONE)
    );
  }

  private List<String> simpleNames(java.util.stream.Stream<Instruction> instructions) {
    return instructions.map(Instruction::simpleName).sorted().toList();
  }

  private static final class RouteProbePass extends AbstractIssPass {
    private RouteProbePass(IssConfiguration configuration) {
      super(configuration);
    }

    @Override
    public PassName getName() {
      return PassName.of("ISS Route Probe");
    }

    @Override
    public @Nullable Object execute(PassResults passResults, Specification viam) {
      return null;
    }
  }
}
