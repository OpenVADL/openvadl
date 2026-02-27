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

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import vadl.configuration.IssConfiguration;
import vadl.cppCodeGen.CppTypeMap;
import vadl.iss.passes.extensions.IssAccessorRegistry;
import vadl.iss.passes.extensions.RegInfo;
import vadl.iss.passes.extensions.RegInfo.AliasAccessorDescriptor;
import vadl.viam.ArtificialResource;
import vadl.viam.RegisterTensor;

/**
 * Renders helper/procedure/exceptions-side alias read accessors for CPU source/header templates.
 */
final class AliasCpuAccessors {

  private AliasCpuAccessors() {
  }

  static List<Map<String, Object>> renderReadAccessors(IssAccessorRegistry accessorRegistry,
                                                       IssConfiguration config) {
    return accessorRegistry.aliasAccessors(RegInfo.AccessType.READ, RegInfo.BackendKind.CPU_HELPER)
        .stream()
        .map(alias -> renderReadAccessor(alias, config))
        .toList();
  }

  static List<Map<String, Object>> renderWriteAccessors(IssAccessorRegistry accessorRegistry,
                                                        IssConfiguration config) {
    return accessorRegistry.aliasAccessors(RegInfo.AccessType.WRITE, RegInfo.BackendKind.CPU_HELPER)
        .stream()
        .map(alias -> renderWriteAccessor(alias, config))
        .toList();
  }

