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

package vadl.iss.passes.extensions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import vadl.configuration.IssConfiguration;
import vadl.iss.passes.tcgLowering.Tcg_32_64;
import vadl.template.Renderable;
import vadl.viam.Definition;
import vadl.viam.DefinitionExtension;
import vadl.viam.ExceptionDef;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Parameter;

public class ExceptionInfo extends DefinitionExtension<InstructionSetArchitecture>
    implements Renderable {

  private IssConfiguration configuration;
  private List<Entry> exception = new ArrayList<>();

  public ExceptionInfo(IssConfiguration configuration) {
    this.configuration = configuration;
  }

  public List<Entry> entries() {
    return exception;
  }

  public void addException(ExceptionDef def) {
    exception.add(new Entry(def));
  }

  @Override
  public Class<? extends Definition> extendsDefClass() {
    return InstructionSetArchitecture.class;
  }

  @Override
  public Map<String, Object> renderObj() {
    return Map.of(
        "exceptions", exception
    );
  }

  public class Entry implements Renderable {
    public String name;
    public ExceptionDef def;
    public Map<Parameter, Param> params;


    public Entry(ExceptionDef def) {
      this.name = def.simpleName();
      this.params = new HashMap<>();
      for (Parameter p : def.parameters()) {
        params.put(p, new Param(
            p,
            Tcg_32_64.nextFitting(p.type().asDataType().bitWidth()),
            "arg_" + this.name.toLowerCase() + "_" + p.simpleName()
        ));
      }
      this.def = def;
    }

    public String enumName() {
      return configuration.targetName().toUpperCase() + "_EXCP_" + name.toUpperCase();
    }

    public String handlingFuncName() {
      return configuration.targetName().toLowerCase() + "_do_" + name.toLowerCase();
    }

    public String helperDef() {
      // 1. env, ... parameter
      var nrArgs = 1 + def.parameters().length;
      var argTypes = Arrays.stream(def.parameters())
          .map(p -> Tcg_32_64.nextFitting(p.type().asDataType().bitWidth()))
          .map(Enum::toString)
          .collect(Collectors.joining(", "));
      return "DEF_HELPER_" + nrArgs + "(raise_" + name.toLowerCase() + ", noreturn, env, "
          + argTypes + ")";
    }

    public String helperImpl() {
      // 1. env, 2. index, ... parameter
      var params = this.params.values().stream()
          .map(p -> p.cType() + " " + p.param.simpleName())
          .collect(Collectors.joining(", "));
      var targetUpper = configuration.targetName().toUpperCase();
      var targetLower = configuration.targetName().toLowerCase();
      var sb = new StringBuilder();
      sb.append("void helper_raise_" + name.toLowerCase())
          .append("(CPU" + targetUpper + "State *env, " + params)
          .append(") {\n");
      for (Param p : this.params.values()) {
        sb.append("\tenv->" + p.nameInCpuState + " = " + p.param.simpleName() + ";\n");
      }
      sb.append("\t" + targetLower + "_raise_exception(env, ")
          .append(enumName() + ");\n}");
      return sb.toString();
    }

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "name_lower", name.toLowerCase(),
          "helper_def", helperDef(),
          "helper_impl", helperImpl(),
          "enum_name", enumName(),
          "params", params.values().stream().toList(),
          "handling_func", handlingFuncName()
      );
    }
  }

  public record Param(
      Parameter param,
      Tcg_32_64 tcgWidth,
      String nameInCpuState
  ) implements Renderable {

    public String cType() {
      return "uint" + tcgWidth.width + "_t";
    }

    public int width() {
      return tcgWidth.width;
    }

    @Override
    public Map<String, Object> renderObj() {
      return Map.of(
          "name_in_cpu", nameInCpuState,
          "c_type", cType()
      );
    }
  }
}
