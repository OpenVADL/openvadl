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

package vadl.gcb.valuetypes;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import vadl.utils.Pair;
import vadl.viam.Abi;
import vadl.viam.RegisterResource;

/**
 * Like a {@link CompilerRegister} but contains the index in the register file.
 * This distinction is important since not all {@link CompilerRegister} are indexed e.g. PC.
 */
public class IndexedCompilerRegister extends CompilerRegister {

  private final int index;

  /**
   * Constructor.
   */
  private IndexedCompilerRegister(String regFileName,
                                  int index,
                                  String asmName,
                                  List<String> altNames,
                                  int dwarfNumber,
                                  boolean isArtificial,
                                  RegisterResource registerFile) {
    super(regFileName,
        asmName,
        altNames,
        dwarfNumber,
        index,
        isArtificial,
        registerFile);
    this.index = index;
  }

  /**
   * Generate {@link CompilerRegister} from registerFile.
   *
   * @param registerFile      is the register file from which the registers should be generated
   *                          from.
   * @param abi               for the compiler.
   * @param dwarfNumberOffset is the offset for calculating the dwarf numbers because we cannot
   *                          assume that there is only one register file.
   * @param isArtificial      registers in {@code registerFile} do not really exist.
   * @return a list of registers generated from the register file.
   */
  public static List<CompilerRegister> fromRegisterFile(RegisterResource registerFile,
                                                        Abi abi,
                                                        int dwarfNumberOffset,
                                                        boolean isArtificial) {
    var bitWidth = Objects.requireNonNull(registerFile.addressType()).bitWidth();
    var numberOfRegisters = (int) Math.pow(2, bitWidth);
    var all = IntStream.range(0, numberOfRegisters).boxed().collect(Collectors.toSet());

    var registers = new ArrayList<CompilerRegister>();
    for (var addr : all) {
      var altNames =
          abi.aliases().getOrDefault(Pair.of(registerFile, addr), Collections.emptyList())
              .stream().map(Abi.RegisterAlias::value).toList();
      var alias = altNames
          .stream().findFirst()
          .orElse(CompilerRegisterUtils.indexedRegisterName(registerFile, addr));

      int dwarfNumber = dwarfNumberOffset + addr;

      registers.add(
          new IndexedCompilerRegister(
              CompilerRegisterUtils.indexedRegisterName(registerFile, addr),
              addr,
              alias,
              altNames,
              dwarfNumber,
              isArtificial,
              registerFile));
    }

    return registers;
  }

  public int index() {
    return index;
  }
}
