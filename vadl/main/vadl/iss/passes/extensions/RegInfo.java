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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.cppCodeGen.CppTypeMap;
import vadl.iss.IssUtils;
import vadl.template.Renderable;
import vadl.viam.Definition;
import vadl.viam.DefinitionExtension;
import vadl.viam.RegisterTensor;
import vadl.viam.graph.dependency.ReadRegTensorNode;
import vadl.viam.graph.dependency.WriteRegTensorNode;

/**
 * A ISS class that contains additional information and helper methods for {@link RegisterTensor}s.
 * It is a {@link Renderable}, so it can be directly added as variable to template rendering.
 */
public class RegInfo extends DefinitionExtension<RegisterTensor> implements Renderable {


  @Nullable
  private Map<String, Object> renderObj;
  private final IssConfiguration config;
  private final boolean isGVecValue;

  public RegInfo(IssConfiguration config,
                 RegisterTensor reg,
                 List<ReadRegTensorNode> reads,
                 List<WriteRegTensorNode> writes) {
    this.config = config;
    this.isGVecValue = isGVec(reg, reads, writes);
  }

  public RegisterTensor reg() {
    return extendingDef();
  }

  /**
   * Names of all registers in a multidimensional register tensor.
   * In the case of multi dimensional tensors, we will only build names for the outermost dimension.
   */
  public List<String> names() {
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

  /// A register will be handled as generic vector if it's inner type
  /// does not fit into the target size (max 64 bit), or if it has vector-style accesses.
  public boolean isGVec() {
    return isGVecValue;
  }

  public int valueCTypeWidth() {
    return CppTypeMap.nextFittingBitSize(
        reg().resultType(reg().maxNumberOfAccessIndices()).bitWidth());
  }

  public String valueCType() {
    return CppTypeMap.nextFittingUInt(reg().resultType(reg().maxNumberOfAccessIndices()));
  }

  /**
   * The type of the CPU state register field must be the same size as the corresponding
   * TCG variable, otherwise there would be an overflow when QEMU synchronizes the
   * TCG variables with the CPU state object.
   */
  public int cpuStateTypeWidth() {
    return isGVec() ? valueCTypeWidth() : config.targetSize().width;
  }

  @Override
  @SuppressWarnings("VariableDeclarationUsageDistance")
  public Map<String, Object> renderObj() {
    if (renderObj == null) {
      var dims = renderIndexDims();
      var nameLower = name().toLowerCase();
      var renderParams = renderGetterArgs(dims);
      var renderParamsComma = renderParams.isEmpty() ? "" : ", " + renderParams;
      var resultType = reg().resultType(reg().maxNumberOfAccessIndices());
      var cpuStateName = "CPU" + config.targetName().toUpperCase() + "State";
      renderObj = new HashMap<>();
      renderObj.put("name", name());
      renderObj.put("name_lower", nameLower);
      renderObj.put("name_upper", name().toUpperCase());
      renderObj.put("index_dims", dims);
      renderObj.put("value_width", resultType.bitWidth());
      renderObj.put("value_c_type", valueCType());
      renderObj.put("cpu_state_type_width", cpuStateTypeWidth());
      renderObj.put("names", names());
      renderObj.put("is_tcg", !isGVec());
      renderObj.put("is_gvec", isGVec());
      renderObj.put("constraints", renderConstraints(dims));
      renderObj.put("getter_params", renderParamsComma);
      renderObj.put("cpu_getter_signature",
          valueCType() + " get_cpu_" + nameLower + "(" + cpuStateName + "* env"
              + renderParamsComma + ")");
      renderObj.put("cpu_setter_signature",
          "void set_cpu_" + nameLower + "(" + cpuStateName + "* env"
              + renderParamsComma + ", " + valueCType() + " val)");
      renderObj.put("c_array_def", renderCArrayDef());
      renderObj.put("c_array_index", cArrayIndex("d"));
      renderObj.put("c_reg_name_array_def", renderCRegNameArrayDef());
    }
    return renderObj;
  }

  /**
   * Returns the array access for registers in the cpu state.
   * CPU registers are rendered as a single value or 1D array, where all dimensions
   * are flattened to one.
   */
  public String cArrayIndex(String indexPrefix) {
    if (reg().indexDimensions().isEmpty()) {
      return "";
    }
    var indexVars = reg().indexDimensions().stream().map(d -> indexPrefix + d.index()).toList();
    return "[" + IssUtils.cIndex(indexVars, reg()) + "]";
  }

  private String renderCRegNameArrayDef() {
    if (reg().indexDimensions().isEmpty()) {
      return "";
    }
    // we only have a name for the first dimension
    return "[" + reg().dimensions().getFirst().size() + "]";
  }

  private String renderCArrayDef() {
    var sb = new StringBuilder();

    if (isGVec()) {
      // if the register is a gvec, we will use a single-dimensional array
      // for simplicity when accessing it.
      var elementSize = valueCTypeWidth();
      var numElements = reg().totalWidth() / elementSize;
      sb.append("[").append(numElements).append("]");
    } else {
      // else we use a multi-dimensional array, where each innermost element
      // represents a single register that is mapped to an TCG variable.
      reg().dimensions().stream().limit(reg().maxNumberOfAccessIndices())
          .forEach(dim -> sb.append("[").append(dim.size()).append("]"));
    }
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
    return reg().constraints().stream()
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

  /// Checks if an access pattern is a vector access (operates on multiple elements).
  ///
  /// @param reg        the register tensor being accessed
  /// @param numIndices number of indices used in the access
  /// @return true if this is a vector access
  ///
  private static boolean isVectorAccess(RegisterTensor reg, int numIndices) {
    int resultWidth = reg.resultType(numIndices).bitWidth();

    // If this access operates on more than 64 bits, it's a vector access
    if (resultWidth > 64) {
      return true;
    }

    // If accessing with fewer indices than max, it's a vector access
    return numIndices < reg.maxNumberOfAccessIndices();
  }

  /// Determines whether a register tensor should be treated as a generic vector (gvec)
  /// or as individual TCG variables.
  ///
  /// A register is considered a gvec if:
  /// - Any single element exceeds 64 bits (can't fit in TCG scalar types)
  /// - It has vector-style accesses (accessing multiple elements at once)
  /// - The total register width exceeds 64 bits AND it's not a register file with
  ///   only fully-indexed accesses
  ///
  /// Examples:
  /// - `register GPR: Bits<5><64>` with accesses `GPR(i)` → false
  ///   (register file, fully indexed, 64-bit elements fit in TCG)
  /// - `register VR: Bits<32><128>` with accesses `VR(i)` → true
  ///   (128-bit elements don't fit in TCG)
  /// - `register VR: Bits<32><128>` with accesses `VR` → true
  ///   (vector access across all dimensions)
  /// - `register SIMD: Bits<8><32><32>` with accesses `SIMD(i)` → true
  ///   (accessing 32*32=1024 bits at once)
  ///
  /// @param reg    the register tensor to analyze
  /// @param reads  all read accesses to this register
  /// @param writes all write accesses to this register
  /// @return true if the register should be treated as a gvec, false for TCG variables
  @SuppressWarnings("OverloadMethodsDeclarationOrder")
  private static boolean isGVec(RegisterTensor reg,
                                List<ReadRegTensorNode> reads,
                                List<WriteRegTensorNode> writes) {

    // 1. Check if any individual element (fully indexed access) exceeds 64 bits
    //    If so, even scalar accesses can't use TCG variables
    int fullyIndexedWidth = reg.resultType(reg.maxNumberOfAccessIndices()).bitWidth();
    if (fullyIndexedWidth > 64) {
      return true;
    }

    // TODO: We should propably do proper analysis if the register is used
    //       within loops only, instead of the 2 dimensions heuristic. But it's good enough for now.
    // 2. Check if it is more than 2 dimensions.
    //    If so, it is typically used as vector registers within loops.
    if (reg.dimensions().size() > 2) {
      return true;
    }

    // 3. If we have vector-style accesses, use gvec
    //    Otherwise, all accesses are fully indexed (scalar accesses)
    //    Even if total width > 64 bits, we can use separate TCG variables
    //    for each element (like GPR[32] where each register is separate)
    return reads.stream()
        .anyMatch(read -> isVectorAccess(reg, read.indices().size()))
        || writes.stream()
        .anyMatch(write -> isVectorAccess(reg, write.indices().size()));
  }
}
