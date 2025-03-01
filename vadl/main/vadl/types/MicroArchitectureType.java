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

package vadl.types;

import javax.annotation.Nullable;

/**
 * Types necessary for the Micro Architecture description.
 */
public abstract class MicroArchitectureType extends Type {

  private static @Nullable FetchResultType fetchResult;

  /**
   * Get FetchResultType.
   *
   * @return FetchResultType instance
   */
  public static FetchResultType fetchResult() {
    if (fetchResult == null) {
      fetchResult = new FetchResultType();
    }
    return fetchResult;
  }

  private static @Nullable InstructionType instruction;

  /**
   * Get InstructionType.
   *
   * @return InstructionType instance
   */
  public static InstructionType instruction() {
    if (instruction == null) {
      instruction = new InstructionType();
    }
    return instruction;
  }
}
