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

package vadl.lcb.template.utils;

import vadl.error.Diagnostic;
import vadl.viam.Abi;
import vadl.viam.RegisterTensor;

/**
 * LLVM requires the data layout on multiple places in the code. This class
 * unifies access for it in vadl.
 */
public class DataLayoutProvider {
  /**
   * Holds information about the data layout of the target.
   */
  public record DataLayout(boolean isBigEndian, int pointerSize, int pointerAlignment) {
  }

  /**
   * Creates a string representation from register file.
   */
  public static DataLayout createDataLayout(Abi abi, RegisterTensor generalPurposeRegisterFile) {
    generalPurposeRegisterFile.ensure(generalPurposeRegisterFile.isRegisterFile(),
        "must not be null");
    return new DataLayout(false,
        generalPurposeRegisterFile.resultType().bitWidth(),
        pointerAlignment(abi));
  }

  /**
   * Creates a string representation from {@link DataLayout}.
   */
  public static String createDataLayoutString(DataLayout dataLayout) {
    String loweredEndian = dataLayout.isBigEndian ? "E-" : "e-";
    String loweredPointer =
        String.format("p:%d:%d-", dataLayout.pointerSize, dataLayout.pointerSize);
    return String.format("%sm:e-%sS0-a:0:64-i1:8:8-i8:8:8-i16:16:16-i32:32:32-i64:32:64",
        loweredEndian, loweredPointer);
  }

  /**
   * Extract the pointer alignment from {@link Abi}. It will throw an error when it does not exist.
   */
  public static int pointerAlignment(Abi abi) {
    return abi.clangTypes().stream()
        .filter(x -> x instanceof Abi.AbstractClangType.NumericClangType numericClangType
            && numericClangType.typeName() ==
            Abi.AbstractClangType.NumericClangType.TypeName.POINTER_ALIGN)
        .findFirst()
        .map(x -> Integer.parseInt(x.value()))
        .orElseThrow(
            () -> Diagnostic.error("Pointer alignment is not defined",
                abi.location()).build());
  }
}