  private static Map<String, Object> renderReadAccessor(AliasAccessorDescriptor descriptor,
                                                        IssConfiguration config) {
    var alias = descriptor.alias();
    var semantics = alias.semantics();
    var base = semantics.baseTensor();
    var baseWidth = base.resultType(base.indexDimensions().size()).bitWidth();
    if (baseWidth > 64 && alias.resultType().bitWidth() % 8 != 0) {
      throw new IllegalStateException(
          "Wide-base alias helper read accessors require byte-aligned result width for "
              + alias.simpleName());
    }
    var argDecls = renderArgDecls(descriptor.accessorArgs());
    var baseIndexCount = semantics.baseTensor().indexDimensions().size();
    var baseArgList = renderArgList(descriptor);
    var cpuStateType = "CPU" + config.targetName().toUpperCase() + "State";
    var retType = toCUnsignedType(alias.resultType().bitWidth());
    var baseGetter = baseGetterName(base, baseWidth);
    final var signature = retType + " cpu_get_" + alias.simpleName().toLowerCase()
        + "(" + cpuStateType + " *env" + argDecls + ")";

    var body = new StringBuilder();
    var zero = zeroCheck(descriptor);
    if (zero != null) {
      body.append("if (")
          .append(zero)
          .append(") {\n  return 0;\n}\n");
    }
    var slice = semantics.aliasSlice();
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
        body.append("lsb += ((uint64_t) ")
            .append(descriptor.accessorArgs().get(descriptor.expansionArgStart() + i).name())
            .append(") * ")
            .append(stride)
            .append(";\n");
      }
      if (baseWidth <= 64) {
        body.append("uint64_t base = ")
            .append(baseGetter)
            .append("(env")
            .append(baseArgList)
            .append(");\n");
        body.append("uint64_t shifted = VADL_lsr(base, ")
            .append(baseWidth)
            .append(", lsb, 64);\n");
        body.append("return ")
            .append(toTypedExtract("shifted", baseWidth, alias.resultType().bitWidth()))
            .append(";");
      } else {
        body.append("size_t off = ")
            .append(flatBaseIndexExpr(descriptor))
            .append(" * ((size_t) ")
            .append(baseWidth / 8)
            .append(");\n");
        body.append("off += (size_t) (lsb >> 3);\n");
        body.append(retType).append(" out = 0;\n");
        body.append("memcpy(&out, ((uint8_t*) env->")
            .append(base.simpleName().toLowerCase())
            .append(") + off, ")
            .append(alias.resultType().bitWidth() / 8)
            .append(");\n");
        body.append("return out;");
      }
    } else {
      body.append("uint64_t base = ")
          .append(baseGetter)
          .append("(env")
          .append(baseArgList)
          .append(");\n");
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

  private static Map<String, Object> renderWriteAccessor(AliasAccessorDescriptor descriptor,
                                                         IssConfiguration config) {
    var alias = descriptor.alias();
    var semantics = alias.semantics();
    var base = semantics.baseTensor();
    var baseWidth = base.resultType(base.indexDimensions().size()).bitWidth();
    if (baseWidth > 64 && alias.resultType().bitWidth() % 8 != 0) {
      throw new IllegalStateException(
          "Wide-base alias helper write accessors require byte-aligned result width for "
              + alias.simpleName());
    }

    var argDecls = renderArgDecls(descriptor.accessorArgs());
    var baseIndexCount = semantics.baseTensor().indexDimensions().size();
    var baseArgList = renderArgList(descriptor);
    var cpuStateType = "CPU" + config.targetName().toUpperCase() + "State";
    var valueType = toCUnsignedType(alias.resultType().bitWidth());
    var baseGetter = baseGetterName(base, baseWidth);
    var baseSetter = baseSetterName(base, baseWidth);
    var signature = "void cpu_set_" + alias.simpleName().toLowerCase()
        + "(" + cpuStateType + " *env" + argDecls + ", " + valueType + " value)";

    var body = new StringBuilder();
    var zero = zeroCheck(descriptor);
    if (zero != null) {
      body.append("if (")
          .append(zero)
          .append(") {\n  return;\n}\n");
    }

    var isExpansion = semantics.totalIndexCount() > baseIndexCount;
    var slice = semantics.aliasSlice();
    if (isExpansion) {
      if (semantics.overwriteMode() != ArtificialResource.OverwriteMode.MERGE) {
        throw new IllegalStateException(
            "Wide-base expansion alias helper writes only support overwrite=merge for "
                + alias.simpleName());
      }
      var dynamicConsumed = Math.max(0, baseIndexCount - semantics.fixedIndices().size());
      var remainingDimensions = semantics.dynamicDimensions().subList(
          dynamicConsumed,
          semantics.dynamicDimensions().size()
      );
      body.append("uint64_t lsb = 0;\n");
      for (int i = 0; i < remainingDimensions.size(); i++) {
        long stride = strideForVirtualIndex(alias.resultType().bitWidth(), remainingDimensions, i);
        body.append("lsb += ((uint64_t) ")
            .append(descriptor.accessorArgs().get(descriptor.expansionArgStart() + i).name())
            .append(") * ")
            .append(stride)
            .append(";\n");
      }
      if (baseWidth <= 64) {
        body.append("uint64_t base = ")
            .append(baseGetter)
            .append("(env")
            .append(baseArgList)
            .append(");\n");
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
      } else {
        body.append("size_t off = ")
            .append(flatBaseIndexExpr(descriptor))
            .append(" * ((size_t) ")
            .append(baseWidth / 8)
            .append(");\n");
        body.append("off += (size_t) (lsb >> 3);\n");
        body.append("memcpy(((uint8_t*) env->")
            .append(base.simpleName().toLowerCase())
            .append(") + off, &value, ")
            .append(alias.resultType().bitWidth() / 8)
            .append(");");
      }
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

  private static String renderArgDecls(List<RegInfo.AccessorArg> args) {
    if (args.isEmpty()) {
      return "";
    }
    return args.stream()
        .map(arg -> ", " + arg.ctype() + " " + arg.name())
        .collect(java.util.stream.Collectors.joining());
  }

  private static String renderArgList(AliasAccessorDescriptor descriptor) {
    if (descriptor.baseArgBindings().isEmpty()) {
      return "";
    }
    var args = descriptor.baseArgBindings().stream()
        .map(binding -> {
          if (binding instanceof RegInfo.FixedArgBinding fixed) {
            return "((" + CppTypeMap.nextFittingUInt(fixed.value().type()) + ") "
                + fixed.value().hexadecimal() + ")";
          }
          var forwarded = (RegInfo.ForwardedArgBinding) binding;
          return descriptor.accessorArgs().get(forwarded.accessorArgIndex()).name();
        })
        .collect(java.util.stream.Collectors.joining(", "));
    return ", " + args;
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

  private static @Nullable String zeroCheck(AliasAccessorDescriptor descriptor) {
    var guard = descriptor.zeroGuard();
    if (guard == null) {
      return null;
    }
    if (guard instanceof RegInfo.AlwaysZeroGuard) {
      return "1";
    }
    var conditional = (RegInfo.ConditionalZeroGuard) guard;
    return conditional.matches().stream()
        .map(match -> descriptor.accessorArgs().get(match.accessorArgIndex()).name()
            + " == ((" + CppTypeMap.nextFittingUInt(match.value().type()) + ") "
            + match.value().hexadecimal() + ")")
        .collect(java.util.stream.Collectors.joining(" && "));
  }

  private static String flatBaseIndexExpr(AliasAccessorDescriptor descriptor) {
    var dims = descriptor.alias().semantics().baseTensor().indexDimensions();
    if (dims.isEmpty()) {
      return "((size_t) 0)";
    }
    var args = descriptor.baseArgBindings().stream()
        .map(binding -> {
          if (binding instanceof RegInfo.FixedArgBinding fixed) {
            return "((size_t) " + fixed.value().intValue() + ")";
          }
          var forwarded = (RegInfo.ForwardedArgBinding) binding;
          return "((size_t) " + descriptor.accessorArgs().get(forwarded.accessorArgIndex()).name()
              + ")";
        })
        .toList();
    var expr = args.get(0);
    for (int i = 1; i < dims.size(); i++) {
      expr = "((" + expr + ") * ((size_t) " + dims.get(i).size() + ") + " + args.get(i) + ")";
    }
    return expr;
  }

  private static String toCUnsignedType(int width) {
    return switch (CppTypeMap.nextFittingBitSize(width)) {
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
