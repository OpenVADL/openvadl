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

package vadl.iss;

import ch.qos.logback.classic.Level;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vadl.AbstractTest;
import vadl.configuration.GeneralConfiguration;
import vadl.configuration.IssConfiguration;
import vadl.dump.HtmlDumpPass;
import vadl.pass.PassOrders;
import vadl.pass.exception.DuplicatedPassKeyException;

public class IssLoweringTest extends AbstractTest {

  private static final Logger log = LoggerFactory.getLogger(IssLoweringTest.class);

  @BeforeEach
  void setUp() {
    var logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(HtmlDumpPass.class);
    logger.setLevel(Level.DEBUG);
  }
  
  @Test
  void issRiscvLoweringTest() throws IOException, DuplicatedPassKeyException {
    var config =
        new IssConfiguration(new GeneralConfiguration(Path.of("build/test-output"), true));

    setupPassManagerAndRunSpec("sys/risc-v/rv64im.vadl",
        PassOrders.iss(config)
    );
  }

  @Test
  void issAarch64LoweringTest() throws IOException, DuplicatedPassKeyException {
    var config =
        new IssConfiguration(new GeneralConfiguration(Path.of("build/test-output"), false));

    setupPassManagerAndRunSpec("sys/aarch64/virt.vadl",
        PassOrders.iss(config)
    );
  }
}
