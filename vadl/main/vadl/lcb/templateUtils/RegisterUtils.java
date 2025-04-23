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

package vadl.lcb.templateUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import vadl.template.Renderable;
import vadl.utils.Pair;
import vadl.viam.Abi;
import vadl.viam.RegisterTensor;

/**
 * Utility class for registers.
 */
public class RegisterUtils {

  /**
   * Represents a single register in a register class.
   *
   * @param index   of the register in the file.
   * @param name    of the register.
   * @param aliases of the register.
   */
  public record Register(int index, String name, List<String> aliases) implements Renderable {

    /**
     * Return the first {@code alias} or {@code name} if {@code aliases} is empty.
     */
    public String getAsmName() {
      if (!aliases.isEmpty()) {
        return aliases.get(0);
      }
      return name;
    }

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "index", index,
          "name", name,
          "alias", !aliases.isEmpty() ? aliases.get(0) : "",
          "aliases", aliases,
          "getAsmName", getAsmName()
      );
    }
  }

  /**
   * Wrapper class for register file because the register file does
   * not specify the individual registers.
   */
  public record RegisterClass(RegisterTensor registerFile, List<Register> registers)
      implements
      Renderable {

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "registerFile", Map.of(
              "name", registerFile.simpleName()
          ),
          "registers", registers
      );
    }
  }


  /**
   * Constructing a {@link RegisterClass} from registerFile.
   */
  @Nonnull
  public static RegisterClass getRegisterClass(
      RegisterTensor registerFile,
      @Nullable Map<Pair<RegisterTensor, Integer>, List<Abi.RegisterAlias>> aliases) {
    return new RegisterClass(registerFile,
        IntStream.range(0,
                (int) Math.pow(2, Objects.requireNonNull(registerFile.addressType()).bitWidth()))
            .mapToObj(i -> {
              var name = registerFile.identifier.simpleName() + i;

              List<String> aliasesNames = new ArrayList<>();
              if (aliases != null) {
                Optional.ofNullable(aliases.get(Pair.of(registerFile, i))).ifPresent(
                    regAliases -> regAliases.forEach(regAlias -> aliasesNames.add(regAlias.value()))
                );
              }
              return new Register(i, name, aliasesNames);
            }).toList());
  }
}
