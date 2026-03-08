// SPDX-FileCopyrightText : © 2025-2026 TU Wien <vadl@tuwien.ac.at>
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
import vadl.iss.passes.IssGdbInfoExtractionPass;
import vadl.iss.passes.extensions.RegInfo;
import vadl.iss.template.IssTemplateRenderingPass;
import vadl.pass.PassResults;
import vadl.utils.codegen.CCodeBuilder;
import vadl.utils.codegen.CStringBuilder;
import vadl.viam.RegisterTensor;
import vadl.viam.Specification;

/**
 * Emits the {@code target/gen-arch/gdbstub.c} file which implements target-specific callback
 * that are used by QEMU's generic gdbstub to read and modify the CPU state.
 * The minimal required callbacks are read/write of CPU registers.
 *
 * <p>The {@link vadl.iss.template.gdb_xml.EmitIssGdbXmlPass} emits the CPU register information
 * used by GDB to address certain registers.</p>
 */
public class EmitIssGdbStubPass extends IssTemplateRenderingPass {
  private record RegGroup(
      String helperId,
      List<IssGdbInfoExtractionPass.Result.Reg> regs,
      int startGdbNr,
      int endGdbNr
  ) {
    IssGdbInfoExtractionPass.Result.Reg sample() {
      return regs.get(0);
    }
  }

  public EmitIssGdbStubPass(IssConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected String issTemplatePath() {
    return "target/gen-arch/gdbstub.c";
  }

  @Override
  protected Map<String, Object> createVariables(PassResults passResults,
                                                Specification specification) {
    var vars = super.createVariables(passResults, specification);
    var gdbInfo = passResults.lastResultOf(IssGdbInfoExtractionPass.class,
        IssGdbInfoExtractionPass.Result.class);
    var groups = groupRegs(gdbInfo.regs());
    vars.put("regs", gdbInfo.regs());
    vars.put("gdb_helpers", createHelpers(groups));
    vars.put("read_regs", createRead(groups));
    vars.put("write_regs", createWrite(groups));
    return vars;
  }

  private String createHelpers(List<RegGroup> groups) {
    var helpers = new CStringBuilder();
    for (var group : groups) {
      emitReadHelper(helpers, group);
      helpers.newLine();
      emitWriteHelper(helpers, group);
      helpers.newLine();
    }
    return helpers.toString();
  }

  private String createRead(List<RegGroup> groups) {
    var read = new CStringBuilder();
    read.indent();
    for (var group : groups) {
      read.ifStmt(" n >= " + group.startGdbNr() + " && n <= " + group.endGdbNr(), () ->
          read.stmt("return gdb_read_" + group.helperId() + "(env, mem_buf, n - "
              + group.startGdbNr() + ")"));
    }
    read.stmt("return 0");
    return read.toString();
  }

  private String createWrite(List<RegGroup> groups) {
    var write = new CStringBuilder();
    write.indent();
    for (var group : groups) {
      write.ifStmt(" n >= " + group.startGdbNr() + " && n <= " + group.endGdbNr(), () ->
          write.stmt("return gdb_write_" + group.helperId() + "(env, mem_buf, n - "
              + group.startGdbNr() + ")"));
    }
    write.stmt("return 0");
    return write.toString();
  }

  private static List<RegGroup> groupRegs(List<IssGdbInfoExtractionPass.Result.Reg> regs) {
    var groups = new ArrayList<RegGroup>();
    if (regs.isEmpty()) {
      return groups;
    }
    var currentOrigin = regs.get(0).origin();
    var current = new ArrayList<IssGdbInfoExtractionPass.Result.Reg>();
    for (var reg : regs) {
      if (!reg.origin().equals(currentOrigin)) {
        groups.add(makeGroup(current));
        current = new ArrayList<>();
        currentOrigin = reg.origin();
      }
      current.add(reg);
    }
    groups.add(makeGroup(current));
    return groups;
  }

  private static RegGroup makeGroup(List<IssGdbInfoExtractionPass.Result.Reg> groupRegs) {
    var sample = groupRegs.get(0);
    var helperId = sanitizeIdent(sample.origin().simpleName().toLowerCase() + "_" + sample.gdbNr());
    return new RegGroup(
        helperId,
        List.copyOf(groupRegs),
        groupRegs.get(0).gdbNr(),
        groupRegs.get(groupRegs.size() - 1).gdbNr()
    );
  }

  private static String sanitizeIdent(String name) {
    var out = new StringBuilder();
    for (int i = 0; i < name.length(); i++) {
      var c = name.charAt(i);
      if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
          || c == '_') {
        out.append(c);
      } else {
        out.append('_');
      }
    }
    return out.toString();
  }

