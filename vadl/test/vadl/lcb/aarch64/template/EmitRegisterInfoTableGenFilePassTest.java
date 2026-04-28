// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.lcb.aarch64.template;

import static vadl.TestUtils.assertEqualsFileLines;

import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import vadl.gcb.valuetypes.TargetName;
import vadl.lcb.AbstractLcbTest;
import vadl.lcb.template.lib.Target.EmitRegisterInfoTableGenFilePass;
import vadl.pass.PassKey;
import vadl.pass.exception.DuplicatedPassKeyException;
import vadl.template.AbstractTemplateRenderingPass;

public class EmitRegisterInfoTableGenFilePassTest extends AbstractLcbTest {
  @Test
  void testLowering() throws IOException, DuplicatedPassKeyException {
    // Given
    var configuration = getConfiguration(false);
    configuration.setTargetName(new TargetName("aarch64temp"));
    var testSetup = runLcb(configuration, "sys/aarch64/aarch64-abi.vadl",
        new PassKey(EmitRegisterInfoTableGenFilePass.class.getName()));

    // When
    var passResult =
        (AbstractTemplateRenderingPass.Result) testSetup.passManager().getPassResults()
            .lastResultOf(EmitRegisterInfoTableGenFilePass.class);

    // Then
    var resultFile = passResult.emittedFile().toFile();
    var actual = FileUtils.readFileToString(resultFile, "UTF-8")
        .lines()
        .map(String::stripTrailing)
        .collect(Collectors.joining("\n"));

    var fs = Path.of(
        "test/resources/snapshots/aarch64/RegisterInfo.td");

    assertEqualsFileLines(fs, actual);
  }
}
