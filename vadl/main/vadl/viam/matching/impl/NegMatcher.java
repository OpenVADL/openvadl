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

package vadl.viam.matching.impl;

import vadl.viam.graph.Node;
import vadl.viam.matching.Matcher;

/**
 * Negated matcher. Wraps a matcher and returns true, if it would return false.
 */
public class NegMatcher implements Matcher {

  private final Matcher matcher;

  public NegMatcher(Matcher matcher) {
    this.matcher = matcher;
  }

  @Override
  public boolean matches(Node node) {
    return !matcher.matches(node);
  }

  @Override
  public Matcher swapOperands() {
    return new NegMatcher(matcher.swapOperands());
  }

  @Override
  public Matcher not() {
    return matcher;
  }
}
