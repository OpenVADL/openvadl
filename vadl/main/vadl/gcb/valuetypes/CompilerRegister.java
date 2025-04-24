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

package vadl.gcb.valuetypes;

import java.util.List;

/**
 * Extends the register with information which a compiler requires.
 */
public abstract class CompilerRegister {
  protected final String name;
  protected final String asmName;
  protected final List<String> altNames;
  protected final int dwarfNumber;
  protected final int hwEncodingValue;

  /**
   * Constructor.
   */
  public CompilerRegister(String name,
                          String asmName,
                          List<String> altNames,
                          int dwarfNumber,
                          int hwEncodingValue) {
    this.name = name;
    this.asmName = asmName;
    this.altNames = altNames;
    this.dwarfNumber = dwarfNumber;
    this.hwEncodingValue = hwEncodingValue;
  }

  public String name() {
    return name;
  }

  public String asmName() {
    return asmName;
  }

  public List<String> altNames() {
    return altNames;
  }

  public int dwarfNumber() {
    return dwarfNumber;
  }

  public int hwEncodingValue() {
    return hwEncodingValue;
  }
}
