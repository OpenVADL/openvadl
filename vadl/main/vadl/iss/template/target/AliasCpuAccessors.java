// SPDX-FileCopyrightText : © 2026 TU Wien <vadl@tuwien.ac.at>
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

package vadl.iss.template.target;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import vadl.configuration.IssConfiguration;
import vadl.viam.ArtificialResource;
import vadl.viam.Constant;
import vadl.viam.RegisterTensor;
import vadl.viam.Specification;

/**
 * Renders helper/procedure/exceptions-side alias read accessors for CPU source/header templates.
 */
final class AliasCpuAccessors {

  private AliasCpuAccessors() {
  }

  static List<Map<String, Object>> renderReadAccessors(Specification specification,
                                                       IssConfiguration config) {
    var out = new ArrayList<Map<String, Object>>();
    specification.artificialResources()
        .filter(AliasCpuAccessors::supportsReadAccessor)
        .forEach(alias -> out.add(renderReadAccessor(alias, config)));
    return out;
  }

  static List<Map<String, Object>> renderWriteAccessors(Specification specification,
                                                        IssConfiguration config) {
    var out = new ArrayList<Map<String, Object>>();
    specification.artificialResources()
        .filter(AliasCpuAccessors::supportsWriteAccessor)
        .forEach(alias -> out.add(renderWriteAccessor(alias, config)));
    return out;
  }

  static boolean supportsReadAccessor(ArtificialResource alias) {
    var semantics = alias.semantics();
    if (alias.resultType().bitWidth() > 64) {
      return false;
    }
    var baseIndexCount = semantics.baseTensor().indexDimensions().size();
    var totalIndexCount = semantics.totalIndexCount();
    if (totalIndexCount < baseIndexCount) {
      return false;
    }
    var isExpansion = totalIndexCount > baseIndexCount;
    var slice = semantics.aliasSlice();
    if (slice != null && !slice.isContinuous()) {
      return false;
    }
    // We currently support either:
    //  - base aliases with optional continuous slice
    //  - expansion aliases without additional slices.
    return !isExpansion || slice == null;
  }

  static boolean supportsWriteAccessor(ArtificialResource alias) {
    return supportsReadAccessor(alias);
  }

  private static Map<String, Object> renderReadAccessor(ArtificialResource alias,
                                                        IssConfiguration config) {
    var semantics = alias.semantics();
    var base = semantics.baseTensor();
    var baseWidth = base.resultType(base.indexDimensions().size()).bitWidth();
    if (!supportsReadAccessor(alias)) {
      throw new IllegalStateException(
          "Unsupported alias helper read accessor shape for " + alias.simpleName());
    }
    if (baseWidth > 64) {
      throw new IllegalStateException(
          "Alias helper accessors support only <= 64-bit reads, but " + alias.simpleName()
              + " maps to " + baseWidth + " bits.");
    }

    var totalIndexCount = semantics.totalIndexCount();
    var argDecls = renderArgDecls(totalIndexCount);
    var baseArgList = renderArgList(semantics.baseTensor().indexDimensions().size());
    var cpuStateType = "CPU" + config.targetName().toUpperCase() + "State";
    var retType = toCUnsignedType(baseWidth);
    var baseGetter = baseGetterName(base, baseWidth);
    final var signature = retType + " cpu_get_" + alias.simpleName().toLowerCase()
        + "(" + cpuStateType + " *env" + argDecls + ")";

    var body = new StringBuilder();
    var zero = semantics.zeroConstraint();
    if (zero != null && !zero.indices().isEmpty()) {
      body.append("if (")
          .append(zeroCheck(zero.indices()))
          .append(") {\n  return 0;\n}\n");
    }
    body.append("uint64_t base = ")
        .append(baseGetter)
        .append("(env")
        .append(baseArgList)
        .append(");\n");
    var slice = semantics.aliasSlice();
    var baseIndexCount = semantics.baseTensor().indexDimensions().size();
    var isExpansion = semantics.totalIndexCount() > baseIndexCount;
    if (isExpansion) {
      var dynamicConsumed = Math.max(0, baseIndexCount - semantics.fixedIndices().size());
      var remainingDimensions = semantics.dynamicDimensions().subList(
          dynamicConsumed,
          semantics.dynamicDimensions().size()
      );
      body.append("uint64_t lsb = 0;\n");
      for (int i = 0; i < remainingDimensions.size(); i++) {
        long stride = strideForVirtualIndex(alias.resultType().bitWidth(), remainingDimensions, i);
        body.append("lsb += ((uint64_t) d")
            .append(baseIndexCount + i)
            .append(") * ")
            .append(stride)
            .append(";\n");
      }
      body.append("uint64_t shifted = VADL_lsr(base, ")
          .append(baseWidth)
          .append(", lsb, 64);\n");
      body.append("return ")
          .append(toTypedExtract("shifted", baseWidth, alias.resultType().bitWidth()))
          .append(";");
    } else {
      if (slice == null) {
        body.append("return ")
            .append(toTypedExtract("base", baseWidth, alias.resultType().bitWidth()))
            .append(";");
      } else {
        body.append("uint64_t shifted = ")
            .append("VADL_lsr(base, ")
            .append(baseWidth)
            .append(", ((uint64_t) ")
            .append(slice.lsb())
            .append("), 64);\n");
        body.append("return ")
            .append(toTypedExtract("shifted", baseWidth, alias.resultType().bitWidth()))
            .append(";");
      }
    }

    return Map.of(
        "signature", signature,
        "body", body.toString()
    );
  }

