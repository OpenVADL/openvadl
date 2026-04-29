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

package vadl.cli.decoder.mixin;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import picocli.CommandLine;
import vadl.cli.decoder.DecoderOptsConverter;
import vadl.configuration.DecoderOptions.Generator;
import vadl.configuration.DecoderOptions.OptionalStep;

/**
 * Decoder options available to the {@link vadl.cli.IssCommand}.
 */
public class IssDecoderOptions extends DecoderMixin {

  @CommandLine.Option(names = "--decoder",
      split = ",",
      description = "Options for the decoder generation. Valid options are: "
          + "${COMPLETION-CANDIDATES}",
      completionCandidates = IssDecoderOptsContributor.class,
      converter = IssDecoderOptsContributor.class
  )
  private final Set<DecoderOpt> decoderOptions = new LinkedHashSet<>();

  @Override
  protected Set<DecoderOpt> getOptions() {
    return decoderOptions;
  }

  private static class IssDecoderOptsContributor extends DecoderOptsConverter {

    public IssDecoderOptsContributor() {
      strategies = Arrays.stream(Generator.values())
          .filter(g -> g != Generator.RTL_TABLE)
          .toArray(Generator[]::new);
      steps = Arrays.stream(OptionalStep.values())
          .filter(s -> s != OptionalStep.OPT_ALL)
          .toArray(OptionalStep[]::new);
    }

  }

}
