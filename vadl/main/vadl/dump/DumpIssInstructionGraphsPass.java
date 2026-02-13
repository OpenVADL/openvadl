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

package vadl.dump;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import vadl.configuration.GeneralConfiguration;
import vadl.pass.Pass;
import vadl.pass.PassKey;
import vadl.pass.PassName;
import vadl.pass.PassResults;
import vadl.utils.ViamUtils;
import vadl.viam.Instruction;
import vadl.viam.Specification;

/**
 * Dumps one dot file per instruction behavior after each executed pass.
 *
 * <p>Files are written to:
 * {@code <output>/dump/iss-pass-graphs/<pass-key>/<instruction>.dot}</p>
 */
public class DumpIssInstructionGraphsPass extends Pass {
  public record Result(Path outputDirectory, int fileCount, PassKey sourcePass) {
  }

  public DumpIssInstructionGraphsPass(GeneralConfiguration configuration) {
    super(configuration);
  }

  @Override
  public PassName getName() {
    return PassName.of("Dump ISS Instruction Graphs");
  }

  @Override
  public Result execute(PassResults passResults, Specification viam) throws IOException {
    if (passResults.size() == 0) {
      var emptyDir = configuration().outputPath().resolve("dump").resolve("iss-pass-graphs");
      Files.createDirectories(emptyDir);
      return new Result(emptyDir, 0, new PassKey("none"));
    }

    var previousPass = passResults.lastExecution();
    var passDirName = sanitize(previousPass.passKey().value());
    var outputDir = configuration().outputPath()
        .resolve("dump")
        .resolve("iss-pass-graphs")
        .resolve(passDirName);
    Files.createDirectories(outputDir);

    var counts = new HashMap<String, Integer>();
    int written = 0;

    var instructions = ViamUtils.findDefinitionsByFilter(viam, d -> d instanceof Instruction)
        .stream()
        .map(Instruction.class::cast)
        .sorted((a, b) -> a.identifier().name().compareTo(b.identifier().name()))
        .toList();

    for (var instruction : instructions) {
      var baseName = sanitize(instruction.identifier().name());
      var fileName = uniqueName(baseName, counts) + ".dot";
      var outFile = outputDir.resolve(fileName);
      var dot = CollectBehaviorDotGraphPass.createDotGraphFor(instruction.behavior());
      Files.writeString(outFile, dot, StandardCharsets.UTF_8);
      ArtifactTracker.addDump(outFile);
      written++;
    }

    return new Result(outputDir, written, previousPass.passKey());
  }

  private static String uniqueName(String base, Map<String, Integer> usedNames) {
    var count = usedNames.getOrDefault(base, 0);
    usedNames.put(base, count + 1);
    if (count == 0) {
      return base;
    }
    return base + "_" + count;
  }

  private static String sanitize(String value) {
    var sanitized = value.replaceAll("[^\\w.-]", "_");
    if (sanitized.isBlank()) {
      return "unnamed";
    }
    return sanitized;
  }
}
