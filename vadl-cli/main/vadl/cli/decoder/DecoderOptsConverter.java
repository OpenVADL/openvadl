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

package vadl.cli.decoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nonnull;
import picocli.CommandLine;
import vadl.cli.decoder.mixin.DecoderMixin.DecoderOpt;
import vadl.cli.decoder.mixin.DecoderMixin.DecoderStep;
import vadl.cli.decoder.mixin.DecoderMixin.DecoderStrategy;
import vadl.configuration.DecoderOptions;

/**
 * Base decoder options contributor and converter.
 */
public abstract class DecoderOptsConverter
    implements Iterable<String>, CommandLine.ITypeConverter<DecoderOpt> {

  private static final String KEY_STRATEGY = "strategy";
  private static final String KEY_STEP_INCLUDE = "with";
  private static final String KEY_STEP_EXCLUDE = "skip";

  /**
   * Allow subclasses to override the available options.
   */
  protected DecoderOptions.Generator[] strategies = DecoderOptions.Generator.values();
  protected DecoderOptions.OptionalStep[] steps = DecoderOptions.OptionalStep.values();

  @Override
  public DecoderOpt convert(String value) {
    final String[] fragments = value.split("=", -1);
    if (fragments.length != 2) {
      throw new CommandLine.TypeConversionException(
          "Unable to parse decoder option '%s'".formatted(value));
    }

    if (KEY_STRATEGY.equals(fragments[0].trim())) {
      var val = fragments[1].trim();
      var strategy = DecoderOptions.Generator.fromSelector(val, strategies);
      if (strategy != null) {
        return new DecoderStrategy(strategy);
      }

      throw new CommandLine.TypeConversionException(
          "Unable to parse decoder strategy '%s'. Available strategies are: %s".formatted(val,
              Arrays.stream(strategies)
                  .map(DecoderOptions.Generator::getSelector).toList()));
    }

    if (KEY_STEP_EXCLUDE.equals(fragments[0].trim())) {
      var val = fragments[1].trim();
      var step = DecoderOptions.OptionalStep.fromSelector(val, steps);
      if (step != null) {
        return new DecoderStep(step, false);
      }

      throw new CommandLine.TypeConversionException(
          "Unable to parse decoder step to disable: '%s'. Available options are: %s".formatted(val,
              Arrays.stream(steps)
                  .map(DecoderOptions.OptionalStep::getSelector).toList()));
    }

    if (KEY_STEP_INCLUDE.equals(fragments[0].trim())) {
      var val = fragments[1].trim();
      var step = DecoderOptions.OptionalStep.fromSelector(val, steps);
      if (step != null) {
        return new DecoderStep(step, true);
      }

      throw new CommandLine.TypeConversionException(
          "Unable to parse decoder step to enable: '%s'. Available options are: %s".formatted(val,
              Arrays.stream(steps)
                  .map(DecoderOptions.OptionalStep::getSelector).toList()));
    }

    throw new CommandLine.TypeConversionException(
        "Illegal decoder step '%s'. Available options are: %s".formatted(value,
            List.of(KEY_STEP_INCLUDE, KEY_STEP_EXCLUDE, KEY_STRATEGY)));
  }

  /**
   * Get available options.
   *
   * @return The formatted options.
   */
  public List<String> getOptions() {

    final List<String> options = new ArrayList<>();
    for (DecoderOptions.Generator generator : strategies) {
      options.add(
          "%n%s=%s (%s)".formatted(KEY_STRATEGY, generator.getSelector(), generator.getDesc()));
    }
    for (DecoderOptions.OptionalStep step : steps) {
      options.add(
          "%n[%s|%s]=%s (%s)".formatted(KEY_STEP_INCLUDE, KEY_STEP_EXCLUDE,
              step.getSelector(), step.getDesc()));
    }
    return options;
  }

  @Nonnull
  @Override
  public Iterator<String> iterator() {
    return getOptions().iterator();
  }
}
