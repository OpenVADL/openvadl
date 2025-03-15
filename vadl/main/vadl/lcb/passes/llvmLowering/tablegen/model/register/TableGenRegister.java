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

package vadl.lcb.passes.llvmLowering.tablegen.model.register;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import vadl.gcb.valuetypes.CompilerRegister;
import vadl.gcb.valuetypes.ProcessorName;
import vadl.gcb.valuetypes.TargetName;
import vadl.template.Renderable;

/**
 * Represents a single register in TableGen.
 */
public record TableGenRegister(TargetName namespace,
                               CompilerRegister compilerRegister,
                               int hwEncodingMsb,
                               Optional<Integer> index) implements Renderable {

  public String altNamesString() {
    return String.join(", ",
        compilerRegister.altNames().stream().map(x -> "\"" + x + "\"").toList());
  }

  @Override
  public Map<String, Object> renderObj() {
    var map = new HashMap<String, Object>();
    map.put("namespace", namespace);
    map.put("name", compilerRegister.name());
    map.put("asmName", compilerRegister.asmName());
    map.put("dwarfNumber", compilerRegister.dwarfNumber());
    map.put("hwEncodingMsb", hwEncodingMsb);
    map.put("hwEncodingValue", compilerRegister.hwEncodingValue());
    map.put("altNamesString", altNamesString());
    index.ifPresent(integer -> map.put("index", integer));
    return map;
  }
}
