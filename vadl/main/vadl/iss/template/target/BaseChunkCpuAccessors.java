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
import vadl.iss.passes.extensions.RegInfo;
import vadl.viam.RegisterTensor;
import vadl.viam.Specification;

/**
 * Renders dynamic chunk read accessors for CPU-vector base register accesses.
 *
 * <p>These accessors are consumed by helper/procedure/exception emitters for unified
 * {@code IssReadRegNode(BASE, CHUNK)} reads with translation-time or runtime bit windows.
 */
final class BaseChunkCpuAccessors {

  private BaseChunkCpuAccessors() {
  }

  static List<Map<String, Object>> renderReadAccessors(Specification specification,
                                                       IssConfiguration config) {
    var out = new ArrayList<Map<String, Object>>();
    specification.isa().get().registerTensors().forEach(reg -> {
      var info = reg.expectExtension(RegInfo.class);
      if (info.execClass() == RegInfo.ExecClass.CPU_VECTOR) {
        out.add(renderReadAccessor(reg, config));
      }
    });
    return out;
  }

  static List<Map<String, Object>> renderWriteAccessors(Specification specification,
                                                        IssConfiguration config) {
    var out = new ArrayList<Map<String, Object>>();
    specification.isa().get().registerTensors().forEach(reg -> {
      var info = reg.expectExtension(RegInfo.class);
      if (info.execClass() == RegInfo.ExecClass.CPU_VECTOR) {
        out.add(renderWriteAccessor(reg, config));
      }
    });
    return out;
  }

  private static Map<String, Object> renderReadAccessor(RegisterTensor reg,
                                                        IssConfiguration config) {
    var body = new StringBuilder();
    var containerWidth = reg.resultType(reg.indexDimensions().size()).bitWidth();
    body.append("assert(bit_width > 0);\n");
    body.append("assert(bit_width <= 64);\n");
    body.append("assert(bit_offset + bit_width <= ((uint64_t) ")
        .append(containerWidth)
        .append("));\n");
    for (int i = 0; i < reg.indexDimensions().size(); i++) {
      body.append("assert(i")
          .append(i)
          .append(" < ")
          .append(reg.indexDimensions().get(i).size())
          .append(");\n");
    }
    body.append("size_t reg_off = ")
        .append(flatBaseIndexExpr(reg))
        .append(" * ((size_t) ")
        .append(containerWidth / 8)
        .append(");\n");
    body.append("if ((bit_offset & UINT64_C(0x7)) == UINT64_C(0) && (bit_width & 7u) == 0) {\n");
    body.append("  size_t byte_off = reg_off + (size_t) (bit_offset >> 3);\n");
    body.append("  size_t byte_count = (size_t) (bit_width >> 3);\n");
    body.append("  uint64_t out = 0;\n");
    body.append("  memcpy(&out, ((uint8_t*) env->")
        .append(reg.simpleName().toLowerCase())
        .append(") + byte_off, byte_count);\n");
    body.append("  return out;\n");
    body.append("}\n");
    body.append("uint64_t out = 0;\n");
    body.append("for (uint32_t b = 0; b < bit_width; b++) {\n");
    body.append("  uint64_t src_bit = bit_offset + ((uint64_t) b);\n");
    body.append("  size_t src_byte = reg_off + (size_t) (src_bit >> 3);\n");
    body.append("  uint8_t src = *(((uint8_t*) env->")
        .append(reg.simpleName().toLowerCase())
        .append(") + src_byte);\n");
    body.append("  uint64_t bit = (uint64_t) ((src >> (src_bit & 7)) & 1u);\n");
    body.append("  out |= (bit << b);\n");
    body.append("}\n");
    body.append("return out;");

    var signature = "uint64_t cpu_get_" + reg.simpleName().toLowerCase() + "_chunk("
        + "CPU" + config.targetName().toUpperCase() + "State *env"
        + renderArgDecls(reg.indexDimensions().size())
        + ", uint64_t bit_offset, uint32_t bit_width)";
    return Map.of(
        "signature", signature,
        "body", body.toString()
    );
  }

  private static Map<String, Object> renderWriteAccessor(RegisterTensor reg,
                                                         IssConfiguration config) {
    var body = new StringBuilder();
    var containerWidth = reg.resultType(reg.indexDimensions().size()).bitWidth();
    body.append("assert(bit_width > 0);\n");
    body.append("assert(bit_width <= 64);\n");
    body.append("assert(bit_offset + bit_width <= ((uint64_t) ")
        .append(containerWidth)
        .append("));\n");
    for (int i = 0; i < reg.indexDimensions().size(); i++) {
      body.append("assert(i")
          .append(i)
          .append(" < ")
          .append(reg.indexDimensions().get(i).size())
          .append(");\n");
    }
    body.append("size_t reg_off = ")
        .append(flatBaseIndexExpr(reg))
        .append(" * ((size_t) ")
        .append(containerWidth / 8)
        .append(");\n");
    body.append("if ((bit_offset & UINT64_C(0x7)) == UINT64_C(0) && (bit_width & 7u) == 0) {\n");
    body.append("  size_t byte_off = reg_off + (size_t) (bit_offset >> 3);\n");
    body.append("  size_t byte_count = (size_t) (bit_width >> 3);\n");
    body.append("  uint64_t tmp = value;\n");
    body.append("  memcpy(((uint8_t*) env->")
        .append(reg.simpleName().toLowerCase())
        .append(") + byte_off, &tmp, byte_count);\n");
    body.append("  return;\n");
    body.append("}\n");
    body.append("for (uint32_t b = 0; b < bit_width; b++) {\n");
    body.append("  uint64_t dst_bit = bit_offset + ((uint64_t) b);\n");
    body.append("  size_t dst_byte = reg_off + (size_t) (dst_bit >> 3);\n");
    body.append("  uint8_t *dst = ((uint8_t*) env->")
        .append(reg.simpleName().toLowerCase())
        .append(") + dst_byte;\n");
    body.append("  uint8_t bit = (uint8_t) ((value >> b) & UINT64_C(0x1));\n");
    body.append("  uint8_t mask = (uint8_t) (1u << (dst_bit & 7));\n");
    body.append("  *dst = (uint8_t) ((*dst & (uint8_t)(~mask)) | (uint8_t)(bit ? mask : 0));\n");
    body.append("}\n");

    var signature = "void cpu_set_" + reg.simpleName().toLowerCase() + "_chunk("
        + "CPU" + config.targetName().toUpperCase() + "State *env"
        + renderArgDecls(reg.indexDimensions().size())
        + ", uint64_t bit_offset, uint32_t bit_width, uint64_t value)";
    return Map.of(
        "signature", signature,
        "body", body.toString()
    );
  }

  private static String renderArgDecls(int argCount) {
    var sb = new StringBuilder();
    for (int i = 0; i < argCount; i++) {
      sb.append(", uint32_t i").append(i);
    }
    return sb.toString();
  }

  private static String flatBaseIndexExpr(RegisterTensor base) {
    var dims = base.indexDimensions();
    if (dims.isEmpty()) {
      return "((size_t) 0)";
    }
    var expr = "((size_t) i0)";
    for (int i = 1; i < dims.size(); i++) {
      expr = "((" + expr + ") * ((size_t) " + dims.get(i).size() + ") + ((size_t) i" + i + "))";
    }
    return expr;
  }
}
