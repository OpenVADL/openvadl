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
import java.util.Iterator;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.stream.Streams;
import picocli.CommandLine;
import vadl.configuration.IssConfiguration;

class Helpers {
}


class IssOptsConverter implements CommandLine.ITypeConverter<IssConfiguration.IssOptsToSkip>
    , Iterable<String> {

  @Override
  public IssConfiguration.IssOptsToSkip convert(String value) {
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
        .map(v -> v.name().toLowerCase().replace('_', '-'))
        .sorted()
        .iterator();
  }
}