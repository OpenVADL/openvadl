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

package vadl.configuration;

import java.util.Arrays;
import javax.annotation.Nullable;

/**
 * Options for configuring the VDT generator.
 */
public class DecoderOptions {

  /**
   * The possible VADL decode tree generator strategies to choose from.
   */
  public enum Generator {
    REGULAR("Regular decoder generator", "regular"),
    IRREGULAR("Irregular decoder generator, default", "irregular"),
    RTL_TABLE("RTL table based decoder", "rtl-table"),
    ;

    private final String selector;
    private final String desc;

    Generator(String desc, String selector) {
      this.desc = desc;
      this.selector = selector;
    }

    public String getSelector() {
      return selector;
    }

    public String getDesc() {
      return desc;
    }

    /**
     * Resolve the generator strategy from its selector.
     *
     * @param selector The selector to match.
     * @return The generator, or null if no match was found.
     */
    @Nullable
    public static Generator fromSelector(String selector) {
      if (selector == null || selector.isBlank()) {
        return null;
      }
      for (Generator gen : values()) {
        if (gen.getSelector().equals(selector)) {
          return gen;
        }
      }
      return null;
    }
  }

  /**
   * The options for decoder generation which can be disabled.
   */
  public enum OptionToSkip {

    OPT_CONSTRAINT_SYNTHESIS("Skip constraint synthesis for decoder generation, default: enabled",
        "constraint-synthesis"),

    OPT_ENCODING_VERIFICATION("Skip the encoding verification, default: enabled",
        "encoding-verification"),

    OPT_DECODER_VERIFICATION("Skip the correctness verification of the decode tree, default: "
                                 + "enabled", "decoder-verification");

    private final String selector;
    private final String desc;

    OptionToSkip(String desc, String selector) {
      this.desc = desc;
      this.selector = selector;
    }

    public String getDesc() {
      return desc;
    }

    public String getSelector() {
      return selector;
    }

    /**
     * Resolve the skipped option from its selector.
     *
     * @param selector The selector to match.
     * @return The skipped option, or null if no match was found.
     */
    @Nullable
    public static OptionToSkip fromSelector(String selector) {
      if (selector == null || selector.isBlank()) {
        return null;
      }
      for (OptionToSkip opt : values()) {
        if (opt.getSelector().equals(selector)) {
          return opt;
        }
      }
      return null;
    }
  }

  private OptionToSkip[] optsToSkip;
  private Generator generator;

  public DecoderOptions() {
    optsToSkip = new OptionToSkip[0];
    generator = Generator.IRREGULAR;
  }

  public OptionToSkip[] getOptsToSkip() {
    return optsToSkip;
  }

  public void setOptsToSkip(OptionToSkip[] optsToSkip) {
    this.optsToSkip = optsToSkip;
  }

  public Generator getGenerator() {
    return generator;
  }

  public void setGenerator(Generator generator) {
    this.generator = generator;
  }

  @Override
  public String toString() {
    return "DecoderOptions{"
        + "optsToSkip=" + Arrays.toString(optsToSkip)
        + ", generator=" + generator
        + '}';
  }
}
