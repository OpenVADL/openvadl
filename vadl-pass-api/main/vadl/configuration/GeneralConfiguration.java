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

package vadl.configuration;

import java.nio.file.Path;
import javax.annotation.Nullable;

/**
 * This configuration holds information for all passes.
 */
public class GeneralConfiguration {
  @Nullable
  private final Path inputPath;
  private final Path outputPath;
  private final DumpMode dumpMode;
  private boolean dryRun = false;
  private DecoderOptions decoderOptions = new DecoderOptions();


  /**
   * Constructs a new GeneralConfiguration object with the specified input path, output path,
   * and dump mode.
   */
  public GeneralConfiguration(@Nullable Path inputPath, Path outputPath, DumpMode dumpMode) {
    this.inputPath = inputPath;
    this.outputPath = outputPath;
    this.dumpMode = dumpMode;
  }

  public GeneralConfiguration(Path outputPath, DumpMode dumpMode) {
    this(null, outputPath, dumpMode);
  }

  /**
   * Construct from an existing configuration.
   *
   * @param generalConfig the configuration to copy.
   */
  public GeneralConfiguration(GeneralConfiguration generalConfig) {
    this(generalConfig.inputPath, generalConfig.outputPath, generalConfig.dumpMode);
    decoderOptions = generalConfig.getDecoderOptions();
    dryRun = generalConfig.isDryRun();
  }

  @Nullable
  public Path inputPath() {
    return inputPath;
  }

  public Path outputPath() {
    return outputPath;
  }

  public DumpMode dumpMode() {
    return dumpMode;
  }

  public boolean doDump() {
    return dumpMode == DumpMode.ALWAYS;
  }

  public boolean isDryRun() {
    return dryRun;
  }

  public void setDryRun(boolean dryRun) {
    this.dryRun = dryRun;
  }

  public DecoderOptions getDecoderOptions() {
    return decoderOptions;
  }

  public void setDecoderOptions(DecoderOptions decoderOptions) {
    this.decoderOptions = decoderOptions;
  }
}
