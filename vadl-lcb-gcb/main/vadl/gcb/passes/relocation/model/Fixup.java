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

package vadl.gcb.passes.relocation.model;

/**
 * Every {@link Fixup} is like a {@link CompilerRelocation}. But not every
 * {@link CompilerRelocation} is a {@link Fixup}. The compiler can resolve symbols in the same
 * compilation unit. During a limited time, when it is not clear whether it can be resolved or not,
 * it is {@link Fixup}. Later it will be mapped into a {@link CompilerRelocation} when the
 * address is only known at compile-time.
 */
public class Fixup {
  private final CompilerRelocation.Kind kind;
  private final HasRelocationComputationAndUpdate implementedRelocation;

  /**
   * Create a fixup for relocations which have been generated for an immediate.
   */
  public Fixup(AutomaticallyGeneratedRelocation automaticallyGeneratedRelocation) {
    this.kind = automaticallyGeneratedRelocation.kind;
    this.implementedRelocation = automaticallyGeneratedRelocation;
  }

  /**
   * Create a fixup for relocations which have been specified from a user.
   */
  public Fixup(ImplementedUserSpecifiedRelocation implementedUserSpecifiedRelocation) {
    this.kind = implementedUserSpecifiedRelocation.kind;
    this.implementedRelocation = implementedUserSpecifiedRelocation;
  }

  /**
   * Get the name of the fixup.
   */
  public FixupName name() {
    return new FixupName(
        "fixup_"
            + implementedRelocation.valueRelocation().functionName().identifier().simpleName() + "_"
            + implementedRelocation.identifier().lower());
  }

  public CompilerRelocation.Kind kind() {
    return kind;
  }

  public HasRelocationComputationAndUpdate implementedRelocation() {
    return implementedRelocation;
  }
}
