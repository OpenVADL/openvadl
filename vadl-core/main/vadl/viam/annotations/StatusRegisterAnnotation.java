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

package vadl.viam.annotations;

import vadl.viam.RegisterTensor;

/**
 * Indicates that the {@link RegisterTensor} is a status register and needs special
 * treatment in the compiler-generator.
 */
public class StatusRegisterAnnotation extends vadl.viam.Annotation<RegisterTensor> {

  @Override
  public Class<RegisterTensor> parentDefinitionClass() {
    return RegisterTensor.class;
  }

  /**
   * Register annotation for the negative status register.
   */
  public static class NegativeStatusRegisterAnnotation extends StatusRegisterAnnotation {
  }

  /**
   * Register annotation for the zero status register.
   */
  public static class ZeroStatusRegisterAnnotation extends StatusRegisterAnnotation {
  }

  /**
   * Register annotation for the carry status register.
   */
  public static class CarryStatusRegisterAnnotation extends StatusRegisterAnnotation {
  }

  /**
   * Register annotation for the overflow status register.
   */
  public static class OverflowStatusRegisterAnnotation extends StatusRegisterAnnotation {
  }
}
