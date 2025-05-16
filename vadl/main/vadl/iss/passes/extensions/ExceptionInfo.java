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
import vadl.iss.passes.IssInfoRetrievalPass;
import vadl.iss.passes.tcgLowering.Tcg_32_64;
import vadl.template.Renderable;
import vadl.viam.Definition;
import vadl.viam.DefinitionExtension;
import vadl.viam.ExceptionDef;
import vadl.viam.InstructionSetArchitecture;
import vadl.viam.Parameter;

/**
 * A {@link InstructionSetArchitecture} extension that provides information about existing
 * {@link ExceptionDef}s. It is added by the {@link IssInfoRetrievalPass}.
 * It consists of a list of {@link Entry}s that hold the information of an individual
 * exception.
 *
 * @see Entry
 * @see IssInfoRetrievalPass
 * @see vadl.iss.codegen.IssExceptionHandlingCodeGenerator
 * @see vadl.iss.template.IssTemplateRenderingPass
 */
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

  /**
   * Holds the information of a single exception.
   * It provides methods required during QEMU generation, such as the name in the
   * exception enum in {@code cpu-bits.h}.
   *
   * <p>It also holds a parameter map, that is constructed on initialization
   * which provides ISS specific information about parameters, using the
   * {@link Param} class.</p>
   *
   * @see ExceptionInfo
   * @see vadl.iss.codegen.IssExceptionHandlingCodeGenerator
   */
  public class Entry implements Renderable {
    public String name;
    public ExceptionDef def;
    public Map<Parameter, Param> params;


    /**
     * Construct the exception entry and processes the {@link Parameter} to
     * construct a map of {@link Param}s.
     *
     * @param def the VIAM exception definition
     */
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

    /**
     * The name used in the exception enum in {@code cpu-bits.h}.
     */
    public String enumName() {
      return configuration.targetName().toUpperCase() + "_EXCP_" + name.toUpperCase();
    }

    /**
     * The name of the handling function in the {@code do_exceptions.c.inc}.
     * E.g. {@code rv64im_do_exc}.
     *
     * @see vadl.iss.template.target.EmitIssDoExcCIncPass
     * @see vadl.iss.template.target.EmitIssCpuSourcePass
     */
    public String handlingFuncName() {
      return configuration.targetName().toLowerCase() + "_do_" + name.toLowerCase();
    }

    /**
     * The helper definition emitted in {@code helper.h}.
     * E.g. {@code DEF_HELPER_2(raise_exc, noreturn, env, i64)} which produces the
     * signatures for {@code gen_helper_raise_exc()} and the {@code helper_raise_exc}
     * declaration which is provided by the {@link #helperImpl()}.
     */
    public String helperDef() {
      // 1. env, ... parameter
      var nrArgs = 1 + def.parameters().length;
      var argTypes = Arrays.stream(def.parameters())
          // all parameters are passed as target_size containers
          .map(p -> configuration.targetSize())
          // .map(p -> Tcg_32_64.nextFitting(p.type().asDataType().bitWidth()))
          .map(Enum::toString)
          .collect(Collectors.joining(", "));
      argTypes = argTypes.isEmpty() ? "" : ", " + argTypes;
      return "DEF_HELPER_" + nrArgs + "(raise_" + name.toLowerCase() + ", noreturn, env"
          + argTypes + ")";
    }

    /**
     * Provides the implementation of the helper function defined by {@link #helperDef()}
     * and emitted in {@code helper.c}.
     * It is called during instruction execution, and its only job is to set the
     * arguments for the exception in the CPU state, and calling {@code raise_exception()}.
     *
     * <pre>{@code
     * void helper_raise_exc(CPURV64IMZICSRState *env, uint64_t cause) {
     *   env->arg_exc_cause = (uint32_t) cause;
     *   rv64imzicsr_raise_exception(env, RV64IMZICSR_EXCP_EXC);
     * }
     * }</pre>
     */
    public String helperImpl() {
      // 1. env, 2. index, ... parameter (in target size container)
      var params = this.params.values().stream()
          .map(p -> "uint" + configuration.targetSize().width + "_t " + p.param.simpleName())
          .collect(Collectors.joining(", "));
      params = params.isEmpty() ? "" : ", " + params;
      var targetUpper = configuration.targetName().toUpperCase();
      var targetLower = configuration.targetName().toLowerCase();
      var sb = new StringBuilder();
      sb.append("void helper_raise_" + name.toLowerCase())
          .append("(CPU" + targetUpper + "State *env" + params)
          .append(") {\n");
      for (Param p : this.params.values()) {
        sb.append("\tenv->" + p.nameInCpuState + " = (" + p.cType() + ") " + p.param.simpleName()
            + ";\n");
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

  /**
   * An ISS internal representation of exception parameters.
   * The most important field is the {@link #nameInCpuState} which defines the field name
   * used in the CPU state to hold the parameter.
   * We need this, as the exception handling function is not directly called, but
   * from the exception handling step in the QEMU main execution loop.
   * So instead when the exception is raise, we store the arguments in the CPU state and
   * in the exception handling function, we get them from the CPU state.
   *
   * @param param          the VIAM parameter this corresponds to
   * @param tcgWidth       of the parameter
   * @param nameInCpuState field name in the CPU state
   */
  public record Param(
      Parameter param,
      Tcg_32_64 tcgWidth,
      String nameInCpuState
  ) implements Renderable {

    @SuppressWarnings("MethodName")
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
