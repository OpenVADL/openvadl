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

import java.util.Set;
import picocli.CommandLine;
import picocli.CommandLine.MaxValuesExceededException;
import vadl.configuration.DecoderOptions;
import vadl.configuration.DecoderOptions.Generator;
import vadl.configuration.DecoderOptions.OptionalStep;

/**
 * Base functionality for the decoder option mixins.
 */
public abstract class DecoderMixin {

  protected abstract Set<DecoderOpt> getOptions();

  /**
   * Get the decoder options passed by the user.
   *
   * @param cmd The command line.
   * @return The options.
   */
  public DecoderOptions getDecoderOptions(CommandLine cmd) {

    final var decoderOptions = getOptions();

    if (decoderOptions == null) {
      return new DecoderOptions();
    }

    final DecoderOptions result = new DecoderOptions();

    var strategies = decoderOptions.stream()
        .filter(DecoderStrategy.class::isInstance)
        .map(DecoderStrategy.class::cast)
        .toList();

    if (strategies.size() > 1) {

      throw new MaxValuesExceededException(cmd, "Multiple decoder strategies are not allowed.");
    }

    if (strategies.size() == 1) {
      result.setGenerator(strategies.getFirst().generator());
    }

    decoderOptions.stream()
        .filter(DecoderStep.class::isInstance)
        .map(DecoderStep.class::cast)
        .forEach(step -> result.getOpts().put(step.step(), step.enabled()));

    return result;
  }

  /**
   * Common base interface for a decoder option.
   */
  public sealed interface DecoderOpt {
  }

  /**
   * Strategy option.
   *
   * @param generator The generator strategy.
   */
  public record DecoderStrategy(Generator generator) implements DecoderOpt {
  }

  /**
   * Optional step option.
   *
   * @param step The step to skip.
   */
  public record DecoderStep(OptionalStep step, boolean enabled) implements DecoderOpt {
  }

}
