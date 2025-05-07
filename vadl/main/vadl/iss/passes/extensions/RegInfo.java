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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import vadl.cppCodeGen.CppTypeMap;
import vadl.template.Renderable;
import vadl.viam.Definition;
import vadl.viam.DefinitionExtension;
import vadl.viam.RegisterTensor;

/**
 * A ISS class that contains additional information and helper methods for {@link RegisterTensor}s.
 * It is a {@link Renderable}, so it can be directly added as variable to template rendering.
 */
public class RegInfo extends DefinitionExtension<RegisterTensor> implements Renderable {

  @Nullable
  private Map<String, Object> renderObj;

  public RegisterTensor reg() {
    return extendingDef();
  }

  /**
   * Names of all registers in a multidimensional register tensor.
   */
  public List<String> names() {
    // TODO: This is not generic (only true for register file .. 2 dimensions)
    return reg().isSingleRegister() ? List.of(reg().simpleName()) :
        IntStream.range(0, reg().outermostDim().size())
            .mapToObj(i -> reg().simpleName() + i)
            .toList();
  }

  @Override
  public Class<? extends Definition> extendsDefClass() {
    return RegisterTensor.class;
  }

  /**
   * The name of the register tensor.
   */
  public String name() {
    return reg().simpleName();
  }

  public String valueCType() {
    return CppTypeMap.nextFittingUInt(reg().resultType(reg().maxNumberOfAccessIndices()));
  }

  @Override
  @SuppressWarnings("VariableDeclarationUsageDistance")
  public Map<String, Object> renderObj() {
    if (renderObj == null) {
      var dims = renderIndexDims();
      var nameLower = name().toLowerCase();
      var renderParams = renderGetterArgs(dims);
      var resultType = reg().resultType(reg().maxNumberOfAccessIndices());
      renderObj = new HashMap<>();
      renderObj.put("name", name());
      renderObj.put("name_lower", nameLower);
      renderObj.put("name_upper", name().toUpperCase());
      renderObj.put("index_dims", dims);
      renderObj.put("value_width", resultType.bitWidth());
      renderObj.put("value_c_type", valueCType());
      renderObj.put("names", names());
      renderObj.put("constraints", renderConstraints(dims));
      renderObj.put("getter_params", renderParams.isEmpty() ? "" : ", " + renderParams);
      // as getter_params but without leading comma
      renderObj.put("getter_params_no_comma", renderParams);
      renderObj.put("getter_params_post_comma", renderParams.isEmpty() ? "" : renderParams + ", ");
      renderObj.put("c_array_def", renderCArrayDef());
    }
    return renderObj;
  }

  private String renderCArrayDef() {
    var sb = new StringBuilder();
    reg().dimensions().stream().limit(reg().maxNumberOfAccessIndices())
        .forEach(dim -> {
          sb.append("[" + dim.size() + "]");
        });
    return sb.toString();
  }

  private List<?> renderIndexDims() {
    var dims = reg().dimensions();
    return IntStream.range(0, dims.size() - 1).mapToObj(i ->
        Map.of(
            "size", dims.get(i).size(),
            "index_ctype", CppTypeMap.getCppTypeNameByVadlType(
                Objects.requireNonNull(dims.get(i).indexType().fittingCppType())),
            "arg_name", "d" + i
        )
    ).toList();
  }

  private String renderGetterArgs(List<?> dims) {
    var args = dims.stream()
        .map(d -> {
          var dim = ((Map<?, ?>) d);
          return dim.get("index_ctype") + " " + dim.get("arg_name");
        })
        .collect(Collectors.joining(", "));
    return args;
  }

  private List<?> renderConstraints(List<?> dims) {
    // TODO: This is not generic and only works for 2-dimensional registers
    return Arrays.stream(reg().constraints())
        .map(c -> {

          var check = new StringBuilder();
          var tcgName = new StringBuilder("const" + reg().simpleName().toLowerCase());
          for (int i = 0; i < c.indices().size(); i++) {
            if (i != 0) {
              check.append(" && ");
            }
            check.append(((Map<?, ?>) dims.get(i)).get("arg_name")).append(" == ")
                .append(c.indices().get(i).hexadecimal());
            tcgName.append("_").append(c.indices().get(i).decimal());
          }

          return Map.of(
              "index", c.indices().getFirst().intValue(),
              "value", c.value().intValue(),
              "check", check.toString(),
              "tcg_name", tcgName.toString()
          );
        }).toList();
  }
}