  private void emitReadHelper(CCodeBuilder builder, RegGroup group) {
    var sample = group.sample();
    var origin = sample.origin();
    var bitSize = sample.bitSize();
    var base = regBaseName(origin);
    var indexVars = indexVars(origin);
    builder.appendLn("static int gdb_read_" + group.helperId()
            + "(struct CPUArchState *env, GByteArray *mem_buf, int idx) {")
        .indent();
    if (usesChunkAccess(sample)) {
      emitUnflattenIndex(builder, origin);
      var callPrefix = chunkCallPrefix(origin, indexVars);
      if (bitSize > 64) {
        emitReadChunkedWide(builder, bitSize, callPrefix);
      } else {
        emitReadChunkedScalar(builder, bitSize, callPrefix);
      }
    } else if (origin.isSingleRegister()) {
      builder.stmt("return " + scalarGetter(bitSize) + "(mem_buf, env->" + base + ")");
    } else {
      builder.stmt("return " + scalarGetter(bitSize) + "(mem_buf, env->" + base + "[idx])");
    }
    builder.unindent().appendLn("}");
  }

  private void emitWriteHelper(CCodeBuilder builder, RegGroup group) {
    var sample = group.sample();
    var origin = sample.origin();
    var bitSize = sample.bitSize();
    var base = regBaseName(origin);
    var indexVars = indexVars(origin);
    var constrainedIndices = constrainedFlatIndices(origin);
    builder.appendLn("static int gdb_write_" + group.helperId()
            + "(struct CPUArchState *env, uint8_t *mem_buf, int idx) {")
        .indent();
    if (!constrainedIndices.isEmpty()) {
      builder.ifStmt(constrainedIndexCond(constrainedIndices), () ->
          builder.stmt("return " + wireSizeBytes(bitSize)));
    }
    if (usesChunkAccess(sample)) {
      emitUnflattenIndex(builder, origin);
      var callPrefix = chunkSetCallPrefix(origin, indexVars);
      if (bitSize > 64) {
        emitWriteChunkedWide(builder, bitSize, callPrefix);
      } else {
        emitWriteChunkedScalar(builder, bitSize, callPrefix);
      }
    } else if (origin.isSingleRegister()) {
      builder.stmt("env->" + base + " = " + scalarLoader(bitSize) + "(mem_buf)")
          .stmt("return " + wireSizeBytes(bitSize));
    } else {
      builder.stmt("env->" + base + "[idx] = " + scalarLoader(bitSize) + "(mem_buf)")
          .stmt("return " + wireSizeBytes(bitSize));
    }
    builder.unindent().appendLn("}");
  }

  private static void emitUnflattenIndex(CCodeBuilder builder, RegisterTensor origin) {
    var dims = origin.indexDimensions().stream().map(RegisterTensor.Dimension::size).toList();
    if (dims.isEmpty()) {
      return;
    }
    builder.varDecl("int", "tmp_idx", "idx");
    for (int i = dims.size() - 1; i >= 0; i--) {
      var dim = dims.get(i);
      if (i == 0) {
        builder.varDecl("uint32_t", "i0", "(uint32_t) tmp_idx");
      } else {
        builder.varDecl("uint32_t", "i" + i, "(uint32_t) (tmp_idx % " + dim + ")")
            .stmt("tmp_idx /= " + dim);
      }
    }
  }

