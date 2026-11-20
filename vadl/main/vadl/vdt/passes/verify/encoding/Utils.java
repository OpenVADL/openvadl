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

package vadl.vdt.passes.verify.encoding;

import static vadl.error.Diagnostic.error;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import vadl.configuration.GeneralConfiguration;
import vadl.dump.ArtifactTracker;
import vadl.error.Diagnostic;
import vadl.vdt.impl.irregular.model.DecodeEntry;
import vadl.vdt.utils.BitVector;
import vadl.vdt.utils.SequentialInstructionDecoder;
import vadl.viam.Definition;
import vadl.viam.Specification;

/**
 * Utility functions shared by the varification passes.
 */
public class Utils {

  static Diagnostic overlappingInstructionError(BitVector encoding, List<DecodeEntry> entries) {

    // Find all encoding definitions matching the counterexample

    final var decoder = new SequentialInstructionDecoder(entries);
    final List<DecodeEntry> overlaps = decoder.decode(encoding);

    // Construct the diagnostic error

    var primary = overlaps.getFirst().source();
    var insnNames = overlaps.stream()
        .map(DecodeEntry::source)
        .map(Definition::simpleName)
        .toList();

    var diagnostic =
        error(("Overlapping instruction encoding detected: %s. E.g. the encoding 0x%x matches all "
            + "listed encoding definitions.").formatted(insnNames,
            encoding.value()), primary);

    for (DecodeEntry e : overlaps) {
      var others = insnNames.stream()
          .filter(n -> !n.equals(e.source().simpleName())).toList();

      diagnostic.locationDescription(e.source().encoding(),
          "Instruction encoding overlaps with other instruction%s: %s",
          others.size() != 1 ? "s" : "",
          others.size() == 1 ? others.getFirst() : others);
    }

    return diagnostic.build();
  }

  static void dump(GeneralConfiguration config, Specification viam, String content, String suffix)
      throws IOException {
    Path outputDir = config.outputPath()
        .resolve("dump");
    Files.createDirectories(outputDir);

    var outFile = outputDir.resolve(sanitize(viam.simpleName()) + "-encoding." + suffix);
    Files.writeString(outFile, content, StandardCharsets.UTF_8);
    ArtifactTracker.addDump(outFile);
  }

  static String sanitize(String value) {
    var sanitized = value.replaceAll("[^\\w.-]", "_");
    if (sanitized.isBlank()) {
      return "unnamed";
    }
    return sanitized;
  }

}
