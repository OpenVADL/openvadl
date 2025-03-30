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

package vadl.lcb.passes.llvmLowering.domain;

import java.util.Map;
import vadl.error.Diagnostic;
import vadl.template.Renderable;
import vadl.types.DataType;
import vadl.utils.SourceLocation;

enum MachineValueTypeEnum {
  I32,
  I64
}

/**
 * Machine Value Type in LLVM.
 */
public record MachineValueType(MachineValueTypeEnum value) implements Renderable {
  /**
   * Get {@link MachineValueType} from {@link DataType} based on the bitwidth.
   */
  public static MachineValueType from(DataType dataType) {
    if (dataType.bitWidth() == 32) {
      return new MachineValueType(MachineValueTypeEnum.I32);
    } else if (dataType.bitWidth() == 64) {
      return new MachineValueType(MachineValueTypeEnum.I64);
    }

    throw Diagnostic.error("Cannot lower MVT", SourceLocation.INVALID_SOURCE_LOCATION)
        .build();
  }

  @Override
  public Map<String, Object> renderObj() {
    return Map.of("value", value.name().toLowerCase());
  }
}