  private static List<String> indexVars(RegisterTensor origin) {
    var dims = origin.indexDimensions().stream().map(RegisterTensor.Dimension::size).toList();
    var vars = new ArrayList<String>();
    for (int i = 0; i < dims.size(); i++) {
      vars.add("i" + i);
    }
    return vars;
  }

  private static List<Integer> constrainedFlatIndices(RegisterTensor origin) {
    var dims = origin.indexDimensions().stream().map(RegisterTensor.Dimension::size).toList();
    var out = new ArrayList<Integer>();
    for (var c : origin.constraints()) {
      var indices = c.indices().stream()
          .map(i -> ((vadl.viam.Constant.Value) i).intValue())
          .toList();
      out.add(flattenIndex(dims, indices));
    }
    return out.stream().distinct().sorted().toList();
  }

  private static int flattenIndex(List<Integer> dims, List<Integer> indices) {
    if (dims.isEmpty()) {
      return 0;
    }
    int idx = 0;
    int stride = 1;
    for (int i = dims.size() - 1; i >= 0; i--) {
      idx += indices.get(i) * stride;
      stride *= dims.get(i);
    }
    return idx;
  }

  private static String constrainedIndexCond(List<Integer> constrainedIndices) {
    var c = new StringBuilder();
    for (int i = 0; i < constrainedIndices.size(); i++) {
      if (i > 0) {
        c.append(" || ");
      }
      c.append("idx == ").append(constrainedIndices.get(i));
    }
    return c.toString();
  }

  private static void emitReadChunkedScalar(CCodeBuilder builder, int bitSize, String callPrefix) {
    builder.varDecl("uint64_t", "v", callPrefix + "0, " + bitSize + ")")
        .stmt("return " + scalarGetter(bitSize) + "(mem_buf, "
            + scalarValueCast("v", bitSize) + ")");
  }

  private static void emitWriteChunkedScalar(CCodeBuilder builder, int bitSize, String callPrefix) {
    builder.stmt(callPrefix + "0, " + bitSize + ", " + scalarLoader(bitSize) + "(mem_buf))")
        .stmt("return " + wireSizeBytes(bitSize));
  }

  private static void emitReadChunkedWide(CCodeBuilder builder, int bitSize, String callPrefix) {
    builder.varDecl("int", "len", "0")
        .varDecl("uint32_t", "bit_off", "0")
        .varDecl("uint32_t", "rem", String.valueOf(bitSize))
        .whileLoop("rem > 0", () -> {
          builder.varDecl("uint32_t", "chunk_bits")
              .ifStmt("rem >= 64", () -> builder.stmt("chunk_bits = 64"))
              .append(" else if (rem >= 32)").appendLn(" {")
              .indent();
          builder.stmt("chunk_bits = 32");
          builder.unindent().appendLn("}")
              .append(" else if (rem >= 16)").appendLn(" {")
              .indent();
          builder.stmt("chunk_bits = 16");
          builder.unindent().appendLn("}")
              .append(" else if (rem >= 8)").appendLn(" {")
              .indent();
          builder.stmt("chunk_bits = 8");
          builder.unindent().appendLn("}")
              .appendLn(" else {")
              .indent();
          builder.stmt("chunk_bits = rem");
          builder.unindent().appendLn("}");
          builder.varDecl("uint64_t", "v", callPrefix + "bit_off, chunk_bits)")
              .ifStmt("chunk_bits == 64", () -> builder.stmt("len += gdb_get_reg64(mem_buf, v)"))
              .append(" else if (chunk_bits == 32)").appendLn(" {")
              .indent();
          builder.stmt("len += gdb_get_reg32(mem_buf, (uint32_t) v)");
          builder.unindent().appendLn("}")
              .append(" else if (chunk_bits == 16)").appendLn(" {")
              .indent();
          builder.stmt("len += gdb_get_reg16(mem_buf, (uint16_t) v)");
          builder.unindent().appendLn("}")
              .appendLn(" else {")
              .indent();
          builder.stmt("len += gdb_get_reg8(mem_buf, (uint8_t) v)");
          builder.unindent().appendLn("}");
          builder.stmt("bit_off += chunk_bits")
              .stmt("rem -= chunk_bits");
        })
        .stmt("return len");
  }