  private static Map<String, Object> renderWriteAccessor(ArtificialResource alias,
                                                         IssConfiguration config) {
    var semantics = alias.semantics();
    var base = semantics.baseTensor();
    var baseWidth = base.resultType(base.indexDimensions().size()).bitWidth();
    if (!supportsWriteAccessor(alias)) {
      throw new IllegalStateException(
          "Unsupported alias helper write accessor shape for " + alias.simpleName());
    }

    var totalIndexCount = semantics.totalIndexCount();
    var argDecls = renderArgDecls(totalIndexCount);
    var baseArgList = renderArgList(semantics.baseTensor().indexDimensions().size());
    var cpuStateType = "CPU" + config.targetName().toUpperCase() + "State";
    var valueType = toCUnsignedType(alias.resultType().bitWidth());
    var baseGetter = baseGetterName(base, baseWidth);
    var baseSetter = baseSetterName(base, baseWidth);
    var signature = "void cpu_set_" + alias.simpleName().toLowerCase()
        + "(" + cpuStateType + " *env" + argDecls + ", " + valueType + " value)";

    var body = new StringBuilder();
    var zero = semantics.zeroConstraint();
    if (zero != null && !zero.indices().isEmpty()) {
      body.append("if (")
          .append(zeroCheck(zero.indices()))
          .append(") {\n  return;\n}\n");
    }

    var baseIndexCount = semantics.baseTensor().indexDimensions().size();
    var isExpansion = semantics.totalIndexCount() > baseIndexCount;
    var slice = semantics.aliasSlice();
    if (isExpansion) {
      var dynamicConsumed = Math.max(0, baseIndexCount - semantics.fixedIndices().size());
      var remainingDimensions = semantics.dynamicDimensions().subList(
          dynamicConsumed,
          semantics.dynamicDimensions().size()
      );
      body.append("uint64_t base = ")
          .append(baseGetter)
          .append("(env")
          .append(baseArgList)
          .append(");\n");
      body.append("uint64_t lsb = 0;\n");
      for (int i = 0; i < remainingDimensions.size(); i++) {
        long stride = strideForVirtualIndex(alias.resultType().bitWidth(), remainingDimensions, i);
        body.append("lsb += ((uint64_t) d")
            .append(baseIndexCount + i)
            .append(") * ")
            .append(stride)
            .append(";\n");
      }
      body.append("uint64_t mask = VADL_mask(")
          .append(alias.resultType().bitWidth())
          .append(");\n");
      body.append("mask = VADL_lsl(mask, ")
          .append(baseWidth)
          .append(", lsb, 64);\n");
      body.append("uint64_t shifted = VADL_lsl((uint64_t) value, ")
          .append(baseWidth)
          .append(", lsb, 64);\n");
      body.append("uint64_t merged = (base & (~mask)) | (shifted & mask);\n");
      body.append(baseSetter)
          .append("(env")
          .append(baseArgList)
          .append(", (")
          .append(toCUnsignedType(baseWidth))
          .append(") merged);");
      return Map.of("signature", signature, "body", body.toString());
    }

    if (slice == null) {
      body.append(baseSetter)
          .append("(env")
          .append(baseArgList)
          .append(", (")
          .append(toCUnsignedType(baseWidth))
          .append(") value);");
      return Map.of("signature", signature, "body", body.toString());
    }

    if (semantics.overwriteMode() == ArtificialResource.OverwriteMode.MERGE) {
      body.append("uint64_t base = ")
          .append(baseGetter)
          .append("(env")
          .append(baseArgList)
          .append(");\n");
      body.append("uint64_t mask = VADL_mask(")
          .append(slice.bitSize())
          .append(");\n");
      body.append("mask = VADL_lsl(mask, ")
          .append(baseWidth)
          .append(", ((uint64_t) ")
          .append(slice.lsb())
          .append("), 64);\n");
      body.append("uint64_t shifted = VADL_lsl((uint64_t) value, ")
          .append(baseWidth)
          .append(", ((uint64_t) ")
          .append(slice.lsb())
          .append("), 64);\n");
      body.append("uint64_t merged = (base & (~mask)) | (shifted & mask);\n");
      body.append(baseSetter)
          .append("(env")
          .append(baseArgList)
          .append(", (")
          .append(toCUnsignedType(baseWidth))
          .append(") merged);");
      return Map.of("signature", signature, "body", body.toString());
    }

    if (semantics.overwriteMode() == ArtificialResource.OverwriteMode.ZERO) {
      body.append(baseSetter)
          .append("(env")
          .append(baseArgList)
          .append(", (")
          .append(toCUnsignedType(baseWidth))
          .append(") VADL_uextract((uint64_t) value, ")
          .append(alias.resultType().bitWidth())
          .append("));");
      return Map.of("signature", signature, "body", body.toString());
    }

    body.append(baseSetter)
        .append("(env")
        .append(baseArgList)
        .append(", (")
        .append(toCUnsignedType(baseWidth))
        .append(") VADL_sextract((uint64_t) value, ")
        .append(alias.resultType().bitWidth())
        .append("));");
    return Map.of("signature", signature, "body", body.toString());
  }

