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

package vadl.rtl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import vadl.AbstractTest;
import vadl.configuration.DecoderOptions;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.RtlConfiguration;
import vadl.dump.HtmlDumpPass;
import vadl.pass.Pass;
import vadl.pass.PassName;
import vadl.pass.PassOrder;
import vadl.pass.PassOrders;
import vadl.pass.PassResults;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.rtl.passes.DebugOutputPass;
import vadl.rtl.passes.HazardAnalysisPass;
import vadl.rtl.passes.InstructionProgressGraphCreationPass;
import vadl.rtl.passes.InstructionProgressGraphLowerPass;
import vadl.rtl.passes.InstructionProgressGraphMergePass;
import vadl.rtl.passes.MiaMappingCreationPass;
import vadl.rtl.passes.MiaMappingOptimizePass;
import vadl.viam.RegisterTensor;
import vadl.viam.Specification;

/**
 * Simple test of the MiA synthesis steps using {@link InstructionBehaviorCheckPass}.
 */
public class RtlLoweringTest extends AbstractTest {

  private static final Set<String> instructions = Collections.emptySet(); // all instructions

  @Test
  void instructionBehaviorCheck() throws IOException, DuplicatedPassKeyException {

    var generalConfig =
        new GeneralConfiguration(Path.of("build/test-output"), true);
    var config = new RtlConfiguration(generalConfig);
    config.setDummyMia(RtlConfiguration.DummyMia.five);

    var decoderOptions = new DecoderOptions();
    decoderOptions.setGenerator(DecoderOptions.Generator.REGULAR);
    config.setDecoderOptions(decoderOptions);

    var order = PassOrders.rtl(config);
    order.addAfterFirst(PassOrders.ViamCreationPass.class,
        new PruneIsaPass(config, instructions, false));

    addDumpAndCheck(config, order, InstructionProgressGraphCreationPass.class);
    addDumpAndCheck(config, order, MiaMappingCreationPass.class);
    addDumpAndCheck(config, order, InstructionProgressGraphMergePass.class);
    addDumpAndCheck(config, order, MiaMappingOptimizePass.class);
    addDumpAndCheck(config, order, InstructionProgressGraphLowerPass.class, false);

    setupPassManagerAndRunSpec("sys/risc-v/rv32i.vadl", order);
  }

  private void addDumpAndCheck(GeneralConfiguration config, PassOrder order, Class<?> selector) {
    addDumpAndCheck(config, order, selector, true);
  }

  private void addDumpAndCheck(GeneralConfiguration config, PassOrder order, Class<?> selector,
                               boolean useInstructionContext) {

    order.addAfterFirst(selector, new InstructionBehaviorCheckPass(config, useInstructionContext));
    if (config.doDump()) {
      order.addAfterFirst(selector, new HtmlDumpPass(
          HtmlDumpPass.Config.from(config, "check" + selector.getSimpleName(), "")));
    }

  }

  /**
   * Pass for testing that removes VIAM elements to get a simple test case.
   */
  public static class PruneIsaPass extends Pass {

    private final Set<String> instructions;

    private final boolean regTensorConstraints;

    /**
     * New prune ISA pass that removes all instructions, but the ones referenced by a set of names.
     *
     * @param config configuration
     * @param instructions set of instruction names
     */
    public PruneIsaPass(GeneralConfiguration config, Set<String> instructions) {
      super(config);
      this.instructions = instructions;
      this.regTensorConstraints = true;
    }

    /**
     * New prune ISA pass that removes all instructions, but the ones referenced by a set of names.
     * Optionally, remove register tensor constraints.
     *
     * @param config configuration
     * @param instructions set of instruction names
     * @param regTensorConstraints keep register tensor constraints, if true
     */
    public PruneIsaPass(GeneralConfiguration config, Set<String> instructions,
                        boolean regTensorConstraints) {
      super(config);
      this.instructions = instructions;
      this.regTensorConstraints = regTensorConstraints;
    }

    @Override
    public PassName getName() {
      return PassName.of("Prune ISA");
    }

    @Nullable
    @Override
    public Object execute(PassResults passResults, Specification viam) throws IOException {
      viam.isa().ifPresent(isa -> {
        if (!instructions.isEmpty()) {
          isa.ownInstructions().removeIf(ins -> !instructions.contains(ins.simpleName()));
        }
        if (!regTensorConstraints) {
          for (RegisterTensor regTensor : isa.registerTensors()) {
            regTensor.setConstraints();
          }
        }
      });
      return null;
    }

  }
}