  private static void emitWriteChunkedWide(CCodeBuilder builder, int bitSize, String callPrefix) {
    builder.varDecl("uint32_t", "bit_off", "0")
        .varDecl("uint32_t", "byte_off", "0")
        .varDecl("uint32_t", "rem", String.valueOf(bitSize))
        .whileLoop("rem > 0", () -> {
          builder.varDecl("uint32_t", "chunk_bits")
              .varDecl("uint32_t", "chunk_bytes")
              .varDecl("uint64_t", "v")
              .ifStmt("rem >= 64", () ->
                  builder.stmt("chunk_bits = 64")
                      .stmt("chunk_bytes = 8")
                      .stmt("v = ldq_p(mem_buf + byte_off)"))
              .append(" else if (rem >= 32)").appendLn(" {")
              .indent();
          builder.stmt("chunk_bits = 32")
              .stmt("chunk_bytes = 4")
              .stmt("v = ldl_p(mem_buf + byte_off)");
          builder.unindent().appendLn("}")
              .append(" else if (rem >= 16)").appendLn(" {")
              .indent();
          builder.stmt("chunk_bits = 16")
              .stmt("chunk_bytes = 2")
              .stmt("v = lduw_p(mem_buf + byte_off)");
          builder.unindent().appendLn("}")
              .appendLn(" else {")
              .indent();
          builder.stmt("chunk_bits = (rem >= 8) ? 8 : rem")
              .stmt("chunk_bytes = 1")
              .stmt("v = ldub_p(mem_buf + byte_off)");
          builder.unindent().appendLn("}");
          builder.stmt(callPrefix + "bit_off, chunk_bits, v)")
              .stmt("bit_off += chunk_bits")
              .stmt("byte_off += chunk_bytes")
              .stmt("rem -= chunk_bits");
        })
        .stmt("return byte_off");
  }

  private static boolean usesChunkAccess(IssGdbInfoExtractionPass.Result.Reg reg) {
    return reg.bitSize() > 64 || !reg.origin().expectExtension(RegInfo.class).isTcgScalar();
  }

  private static int wireSizeBytes(int bitSize) {
    return (bitSize + 7) / 8;
  }

  private static String scalarGetter(int bitSize) {
    if (bitSize <= 8) {
      return "gdb_get_reg8";
    }
    if (bitSize <= 16) {
      return "gdb_get_reg16";
    }
    if (bitSize <= 32) {
      return "gdb_get_reg32";
    }
    return "gdb_get_reg64";
  }

  private static String scalarLoader(int bitSize) {
    if (bitSize <= 8) {
      return "ldub_p";
    }
    if (bitSize <= 16) {
      return "lduw_p";
    }
    if (bitSize <= 32) {
      return "ldl_p";
    }
    return "ldq_p";
  }

  private static String scalarValueCast(String expr, int bitSize) {
    if (bitSize <= 8) {
      return "(uint8_t) " + expr;
    }
    if (bitSize <= 16) {
      return "(uint16_t) " + expr;
    }
    if (bitSize <= 32) {
      return "(uint32_t) " + expr;
    }
    return expr;
  }

  private static String regBaseName(RegisterTensor origin) {
    return origin.simpleName().toLowerCase();
  }

  private static String chunkCallPrefix(RegisterTensor origin, List<String> indices) {
    var base = regBaseName(origin);
    var args = new StringBuilder("env");
    for (var i : indices) {
      args.append(", ").append(i);
    }
    return "cpu_get_" + base + "_chunk(" + args + ", ";
  }

  private static String chunkSetCallPrefix(RegisterTensor origin, List<String> indices) {
    var base = regBaseName(origin);
    var args = new StringBuilder("env");
    for (var i : indices) {
      args.append(", ").append(i);
    }
    return "cpu_set_" + base + "_chunk(" + args + ", ";
  }
}