  private static String renderArgDecls(int argCount) {
    var sb = new StringBuilder();
    for (int i = 0; i < argCount; i++) {
      sb.append(", uint32_t d").append(i);
    }
    return sb.toString();
  }

  private static String renderArgList(int argCount) {
    if (argCount == 0) {
      return "";
    }
    var args = new StringBuilder();
    for (int i = 0; i < argCount; i++) {
      args.append(", d").append(i);
    }
    return args.toString();
  }

  private static String baseGetterName(RegisterTensor base, int width) {
    var dimSuffix = new StringBuilder();
    if (!base.indexDimensions().isEmpty()) {
      dimSuffix.append("_");
      for (int i = 0; i < base.indexDimensions().size(); i++) {
        if (i > 0) {
          dimSuffix.append("_");
        }
        dimSuffix.append("i").append(base.indexDimensions().get(i).size());
      }
    }
    return "get_" + base.simpleName().toLowerCase() + dimSuffix + "_u" + width;
  }

  private static String baseSetterName(RegisterTensor base, int width) {
    var dimSuffix = new StringBuilder();
    if (!base.indexDimensions().isEmpty()) {
      dimSuffix.append("_");
      for (int i = 0; i < base.indexDimensions().size(); i++) {
        if (i > 0) {
          dimSuffix.append("_");
        }
        dimSuffix.append("i").append(base.indexDimensions().get(i).size());
      }
    }
    return "set_" + base.simpleName().toLowerCase() + dimSuffix + "_u" + width;
  }

  private static String zeroCheck(List<Constant.Value> values) {
    var checks = new ArrayList<String>();
    for (int i = 0; i < values.size(); i++) {
      checks.add("d" + i + " == ((uint32_t) " + values.get(i).hexadecimal() + ")");
    }
    return String.join(" && ", checks);
  }

  private static String toCUnsignedType(int width) {
    return switch (width) {
      case 1 -> "bool";
      case 8 -> "uint8_t";
      case 16 -> "uint16_t";
      case 32 -> "uint32_t";
      case 64 -> "uint64_t";
      default -> throw new IllegalStateException("Unsupported accessor width: " + width);
    };
  }

  private static String toTypedExtract(String valueExpr, int sourceWidth, int resultWidth) {
    if (resultWidth == sourceWidth) {
      return "(" + toCUnsignedType(resultWidth) + ") " + valueExpr;
    }
    return "(" + toCUnsignedType(resultWidth) + ") "
        + "VADL_uextract(" + valueExpr + ", " + resultWidth + ")";
  }

  private static long strideForVirtualIndex(int resultBitWidth,
                                            List<RegisterTensor.Dimension> dimensions,
                                            int index) {
    long stride = resultBitWidth;
    for (int i = index + 1; i < dimensions.size(); i++) {
      stride = Math.multiplyExact(stride, dimensions.get(i).size());
    }
    return stride;
  }
}
