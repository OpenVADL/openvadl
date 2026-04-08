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

package vadl.rtl.riscv;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import vadl.configuration.DecoderOptions;
import vadl.configuration.DumpMode;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.RtlConfiguration;
import vadl.rtl.RtlDockerTest;

class RtlRiscVInstructionTest extends RtlDockerTest {

    @Test
    void rv64imFiveTest() {

      /* GIVEN */
      var generalConfig =
          new GeneralConfiguration(Path.of("build/test-output"), DumpMode.NONE);
      var config = new RtlConfiguration(generalConfig);
      config.setResetVector("reset_vector");

      var decoderOptions = new DecoderOptions();
      decoderOptions.setGenerator(DecoderOptions.Generator.REGULAR);
      config.setDecoderOptions(decoderOptions);

      var image = generateRtlImage("sys/risc-v/mia/rv_5stage.vadl", config);

      runContainer(image,
          /* WHEN */
          c -> c.withCommand("/scripts/test.sh"),
          /* THEN */
          c -> {
            // No post actions for now, we rely on the exit code instead
          });
    }

}
