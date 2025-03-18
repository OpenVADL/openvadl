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

package vadl.cli;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.stream.Streams;
import picocli.CommandLine;
import vadl.configuration.IssConfiguration;

class Helpers {
}


class IssOptsConverter implements CommandLine.ITypeConverter<IssConfiguration.IssOptsToSkip>,
    Iterable<String> {

  @Override
  public IssConfiguration.IssOptsToSkip convert(String value) {
    if (value.equals("help")) {
      // Calculate the maximum width of option names
      int maxOptionLength = Arrays.stream(IssConfiguration.IssOptsToSkip.values())
          .map(opt -> toCliName(opt).length())
          .max(Integer::compare)
          .orElse(0);

      // Define the indentation for descriptions
      int descriptionIndent = 6 + maxOptionLength; // 6 accounts for "    - " and two spaces

      // Print help message for available options
      System.out.println("Available optimizations to skip:");
      Arrays.stream(IssConfiguration.IssOptsToSkip.values())
          .sorted(Comparator.comparing(v -> v.name()))
          .forEach(opt ->
              printFormattedOption(toCliName(opt), opt.desc, maxOptionLength,
                  descriptionIndent));

      System.exit(0);
    }

    try {
      return IssConfiguration.IssOptsToSkip.valueOf(value.toUpperCase().replace('-', '_'));
    } catch (IllegalArgumentException e) {
      throw new CommandLine.TypeConversionException(
          "\nAvailable options are %s".formatted(
              Streams.of(iterator()).sorted().collect(Collectors.joining(", "))
          )
      );
    }
  }

  @Nonnull
  @Override
  public Iterator<String> iterator() {
    return Arrays.stream(IssConfiguration.IssOptsToSkip.values())
        .map(this::toCliName)
        .sorted()
        .iterator();
  }

  private String toCliName(IssConfiguration.IssOptsToSkip value) {
    return value.name().toLowerCase().replace('_', '-');
  }

  private static void printFormattedOption(String optionName, String description, int nameWidth,
                                           int descriptionIndent) {
    // Split the description into lines
    String[] lines = description.split("\n", -1);

    // Print the first line with the option name
    System.out.printf("    - %-" + nameWidth + "s  %s%n", optionName, lines[0]);

    // Print subsequent lines with indentation
    String format = "%" + (descriptionIndent + 2) + "s%s%n";
    for (int i = 1; i < lines.length; i++) {
      System.out.printf(format, "", lines[i]);
    }
  }
}