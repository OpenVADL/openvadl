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

package vadl.viam.asm;

import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Represents a token of the asm parser.
 */
public class AsmToken {
  String ruleName;
  @Nullable
  String stringLiteral;

  public AsmToken(String ruleName, @Nullable String stringLiteral) {
    this.ruleName = ruleName;
    this.stringLiteral = stringLiteral;
  }

  public String getRuleName() {
    return ruleName;
  }

  @Nullable
  public String getStringLiteral() {
    return stringLiteral;
  }

  @Override
  public String toString() {
    if (stringLiteral != null) {
      return '"' + stringLiteral + '"';
    }
    return ruleName;
  }

  /**
   * An AsmToken with ruleName=IDENTIFIER and stringLiteral=null
   * is equal to an AsmToken with ruleName=IDENTIFIER and stringLiteral="something"
   * since the parser cannot decide which alternative to choose.
   *
   * @param o the other AsmToken
   * @return whether the two AsmTokens are equal from the AsmParsers viewpoint
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AsmToken that = (AsmToken) o;

    if (ruleName.equals(that.ruleName)) {
      if (stringLiteral == null || that.stringLiteral == null) {
        return true;
      }
      return stringLiteral.equals(that.stringLiteral);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(ruleName);
  }
}
