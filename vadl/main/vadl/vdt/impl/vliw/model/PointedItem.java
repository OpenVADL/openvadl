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

package vadl.vdt.impl.vliw.model;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import vadl.vdt.impl.vliw.model.Counter.BitCounter;
import vadl.vdt.impl.vliw.model.Counter.IntCounter;
import vadl.viam.Group;

/**
 * Pointed item of an extended pointed expressions.
 */
public sealed interface PointedItem {

  String toString(boolean withCounter);

  record Literal(Group.Literal lit, boolean pointed) implements PointedItem {

    @Nonnull
    @Override
    public String toString() {
      return toString(false);
    }

    @Override
    public String toString(boolean withCounter) {
      return (pointed ? "*" : "") + lit.op().simpleName();
    }
  }

  record Sequence(List<PointedItem> elements) implements PointedItem {

    @Nonnull
    @Override
    public String toString() {
      return toString(false);
    }

    @Override
    public String toString(boolean withCounter) {
      return elements.stream()
          .map(i -> i.toString(withCounter))
          .collect(Collectors.joining("."));
    }
  }

  record Alternation(List<PointedItem> elements) implements PointedItem {

    @Nonnull
    @Override
    public String toString() {
      return toString(false);
    }

    @Override
    public String toString(boolean withCounter) {
      return elements.stream()
          .map(i -> i.toString(withCounter))
          .collect(Collectors.joining("|"));
    }
  }

  record Repetition(PointedItem expr, BigInteger from, BigInteger to, IntCounter counter,
                    boolean pointed) implements PointedItem {

    @Nonnull
    @Override
    public String toString() {
      return toString(false);
    }

    public String toString(boolean withCounter) {
      return "%s(%s)%s<%d..%d>".formatted(
          pointed ? "*" : "",
          expr,
          withCounter ? "[" + counter.id() + "]" : "",
          from,
          to
      );
    }

  }

  record Permutation(List<PointedItem> elements, BitCounter counter, boolean pointed)
      implements PointedItem {

    @Nonnull
    @Override
    public String toString() {
      return toString(false);
    }

    public String toString(boolean withCounter) {
      return (pointed ? "*" : "") + (withCounter ? "[" + counter.id() + "]" : "") + "{"
          + elements.stream()
          .map(PointedItem::toString)
          .collect(Collectors.joining(", ")) + "}";
    }
  }

}
