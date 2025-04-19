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

package vadl.iss.template;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;
import vadl.cppCodeGen.CppTypeMap;
import vadl.viam.Register;
import vadl.viam.RegisterFile;
import vadl.viam.Specification;

/**
 * A set of utils to help render ISS templates.
 */
public class IssRenderUtils {

  /**
   * Maps a given {@link RegisterFile} to a map of values.
   */
  public static Map<String, Object> map(RegisterFile rf) {
    var size = (int) Math.pow(2, rf.addressType().bitWidth());
    var name = rf.identifier.simpleName();
    return Map.of(
        "name", name,
        "name_upper", name.toUpperCase(),
        "name_lower", name.toLowerCase(),
        "size", String.valueOf(size),
        "value_width", rf.resultType().bitWidth(),
        "value_c_type", CppTypeMap.getCppTypeNameByVadlType(rf.resultType()),
        "names", IntStream.range(0, size)
            .mapToObj(i -> name + i)
            .toList(),
        "constraints", Arrays.stream(rf.constraints())
            .map(c -> Map.of(
                "index", c.indices().getFirst().intValue(),
                "value", c.value().intValue()
            )).toList()
    );
  }

  /**
   * Maps a given {@link Register} to a map of values.
   */
  public static Map<String, String> map(Register reg) {
    return Map.of(
        "name", reg.identifier.simpleName(),
        "name_upper", reg.identifier.simpleName().toUpperCase(),
        "name_lower", reg.identifier.simpleName().toLowerCase(),
        "c_type", CppTypeMap.getCppTypeNameByVadlType(reg.type())
    );
  }

  /**
   * Maps given {@link Specification} to a list of register mappings using
   * {@link #map(Register)}.
   */
  public static List<Map<String, String>> mapRegs(Specification spec) {
    return spec.isa().get()
        .ownRegisters()
        .stream().map(IssRenderUtils::map)
        .toList();
  }


  /**
   * Maps given {@link Specification} to a list of register file mappings using
   * {@link #map(Register)}.
   */
  public static List<Map<String, Object>> mapRegFiles(Specification spec) {
    return spec.isa().get()
        .ownRegisterFiles()
        .stream().map(IssRenderUtils::map)
        .toList();
  }

  /**
   * Maps given {@link Specification} to the register mapping of the ISA's PC.
   */
  public static Map<String, String> mapPc(Specification spec) {
    var pcReg = (Register) Objects.requireNonNull(spec.isa().get()
        .pc()).registerTensor();

    return map(pcReg);
  }

}
