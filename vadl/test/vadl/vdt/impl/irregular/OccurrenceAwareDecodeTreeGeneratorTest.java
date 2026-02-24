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

package vadl.vdt.impl.irregular;

import static vadl.configuration.DecoderOptions.Generator.IRREGULAR;
import static vadl.configuration.DecoderOptions.Generator.OCCURRENCE_AWARE;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.AbstractTest;
import vadl.configuration.DecoderOptions;
import vadl.configuration.DumpMode;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.PassManager;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.vdt.model.Node;
import vadl.vdt.passes.VdtLoweringPass;
import vadl.vdt.target.common.DecisionTreeStatsCalculator;
import vadl.vdt.target.dump.TextGraphGenerator;

class OccurrenceAwareDecodeTreeGeneratorTest extends AbstractTest {

  private static final Logger log =
      LoggerFactory.getLogger(OccurrenceAwareDecodeTreeGeneratorTest.class);

  @Test
  void testGenerateVDTStatic() throws IOException, DuplicatedPassKeyException {

    /* GIVEN */

    var config = new GeneralConfiguration(Path.of("build/test-output"), DumpMode.NONE);
    config.setDecoderOptions(new DecoderOptions()
        .withGenerator(IRREGULAR));

    var spec = runAndGetViamSpecification("sys/risc-v/rv64im.vadl");

    var manager = new PassManager();
    manager.add(PassOrders.check(config));

    /* WHEN */
    manager.run(spec);

    /* THEN */

    var decodeTree = manager.getPassResults().lastResultOf(VdtLoweringPass.class, Node.class);

    Assertions.assertNotNull(decodeTree);

    log.info("Statistics: {}", DecisionTreeStatsCalculator.statistics(decodeTree));
  }

  static Stream<Arguments> argsGenerateVDTOccurrenceAware() {
    return DoubleStream
        .of(32, 16, 8, 4, 2, 1, 0.5, 0.25, 0.125, 0.0625)
        .mapToObj(Arguments::of);
  }

  @ParameterizedTest
  @CsvSource("1")
  //@MethodSource("argsGenerateVDTOccurrenceAware")
  void testGenerateVDTOccurrenceAware(double memoryPenalty) throws IOException, DuplicatedPassKeyException {

    /* GIVEN */

    var config = new GeneralConfiguration(Path.of("build/test-output"), DumpMode.NONE);
    config.setDecoderOptions(new DecoderOptions()
        .withGenerator(OCCURRENCE_AWARE)
        .withMemoryPenalty(memoryPenalty)
        .withOptsToSkip(DecoderOptions.OptionToSkip.OPT_DECODER_VERIFICATION)
    );

    var spec = runAndGetViamSpecification("sys/risc-v/rv64im.vadl");

    var manager = new PassManager();
    manager.add(PassOrders.check(config));

    /* WHEN */
    manager.run(spec);

    /* THEN */

    var decodeTree = manager.getPassResults().lastResultOf(VdtLoweringPass.class, Node.class);

    Assertions.assertNotNull(decodeTree);

    log.info("Statistics: {}", DecisionTreeStatsCalculator.statistics(decodeTree));
    log.info("VDT:\n{}", new TextGraphGenerator(decodeTree).generate());
  }
}
