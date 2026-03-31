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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import vadl.viam.GeneratesRegisterFileName;

/**
 * Extends the register with information which a compiler requires.
 */
public abstract class CompilerRegister {
  /**
   * Indicates the indices for sub registers.
   */
  public enum SubRegIndexEnum {
    SUB_32("sub_32"),
    SUB_32_HI("sub_32_hi"),
    FULL_64("full_64");

    private final String name;

    SubRegIndexEnum(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }

  /**
   * Represents an index for a sub register.
   */
  public static class SubRegIndex {
    private final SubRegIndexEnum subRegIndex;
    private int version = 0;

    public SubRegIndex(SubRegIndexEnum subRegIndex) {
      this.subRegIndex = subRegIndex;
    }

    public SubRegIndexEnum inner() {
      return subRegIndex;
    }

    /**
     * Get the name of the register.
     */
    public String name() {
      if (version == 0) {
        return subRegIndex.name();
      } else {
        return subRegIndex.name() + "_" + version;
      }
    }

    public void incrementVersion() {
      version++;
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      SubRegIndex that = (SubRegIndex) o;
      return version == that.version && subRegIndex == that.subRegIndex;
    }

    @Override
    public int hashCode() {
      return Objects.hash(subRegIndex, version);
    }
  }

  protected final String name;
  protected final String asmName;
  protected final List<String> altNames;
  protected final List<CompilerRegister> subRegs;
  protected final List<SubRegIndex> subRegIndices;
  protected final GeneratesRegisterFileName registerFile;

  protected final int dwarfNumber;
  protected final int hwEncodingValue;
  protected final boolean isArtificial;

  /**
   * Constructor.
   */
  public CompilerRegister(String name,
                          String asmName,
                          List<String> altNames,
                          int dwarfNumber,
                          int hwEncodingValue,
                          boolean isArtificial,
                          GeneratesRegisterFileName registerFile) {
    this.name = name;
    this.asmName = asmName;
    this.altNames = altNames;
    this.dwarfNumber = dwarfNumber;
    this.hwEncodingValue = hwEncodingValue;
    this.subRegs = new ArrayList<>();
    this.subRegIndices = new ArrayList<>();
    this.isArtificial = isArtificial;
    this.registerFile = registerFile;
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

  public boolean isArtificial() {
    return isArtificial;
  }

  public List<CompilerRegister> subRegs() {
    return subRegs;
  }

  public List<SubRegIndex> subRegIndices() {
    return subRegIndices;
  }

  public GeneratesRegisterFileName registerFile() {
    return registerFile;
  }

  /**
   * Adding {@link CompilerRegister} as {@link #subRegs}.
   */
  public void addSubReg(CompilerRegister subRegister, SubRegIndex subRegIndex) {
    subRegIndices.add(subRegIndex);
    subRegs.add(subRegister);
  }
}