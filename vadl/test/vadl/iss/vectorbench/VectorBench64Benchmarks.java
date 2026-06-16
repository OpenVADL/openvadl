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

package vadl.iss.vectorbench;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.annotation.Nullable;
import vadl.TestUtils;
import vadl.asm.ElfProgramBuilder;
import vadl.asm.InstructionEncoder;
import vadl.asm.ProgramAssembler;
import vadl.types.Type;
import vadl.utils.Disassembler;
import vadl.viam.Constant;
import vadl.viam.InstructionSetArchitecture;

/**
 * Generates the complete synthetic {@code VectorBench64} benchmark corpus.
 *
 * <p>Each benchmark case is emitted as a standalone ELF program that initializes architectural
 * state, runs a tight loop dominated by one instruction pattern, verifies the final result with a
 * guest-side checksum, and exits via HTIF. The generated directory also contains a manifest and a
 * Python runner used by the JUnit benchmark harness and the GitHub workflow.
 */
public final class VectorBench64Benchmarks {

  static final long TEXT_BASE = 0x80000000L;
  static final long DATA_BASE = 0x80010000L;
  static final long TOHOST_ADDR = 0x80020000L;
  static final long FROMHOST_ADDR = 0x80020008L;

  static final int DATA_OFFSET = (int) (DATA_BASE - TEXT_BASE);
  static final int TOHOST_OFFSET = (int) (TOHOST_ADDR - TEXT_BASE);

  static final int VEC_BYTES = 128;
  static final int ROW_BYTES = 32;
  static final int MAT_BYTES = 128;
  static final int HUGE_BYTES = 4096;

  private static final Path VADL_SPEC = resolveProjectPath("sys/vectorbench/vectorbench64.vadl");
  private static final InstructionSetArchitecture ISA =
      TestUtils.compileToViam(readSpecSource()).isa().orElseThrow();
  private static final Set<String> IMPLICIT_ZERO_OPERANDS = Set.of("mode");
  private static final int OPCODE_VMAC = 0b0100110;

  private static final long CHECKSUM_SEED = 0x243f6a8885a308d3L;
  private static final int CHECKSUM_MUL = 0x9e3779b1;

  private static final int REG_LOOP = 28;
  private static final int REG_PTR0 = 20;
  private static final int REG_PTR1 = 21;
  private static final int REG_ACC = 22;
  private static final int REG_MUL = 23;
  private static final int REG_TMP = 24;
  private static final int REG_EXPECTED_PTR = 25;
  private static final int REG_EXPECTED = 26;
  private static final int REG_TOHOST = 27;

  private static final int REG_SCALAR0 = 1;
  private static final int REG_SCALAR1 = 2;
  private static final int REG_SCALAR2 = 3;
  private static final int REG_SCALAR3 = 4;
  private static final int REG_SCALAR4 = 5;
  private static final int REG_SCALAR5 = 6;
  private static final int REG_SCALAR6 = 7;
  private static final int REG_SCALAR7 = 8;

  private static final int V_DEST = 0;
  private static final int V_SRC1 = 1;
  private static final int V_SRC2 = 2;
  private static final int V_SRC3 = 3;
  private static final int P_MASK = 0;
  private static final int R_DEST = 0;
  private static final int R_SRC1 = 1;
  private static final int M_DEST = 0;
  private static final int M_SRC1 = 1;
  private static final int M_SRC2 = 2;
  private static final int H_DEST = 0;
  private static final int H_SRC1 = 1;
  private static final int H_SRC2 = 2;
  private static final int H_SRC3 = 3;

  private static final int[] VECTOR_SCALARS = {7, 11, -5, 19};
  private static final int[] VECTOR_VL_SCALARS = {8, 17, 31, 5};
  private static final int[] VECTOR_MASKS = {
      0xA55A3CC5,
      0x0F0FF0F0,
      0xC3C33C3C,
      0x5AA5CC33
  };

  private static final double DEFAULT_ITERATION_SCALE = readIterationScale();
  private static final ThreadLocal<Double> ITERATION_SCALE_OVERRIDE = new ThreadLocal<>();

  private static final BenchmarkProfile PROFILE_VECTOR_VADD_DO = profile(83_218, 4);
  private static final BenchmarkProfile PROFILE_VECTOR_VADD_TENSOR = profile(75_949, 4);
  private static final BenchmarkProfile PROFILE_VECTOR_VADD_VX = profile(40_921, 8);
  private static final BenchmarkProfile PROFILE_VECTOR_VADD_VX_INC = profile(40_921, 8);
  private static final BenchmarkProfile PROFILE_VECTOR_VMOV = profile(28_362, 12);
  private static final BenchmarkProfile PROFILE_VECTOR_VBCAST = profile(28_429, 12);
  private static final BenchmarkProfile PROFILE_VECTOR_VADD_VL_08 = profile(48_376, 6);
  private static final BenchmarkProfile PROFILE_VECTOR_VADD_VL_17 = profile(51_923, 6);
  private static final BenchmarkProfile PROFILE_VECTOR_VADD_PRED = profile(77_943, 4);
  private static final BenchmarkProfile PROFILE_VECTOR_VADD_COND = profile(79_344, 4);
  private static final BenchmarkProfile PROFILE_VECTOR_VSEL = profile(77_580, 4);
  private static final BenchmarkProfile PROFILE_PREDICATE_VCMPLT_PVV = profile(512_821, 6);
  private static final BenchmarkProfile PROFILE_VECTOR_VREDSUM = profile(903_048, 12);
  private static final BenchmarkProfile PROFILE_VECTOR_VMAC_VVV = profile(37_965, 8);
  private static final BenchmarkProfile PROFILE_VECTOR_VADD_XINC = profile(27_441, 12);
  private static final BenchmarkProfile PROFILE_PREDICATE_PTRUE = profile(31_396, 64);
  private static final BenchmarkProfile PROFILE_PREDICATE_PWHILELT = profile(60_616, 32);
  private static final BenchmarkProfile PROFILE_PREDICATE_PAND = profile(38_320, 32);
  private static final BenchmarkProfile PROFILE_PREDICATE_PFIRST = profile(24_177, 64);
  private static final BenchmarkProfile PROFILE_PREDICATE_PNEXT = profile(23_129, 64);
  private static final BenchmarkProfile PROFILE_PREDICATE_CNTP = profile(104_001, 64);
  private static final BenchmarkProfile PROFILE_SCALAR_INCP = profile(165_996, 40);
  private static final BenchmarkProfile PROFILE_SCALAR_ADDVL = profile(34_752_389, 129);
  private static final BenchmarkProfile PROFILE_MEMORY_PLD = profile(15_393, 128);
  private static final BenchmarkProfile PROFILE_MEMORY_PST = profile(128_159, 128);
  private static final BenchmarkProfile PROFILE_VECTOR_VZIP_LO = profile(57_384, 6);
  private static final BenchmarkProfile PROFILE_VECTOR_VEXT = profile(56_825, 6);
  private static final BenchmarkProfile PROFILE_VECTOR_VUZP_LO = profile(57_465, 6);
  private static final BenchmarkProfile PROFILE_VECTOR_VZEXT8 = profile(41_735, 6);
  private static final BenchmarkProfile PROFILE_MEMORY_VLD1_PRED_Z = profile(34_539, 8);
  private static final BenchmarkProfile PROFILE_MEMORY_VST1_PRED = profile(215_223, 8);
  private static final BenchmarkProfile PROFILE_MEMORY_VLD1_PRED_Z_RR = profile(69_027, 4);
  private static final BenchmarkProfile PROFILE_MEMORY_VST1_PRED_RR = profile(213_594, 8);
  private static final BenchmarkProfile PROFILE_MEMORY_VLD2_PRED_Z = profile(94_581, 3);
  private static final BenchmarkProfile PROFILE_MEMORY_VST2_PRED = profile(400_936, 5);
  private static final BenchmarkProfile PROFILE_MEMORY_VLD3_PRED_Z = profile(56_217, 3);
  private static final BenchmarkProfile PROFILE_MEMORY_VST3_PRED = profile(484_918, 3);
  private static final BenchmarkProfile PROFILE_MEMORY_VLD4_PRED_Z = profile(42_048, 3);
  private static final BenchmarkProfile PROFILE_MEMORY_VST4_PRED = profile(359_128, 3);
  private static final BenchmarkProfile PROFILE_MEMORY_VGATHER_PRED_Z = profile(68_225, 4);
  private static final BenchmarkProfile PROFILE_MEMORY_VSCATTER_PRED = profile(206_115, 8);
  private static final BenchmarkProfile PROFILE_MATRIX_MADD_DO = profile(1_531_649, 4);
  private static final BenchmarkProfile PROFILE_MATRIX_MADD_TENSOR = profile(1_541_891, 4);
  private static final BenchmarkProfile PROFILE_MATRIX_MADD_ROW_BCAST = profile(1_011_874, 8);
  private static final BenchmarkProfile PROFILE_MATRIX_MREDUCE_COLS = profile(1_397_765, 8);
  private static final BenchmarkProfile PROFILE_HUGE_HADD_TENSOR = profile(48_173, 4);

  @FunctionalInterface
  private interface BodyEmitter {
    void emit(ProgramAssembler assembler);
  }

  @FunctionalInterface
  private interface VectorBinaryInstrEmitter {
    void emit(ProgramAssembler assembler, int vd, int vs1, int vs2);
  }

  @FunctionalInterface
  private interface VectorScalarInstrEmitter {
    void emit(ProgramAssembler assembler, int vd, int vs, int scalarOperand);
  }

  @FunctionalInterface
  private interface VectorScalarBinaryInstrEmitter {
    void emit(ProgramAssembler assembler, int vd, int vs1, int vs2, int scalarOperand);
  }

  @FunctionalInterface
  private interface VectorPredInstrEmitter {
    void emit(ProgramAssembler assembler, int vd, int pm, int vs1, int vs2);
  }

  @FunctionalInterface
  private interface VectorUnaryInstrEmitter {
    void emit(ProgramAssembler assembler, int vd, int vs1);
  }

  @FunctionalInterface
  private interface VectorTernaryInstrEmitter {
    void emit(ProgramAssembler assembler, int vd, int vs1, int vs2, int vs3);
  }

  @FunctionalInterface
  private interface MatrixBinaryInstrEmitter {
    void emit(ProgramAssembler assembler, int md, int ms1, int ms2);
  }

  @FunctionalInterface
  private interface MatrixRowInstrEmitter {
    void emit(ProgramAssembler assembler, int md, int ms1, int rr1);
  }

  @FunctionalInterface
  private interface HugeBinaryInstrEmitter {
    void emit(ProgramAssembler assembler, int hd, int hs1, int hs2);
  }

  @FunctionalInterface
  private interface LaneBinaryOp {
    int apply(int destOld, int src1, int src2, int lane);
  }

  @FunctionalInterface
  private interface LaneScalarOp {
    int apply(int destOld, int src, int scalar, int lane);
  }

  @FunctionalInterface
  private interface LaneScalarBinaryOp {
    int apply(int destOld, int src1, int src2, int scalar, int lane);
  }

  @FunctionalInterface
  private interface LaneUnaryOp {
    int apply(int destOld, int src, int lane);
  }

  @FunctionalInterface
  private interface LaneTernaryOp {
    int apply(int destOld, int src1, int src2, int src3, int lane);
  }

  /**
   * Reusable sizing profile for one generated benchmark case.
   *
   * @param baseIterations outer loop trip count in the generated guest program before scaling
   * @param chainRounds number of repeated same-shape chains per outer loop iteration
   */
  private record BenchmarkProfile(int baseIterations, int chainRounds) {
    int iterations() {
      return scaleIterations(baseIterations);
    }

    int bodyInstructions(int chainLength) {
      return chainRounds * chainLength;
    }
  }

  private static BenchmarkProfile profile(int iterations, int chainRounds) {
    return new BenchmarkProfile(iterations, chainRounds);
  }

  private static int scaleIterations(int iterations) {
    long scaled = Math.round(iterations * currentIterationScale());
    return (int) Math.max(1L, scaled);
  }

  private static double currentIterationScale() {
    var override = ITERATION_SCALE_OVERRIDE.get();
    return override == null ? DEFAULT_ITERATION_SCALE : override;
  }

  /**
   * Reads the optional local debug scaling factor for benchmark sizes.
   *
   * <p>{@code VECTORBENCH64_ITERATION_SCALE} is intended for development only so benchmark edits can
   * be validated without paying the cost of the full published benchmark suite.
   */
  private static double readIterationScale() {
    var value = System.getenv("VECTORBENCH64_ITERATION_SCALE");
    if (value == null || value.isBlank()) {
      return 1.0d;
    }
    double scale = Double.parseDouble(value.trim());
    if (!(scale > 0.0d)) {
      throw new IllegalArgumentException(
          "VECTORBENCH64_ITERATION_SCALE must be > 0, got " + value);
    }
    return scale;
  }

  /**
   * Metadata for one generated benchmark ELF.
   */
  public record BenchmarkArtifact(
      String id,
      String category,
      int iterations,
      int bodyRepeats,
      int activeElements,
      int resultBytes,
      Path elfPath
  ) {
  }

  /**
   * Complete output of one generation pass.
   */
  public record GeneratedBenchmarks(
      List<BenchmarkArtifact> artifacts,
      Path manifestPath
  ) {
  }

  private static final ThreadLocal<Disassembler> DISASSEMBLER = new ThreadLocal<>();

  private VectorBench64Benchmarks() {
  }

  private static String readSpecSource() {
    try {
      return Files.readString(VADL_SPEC);
    } catch (IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private static Path resolveProjectPath(String path) {
    var base = System.getenv("PROJECT_ROOT");
    if (base == null || base.isBlank()) {
      return Path.of(path).normalize();
    }
    return Path.of(base).resolve(path).normalize();
  }

  private static ProgramAssembler newProgram() {
    return new ProgramAssembler(ISA, ByteOrder.LITTLE_ENDIAN);
  }

  private static InstructionEncoder.Operand operand(String name, long value) {
    return InstructionEncoder.Operand.of(name, value);
  }

  private static void emitInstruction(ProgramAssembler asm, String instructionName,
                                      InstructionEncoder.Operand... operands) {
    asm.text().emit(instructionName, completeOperands(asm, instructionName, operands));
  }

  private static void label(ProgramAssembler asm, String name) {
    asm.text().label(name);
  }

  private static InstructionEncoder.Operand[] completeOperands(ProgramAssembler asm,
                                                               String instructionName,
                                                               InstructionEncoder.Operand... operands) {
    var completed = new ArrayList<>(Arrays.asList(operands));
    var present = new LinkedHashSet<String>();
    for (var operand : operands) {
      present.add(operand.name());
    }
    for (var operandName : IMPLICIT_ZERO_OPERANDS) {
      if (!present.contains(operandName) && asm.encoder().operandNames(instructionName).contains(
          operandName)) {
        completed.add(operand(operandName, 0));
      }
    }
    return completed.toArray(InstructionEncoder.Operand[]::new);
  }

  private static long dataLabelAddress(ProgramAssembler asm, String name) {
    return DATA_BASE + asm.session().labelOffset(".data", name);
  }

  private static long addData(ProgramAssembler asm, String name, byte[] bytes, int alignment) {
    var data = asm.data();
    data.align(alignment);
    data.label(name);
    data.writeBytes(bytes);
    return dataLabelAddress(asm, name);
  }

  private static long reserveData(ProgramAssembler asm, String name, int size, int alignment) {
    return addData(asm, name, new byte[size], alignment);
  }

  private static byte[] getTextBytes(ProgramAssembler asm) {
    return asm.session().resolvedSectionBytes(".text");
  }

  private static Map<String, Integer> getTextLabels(ProgramAssembler asm) {
    return asm.session().labels(".text");
  }

  private static byte[] buildElf(ProgramAssembler asm) {
    var builder = new ElfProgramBuilder(asm.session(), TEXT_BASE);
    int textSection = builder.addResolvedSessionSection(".text", ".text",
        ElfProgramBuilder.SHF_ALLOC | ElfProgramBuilder.SHF_EXECINSTR, TEXT_BASE, 4);
    builder.addRawSessionSection(".data", ".data",
        ElfProgramBuilder.SHF_ALLOC | ElfProgramBuilder.SHF_WRITE, DATA_BASE, 8);
    int tohostSection = builder.addSection(".tohost", ElfProgramBuilder.SHT_PROGBITS,
        ElfProgramBuilder.SHF_ALLOC | ElfProgramBuilder.SHF_WRITE, TOHOST_ADDR, new byte[16], 8);
    builder.addLoadSegment(ElfProgramBuilder.PT_LOAD, 0x7, TEXT_BASE, TEXT_BASE,
        TOHOST_OFFSET + 16, TOHOST_OFFSET + 16, 0x1000);
    builder.addSymbol("_start", TEXT_BASE, 0,
        ElfProgramBuilder.info(ElfProgramBuilder.STB_GLOBAL, ElfProgramBuilder.STT_FUNC),
        textSection);
    builder.addSymbol("tohost", TOHOST_ADDR, 8,
        ElfProgramBuilder.info(ElfProgramBuilder.STB_GLOBAL, ElfProgramBuilder.STT_OBJECT),
        tohostSection);
    builder.addSymbol("fromhost", FROMHOST_ADDR, 8,
        ElfProgramBuilder.info(ElfProgramBuilder.STB_GLOBAL, ElfProgramBuilder.STT_OBJECT),
        tohostSection);
    return builder.build();
  }

  private static void writeElf(ProgramAssembler asm, Path path) throws IOException {
    Files.createDirectories(path.getParent());
    Files.write(path, buildElf(asm));
  }

  private static void loadImm(ProgramAssembler asm, int rd, long value) {
    if (value >= -2048 && value <= 2047) {
      addi(asm, rd, 0, (int) value);
      return;
    }
    if (value < Integer.MIN_VALUE || value > 0xFFFF_FFFFL) {
      long literalAddr = addData(
          asm,
          String.format("imm_%016x_%04x", value, asm.data().size()),
          longToBytes(value),
          8);
      loadAddress(asm, rd, literalAddr);
      load64(asm, rd, rd, 0);
      return;
    }
    long hi = (value + 0x800L) >> 12;
    long lo = value - (hi << 12);
    lui(asm, rd, (int) hi);
    addi(asm, rd, rd, (int) lo);
  }

  private static void loadAddress(ProgramAssembler asm, int rd, long address) {
    loadImm(asm, rd, address);
  }

  private static void load64(ProgramAssembler asm, int rd, int base, int offset) {
    emitInstruction(asm, "LD", operand("rd", rd), operand("rs1", base), operand("imm", offset));
  }

  private static void store64(ProgramAssembler asm, int rs2, int base, int offset) {
    emitInstruction(asm, "SD", operand("rs2", rs2), operand("rs1", base), operand("imm", offset));
  }

  private static void lui(ProgramAssembler asm, int rd, int imm20) {
    emitInstruction(asm, "LUI", operand("rd", rd), operand("immHi", imm20));
  }

  private static void addi(ProgramAssembler asm, int rd, int rs1, int imm12) {
    emitInstruction(asm, "ADDI", operand("rd", rd), operand("rs1", rs1), operand("imm", imm12));
  }

  private static void addvl(ProgramAssembler asm, int rd, int rs1, int imm12) {
    emitInstruction(asm, "ADDVL", operand("rd", rd), operand("rs1", rs1), operand("imm", imm12));
  }

  private static void add(ProgramAssembler asm, int rd, int rs1, int rs2) {
    emitInstruction(asm, "ADD", operand("rd", rd), operand("rs1", rs1), operand("rs2", rs2));
  }

  private static void sub(ProgramAssembler asm, int rd, int rs1, int rs2) {
    emitInstruction(asm, "SUB", operand("rd", rd), operand("rs1", rs1), operand("rs2", rs2));
  }

  private static void mul(ProgramAssembler asm, int rd, int rs1, int rs2) {
    emitInstruction(asm, "MUL", operand("rd", rd), operand("rs1", rs1), operand("rs2", rs2));
  }

  private static void beq(ProgramAssembler asm, int rs1, int rs2, String label) {
    asm.text().emitLabelRelative("BEQ", label, "immS",
        operand("rs1", rs1), operand("rs2", rs2));
  }

  private static void bne(ProgramAssembler asm, int rs1, int rs2, String label) {
    asm.text().emitLabelRelative("BNE", label, "immS",
        operand("rs1", rs1), operand("rs2", rs2));
  }

  private static void jal(ProgramAssembler asm, int rd, String label) {
    asm.text().emitLabelRelative("JAL", label, "immS", operand("rd", rd));
  }

  private static void vld(ProgramAssembler asm, int vd, int base) {
    emitInstruction(asm, "VLD", operand("vr", vd), operand("base", base));
  }

  private static void vst(ProgramAssembler asm, int vs, int base) {
    emitInstruction(asm, "VST", operand("vr", vs), operand("base", base));
  }

  private static void rld(ProgramAssembler asm, int rd, int base) {
    emitInstruction(asm, "RLD", operand("vr", rd), operand("base", base));
  }

  private static void rst(ProgramAssembler asm, int rs, int base) {
    emitInstruction(asm, "RST", operand("vr", rs), operand("base", base));
  }

  private static void mld(ProgramAssembler asm, int md, int base) {
    emitInstruction(asm, "MLD", operand("vr", md), operand("base", base));
  }

  private static void mst(ProgramAssembler asm, int ms, int base) {
    emitInstruction(asm, "MST", operand("vr", ms), operand("base", base));
  }

  private static void hld(ProgramAssembler asm, int hd, int base) {
    emitInstruction(asm, "HLD", operand("vr", hd), operand("base", base));
  }

  private static void hst(ProgramAssembler asm, int hs, int base) {
    emitInstruction(asm, "HST", operand("vr", hs), operand("base", base));
  }

  private static void pmovx(ProgramAssembler asm, int pd, int rs1) {
    emitInstruction(asm, "PMOVX", operand("pd", pd), operand("rs1", rs1));
  }

  private static void ptrue(ProgramAssembler asm, int pd) {
    emitInstruction(asm, "PTRUE", operand("pd", pd), operand("ps2", 0), operand("src1", 0));
  }

  private static void pand(ProgramAssembler asm, int pd, int ps1, int ps2) {
    emitInstruction(asm, "PAND", operand("pd", pd), operand("ps1", ps1), operand("ps2", ps2));
  }

  private static void pfirst(ProgramAssembler asm, int pd, int pm, int ps1) {
    emitInstruction(asm, "PFIRST", operand("pd", pd), operand("ps2", pm), operand("ps1", ps1));
  }

  private static void pnext(ProgramAssembler asm, int pd, int pm, int ps1) {
    emitInstruction(asm, "PNEXT", operand("pd", pd), operand("ps2", pm), operand("ps1", ps1));
  }

  private static void pwhilelt(ProgramAssembler asm, int pd, int xs1, int xs2) {
    emitInstruction(asm, "PWHILELT",
        operand("pd", pd), operand("xs1", xs1), operand("xs2", xs2));
  }

  private static void cntp(ProgramAssembler asm, int rd, int ps1) {
    emitInstruction(asm, "CNTP", operand("rd", rd), operand("ps1", ps1), operand("rs1", 0));
  }

  private static void incp(ProgramAssembler asm, int rd, int rs1, int ps1) {
    emitInstruction(asm, "INCP", operand("rd", rd), operand("rs1", rs1), operand("ps1", ps1));
  }

  private static void pld(ProgramAssembler asm, int pt, int base) {
    emitInstruction(asm, "PLD", operand("pt", pt), operand("base", base), operand("mode", 0));
  }

  private static void pst(ProgramAssembler asm, int pt, int base) {
    emitInstruction(asm, "PST", operand("pt", pt), operand("base", base), operand("mode", 0));
  }

  private static void vld1PredZ(ProgramAssembler asm, int vr, int pm, int base) {
    emitInstruction(asm, "VLD1_PRED_Z", operand("vr", vr), operand("pm", pm),
        operand("base", base));
  }

  private static void vst1Pred(ProgramAssembler asm, int vr, int pm, int base) {
    emitInstruction(asm, "VST1_PRED", operand("vr", vr), operand("pm", pm), operand("base", base));
  }

  private static void vld1PredZRr(ProgramAssembler asm, int vr, int pm, int xoff, int base) {
    emitInstruction(asm, "VLD1_PRED_Z_RR",
        operand("vr", vr), operand("pm", pm), operand("xoff", xoff), operand("base", base));
  }

  private static void vst1PredRr(ProgramAssembler asm, int vr, int pm, int xoff, int base) {
    emitInstruction(asm, "VST1_PRED_RR",
        operand("vr", vr), operand("pm", pm), operand("xoff", xoff), operand("base", base));
  }

  private static void vld2PredZ(ProgramAssembler asm, int vr, int pm, int base) {
    emitInstruction(asm, "VLD2_PRED_Z", operand("vr", vr), operand("pm", pm),
        operand("base", base));
  }

  private static void vst2Pred(ProgramAssembler asm, int vr, int pm, int base) {
    emitInstruction(asm, "VST2_PRED", operand("vr", vr), operand("pm", pm), operand("base", base));
  }

  private static void vld3PredZ(ProgramAssembler asm, int vr, int pm, int base) {
    emitInstruction(asm, "VLD3_PRED_Z", operand("vr", vr), operand("pm", pm),
        operand("base", base));
  }

  private static void vst3Pred(ProgramAssembler asm, int vr, int pm, int base) {
    emitInstruction(asm, "VST3_PRED", operand("vr", vr), operand("pm", pm), operand("base", base));
  }

  private static void vld4PredZ(ProgramAssembler asm, int vr, int pm, int base) {
    emitInstruction(asm, "VLD4_PRED_Z", operand("vr", vr), operand("pm", pm),
        operand("base", base));
  }

  private static void vst4Pred(ProgramAssembler asm, int vr, int pm, int base) {
    emitInstruction(asm, "VST4_PRED", operand("vr", vr), operand("pm", pm), operand("base", base));
  }

  private static void vgatherPredZ(ProgramAssembler asm, int vr, int pm, int voff, int base) {
    emitInstruction(asm, "VGATHER_PRED_Z",
        operand("vr", vr), operand("pm", pm), operand("voff", voff), operand("base", base));
  }

  private static void vscatterPred(ProgramAssembler asm, int vr, int pm, int voff, int base) {
    emitInstruction(asm, "VSCATTER_PRED",
        operand("vr", vr), operand("pm", pm), operand("voff", voff), operand("base", base));
  }

  private static void maddDo(ProgramAssembler asm, int md, int ms1, int ms2) {
    emitInstruction(asm, "MADD_DO", operand("md", md), operand("ms1", ms1), operand("ms2", ms2));
  }

  private static void maddTensor(ProgramAssembler asm, int md, int ms1, int ms2) {
    emitInstruction(asm, "MADD_TENSOR",
        operand("md", md), operand("ms1", ms1), operand("ms2", ms2));
  }

  private static void maddRowBcast(ProgramAssembler asm, int md, int ms1, int rr1) {
    emitInstruction(asm, "MADD_ROW_BCAST",
        operand("md", md), operand("ms1", ms1), operand("rr1", rr1));
  }

  private static void mreduceCols(ProgramAssembler asm, int rd, int ms1) {
    emitInstruction(asm, "MREDUCE_COLS", operand("rd", rd), operand("ms1", ms1));
  }

  private static void haddTensor(ProgramAssembler asm, int hd, int hs1, int hs2) {
    emitInstruction(asm, "HADD_TENSOR",
        operand("hd", hd), operand("hs1", hs1), operand("hs2", hs2));
  }

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      throw new IllegalArgumentException("Usage: VectorBench64Benchmarks <output-dir>");
    }
    generate(Path.of(args[0]), null);
  }

  /**
   * Generates the benchmark corpus in {@code outputDir}.
   *
   * <p>Passing a disassembler additionally emits assembly listings next to the ELFs, which is
   * useful when debugging code generation or inspecting benchmark binaries.
   */
  public static GeneratedBenchmarks generate(Path outputDir, @Nullable Disassembler disassembler)
      throws IOException {
    return generate(outputDir, disassembler, null);
  }

  /**
   * Generates the benchmark corpus with an optional per-call iteration-scale override.
   *
   * <p>This is used by fast smoke coverage so the full corpus can be exercised without mutating the
   * published benchmark sizing.
   */
  public static GeneratedBenchmarks generate(Path outputDir,
                                             @Nullable Disassembler disassembler,
                                             @Nullable Double iterationScaleOverride)
      throws IOException {
    DISASSEMBLER.set(disassembler);
    if (iterationScaleOverride != null) {
      if (!(iterationScaleOverride > 0.0d)) {
        throw new IllegalArgumentException(
            "iterationScaleOverride must be > 0, got " + iterationScaleOverride);
      }
      ITERATION_SCALE_OVERRIDE.set(iterationScaleOverride);
    }
    try {
      return generateImpl(outputDir);
    } finally {
      DISASSEMBLER.remove();
      ITERATION_SCALE_OVERRIDE.remove();
    }
  }

  private static GeneratedBenchmarks generateImpl(Path outputDir) throws IOException {
    Files.createDirectories(outputDir);
    var artifacts = new ArrayList<BenchmarkArtifact>();

    artifacts.add(buildVaddDo(outputDir));
    artifacts.add(buildVaddTensor(outputDir));
    artifacts.add(buildVaddVx(outputDir));
    artifacts.add(buildVaddVxInc(outputDir));
    artifacts.add(buildVmov(outputDir));
    artifacts.add(buildVbcast(outputDir));
    artifacts.add(buildVaddVl(outputDir, "vector-vadd-vl-08", 8));
    artifacts.add(buildVaddVl(outputDir, "vector-vadd-vl-17", 17));
    artifacts.add(buildVaddPred(outputDir));
    artifacts.add(buildVaddCond(outputDir));
    artifacts.add(buildVsel(outputDir));
    artifacts.add(buildVcmpltPvv(outputDir));
    artifacts.add(buildVredSum(outputDir));
    artifacts.add(buildVmac(outputDir));
    artifacts.add(buildVaddXinc(outputDir));
    artifacts.add(buildPtrue(outputDir));
    artifacts.add(buildPwhilelt(outputDir));
    artifacts.add(buildPand(outputDir));
    artifacts.add(buildPfirst(outputDir));
    artifacts.add(buildPnext(outputDir));
    artifacts.add(buildCntp(outputDir));
    artifacts.add(buildIncp(outputDir));
    artifacts.add(buildAddvl(outputDir));
    artifacts.add(buildPld(outputDir));
    artifacts.add(buildPst(outputDir));
    artifacts.add(buildVzipLo(outputDir));
    artifacts.add(buildVext(outputDir));
    artifacts.add(buildVuzpLo(outputDir));
    artifacts.add(buildVwidenU8h(outputDir));
    artifacts.add(buildVld1PredZ(outputDir));
    artifacts.add(buildVst1Pred(outputDir));
    artifacts.add(buildVld1PredZRr(outputDir));
    artifacts.add(buildVst1PredRr(outputDir));
    artifacts.add(buildVld2PredZ(outputDir));
    artifacts.add(buildVst2Pred(outputDir));
    artifacts.add(buildVld3PredZ(outputDir));
    artifacts.add(buildVst3Pred(outputDir));
    artifacts.add(buildVld4PredZ(outputDir));
    artifacts.add(buildVst4Pred(outputDir));
    artifacts.add(buildVgatherPredZ(outputDir));
    artifacts.add(buildVscatterPred(outputDir));
    artifacts.add(buildMaddDo(outputDir));
    artifacts.add(buildMaddTensor(outputDir));
    artifacts.add(buildMaddRowBcast(outputDir));
    artifacts.add(buildMreduceCols(outputDir));
    artifacts.add(buildHaddTensor(outputDir));

    var manifest = outputDir.resolve("manifest.csv");
    writeManifest(manifest, artifacts);

    return new GeneratedBenchmarks(List.copyOf(artifacts), manifest);
  }

  private static BenchmarkArtifact buildVaddDo(Path outputDir) throws IOException {
    return buildVectorBinaryChainCase(outputDir, "vector-vadd-do", "vector-do",
        PROFILE_VECTOR_VADD_DO,
        (asm, vd, vs1, vs2) -> emitInstruction(asm, "VADD_DO_VV",
            operand("vd", vd), operand("vs1", vs1), operand("vs2", vs2)),
        (destOld, src1, src2, lane) -> src1 + src2);
  }

  private static BenchmarkArtifact buildVaddTensor(Path outputDir) throws IOException {
    return buildVectorBinaryChainCase(outputDir, "vector-vadd-tensor", "vector-tensor",
        PROFILE_VECTOR_VADD_TENSOR,
        (asm, vd, vs1, vs2) -> emitInstruction(asm, "VADD_TENSOR_VV",
            operand("vd", vd), operand("vs1", vs1), operand("vs2", vs2)),
        (destOld, src1, src2, lane) -> src1 + src2);
  }

  private static BenchmarkArtifact buildVaddVx(Path outputDir) throws IOException {
    var profile = PROFILE_VECTOR_VADD_VX;
    var regs = initialVectorRegs();
    var finalRegs = applyVectorScalarChain(
        regs,
        profile.iterations() * profile.chainRounds(),
        VECTOR_SCALARS,
        (destOld, src, scalar, lane) -> src + scalar);
    byte[] resultBytes = intsToBytes(finalRegs[3]);

    var asm = newProgram();
    emitVectorChainSetup(asm, regs);
    loadScalarRegs(asm, VECTOR_SCALARS);
    long resultAddr = reserveData(asm, "result", resultBytes.length, 16);
    long expectedAddr = addData(asm,
        "expected_checksum",
        longToBytes(checksum(resultBytes)),
        8);
    emitLoop(asm, profile.iterations(), () ->
        emitVectorScalarChain(asm, profile.chainRounds(),
            (innerAsm, vd, vs, scalarOp) -> emitInstruction(innerAsm, "VADD_VX",
                operand("vd", vd), operand("vs2", vs), operand("xs1", scalarOp))));
    storeSingleVectorRegAndExit(asm, resultAddr, expectedAddr, 3);
    var elfPath = outputDir.resolve("vector-vadd-vx.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("vector-vadd-vx", "vector-scalar", profile.iterations(),
        profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildVaddVxInc(Path outputDir) throws IOException {
    var profile = PROFILE_VECTOR_VADD_VX_INC;
    var regs = initialVectorRegs();
    var computedScalars = Arrays.stream(VECTOR_SCALARS).map(x -> x + 1).toArray();
    var finalRegs = applyVectorScalarChain(
        regs,
        profile.iterations() * profile.chainRounds(),
        computedScalars,
        (destOld, src, scalar, lane) -> src + scalar);
    byte[] resultBytes = intsToBytes(finalRegs[3]);

    var asm = newProgram();
    emitVectorChainSetup(asm, regs);
    loadScalarRegs(asm, VECTOR_SCALARS);
    long resultAddr = reserveData(asm, "result", resultBytes.length, 16);
    long expectedAddr = addData(asm,
        "expected_checksum",
        longToBytes(checksum(resultBytes)),
        8);
    emitLoop(asm, profile.iterations(), () ->
        emitVectorScalarChain(asm, profile.chainRounds(),
            (innerAsm, vd, vs, scalarOp) -> emitInstruction(innerAsm, "VADD_VX_INC",
                operand("vd", vd), operand("vs2", vs), operand("xs1", scalarOp))));
    storeSingleVectorRegAndExit(asm, resultAddr, expectedAddr, 3);
    var elfPath = outputDir.resolve("vector-vadd-vx-inc.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("vector-vadd-vx-inc", "vector-scalar-computed",
        profile.iterations(), profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildVmov(Path outputDir) throws IOException {
    var profile = PROFILE_VECTOR_VMOV;
    var regs = initialVectorRegs();
    var finalRegs = applyVectorUnaryChain(
        regs,
        profile.iterations() * profile.chainRounds(),
        (destOld, src, lane) -> src);
    byte[] resultBytes = intsToBytes(finalRegs[3]);

    var asm = newProgram();
    emitVectorChainSetup(asm, regs);
    long resultAddr = reserveData(asm, "result", resultBytes.length, 16);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(),
        () -> emitVectorUnaryChain(asm, profile.chainRounds(),
            (innerAsm, vd, vs1) -> emitInstruction(innerAsm, "VMOV_VV",
                operand("vd", vd), operand("vs1", vs1), operand("vs2", 0))));
    storeSingleVectorRegAndExit(asm, resultAddr, expectedAddr, 3);
    var elfPath = outputDir.resolve("vector-vmov.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("vector-vmov", "vector-move", profile.iterations(),
        profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildVbcast(Path outputDir) throws IOException {
    var profile = PROFILE_VECTOR_VBCAST;
    int[][] finalRegs = new int[4][32];
    for (int reg = 0; reg < finalRegs.length; reg++) {
      for (int lane = 0; lane < finalRegs[reg].length; lane++) {
        finalRegs[reg][lane] = VECTOR_SCALARS[reg];
      }
    }
    byte[] resultBytes = concat(
        intsToBytes(finalRegs[0]),
        intsToBytes(finalRegs[1]),
        intsToBytes(finalRegs[2]),
        intsToBytes(finalRegs[3]));

    var asm = newProgram();
    loadScalarRegs(asm, VECTOR_SCALARS);
    long resultAddr = reserveData(asm, "result", resultBytes.length, 16);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(),
        () -> emitVectorScalarChain(asm, profile.chainRounds(),
            (innerAsm, vd, ignoredVs, scalarOp) -> emitInstruction(innerAsm, "VBCAST_X",
                operand("vd", vd), operand("xs1", scalarOp), operand("vs2", 0))));
    storeVectorRegsAndExit(asm, resultAddr, expectedAddr, 0, 1, 2, 3);
    var elfPath = outputDir.resolve("vector-vbcast.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("vector-vbcast", "vector-broadcast", profile.iterations(),
        profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildVaddVl(Path outputDir, String id, int activeLanes)
      throws IOException {
    var profile = switch (activeLanes) {
      case 8 -> PROFILE_VECTOR_VADD_VL_08;
      case 17 -> PROFILE_VECTOR_VADD_VL_17;
      default -> throw new IllegalArgumentException("unsupported vector-vadd-vl variant: " + id);
    };
    var vlScalars = new int[] {
        activeLanes,
        Math.max(1, Math.min(31, activeLanes + 5)),
        Math.max(1, Math.min(31, activeLanes * 2)),
        Math.max(1, Math.min(31, activeLanes - 3))
    };
    var regs = initialVectorRegs();
    var finalRegs = applyVectorScalarBinaryChain(
        regs,
        profile.iterations() * profile.chainRounds(),
        vlScalars,
        (destOld, src1, src2, scalar, lane) -> lane < scalar ? src1 + src2 : 0);
    byte[] resultBytes = intsToBytes(finalRegs[3]);

    var asm = newProgram();
    emitVectorChainSetup(asm, regs);
    loadScalarRegs(asm, vlScalars);
    long resultAddr = reserveData(asm, "result", resultBytes.length, 16);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(),
        () -> emitVectorScalarBinaryChain(asm, profile.chainRounds(),
            (innerAsm, vd, vs1, vs2, scalarOp) -> emitInstruction(innerAsm, "VADD_VL",
                operand("vd", vd), operand("vs1", vs1), operand("vs2", vs2),
                operand("xc", scalarOp))));
    storeSingleVectorRegAndExit(asm, resultAddr, expectedAddr, 3);
    var elfPath = outputDir.resolve(id + ".elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact(id, "vector-variable-vl", profile.iterations(),
        profile.bodyInstructions(4), activeLanes, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildVaddPred(Path outputDir) throws IOException {
    var profile = PROFILE_VECTOR_VADD_PRED;
    var regs = initialVectorRegs();
    var finalRegs = applyVectorPredChain(
        regs,
        profile.iterations() * profile.chainRounds(),
        VECTOR_MASKS);
    byte[] resultBytes = intsToBytes(finalRegs[3]);

    var asm = newProgram();
    emitVectorChainSetup(asm, regs);
    loadMaskRegs(asm, VECTOR_MASKS);
    long resultAddr = reserveData(asm, "result", resultBytes.length, 16);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(),
        () -> emitVectorPredChain(asm, profile.chainRounds(),
            (innerAsm, vd, pm, vs1, vs2) -> emitInstruction(innerAsm, "VADD_PRED",
                operand("vd", vd), operand("pm", pm), operand("vs1", vs1),
                operand("vs2", vs2))));
    storeSingleVectorRegAndExit(asm, resultAddr, expectedAddr, 3);
    var elfPath = outputDir.resolve("vector-vadd-pred.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("vector-vadd-pred", "vector-predicate-preserve",
        profile.iterations(), profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildVaddCond(Path outputDir) throws IOException {
    return buildVectorBinaryChainCase(outputDir, "vector-vadd-cond", "vector-lane-condition",
        PROFILE_VECTOR_VADD_COND,
        (asm, vd, vs1, vs2) -> emitInstruction(asm, "VADD_COND",
            operand("vd", vd), operand("vs1", vs1), operand("vs2", vs2)),
        (destOld, src1, src2, lane) -> (src1 & 1) != 0 ? src1 + src2 : src1);
  }

  private static BenchmarkArtifact buildVredSum(Path outputDir) throws IOException {
    return buildReductionChainCase(outputDir, "vector-vredsum", "vector-fold-sum",
        PROFILE_VECTOR_VREDSUM);
  }

  private static BenchmarkArtifact buildVmac(Path outputDir) throws IOException {
    var profile = PROFILE_VECTOR_VMAC_VVV;
    var regs = initialVectorRegs();
    var finalRegs = applyVectorTernaryChain(
        regs,
        profile.iterations() * profile.chainRounds(),
        (destOld, src1, src2, src3, lane) -> src1 * src2 + src3);
    byte[] resultBytes = intsToBytes(finalRegs[3]);

    var asm = newProgram();
    emitVectorChainSetup(asm, regs);
    long resultAddr = reserveData(asm, "result", resultBytes.length, 16);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(),
        () -> emitVectorTernaryChain(asm, profile.chainRounds(),
            (innerAsm, vd, vs1, vs2, vs3) -> emitInstruction(innerAsm, "VMAC_VVV",
                operand("vd", vd), operand("vs1", vs1), operand("vs2", vs2),
                operand("vc", vs3))));
    storeSingleVectorRegAndExit(asm, resultAddr, expectedAddr, 3);
    var elfPath = outputDir.resolve("vector-vmac-vvv.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("vector-vmac-vvv", "vector-composite-expression",
        profile.iterations(), profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildVaddXinc(Path outputDir) throws IOException {
    var profile = PROFILE_VECTOR_VADD_XINC;
    var regs = initialVectorRegs();
    var finalRegs = applyVectorBinaryChain(
        regs,
        profile.iterations() * profile.chainRounds(),
        (destOld, src1, src2, lane) -> src1 + src2);
    long scalarResult = (long) profile.iterations() * profile.bodyInstructions(4);
    byte[] resultBytes = concat(intsToBytes(finalRegs[3]), longToBytes(scalarResult));

    var asm = newProgram();
    emitVectorChainSetup(asm, regs);
    loadImm(asm, REG_SCALAR0, 0);
    long resultAddr = reserveData(asm, "result", resultBytes.length, 16);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(),
        () -> emitVectorBinaryChain(asm, profile.chainRounds(),
            (innerAsm, vd, vs1, vs2) -> emitInstruction(innerAsm, "VADD_XINC",
                operand("vd", vd), operand("vs1", vs1), operand("vs2", vs2),
                operand("rd", REG_SCALAR0))));
    loadAddress(asm, REG_PTR0, resultAddr);
    vst(asm, 3, REG_PTR0);
    loadAddress(asm, REG_PTR1, resultAddr + VEC_BYTES);
    store64(asm, REG_SCALAR0, REG_PTR1, 0);
    emitChecksumAndExit(asm, resultAddr, resultBytes.length, expectedAddr);
    var elfPath = outputDir.resolve("vector-vadd-xinc.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("vector-vadd-xinc", "vector-scalar-composite",
        profile.iterations(), profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildVsel(Path outputDir) throws IOException {
    var profile = PROFILE_VECTOR_VSEL;
    var regs = initialVectorRegs();
    var finalRegs = applyVectorSelectChain(
        regs,
        profile.iterations() * profile.chainRounds(),
        VECTOR_MASKS);
    byte[] resultBytes = intsToBytes(finalRegs[3]);

    var asm = newProgram();
    emitVectorChainSetup(asm, regs);
    loadMaskRegs(asm, VECTOR_MASKS);
    long resultAddr = reserveData(asm, "result", resultBytes.length, 16);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(),
        () -> emitVectorPredChain(asm, profile.chainRounds(),
            (innerAsm, vd, pm, vs1, vs2) -> emitInstruction(innerAsm, "VSEL",
                operand("vd", vd), operand("pm", pm), operand("vs1", vs1),
                operand("vs2", vs2))));
    storeSingleVectorRegAndExit(asm, resultAddr, expectedAddr, 3);
    var elfPath = outputDir.resolve("vector-vsel.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("vector-vsel", "vector-predicate-select",
        profile.iterations(), profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildVcmpltPvv(Path outputDir) throws IOException {
    var profile = PROFILE_PREDICATE_VCMPLT_PVV;
    var regs = initialEightVectorRegs(extraVectorRegs());
    byte[] resultBytes = intsToBytes(new int[] {
        compareMaskLt(regs[0], regs[4]),
        compareMaskLt(regs[1], regs[5]),
        compareMaskLt(regs[2], regs[6]),
        compareMaskLt(regs[3], regs[7])
    });

    var asm = newProgram();
    emitVectorRegsSetup(asm, regs, "cmp_init_");
    long resultAddr = reserveData(asm, "result", resultBytes.length, 8);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(), () -> {
      for (int round = 0; round < profile.chainRounds(); round++) {
        emitInstruction(asm, "VCMPLT_PVV", operand("pd", 0), operand("vs1", 0), operand("vs2", 4));
        emitInstruction(asm, "VCMPLT_PVV", operand("pd", 1), operand("vs1", 1), operand("vs2", 5));
        emitInstruction(asm, "VCMPLT_PVV", operand("pd", 2), operand("vs1", 2), operand("vs2", 6));
        emitInstruction(asm, "VCMPLT_PVV", operand("pd", 3), operand("vs1", 3), operand("vs2", 7));
      }
    });
    storePredicateRegsAndExit(asm, resultAddr, expectedAddr, 0, 1, 2, 3);
    var elfPath = outputDir.resolve("predicate-vcmplt-pvv.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("predicate-vcmplt-pvv", "predicate-vector-compare",
        profile.iterations(), profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildPtrue(Path outputDir) throws IOException {
    var profile = PROFILE_PREDICATE_PTRUE;
    long fullCount = 32;
    byte[] resultBytes = concat(
        longToBytes(fullCount),
        longToBytes(fullCount),
        longToBytes(fullCount),
        longToBytes(fullCount));

    var asm = newProgram();
    long resultAddr = reserveData(asm, "result", resultBytes.length, 8);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(), () -> {
      for (int round = 0; round < profile.chainRounds(); round++) {
        ptrue(asm, 0);
        ptrue(asm, 1);
        ptrue(asm, 2);
        ptrue(asm, 3);
      }
    });
    cntp(asm, REG_SCALAR0, 0);
    cntp(asm, REG_SCALAR1, 1);
    cntp(asm, REG_SCALAR2, 2);
    cntp(asm, REG_SCALAR3, 3);
    storeScalarRegsAndExit(asm, resultAddr, expectedAddr,
        REG_SCALAR0, REG_SCALAR1, REG_SCALAR2, REG_SCALAR3);
    var elfPath = outputDir.resolve("predicate-ptrue.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("predicate-ptrue", "predicate-constant",
        profile.iterations(), profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildPwhilelt(Path outputDir) throws IOException {
    var profile = PROFILE_PREDICATE_PWHILELT;
    long[] counts = {32, 17, 6, 1};
    byte[] resultBytes = concat(
        longToBytes(counts[0]),
        longToBytes(counts[1]),
        longToBytes(counts[2]),
        longToBytes(counts[3]));

    var asm = newProgram();
    loadImm(asm, REG_SCALAR0, 0);
    loadImm(asm, REG_SCALAR1, 7);
    loadImm(asm, REG_SCALAR2, 13);
    loadImm(asm, REG_SCALAR3, 21);
    loadImm(asm, REG_SCALAR4, 32);
    loadImm(asm, REG_SCALAR5, 24);
    loadImm(asm, REG_SCALAR6, 19);
    loadImm(asm, REG_SCALAR7, 22);
    long resultAddr = reserveData(asm, "result", resultBytes.length, 8);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(), () -> {
      for (int round = 0; round < profile.chainRounds(); round++) {
        pwhilelt(asm, 0, REG_SCALAR0, REG_SCALAR4);
        pwhilelt(asm, 1, REG_SCALAR1, REG_SCALAR5);
        pwhilelt(asm, 2, REG_SCALAR2, REG_SCALAR6);
        pwhilelt(asm, 3, REG_SCALAR3, REG_SCALAR7);
      }
    });
    cntp(asm, REG_SCALAR0, 0);
    cntp(asm, REG_SCALAR1, 1);
    cntp(asm, REG_SCALAR2, 2);
    cntp(asm, REG_SCALAR3, 3);
    storeScalarRegsAndExit(asm, resultAddr, expectedAddr,
        REG_SCALAR0, REG_SCALAR1, REG_SCALAR2, REG_SCALAR3);
    var elfPath = outputDir.resolve("predicate-pwhilelt.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("predicate-pwhilelt", "predicate-generate-while",
        profile.iterations(), profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildPand(Path outputDir) throws IOException {
    var profile = PROFILE_PREDICATE_PAND;
    int[] masks = VECTOR_MASKS.clone();
    int[] finalMasks =
        applyPredicateBinaryChain(masks, profile.iterations() * profile.chainRounds());
    byte[] resultBytes = concat(
        longToBytes(maskCount(finalMasks[0])),
        longToBytes(maskCount(finalMasks[1])),
        longToBytes(maskCount(finalMasks[2])),
        longToBytes(maskCount(finalMasks[3])));

    var asm = newProgram();
    loadMaskRegs(asm, masks);
    long resultAddr = reserveData(asm, "result", resultBytes.length, 8);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(), () -> {
      for (int round = 0; round < profile.chainRounds(); round++) {
        pand(asm, 0, 1, 2);
        pand(asm, 1, 0, 3);
        pand(asm, 2, 1, 0);
        pand(asm, 3, 2, 1);
      }
    });
    cntp(asm, REG_SCALAR0, 0);
    cntp(asm, REG_SCALAR1, 1);
    cntp(asm, REG_SCALAR2, 2);
    cntp(asm, REG_SCALAR3, 3);
    storeScalarRegsAndExit(asm, resultAddr, expectedAddr,
        REG_SCALAR0, REG_SCALAR1, REG_SCALAR2, REG_SCALAR3);
    var elfPath = outputDir.resolve("predicate-pand.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("predicate-pand", "predicate-logic",
        profile.iterations(), profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildPfirst(Path outputDir) throws IOException {
    var profile = PROFILE_PREDICATE_PFIRST;
    int[] masks = VECTOR_MASKS.clone();
    int[] finalMasks =
        applyPredicateFirstChain(masks, profile.iterations() * profile.chainRounds());
    byte[] resultBytes = intsToBytes(finalMasks);

    var asm = newProgram();
    loadMaskRegs(asm, masks);
    long resultAddr = reserveData(asm, "result", resultBytes.length, 8);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(), () -> {
      for (int round = 0; round < profile.chainRounds(); round++) {
        pfirst(asm, 0, 1, 0);
        pfirst(asm, 1, 2, 1);
        pfirst(asm, 2, 3, 2);
        pfirst(asm, 3, 0, 3);
      }
    });
    storePredicateRegsAndExit(asm, resultAddr, expectedAddr, 0, 1, 2, 3);
    var elfPath = outputDir.resolve("predicate-pfirst.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("predicate-pfirst", "predicate-first",
        profile.iterations(), profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildPnext(Path outputDir) throws IOException {
    var profile = PROFILE_PREDICATE_PNEXT;
    int[] masks = {
        0x0000FFFF,
        0x00FF00FF,
        0x0F0F0F0F,
        0x33333333
    };
    int[] finalMasks = applyPredicateNextChain(masks, profile.iterations() * profile.chainRounds());
    byte[] resultBytes = intsToBytes(finalMasks);

    var asm = newProgram();
    loadMaskRegs(asm, masks);
    long resultAddr = reserveData(asm, "result", resultBytes.length, 8);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(), () -> {
      for (int round = 0; round < profile.chainRounds(); round++) {
        pnext(asm, 0, 1, 0);
        pnext(asm, 1, 2, 1);
        pnext(asm, 2, 3, 2);
        pnext(asm, 3, 0, 3);
      }
    });
    storePredicateRegsAndExit(asm, resultAddr, expectedAddr, 0, 1, 2, 3);
    var elfPath = outputDir.resolve("predicate-pnext.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("predicate-pnext", "predicate-next",
        profile.iterations(), profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildCntp(Path outputDir) throws IOException {
    var profile = PROFILE_PREDICATE_CNTP;
    long[] counts = {
        maskCount(VECTOR_MASKS[0]),
        maskCount(VECTOR_MASKS[1]),
        maskCount(VECTOR_MASKS[2]),
        maskCount(VECTOR_MASKS[3])
    };
    byte[] resultBytes = concat(
        longToBytes(counts[0]),
        longToBytes(counts[1]),
        longToBytes(counts[2]),
        longToBytes(counts[3]));

    var asm = newProgram();
    loadMaskRegs(asm, VECTOR_MASKS);
    long resultAddr = reserveData(asm, "result", resultBytes.length, 8);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(), () -> {
      for (int round = 0; round < profile.chainRounds(); round++) {
        cntp(asm, REG_SCALAR0, 0);
        cntp(asm, REG_SCALAR1, 1);
        cntp(asm, REG_SCALAR2, 2);
        cntp(asm, REG_SCALAR3, 3);
      }
    });
    storeScalarRegsAndExit(asm, resultAddr, expectedAddr,
        REG_SCALAR0, REG_SCALAR1, REG_SCALAR2, REG_SCALAR3);
    var elfPath = outputDir.resolve("predicate-cntp.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("predicate-cntp", "predicate-count-to-scalar",
        profile.iterations(), profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildIncp(Path outputDir) throws IOException {
    var profile = PROFILE_SCALAR_INCP;
    long[] counts = {
        maskCount(VECTOR_MASKS[0]),
        maskCount(VECTOR_MASKS[1]),
        maskCount(VECTOR_MASKS[2]),
        maskCount(VECTOR_MASKS[3])
    };
    long bodyCount = (long) profile.iterations() * profile.bodyInstructions(4);
    byte[] resultBytes = concat(
        longToBytes(counts[0] * bodyCount / 4),
        longToBytes(counts[1] * bodyCount / 4),
        longToBytes(counts[2] * bodyCount / 4),
        longToBytes(counts[3] * bodyCount / 4));

    var asm = newProgram();
    loadMaskRegs(asm, VECTOR_MASKS);
    loadImm(asm, REG_SCALAR0, 0);
    loadImm(asm, REG_SCALAR1, 0);
    loadImm(asm, REG_SCALAR2, 0);
    loadImm(asm, REG_SCALAR3, 0);
    long resultAddr = reserveData(asm, "result", resultBytes.length, 8);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(), () -> {
      for (int round = 0; round < profile.chainRounds(); round++) {
        incp(asm, REG_SCALAR0, REG_SCALAR0, 0);
        incp(asm, REG_SCALAR1, REG_SCALAR1, 1);
        incp(asm, REG_SCALAR2, REG_SCALAR2, 2);
        incp(asm, REG_SCALAR3, REG_SCALAR3, 3);
      }
    });
    storeScalarRegsAndExit(asm, resultAddr, expectedAddr,
        REG_SCALAR0, REG_SCALAR1, REG_SCALAR2, REG_SCALAR3);
    var elfPath = outputDir.resolve("scalar-incp.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("scalar-incp", "scalar-predicate-increment",
        profile.iterations(), profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildAddvl(Path outputDir) throws IOException {
    var profile = PROFILE_SCALAR_ADDVL;
    long scale = VEC_BYTES;
    long steps = (long) profile.iterations() * profile.chainRounds();
    byte[] resultBytes = concat(
        longToBytes(steps * scale),
        longToBytes(steps * 2 * scale),
        longToBytes(-steps * scale),
        longToBytes(steps * 3 * scale));

    var asm = newProgram();
    loadImm(asm, REG_SCALAR0, 0);
    loadImm(asm, REG_SCALAR1, 0);
    loadImm(asm, REG_SCALAR2, 0);
    loadImm(asm, REG_SCALAR3, 0);
    long resultAddr = reserveData(asm, "result", resultBytes.length, 8);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(), () -> {
      for (int round = 0; round < profile.chainRounds(); round++) {
        addvl(asm, REG_SCALAR0, REG_SCALAR0, 1);
        addvl(asm, REG_SCALAR1, REG_SCALAR1, 2);
        addvl(asm, REG_SCALAR2, REG_SCALAR2, -1);
        addvl(asm, REG_SCALAR3, REG_SCALAR3, 3);
      }
    });
    storeScalarRegsAndExit(asm, resultAddr, expectedAddr,
        REG_SCALAR0, REG_SCALAR1, REG_SCALAR2, REG_SCALAR3);
    var elfPath = outputDir.resolve("scalar-addvl.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("scalar-addvl", "scalar-vector-length",
        profile.iterations(), profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildPld(Path outputDir) throws IOException {
    var profile = PROFILE_MEMORY_PLD;
    int[] masks = {
        0xA55A3CC5,
        0x5AA5CC33,
        0x0FF00FF0,
        0xC3C33C3C
    };
    byte[] resultBytes = intsToBytes(masks);

    var asm = newProgram();
    loadAddress(asm, REG_SCALAR0, addData(asm, "pld_mem_0", intsToBytes(new int[] {masks[0]}), 4));
    loadAddress(asm, REG_SCALAR1, addData(asm, "pld_mem_1", intsToBytes(new int[] {masks[1]}), 4));
    loadAddress(asm, REG_SCALAR2, addData(asm, "pld_mem_2", intsToBytes(new int[] {masks[2]}), 4));
    loadAddress(asm, REG_SCALAR3, addData(asm, "pld_mem_3", intsToBytes(new int[] {masks[3]}), 4));
    long resultAddr = reserveData(asm, "result", resultBytes.length, 8);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(), () -> {
      for (int round = 0; round < profile.chainRounds(); round++) {
        pld(asm, 0, REG_SCALAR0);
        pld(asm, 1, REG_SCALAR1);
        pld(asm, 2, REG_SCALAR2);
        pld(asm, 3, REG_SCALAR3);
      }
    });
    storePredicateRegsAndExit(asm, resultAddr, expectedAddr, 0, 1, 2, 3);
    var elfPath = outputDir.resolve("memory-pld.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("memory-pld", "memory-predicate-load",
        profile.iterations(), profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildPst(Path outputDir) throws IOException {
    var profile = PROFILE_MEMORY_PST;
    int[] masks = {
        0xA55A3CC5,
        0x5AA5CC33,
        0x0FF00FF0,
        0xC3C33C3C
    };
    byte[] resultBytes = intsToBytes(masks);

    var asm = newProgram();
    loadMaskRegs(asm, masks);
    long out0 = reserveData(asm, "pst_mem_0", 4, 4);
    long out1 = reserveData(asm, "pst_mem_1", 4, 4);
    long out2 = reserveData(asm, "pst_mem_2", 4, 4);
    long out3 = reserveData(asm, "pst_mem_3", 4, 4);
    loadAddress(asm, REG_SCALAR0, out0);
    loadAddress(asm, REG_SCALAR1, out1);
    loadAddress(asm, REG_SCALAR2, out2);
    loadAddress(asm, REG_SCALAR3, out3);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(), () -> {
      for (int round = 0; round < profile.chainRounds(); round++) {
        pst(asm, 0, REG_SCALAR0);
        pst(asm, 1, REG_SCALAR1);
        pst(asm, 2, REG_SCALAR2);
        pst(asm, 3, REG_SCALAR3);
      }
    });
    emitChecksumAndExit(asm, out0, resultBytes.length, expectedAddr);
    var elfPath = outputDir.resolve("memory-pst.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("memory-pst", "memory-predicate-store",
        profile.iterations(), profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildVzipLo(Path outputDir) throws IOException {
    var profile = PROFILE_VECTOR_VZIP_LO;
    var regs = initialVectorRegs();
    var finalRegs = applyVectorZipChain(regs, profile.iterations() * profile.chainRounds());
    byte[] resultBytes = intsToBytes(finalRegs[3]);

    var asm = newProgram();
    emitVectorChainSetup(asm, regs);
    long resultAddr = reserveData(asm, "result", resultBytes.length, 16);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(), () -> {
      for (int round = 0; round < profile.chainRounds(); round++) {
        emitInstruction(asm, "VZIP_LO", operand("vd", 0), operand("vs1", 1), operand("vs2", 2));
        emitInstruction(asm, "VZIP_LO", operand("vd", 1), operand("vs1", 0), operand("vs2", 3));
        emitInstruction(asm, "VZIP_LO", operand("vd", 2), operand("vs1", 1), operand("vs2", 0));
        emitInstruction(asm, "VZIP_LO", operand("vd", 3), operand("vs1", 2), operand("vs2", 1));
      }
    });
    storeSingleVectorRegAndExit(asm, resultAddr, expectedAddr, 3);
    var elfPath = outputDir.resolve("vector-vzip-lo.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("vector-vzip-lo", "vector-permute-interleave",
        profile.iterations(), profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildVext(Path outputDir) throws IOException {
    var profile = PROFILE_VECTOR_VEXT;
    var regs = initialVectorRegs();
    var finalRegs = applyVectorExtChain(regs, profile.iterations() * profile.chainRounds(),
        new int[] {1, 1, 1, 1});
    byte[] resultBytes = intsToBytes(finalRegs[3]);

    var asm = newProgram();
    emitVectorChainSetup(asm, regs);
    long resultAddr = reserveData(asm, "result", resultBytes.length, 16);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(), () -> {
      for (int round = 0; round < profile.chainRounds(); round++) {
        emitInstruction(asm, "VEXT",
            operand("vd", 0), operand("vs1", 1), operand("vs2", 2), operand("xc", REG_SCALAR0));
        emitInstruction(asm, "VEXT",
            operand("vd", 1), operand("vs1", 0), operand("vs2", 3), operand("xc", REG_SCALAR1));
        emitInstruction(asm, "VEXT",
            operand("vd", 2), operand("vs1", 1), operand("vs2", 0), operand("xc", REG_SCALAR2));
        emitInstruction(asm, "VEXT",
            operand("vd", 3), operand("vs1", 2), operand("vs2", 1), operand("xc", REG_SCALAR3));
      }
    });
    storeSingleVectorRegAndExit(asm, resultAddr, expectedAddr, 3);
    var elfPath = outputDir.resolve("vector-vext.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("vector-vext", "vector-permute-extract",
        profile.iterations(), profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildVuzpLo(Path outputDir) throws IOException {
    var profile = PROFILE_VECTOR_VUZP_LO;
    int[][] regs = initialVectorRegs();
    var finalRegs = applyVectorUzpChain(regs, profile.iterations() * profile.chainRounds());
    byte[] resultBytes = intsToBytes(finalRegs[3]);

    var asm = newProgram();
    emitVectorChainSetup(asm, regs);
    long resultAddr = reserveData(asm, "result", resultBytes.length, 16);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(), () -> {
      for (int round = 0; round < profile.chainRounds(); round++) {
        emitInstruction(asm, "VUZP_LO", operand("vd", 0), operand("vs1", 1), operand("vs2", 2));
        emitInstruction(asm, "VUZP_LO", operand("vd", 1), operand("vs1", 0), operand("vs2", 3));
        emitInstruction(asm, "VUZP_LO", operand("vd", 2), operand("vs1", 1), operand("vs2", 0));
        emitInstruction(asm, "VUZP_LO", operand("vd", 3), operand("vs1", 2), operand("vs2", 1));
      }
    });
    storeSingleVectorRegAndExit(asm, resultAddr, expectedAddr, 3);
    var elfPath = outputDir.resolve("vector-vuzp-lo.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("vector-vuzp-lo", "vector-permute-deinterleave",
        profile.iterations(), profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildVwidenU8h(Path outputDir) throws IOException {
    var profile = PROFILE_VECTOR_VZEXT8;
    var regs = initialVectorRegs();
    var finalRegs = applyVectorUnaryChain(
        regs,
        profile.iterations() * profile.chainRounds(),
        (destOld, src, lane) -> src & 0xff);
    byte[] resultBytes = intsToBytes(finalRegs[3]);

    var asm = newProgram();
    emitVectorChainSetup(asm, regs);
    long resultAddr = reserveData(asm, "result", resultBytes.length, 16);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(), () -> {
      for (int round = 0; round < profile.chainRounds(); round++) {
        emitInstruction(asm, "VZEXT8", operand("vd", 0), operand("vs1", 1), operand("vs2", 0));
        emitInstruction(asm, "VZEXT8", operand("vd", 1), operand("vs1", 2), operand("vs2", 0));
        emitInstruction(asm, "VZEXT8", operand("vd", 2), operand("vs1", 3), operand("vs2", 0));
        emitInstruction(asm, "VZEXT8", operand("vd", 3), operand("vs1", 0), operand("vs2", 0));
      }
    });
    storeSingleVectorRegAndExit(asm, resultAddr, expectedAddr, 3);
    var elfPath = outputDir.resolve("vector-vzext8.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("vector-vzext8", "vector-widening-zeroextend",
        profile.iterations(), profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildVld1PredZ(Path outputDir) throws IOException {
    var profile = PROFILE_MEMORY_VLD1_PRED_Z;
    int[][] inputs = initialVectorRegs();
    byte[] expected0 = intsToBytes(maskedVector(inputs[0], VECTOR_MASKS[0], true));
    byte[] expected1 = intsToBytes(maskedVector(inputs[1], VECTOR_MASKS[1], true));
    byte[] expected2 = intsToBytes(maskedVector(inputs[2], VECTOR_MASKS[2], true));
    byte[] expected3 = intsToBytes(maskedVector(inputs[3], VECTOR_MASKS[3], true));
    byte[] resultBytes = concat(expected0, expected1, expected2, expected3);

    var asm = newProgram();
    loadMaskRegs(asm, VECTOR_MASKS);
    loadAddress(asm, REG_SCALAR0, addData(asm, "ld1_mem_0", intsToBytes(inputs[0]), 16));
    loadAddress(asm, REG_SCALAR1, addData(asm, "ld1_mem_1", intsToBytes(inputs[1]), 16));
    loadAddress(asm, REG_SCALAR2, addData(asm, "ld1_mem_2", intsToBytes(inputs[2]), 16));
    loadAddress(asm, REG_SCALAR3, addData(asm, "ld1_mem_3", intsToBytes(inputs[3]), 16));
    long resultAddr = reserveData(asm, "result", resultBytes.length, 16);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(), () -> {
      for (int round = 0; round < profile.chainRounds(); round++) {
        vld1PredZ(asm, 0, 0, REG_SCALAR0);
        vld1PredZ(asm, 1, 1, REG_SCALAR1);
        vld1PredZ(asm, 2, 2, REG_SCALAR2);
        vld1PredZ(asm, 3, 3, REG_SCALAR3);
      }
    });
    storeVectorRegsAndExit(asm, resultAddr, expectedAddr, 0, 1, 2, 3);
    var elfPath = outputDir.resolve("memory-vld1-pred-z.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("memory-vld1-pred-z", "memory-predicated-load",
        profile.iterations(), profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildVst1Pred(Path outputDir) throws IOException {
    var profile = PROFILE_MEMORY_VST1_PRED;
    int[][] inputs = initialVectorRegs();
    byte[] block0 = intsToBytes(maskedVector(inputs[0], VECTOR_MASKS[0], false));
    byte[] block1 = intsToBytes(maskedVector(inputs[1], VECTOR_MASKS[1], false));
    byte[] block2 = intsToBytes(maskedVector(inputs[2], VECTOR_MASKS[2], false));
    byte[] block3 = intsToBytes(maskedVector(inputs[3], VECTOR_MASKS[3], false));
    byte[] resultBytes = concat(block0, block1, block2, block3);

    var asm = newProgram();
    emitVectorChainSetup(asm, inputs);
    loadMaskRegs(asm, VECTOR_MASKS);
    long out0 = reserveData(asm, "st1_mem_0", VEC_BYTES, 16);
    long out1 = reserveData(asm, "st1_mem_1", VEC_BYTES, 16);
    long out2 = reserveData(asm, "st1_mem_2", VEC_BYTES, 16);
    long out3 = reserveData(asm, "st1_mem_3", VEC_BYTES, 16);
    loadAddress(asm, REG_SCALAR0, out0);
    loadAddress(asm, REG_SCALAR1, out1);
    loadAddress(asm, REG_SCALAR2, out2);
    loadAddress(asm, REG_SCALAR3, out3);
    long resultAddr = out0;
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(), () -> {
      for (int round = 0; round < profile.chainRounds(); round++) {
        vst1Pred(asm, 0, 0, REG_SCALAR0);
        vst1Pred(asm, 1, 1, REG_SCALAR1);
        vst1Pred(asm, 2, 2, REG_SCALAR2);
        vst1Pred(asm, 3, 3, REG_SCALAR3);
      }
    });
    emitChecksumAndExit(asm, resultAddr, resultBytes.length, expectedAddr);
    var elfPath = outputDir.resolve("memory-vst1-pred.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("memory-vst1-pred", "memory-predicated-store",
        profile.iterations(), profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildVld1PredZRr(Path outputDir) throws IOException {
    var profile = PROFILE_MEMORY_VLD1_PRED_Z_RR;
    int[][] inputs = initialVectorRegs();
    byte[] expected0 = intsToBytes(maskedVector(inputs[0], VECTOR_MASKS[0], true));
    byte[] expected1 = intsToBytes(maskedVector(inputs[1], VECTOR_MASKS[1], true));
    byte[] expected2 = intsToBytes(maskedVector(inputs[2], VECTOR_MASKS[2], true));
    byte[] expected3 = intsToBytes(maskedVector(inputs[3], VECTOR_MASKS[3], true));
    byte[] resultBytes = concat(expected0, expected1, expected2, expected3);

    var asm = newProgram();
    loadMaskRegs(asm, VECTOR_MASKS);
    loadAddress(asm, REG_SCALAR0,
        addData(asm, "ld1_rr_mem_0", concat(new byte[16], intsToBytes(inputs[0])), 16));
    loadAddress(asm, REG_SCALAR1,
        addData(asm, "ld1_rr_mem_1", concat(new byte[16], intsToBytes(inputs[1])), 16));
    loadAddress(asm, REG_SCALAR2,
        addData(asm, "ld1_rr_mem_2", concat(new byte[16], intsToBytes(inputs[2])), 16));
    loadAddress(asm, REG_SCALAR3,
        addData(asm, "ld1_rr_mem_3", concat(new byte[16], intsToBytes(inputs[3])), 16));
    loadImm(asm, REG_SCALAR4, 16);
    loadImm(asm, REG_SCALAR5, 16);
    loadImm(asm, REG_SCALAR6, 16);
    loadImm(asm, REG_SCALAR7, 16);
    long resultAddr = reserveData(asm, "result", resultBytes.length, 16);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(), () -> {
      for (int round = 0; round < profile.chainRounds(); round++) {
        vld1PredZRr(asm, 0, 0, REG_SCALAR4, REG_SCALAR0);
        vld1PredZRr(asm, 1, 1, REG_SCALAR5, REG_SCALAR1);
        vld1PredZRr(asm, 2, 2, REG_SCALAR6, REG_SCALAR2);
        vld1PredZRr(asm, 3, 3, REG_SCALAR7, REG_SCALAR3);
      }
    });
    storeVectorRegsAndExit(asm, resultAddr, expectedAddr, 0, 1, 2, 3);
    var elfPath = outputDir.resolve("memory-vld1-pred-z-rr.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("memory-vld1-pred-z-rr", "memory-predicated-load-indexed",
        profile.iterations(), profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildVst1PredRr(Path outputDir) throws IOException {
    var profile = PROFILE_MEMORY_VST1_PRED_RR;
    int[][] inputs = initialVectorRegs();
    byte[] prefix = new byte[16];
    byte[] block0 = concat(prefix, intsToBytes(maskedVector(inputs[0], VECTOR_MASKS[0], false)));
    byte[] block1 = concat(prefix, intsToBytes(maskedVector(inputs[1], VECTOR_MASKS[1], false)));
    byte[] block2 = concat(prefix, intsToBytes(maskedVector(inputs[2], VECTOR_MASKS[2], false)));
    byte[] block3 = concat(prefix, intsToBytes(maskedVector(inputs[3], VECTOR_MASKS[3], false)));
    byte[] resultBytes = concat(block0, block1, block2, block3);

    var asm = newProgram();
    emitVectorChainSetup(asm, inputs);
    loadMaskRegs(asm, VECTOR_MASKS);
    long out0 = reserveData(asm, "st1_rr_mem_0", block0.length, 16);
    long out1 = reserveData(asm, "st1_rr_mem_1", block1.length, 16);
    long out2 = reserveData(asm, "st1_rr_mem_2", block2.length, 16);
    long out3 = reserveData(asm, "st1_rr_mem_3", block3.length, 16);
    loadAddress(asm, REG_SCALAR0, out0);
    loadAddress(asm, REG_SCALAR1, out1);
    loadAddress(asm, REG_SCALAR2, out2);
    loadAddress(asm, REG_SCALAR3, out3);
    loadImm(asm, REG_SCALAR4, 16);
    loadImm(asm, REG_SCALAR5, 16);
    loadImm(asm, REG_SCALAR6, 16);
    loadImm(asm, REG_SCALAR7, 16);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(), () -> {
      for (int round = 0; round < profile.chainRounds(); round++) {
        vst1PredRr(asm, 0, 0, REG_SCALAR4, REG_SCALAR0);
        vst1PredRr(asm, 1, 1, REG_SCALAR5, REG_SCALAR1);
        vst1PredRr(asm, 2, 2, REG_SCALAR6, REG_SCALAR2);
        vst1PredRr(asm, 3, 3, REG_SCALAR7, REG_SCALAR3);
      }
    });
    emitChecksumAndExit(asm, out0, resultBytes.length, expectedAddr);
    var elfPath = outputDir.resolve("memory-vst1-pred-rr.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("memory-vst1-pred-rr", "memory-predicated-store-indexed",
        profile.iterations(), profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildVld2PredZ(Path outputDir) throws IOException {
    var profile = PROFILE_MEMORY_VLD2_PRED_Z;
    int[][] pairInputs = initialVectorRegs();
    byte[] mem0 = interleavePairs(pairInputs[0], pairInputs[1]);
    byte[] mem1 = interleavePairs(pairInputs[2], pairInputs[3]);
    byte[] resultBytes = concat(
        intsToBytes(maskedVector(pairInputs[0], VECTOR_MASKS[0], true)),
        intsToBytes(maskedVector(pairInputs[1], VECTOR_MASKS[0], true)),
        intsToBytes(maskedVector(pairInputs[2], VECTOR_MASKS[1], true)),
        intsToBytes(maskedVector(pairInputs[3], VECTOR_MASKS[1], true)));

    var asm = newProgram();
    loadMaskRegs(asm, VECTOR_MASKS);
    loadAddress(asm, REG_SCALAR0, addData(asm, "ld2_mem_0", mem0, 16));
    loadAddress(asm, REG_SCALAR1, addData(asm, "ld2_mem_1", mem1, 16));
    long resultAddr = reserveData(asm, "result", resultBytes.length, 16);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(), () -> {
      for (int round = 0; round < profile.chainRounds(); round++) {
        vld2PredZ(asm, 0, 0, REG_SCALAR0);
        vld2PredZ(asm, 2, 1, REG_SCALAR1);
      }
    });
    storeVectorRegsAndExit(asm, resultAddr, expectedAddr, 0, 1, 2, 3);
    var elfPath = outputDir.resolve("memory-vld2-pred-z.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("memory-vld2-pred-z", "memory-predicated-load-pair",
        profile.iterations(), profile.bodyInstructions(2), 64, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildVst2Pred(Path outputDir) throws IOException {
    var profile = PROFILE_MEMORY_VST2_PRED;
    int[][] pairInputs = initialVectorRegs();
    byte[] block0 = interleavePairsMasked(pairInputs[0], pairInputs[1], VECTOR_MASKS[0]);
    byte[] block1 = interleavePairsMasked(pairInputs[2], pairInputs[3], VECTOR_MASKS[1]);
    byte[] resultBytes = concat(block0, block1);

    var asm = newProgram();
    emitVectorChainSetup(asm, pairInputs);
    loadMaskRegs(asm, VECTOR_MASKS);
    long out0 = reserveData(asm, "st2_mem_0", block0.length, 16);
    long out1 = reserveData(asm, "st2_mem_1", block1.length, 16);
    loadAddress(asm, REG_SCALAR0, out0);
    loadAddress(asm, REG_SCALAR1, out1);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(), () -> {
      for (int round = 0; round < profile.chainRounds(); round++) {
        vst2Pred(asm, 0, 0, REG_SCALAR0);
        vst2Pred(asm, 2, 1, REG_SCALAR1);
      }
    });
    emitChecksumAndExit(asm, out0, resultBytes.length, expectedAddr);
    var elfPath = outputDir.resolve("memory-vst2-pred.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("memory-vst2-pred", "memory-predicated-store-pair",
        profile.iterations(), profile.bodyInstructions(2), 64, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildVld3PredZ(Path outputDir) throws IOException {
    var profile = PROFILE_MEMORY_VLD3_PRED_Z;
    int[][] regs = initialSixVectorRegs();
    byte[] mem0 = interleaveTriples(regs[0], regs[1], regs[2]);
    byte[] mem1 = interleaveTriples(regs[3], regs[4], regs[5]);
    byte[] resultBytes = concat(
        intsToBytes(maskedVector(regs[0], VECTOR_MASKS[0], true)),
        intsToBytes(maskedVector(regs[1], VECTOR_MASKS[0], true)),
        intsToBytes(maskedVector(regs[2], VECTOR_MASKS[0], true)),
        intsToBytes(maskedVector(regs[3], VECTOR_MASKS[1], true)),
        intsToBytes(maskedVector(regs[4], VECTOR_MASKS[1], true)),
        intsToBytes(maskedVector(regs[5], VECTOR_MASKS[1], true)));

    var asm = newProgram();
    loadMaskRegs(asm, VECTOR_MASKS);
    loadAddress(asm, REG_SCALAR0, addData(asm, "ld3_mem_0", mem0, 16));
    loadAddress(asm, REG_SCALAR1, addData(asm, "ld3_mem_1", mem1, 16));
    long resultAddr = reserveData(asm, "result", resultBytes.length, 16);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(), () -> {
      for (int round = 0; round < profile.chainRounds(); round++) {
        vld3PredZ(asm, 0, 0, REG_SCALAR0);
        vld3PredZ(asm, 3, 1, REG_SCALAR1);
      }
    });
    storeVectorRegsAndExit(asm, resultAddr, expectedAddr, 0, 1, 2, 3, 4, 5);
    var elfPath = outputDir.resolve("memory-vld3-pred-z.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("memory-vld3-pred-z", "memory-predicated-load-struct3",
        profile.iterations(), profile.bodyInstructions(2), 96, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildVst3Pred(Path outputDir) throws IOException {
    var profile = PROFILE_MEMORY_VST3_PRED;
    int[][] regs = initialSixVectorRegs();
    byte[] block0 = interleaveTriplesMasked(regs[0], regs[1], regs[2], VECTOR_MASKS[0]);
    byte[] block1 = interleaveTriplesMasked(regs[3], regs[4], regs[5], VECTOR_MASKS[1]);
    byte[] resultBytes = concat(block0, block1);

    var asm = newProgram();
    emitVectorRegsSetup(asm, regs, "st3_init_");
    loadMaskRegs(asm, VECTOR_MASKS);
    long out0 = reserveData(asm, "st3_mem_0", block0.length, 16);
    long out1 = reserveData(asm, "st3_mem_1", block1.length, 16);
    loadAddress(asm, REG_SCALAR0, out0);
    loadAddress(asm, REG_SCALAR1, out1);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(), () -> {
      for (int round = 0; round < profile.chainRounds(); round++) {
        vst3Pred(asm, 0, 0, REG_SCALAR0);
        vst3Pred(asm, 3, 1, REG_SCALAR1);
      }
    });
    emitChecksumAndExit(asm, out0, resultBytes.length, expectedAddr);
    var elfPath = outputDir.resolve("memory-vst3-pred.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("memory-vst3-pred", "memory-predicated-store-struct3",
        profile.iterations(), profile.bodyInstructions(2), 96, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildVld4PredZ(Path outputDir) throws IOException {
    var profile = PROFILE_MEMORY_VLD4_PRED_Z;
    int[][] regs = initialEightVectorRegs(extraVectorRegs());
    byte[] mem0 = interleaveQuads(regs[0], regs[1], regs[2], regs[3]);
    byte[] mem1 = interleaveQuads(regs[4], regs[5], regs[6], regs[7]);
    byte[] resultBytes = concat(
        intsToBytes(maskedVector(regs[0], VECTOR_MASKS[0], true)),
        intsToBytes(maskedVector(regs[1], VECTOR_MASKS[0], true)),
        intsToBytes(maskedVector(regs[2], VECTOR_MASKS[0], true)),
        intsToBytes(maskedVector(regs[3], VECTOR_MASKS[0], true)),
        intsToBytes(maskedVector(regs[4], VECTOR_MASKS[1], true)),
        intsToBytes(maskedVector(regs[5], VECTOR_MASKS[1], true)),
        intsToBytes(maskedVector(regs[6], VECTOR_MASKS[1], true)),
        intsToBytes(maskedVector(regs[7], VECTOR_MASKS[1], true)));

    var asm = newProgram();
    loadMaskRegs(asm, VECTOR_MASKS);
    loadAddress(asm, REG_SCALAR0, addData(asm, "ld4_mem_0", mem0, 16));
    loadAddress(asm, REG_SCALAR1, addData(asm, "ld4_mem_1", mem1, 16));
    long resultAddr = reserveData(asm, "result", resultBytes.length, 16);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(), () -> {
      for (int round = 0; round < profile.chainRounds(); round++) {
        vld4PredZ(asm, 0, 0, REG_SCALAR0);
        vld4PredZ(asm, 4, 1, REG_SCALAR1);
      }
    });
    storeVectorRegsAndExit(asm, resultAddr, expectedAddr, 0, 1, 2, 3, 4, 5, 6, 7);
    var elfPath = outputDir.resolve("memory-vld4-pred-z.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("memory-vld4-pred-z", "memory-predicated-load-struct4",
        profile.iterations(), profile.bodyInstructions(2), 128, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildVst4Pred(Path outputDir) throws IOException {
    var profile = PROFILE_MEMORY_VST4_PRED;
    int[][] regs = initialEightVectorRegs(extraVectorRegs());
    byte[] block0 = interleaveQuadsMasked(regs[0], regs[1], regs[2], regs[3], VECTOR_MASKS[0]);
    byte[] block1 = interleaveQuadsMasked(regs[4], regs[5], regs[6], regs[7], VECTOR_MASKS[1]);
    byte[] resultBytes = concat(block0, block1);

    var asm = newProgram();
    emitVectorRegsSetup(asm, regs, "st4_init_");
    loadMaskRegs(asm, VECTOR_MASKS);
    long out0 = reserveData(asm, "st4_mem_0", block0.length, 16);
    long out1 = reserveData(asm, "st4_mem_1", block1.length, 16);
    loadAddress(asm, REG_SCALAR0, out0);
    loadAddress(asm, REG_SCALAR1, out1);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(), () -> {
      for (int round = 0; round < profile.chainRounds(); round++) {
        vst4Pred(asm, 0, 0, REG_SCALAR0);
        vst4Pred(asm, 4, 1, REG_SCALAR1);
      }
    });
    emitChecksumAndExit(asm, out0, resultBytes.length, expectedAddr);
    var elfPath = outputDir.resolve("memory-vst4-pred.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("memory-vst4-pred", "memory-predicated-store-struct4",
        profile.iterations(), profile.bodyInstructions(2), 128, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildVgatherPredZ(Path outputDir) throws IOException {
    var profile = PROFILE_MEMORY_VGATHER_PRED_Z;
    int[][] dataRegs = initialVectorRegs();
    int[][] offsetRegs = offsetVectorRegs();
    byte[] resultBytes = concat(
        intsToBytes(gatherVector(dataRegs[0], offsetRegs[0], VECTOR_MASKS[0], true)),
        intsToBytes(gatherVector(dataRegs[1], offsetRegs[1], VECTOR_MASKS[1], true)),
        intsToBytes(gatherVector(dataRegs[2], offsetRegs[2], VECTOR_MASKS[2], true)),
        intsToBytes(gatherVector(dataRegs[3], offsetRegs[3], VECTOR_MASKS[3], true)));

    var asm = newProgram();
    loadMaskRegs(asm, VECTOR_MASKS);
    emitVectorRegsSetup(asm, offsetRegs, "gather_off_", 4);
    loadAddress(asm, REG_SCALAR0, addData(asm, "gather_mem_0", intsToBytes(dataRegs[0]), 16));
    loadAddress(asm, REG_SCALAR1, addData(asm, "gather_mem_1", intsToBytes(dataRegs[1]), 16));
    loadAddress(asm, REG_SCALAR2, addData(asm, "gather_mem_2", intsToBytes(dataRegs[2]), 16));
    loadAddress(asm, REG_SCALAR3, addData(asm, "gather_mem_3", intsToBytes(dataRegs[3]), 16));
    long resultAddr = reserveData(asm, "result", resultBytes.length, 16);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(), () -> {
      for (int round = 0; round < profile.chainRounds(); round++) {
        vgatherPredZ(asm, 0, 0, 4, REG_SCALAR0);
        vgatherPredZ(asm, 1, 1, 5, REG_SCALAR1);
        vgatherPredZ(asm, 2, 2, 6, REG_SCALAR2);
        vgatherPredZ(asm, 3, 3, 7, REG_SCALAR3);
      }
    });
    storeVectorRegsAndExit(asm, resultAddr, expectedAddr, 0, 1, 2, 3);
    var elfPath = outputDir.resolve("memory-vgather-pred-z.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("memory-vgather-pred-z", "memory-gather",
        profile.iterations(), profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildVscatterPred(Path outputDir) throws IOException {
    var profile = PROFILE_MEMORY_VSCATTER_PRED;
    int[][] dataRegs = initialVectorRegs();
    int[][] offsetRegs = offsetVectorRegs();
    byte[] block0 = scatterVector(dataRegs[0], offsetRegs[0], VECTOR_MASKS[0]);
    byte[] block1 = scatterVector(dataRegs[1], offsetRegs[1], VECTOR_MASKS[1]);
    byte[] block2 = scatterVector(dataRegs[2], offsetRegs[2], VECTOR_MASKS[2]);
    byte[] block3 = scatterVector(dataRegs[3], offsetRegs[3], VECTOR_MASKS[3]);
    byte[] resultBytes = concat(block0, block1, block2, block3);

    var asm = newProgram();
    emitVectorChainSetup(asm, dataRegs);
    emitVectorRegsSetup(asm, offsetRegs, "scatter_off_", 4);
    loadMaskRegs(asm, VECTOR_MASKS);
    long out0 = reserveData(asm, "scatter_mem_0", VEC_BYTES, 16);
    long out1 = reserveData(asm, "scatter_mem_1", VEC_BYTES, 16);
    long out2 = reserveData(asm, "scatter_mem_2", VEC_BYTES, 16);
    long out3 = reserveData(asm, "scatter_mem_3", VEC_BYTES, 16);
    loadAddress(asm, REG_SCALAR0, out0);
    loadAddress(asm, REG_SCALAR1, out1);
    loadAddress(asm, REG_SCALAR2, out2);
    loadAddress(asm, REG_SCALAR3, out3);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(), () -> {
      for (int round = 0; round < profile.chainRounds(); round++) {
        vscatterPred(asm, 0, 0, 4, REG_SCALAR0);
        vscatterPred(asm, 1, 1, 5, REG_SCALAR1);
        vscatterPred(asm, 2, 2, 6, REG_SCALAR2);
        vscatterPred(asm, 3, 3, 7, REG_SCALAR3);
      }
    });
    emitChecksumAndExit(asm, out0, resultBytes.length, expectedAddr);
    var elfPath = outputDir.resolve("memory-vscatter-pred.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("memory-vscatter-pred", "memory-scatter",
        profile.iterations(), profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildMaddDo(Path outputDir) throws IOException {
    return buildMatrixBinaryChainCase(outputDir, "matrix-madd-do", "matrix-do",
        PROFILE_MATRIX_MADD_DO,
        (asm, md, ms1, ms2) -> maddDo(asm, md, ms1, ms2),
        (destOld, src1, src2, lane) -> src1 + src2);
  }

  private static BenchmarkArtifact buildMaddTensor(Path outputDir) throws IOException {
    return buildMatrixBinaryChainCase(outputDir, "matrix-madd-tensor", "matrix-tensor",
        PROFILE_MATRIX_MADD_TENSOR,
        (asm, md, ms1, ms2) -> maddTensor(asm, md, ms1, ms2),
        (destOld, src1, src2, lane) -> src1 + src2);
  }

  private static BenchmarkArtifact buildMaddRowBcast(Path outputDir) throws IOException {
    var profile = PROFILE_MATRIX_MADD_ROW_BCAST;
    var matrices = initialMatrixRegs();
    int[][] rows = initialRowRegs();
    var finalMatrices = applyMatrixRowChain(matrices, rows,
        profile.iterations() * profile.chainRounds());
    byte[] resultBytes = matrixToBytes(finalMatrices[3]);

    var asm = newProgram();
    emitMatrixChainSetup(asm, matrices);
    emitRowChainSetup(asm, rows);
    long resultAddr = reserveData(asm, "result", resultBytes.length, 16);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(),
        () -> emitMatrixRowChain(asm, profile.chainRounds(),
            (innerAsm, md, ms1, rr1) -> maddRowBcast(innerAsm, md, ms1, rr1)));
    storeSingleMatrixRegAndExit(asm, resultAddr, expectedAddr, 3);
    var elfPath = outputDir.resolve("matrix-madd-row-bcast.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("matrix-madd-row-bcast", "matrix-broadcast-dimension",
        profile.iterations(), profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildMreduceCols(Path outputDir) throws IOException {
    var profile = PROFILE_MATRIX_MREDUCE_COLS;
    var matrices = initialMatrixRegs();
    int[][] reducedRows = reduceMatrixRegs(matrices);
    byte[] resultBytes = concat(
        intsToBytes(reducedRows[0]),
        intsToBytes(reducedRows[1]),
        intsToBytes(reducedRows[2]),
        intsToBytes(reducedRows[3]));

    var asm = newProgram();
    emitMatrixChainSetup(asm, matrices);
    long resultAddr = reserveData(asm, "result", resultBytes.length, 16);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(),
        () -> emitMatrixReductionChain(asm, profile.chainRounds(),
            (innerAsm, rd, sourceReg) -> mreduceCols(innerAsm, rd, sourceReg)));
    storeRowRegsAndExit(asm, resultAddr, expectedAddr, 0, 1, 2, 3);
    var elfPath = outputDir.resolve("matrix-mreduce-cols.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("matrix-mreduce-cols", "matrix-reduce-dimension",
        profile.iterations(), profile.bodyInstructions(4), 8, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildHaddTensor(Path outputDir) throws IOException {
    var profile = PROFILE_HUGE_HADD_TENSOR;
    var regs = initialHugeRegs();
    var finalRegs = applyHugeBinaryChain(
        regs,
        profile.iterations() * profile.chainRounds(),
        (destOld, src1, src2, lane) -> src1 + src2);
    byte[] resultBytes = intsToBytes(finalRegs[3]);

    var asm = newProgram();
    emitHugeChainSetup(asm, regs);
    long resultAddr = reserveData(asm, "result", resultBytes.length, 16);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(),
        () -> emitHugeBinaryChain(asm, profile.chainRounds(),
            (innerAsm, hd, hs1, hs2) -> haddTensor(innerAsm, hd, hs1, hs2)));
    storeSingleHugeRegAndExit(asm, resultAddr, expectedAddr, 3);
    var elfPath = outputDir.resolve("huge-hadd-tensor.elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact("huge-hadd-tensor", "huge-segmented-tensor",
        profile.iterations(), profile.bodyInstructions(4), 1024, resultBytes.length, elfPath);
  }

  private static void emitVectorSetup(ProgramAssembler asm, long address, int reg) {
    loadAddress(asm, REG_PTR0, address);
    vld(asm, reg, REG_PTR0);
  }

  private static void emitRowSetup(ProgramAssembler asm, long address, int reg) {
    loadAddress(asm, REG_PTR0, address);
    rld(asm, reg, REG_PTR0);
  }

  private static void emitMatrixSetup(ProgramAssembler asm, long address, int reg) {
    loadAddress(asm, REG_PTR0, address);
    mld(asm, reg, REG_PTR0);
  }

  private static void emitHugeSetup(ProgramAssembler asm, long address, int reg) {
    loadAddress(asm, REG_PTR0, address);
    hld(asm, reg, REG_PTR0);
  }

  private static void emitLoop(ProgramAssembler asm, int iterations, Runnable body) {
    loadImm(asm, REG_LOOP, iterations);
    label(asm, "loop");
    body.run();
    addi(asm, REG_LOOP, REG_LOOP, -1);
    beq(asm, REG_LOOP, 0, "loop_done");
    jal(asm, 0, "loop");
    label(asm, "loop_done");
  }

  /**
   * Emits the shared guest-side checksum verifier and HTIF exit path.
   *
   * <p>The benchmark-specific builder is responsible for storing its final architectural result into
   * {@code resultAddr}. This helper hashes that buffer in guest code, compares it with the
   * host-computed checksum stored at {@code expectedAddr}, and reports success or failure through
   * {@code tohost}.
   */
  private static void emitChecksumAndExit(ProgramAssembler asm, long resultAddr,
                                          int resultBytes, long expectedAddr) {
    if ((resultBytes % 8) != 0) {
      throw new IllegalArgumentException("Result size must be a multiple of 8, got " + resultBytes);
    }

    loadAddress(asm, REG_PTR0, resultAddr);
    loadImm(asm, REG_PTR1, resultBytes / 8);
    loadImm(asm, REG_ACC, CHECKSUM_SEED);
    loadImm(asm, REG_MUL, CHECKSUM_MUL);
    label(asm, "checksum_loop");
    load64(asm, REG_TMP, REG_PTR0, 0);
    mul(asm, REG_ACC, REG_ACC, REG_MUL);
    add(asm, REG_ACC, REG_ACC, REG_TMP);
    addi(asm, REG_PTR0, REG_PTR0, 8);
    addi(asm, REG_PTR1, REG_PTR1, -1);
    bne(asm, REG_PTR1, 0, "checksum_loop");

    loadAddress(asm, REG_EXPECTED_PTR, expectedAddr);
    load64(asm, REG_EXPECTED, REG_EXPECTED_PTR, 0);
    bne(asm, REG_ACC, REG_EXPECTED, "checksum_fail");

    loadAddress(asm, REG_TOHOST, TOHOST_ADDR);
    loadImm(asm, REG_TMP, 1);
    store64(asm, REG_TMP, REG_TOHOST, 0);
    label(asm, "spin_success");
    jal(asm, 0, "spin_success");

    label(asm, "checksum_fail");
    loadAddress(asm, REG_TOHOST, TOHOST_ADDR);
    loadImm(asm, REG_TMP, 3);
    store64(asm, REG_TMP, REG_TOHOST, 0);
    label(asm, "spin_fail");
    jal(asm, 0, "spin_fail");
  }

  private static BenchmarkArtifact buildVectorBinaryChainCase(Path outputDir, String id,
                                                              String category,
                                                              BenchmarkProfile profile,
                                                              VectorBinaryInstrEmitter emitter,
                                                              LaneBinaryOp op)
      throws IOException {
    var regs = initialVectorRegs();
    var finalRegs = applyVectorBinaryChain(
        regs,
        profile.iterations() * profile.chainRounds(),
        op);
    byte[] resultBytes = intsToBytes(finalRegs[3]);

    var asm = newProgram();
    emitVectorChainSetup(asm, regs);
    long resultAddr = reserveData(asm, "result", resultBytes.length, 16);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(),
        () -> emitVectorBinaryChain(asm, profile.chainRounds(), emitter));
    storeSingleVectorRegAndExit(asm, resultAddr, expectedAddr, 3);
    var elfPath = outputDir.resolve(id + ".elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact(id, category, profile.iterations(),
        profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildMatrixBinaryChainCase(Path outputDir, String id,
                                                              String category,
                                                              BenchmarkProfile profile,
                                                              MatrixBinaryInstrEmitter emitter,
                                                              LaneBinaryOp op)
      throws IOException {
    var regs = initialMatrixRegs();
    var finalRegs = applyMatrixBinaryChain(
        regs,
        profile.iterations() * profile.chainRounds(),
        op);
    byte[] resultBytes = matrixToBytes(finalRegs[3]);

    var asm = newProgram();
    emitMatrixChainSetup(asm, regs);
    long resultAddr = reserveData(asm, "result", resultBytes.length, 16);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(),
        () -> emitMatrixBinaryChain(asm, profile.chainRounds(), emitter));
    storeSingleMatrixRegAndExit(asm, resultAddr, expectedAddr, 3);
    var elfPath = outputDir.resolve(id + ".elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact(id, category, profile.iterations(),
        profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static BenchmarkArtifact buildReductionChainCase(Path outputDir, String id,
                                                           String category,
                                                           BenchmarkProfile profile)
      throws IOException {
    var regs = initialVectorRegs();
    long[] results = reduceVectorRegs(regs);
    byte[] resultBytes = concat(
        longToBytes(results[0]),
        longToBytes(results[1]),
        longToBytes(results[2]),
        longToBytes(results[3]));

    var asm = newProgram();
    emitVectorChainSetup(asm, regs);
    long resultAddr = reserveData(asm, "result", resultBytes.length, 8);
    long expectedAddr = addData(asm, "expected_checksum", longToBytes(checksum(resultBytes)), 8);
    emitLoop(asm, profile.iterations(),
        () -> emitReductionChain(asm, profile.chainRounds(),
            (innerAsm, rd, sourceReg) -> emitInstruction(innerAsm, "VREDSUM",
                operand("rd", rd), operand("vs1", sourceReg))));
    storeScalarRegsAndExit(asm, resultAddr, expectedAddr,
        REG_SCALAR0, REG_SCALAR1, REG_SCALAR2, REG_SCALAR3);
    var elfPath = outputDir.resolve(id + ".elf");
    writeElfAndAsm(asm, elfPath);
    return new BenchmarkArtifact(id, category, profile.iterations(),
        profile.bodyInstructions(4), 32, resultBytes.length, elfPath);
  }

  private static void emitVectorChainSetup(ProgramAssembler asm, int[][] regs) {
    for (int reg = 0; reg < regs.length; reg++) {
      emitVectorSetup(asm, addData(asm, "vector_init_" + reg, intsToBytes(regs[reg]), 16), reg);
    }
  }

  private static void emitVectorRegsSetup(ProgramAssembler asm, int[][] regs, String prefix) {
    emitVectorRegsSetup(asm, regs, prefix, 0);
  }

  private static void emitVectorRegsSetup(ProgramAssembler asm, int[][] regs, String prefix,
                                          int startReg) {
    for (int reg = 0; reg < regs.length; reg++) {
      emitVectorSetup(asm, addData(asm, prefix + reg, intsToBytes(regs[reg]), 16), startReg + reg);
    }
  }

  private static void emitRawVectorRegsSetup(ProgramAssembler asm, byte[][] regs,
                                             String prefix) {
    for (int reg = 0; reg < regs.length; reg++) {
      emitVectorSetup(asm, addData(asm, prefix + reg, regs[reg], 16), reg);
    }
  }

  private static void emitMatrixChainSetup(ProgramAssembler asm, int[][][] regs) {
    for (int reg = 0; reg < regs.length; reg++) {
      emitMatrixSetup(asm, addData(asm, "matrix_init_" + reg, matrixToBytes(regs[reg]), 16), reg);
    }
  }

  private static void emitRowChainSetup(ProgramAssembler asm, int[][] rows) {
    for (int reg = 0; reg < rows.length; reg++) {
      emitRowSetup(asm, addData(asm, "row_init_" + reg, intsToBytes(rows[reg]), 16), reg);
    }
  }

  private static void emitHugeChainSetup(ProgramAssembler asm, int[][] regs) {
    for (int reg = 0; reg < regs.length; reg++) {
      emitHugeSetup(asm, addData(asm, "huge_init_" + reg, intsToBytes(regs[reg]), 16), reg);
    }
  }

  private static void loadScalarRegs(ProgramAssembler asm, int[] values) {
    loadImm(asm, REG_SCALAR0, values[0]);
    loadImm(asm, REG_SCALAR1, values[1]);
    loadImm(asm, REG_SCALAR2, values[2]);
    loadImm(asm, REG_SCALAR3, values[3]);
  }

  private static void loadMaskRegs(ProgramAssembler asm, int[] masks) {
    loadScalarRegs(asm, masks);
    pmovx(asm, 0, REG_SCALAR0);
    pmovx(asm, 1, REG_SCALAR1);
    pmovx(asm, 2, REG_SCALAR2);
    pmovx(asm, 3, REG_SCALAR3);
  }

  private static void emitVectorBinaryChain(ProgramAssembler asm, int rounds,
                                            VectorBinaryInstrEmitter emitter) {
    for (int i = 0; i < rounds; i++) {
      emitter.emit(asm, 0, 1, 2);
      emitter.emit(asm, 1, 0, 3);
      emitter.emit(asm, 2, 1, 0);
      emitter.emit(asm, 3, 2, 1);
    }
  }

  private static void emitVectorScalarChain(ProgramAssembler asm, int rounds,
                                            VectorScalarInstrEmitter emitter) {
    int[] scalarRegs = {REG_SCALAR0, REG_SCALAR1, REG_SCALAR2, REG_SCALAR3};
    for (int i = 0; i < rounds; i++) {
      emitter.emit(asm, 0, 1, scalarRegs[0]);
      emitter.emit(asm, 1, 0, scalarRegs[1]);
      emitter.emit(asm, 2, 1, scalarRegs[2]);
      emitter.emit(asm, 3, 2, scalarRegs[3]);
    }
  }

  private static void emitVectorScalarBinaryChain(ProgramAssembler asm, int rounds,
                                                  VectorScalarBinaryInstrEmitter emitter) {
    int[] scalarRegs = {REG_SCALAR0, REG_SCALAR1, REG_SCALAR2, REG_SCALAR3};
    for (int i = 0; i < rounds; i++) {
      emitter.emit(asm, 0, 1, 2, scalarRegs[0]);
      emitter.emit(asm, 1, 0, 3, scalarRegs[1]);
      emitter.emit(asm, 2, 1, 0, scalarRegs[2]);
      emitter.emit(asm, 3, 2, 1, scalarRegs[3]);
    }
  }

  private static void emitVectorPredChain(ProgramAssembler asm, int rounds,
                                          VectorPredInstrEmitter emitter) {
    for (int i = 0; i < rounds; i++) {
      emitter.emit(asm, 0, 0, 1, 2);
      emitter.emit(asm, 1, 1, 0, 3);
      emitter.emit(asm, 2, 2, 1, 0);
      emitter.emit(asm, 3, 3, 2, 1);
    }
  }

  private static void emitVectorUnaryChain(ProgramAssembler asm, int rounds,
                                           VectorUnaryInstrEmitter emitter) {
    for (int i = 0; i < rounds; i++) {
      emitter.emit(asm, 0, 1);
      emitter.emit(asm, 1, 2);
      emitter.emit(asm, 2, 3);
      emitter.emit(asm, 3, 0);
    }
  }

  private static void emitVectorTernaryChain(ProgramAssembler asm, int rounds,
                                             VectorTernaryInstrEmitter emitter) {
    for (int i = 0; i < rounds; i++) {
      emitter.emit(asm, 0, 1, 2, 3);
      emitter.emit(asm, 1, 0, 3, 2);
      emitter.emit(asm, 2, 1, 0, 3);
      emitter.emit(asm, 3, 2, 1, 0);
    }
  }

  private static void emitMatrixBinaryChain(ProgramAssembler asm, int rounds,
                                            MatrixBinaryInstrEmitter emitter) {
    for (int i = 0; i < rounds; i++) {
      emitter.emit(asm, 0, 1, 2);
      emitter.emit(asm, 1, 0, 3);
      emitter.emit(asm, 2, 1, 0);
      emitter.emit(asm, 3, 2, 1);
    }
  }

  private static void emitMatrixRowChain(ProgramAssembler asm, int rounds,
                                         MatrixRowInstrEmitter emitter) {
    for (int i = 0; i < rounds; i++) {
      emitter.emit(asm, 0, 1, 0);
      emitter.emit(asm, 1, 0, 1);
      emitter.emit(asm, 2, 1, 2);
      emitter.emit(asm, 3, 2, 3);
    }
  }

  private static void emitHugeBinaryChain(ProgramAssembler asm, int rounds,
                                          HugeBinaryInstrEmitter emitter) {
    for (int i = 0; i < rounds; i++) {
      emitter.emit(asm, 0, 1, 2);
      emitter.emit(asm, 1, 0, 3);
      emitter.emit(asm, 2, 1, 0);
      emitter.emit(asm, 3, 2, 1);
    }
  }

  @FunctionalInterface
  private interface ReductionInstrEmitter {
    void emit(ProgramAssembler assembler, int destReg, int sourceReg);
  }

  private static void emitReductionChain(ProgramAssembler asm, int rounds,
                                         ReductionInstrEmitter emitter) {
    for (int i = 0; i < rounds; i++) {
      emitter.emit(asm, REG_SCALAR0, 0);
      emitter.emit(asm, REG_SCALAR1, 1);
      emitter.emit(asm, REG_SCALAR2, 2);
      emitter.emit(asm, REG_SCALAR3, 3);
    }
  }

  private static void emitMatrixReductionChain(ProgramAssembler asm, int rounds,
                                               ReductionInstrEmitter emitter) {
    for (int i = 0; i < rounds; i++) {
      emitter.emit(asm, 0, 0);
      emitter.emit(asm, 1, 1);
      emitter.emit(asm, 2, 2);
      emitter.emit(asm, 3, 3);
    }
  }

  private static void storeSingleVectorRegAndExit(ProgramAssembler asm, long resultAddr,
                                                  long expectedAddr, int reg) {
    loadAddress(asm, REG_PTR0, resultAddr);
    vst(asm, reg, REG_PTR0);
    emitChecksumAndExit(asm, resultAddr, VEC_BYTES, expectedAddr);
  }

  private static void storeVectorRegsAndExit(ProgramAssembler asm, long resultAddr,
                                             long expectedAddr, int... regs) {
    for (int i = 0; i < regs.length; i++) {
      loadAddress(asm, REG_PTR0, resultAddr + (long) i * VEC_BYTES);
      vst(asm, regs[i], REG_PTR0);
    }
    emitChecksumAndExit(asm, resultAddr, regs.length * VEC_BYTES,
        expectedAddr);
  }

  private static void storeSingleMatrixRegAndExit(ProgramAssembler asm, long resultAddr,
                                                  long expectedAddr, int reg) {
    loadAddress(asm, REG_PTR0, resultAddr);
    mst(asm, reg, REG_PTR0);
    emitChecksumAndExit(asm, resultAddr, MAT_BYTES, expectedAddr);
  }

  private static void storeSingleHugeRegAndExit(ProgramAssembler asm, long resultAddr,
                                                long expectedAddr, int reg) {
    loadAddress(asm, REG_PTR0, resultAddr);
    hst(asm, reg, REG_PTR0);
    emitChecksumAndExit(asm, resultAddr, HUGE_BYTES, expectedAddr);
  }

  private static void storeRowRegsAndExit(ProgramAssembler asm, long resultAddr,
                                          long expectedAddr, int... regs) {
    for (int i = 0; i < regs.length; i++) {
      loadAddress(asm, REG_PTR0, resultAddr + (long) i * ROW_BYTES);
      rst(asm, regs[i], REG_PTR0);
    }
    emitChecksumAndExit(asm, resultAddr, regs.length * ROW_BYTES,
        expectedAddr);
  }

  private static void storeScalarRegsAndExit(ProgramAssembler asm, long resultAddr,
                                             long expectedAddr, int... regs) {
    for (int i = 0; i < regs.length; i++) {
      loadAddress(asm, REG_PTR0, resultAddr + (long) i * 8);
      store64(asm, regs[i], REG_PTR0, 0);
    }
    emitChecksumAndExit(asm, resultAddr, regs.length * 8, expectedAddr);
  }

  private static void storePredicateRegsAndExit(ProgramAssembler asm, long resultAddr,
                                                long expectedAddr, int... regs) {
    for (int i = 0; i < regs.length; i++) {
      loadAddress(asm, REG_PTR0, resultAddr + (long) i * 4);
      pst(asm, regs[i], REG_PTR0);
    }
    emitChecksumAndExit(asm, resultAddr, regs.length * 4, expectedAddr);
  }

  private static int[][] initialVectorRegs() {
    return new int[][] {
        vectorDataD(),
        vectorDataA(),
        vectorDataB(),
        vectorDataC()
    };
  }

  private static int[][] initialSixVectorRegs() {
    return new int[][] {
        vectorDataD(),
        vectorDataA(),
        vectorDataB(),
        vectorDataC(),
        offsetVectorData(5),
        offsetVectorData(9)
    };
  }

  private static int[][][] initialMatrixRegs() {
    return new int[][][] {
        matrixDataD(),
        matrixDataA(),
        matrixDataB(),
        matrixDataC()
    };
  }

  private static int[][] initialRowRegs() {
    return new int[][] {
        rowData(),
        rowDataB(),
        rowDataC(),
        rowDataD()
    };
  }

  private static int[][] initialHugeRegs() {
    return new int[][] {
        hugeDataD(),
        hugeDataA(),
        hugeDataB(),
        hugeDataC()
    };
  }

  private static int[][] applyVectorBinaryChain(int[][] initialRegs, int rounds, LaneBinaryOp op) {
    int[][] regs = copyVectors(initialRegs);
    for (int round = 0; round < rounds; round++) {
      applyVectorBinaryStep(regs, 0, 1, 2, op);
      applyVectorBinaryStep(regs, 1, 0, 3, op);
      applyVectorBinaryStep(regs, 2, 1, 0, op);
      applyVectorBinaryStep(regs, 3, 2, 1, op);
    }
    return regs;
  }

  private static int[][] applyVectorScalarChain(int[][] initialRegs, int rounds, int[] scalars,
                                                LaneScalarOp op) {
    int[][] regs = copyVectors(initialRegs);
    for (int round = 0; round < rounds; round++) {
      applyVectorScalarStep(regs, 0, 1, scalars[0], op);
      applyVectorScalarStep(regs, 1, 0, scalars[1], op);
      applyVectorScalarStep(regs, 2, 1, scalars[2], op);
      applyVectorScalarStep(regs, 3, 2, scalars[3], op);
    }
    return regs;
  }

  private static int[][] applyVectorScalarBinaryChain(int[][] initialRegs, int rounds,
                                                      int[] scalars,
                                                      LaneScalarBinaryOp op) {
    int[][] regs = copyVectors(initialRegs);
    for (int round = 0; round < rounds; round++) {
      applyVectorScalarBinaryStep(regs, 0, 1, 2, scalars[0], op);
      applyVectorScalarBinaryStep(regs, 1, 0, 3, scalars[1], op);
      applyVectorScalarBinaryStep(regs, 2, 1, 0, scalars[2], op);
      applyVectorScalarBinaryStep(regs, 3, 2, 1, scalars[3], op);
    }
    return regs;
  }

  private static int[][] applyVectorPredChain(int[][] initialRegs, int rounds, int[] masks) {
    int[][] regs = copyVectors(initialRegs);
    for (int round = 0; round < rounds; round++) {
      applyVectorPredStep(regs, 0, 1, 2, masks[0]);
      applyVectorPredStep(regs, 1, 0, 3, masks[1]);
      applyVectorPredStep(regs, 2, 1, 0, masks[2]);
      applyVectorPredStep(regs, 3, 2, 1, masks[3]);
    }
    return regs;
  }

  private static int[][] applyVectorSelectChain(int[][] initialRegs, int rounds, int[] masks) {
    int[][] regs = copyVectors(initialRegs);
    for (int round = 0; round < rounds; round++) {
      applyVectorSelectStep(regs, 0, 1, 2, masks[0]);
      applyVectorSelectStep(regs, 1, 0, 3, masks[1]);
      applyVectorSelectStep(regs, 2, 1, 0, masks[2]);
      applyVectorSelectStep(regs, 3, 2, 1, masks[3]);
    }
    return regs;
  }

  private static int[][] applyVectorUnaryChain(int[][] initialRegs, int rounds, LaneUnaryOp op) {
    int[][] regs = copyVectors(initialRegs);
    for (int round = 0; round < rounds; round++) {
      applyVectorUnaryStep(regs, 0, 1, op);
      applyVectorUnaryStep(regs, 1, 2, op);
      applyVectorUnaryStep(regs, 2, 3, op);
      applyVectorUnaryStep(regs, 3, 0, op);
    }
    return regs;
  }

  private static int[][] applyVectorZipChain(int[][] initialRegs, int rounds) {
    int[][] regs = copyVectors(initialRegs);
    for (int round = 0; round < rounds; round++) {
      applyVectorZipStep(regs, 0, 1, 2);
      applyVectorZipStep(regs, 1, 0, 3);
      applyVectorZipStep(regs, 2, 1, 0);
      applyVectorZipStep(regs, 3, 2, 1);
    }
    return regs;
  }

  private static int[][] applyVectorExtChain(int[][] initialRegs, int rounds, int[] offsets) {
    int[][] regs = copyVectors(initialRegs);
    for (int round = 0; round < rounds; round++) {
      applyVectorExtStep(regs, 0, 1, 2, offsets[0]);
      applyVectorExtStep(regs, 1, 0, 3, offsets[1]);
      applyVectorExtStep(regs, 2, 1, 0, offsets[2]);
      applyVectorExtStep(regs, 3, 2, 1, offsets[3]);
    }
    return regs;
  }

  private static int[][] applyVectorUzpChain(int[][] initialRegs, int rounds) {
    int[][] regs = copyVectors(initialRegs);
    for (int round = 0; round < rounds; round++) {
      applyVectorUzpStep(regs, 0, 1, 2);
      applyVectorUzpStep(regs, 1, 0, 3);
      applyVectorUzpStep(regs, 2, 1, 0);
      applyVectorUzpStep(regs, 3, 2, 1);
    }
    return regs;
  }

  private static byte[][] applyRawUnaryChain(byte[][] initialRegs, int rounds,
                                             java.util.function.Function<byte[], byte[]> op) {
    byte[][] regs = copyRawVectors(initialRegs);
    for (int round = 0; round < rounds; round++) {
      regs[0] = op.apply(regs[1]);
      regs[1] = op.apply(regs[2]);
      regs[2] = op.apply(regs[3]);
      regs[3] = op.apply(regs[0]);
    }
    return regs;
  }

  private static int[][] applyVectorTernaryChain(int[][] initialRegs, int rounds,
                                                 LaneTernaryOp op) {
    int[][] regs = copyVectors(initialRegs);
    for (int round = 0; round < rounds; round++) {
      applyVectorTernaryStep(regs, 0, 1, 2, 3, op);
      applyVectorTernaryStep(regs, 1, 0, 3, 2, op);
      applyVectorTernaryStep(regs, 2, 1, 0, 3, op);
      applyVectorTernaryStep(regs, 3, 2, 1, 0, op);
    }
    return regs;
  }

  private static int[][][] applyMatrixBinaryChain(int[][][] initialRegs, int rounds,
                                                  LaneBinaryOp op) {
    int[][][] regs = copyMatrices(initialRegs);
    for (int round = 0; round < rounds; round++) {
      applyMatrixBinaryStep(regs, 0, 1, 2, op);
      applyMatrixBinaryStep(regs, 1, 0, 3, op);
      applyMatrixBinaryStep(regs, 2, 1, 0, op);
      applyMatrixBinaryStep(regs, 3, 2, 1, op);
    }
    return regs;
  }

  private static int[][][] applyMatrixRowChain(int[][][] initialRegs, int[][] rows, int rounds) {
    int[][][] regs = copyMatrices(initialRegs);
    for (int round = 0; round < rounds; round++) {
      applyMatrixRowStep(regs, rows, 0, 1, 0);
      applyMatrixRowStep(regs, rows, 1, 0, 1);
      applyMatrixRowStep(regs, rows, 2, 1, 2);
      applyMatrixRowStep(regs, rows, 3, 2, 3);
    }
    return regs;
  }

  private static int[][] applyHugeBinaryChain(int[][] initialRegs, int rounds, LaneBinaryOp op) {
    int[][] regs = copyVectors(initialRegs);
    for (int round = 0; round < rounds; round++) {
      applyVectorBinaryStep(regs, 0, 1, 2, op);
      applyVectorBinaryStep(regs, 1, 0, 3, op);
      applyVectorBinaryStep(regs, 2, 1, 0, op);
      applyVectorBinaryStep(regs, 3, 2, 1, op);
    }
    return regs;
  }

  private static long[] reduceVectorRegs(int[][] regs) {
    long[] out = new long[4];
    for (int reg = 0; reg < regs.length; reg++) {
      long acc = 0;
      for (int lane : regs[reg]) {
        acc += Integer.toUnsignedLong(lane);
      }
      out[reg] = acc;
    }
    return out;
  }

  private static int[][] reduceMatrixRegs(int[][][] regs) {
    int[][] out = new int[regs.length][8];
    for (int reg = 0; reg < regs.length; reg++) {
      for (int col = 0; col < 8; col++) {
        int sum = 0;
        for (int row = 0; row < 4; row++) {
          sum += regs[reg][row][col];
        }
        out[reg][col] = sum;
      }
    }
    return out;
  }

  private static void applyVectorBinaryStep(int[][] regs, int dest, int src1, int src2,
                                            LaneBinaryOp op) {
    int[] out = new int[regs[dest].length];
    for (int lane = 0; lane < out.length; lane++) {
      out[lane] = op.apply(regs[dest][lane], regs[src1][lane], regs[src2][lane], lane);
    }
    regs[dest] = out;
  }

  private static void applyVectorScalarStep(int[][] regs, int dest, int src, int scalar,
                                            LaneScalarOp op) {
    int[] out = new int[regs[dest].length];
    for (int lane = 0; lane < out.length; lane++) {
      out[lane] = op.apply(regs[dest][lane], regs[src][lane], scalar, lane);
    }
    regs[dest] = out;
  }

  private static void applyVectorScalarBinaryStep(int[][] regs, int dest, int src1, int src2,
                                                  int scalar, LaneScalarBinaryOp op) {
    int[] out = new int[regs[dest].length];
    for (int lane = 0; lane < out.length; lane++) {
      out[lane] = op.apply(regs[dest][lane], regs[src1][lane], regs[src2][lane], scalar, lane);
    }
    regs[dest] = out;
  }

  private static void applyVectorPredStep(int[][] regs, int dest, int src1, int src2, int mask) {
    int[] out = new int[regs[dest].length];
    for (int lane = 0; lane < out.length; lane++) {
      out[lane] = ((mask >>> lane) & 1) != 0
          ? regs[src1][lane] + regs[src2][lane]
          : regs[dest][lane];
    }
    regs[dest] = out;
  }

  private static void applyVectorSelectStep(int[][] regs, int dest, int src1, int src2, int mask) {
    int[] out = new int[regs[dest].length];
    for (int lane = 0; lane < out.length; lane++) {
      out[lane] = ((mask >>> lane) & 1) != 0 ? regs[src1][lane] : regs[src2][lane];
    }
    regs[dest] = out;
  }

  private static void applyVectorUnaryStep(int[][] regs, int dest, int src, LaneUnaryOp op) {
    int[] out = new int[regs[dest].length];
    for (int lane = 0; lane < out.length; lane++) {
      out[lane] = op.apply(regs[dest][lane], regs[src][lane], lane);
    }
    regs[dest] = out;
  }

  private static void applyVectorZipStep(int[][] regs, int dest, int src1, int src2) {
    int[] out = new int[regs[dest].length];
    for (int lane = 0; lane < out.length; lane++) {
      out[lane] = (lane & 1) == 0 ? regs[src1][lane >>> 1] : regs[src2][lane >>> 1];
    }
    regs[dest] = out;
  }

  private static void applyVectorExtStep(int[][] regs, int dest, int src1, int src2, int offset) {
    int[] out = new int[regs[dest].length];
    for (int lane = 0; lane < out.length; lane++) {
      int idx = lane + offset;
      out[lane] = idx < regs[dest].length ? regs[src1][idx] : regs[src2][idx - regs[dest].length];
    }
    regs[dest] = out;
  }

  private static void applyVectorUzpStep(int[][] regs, int dest, int src1, int src2) {
    int[] out = new int[regs[dest].length];
    for (int lane = 0; lane < out.length; lane++) {
      out[lane] = lane < 16 ? regs[src1][lane * 2] : regs[src2][(lane - 16) * 2];
    }
    regs[dest] = out;
  }

  private static void applyVectorTernaryStep(int[][] regs, int dest, int src1, int src2, int src3,
                                             LaneTernaryOp op) {
    int[] out = new int[regs[dest].length];
    for (int lane = 0; lane < out.length; lane++) {
      out[lane] =
          op.apply(regs[dest][lane], regs[src1][lane], regs[src2][lane], regs[src3][lane], lane);
    }
    regs[dest] = out;
  }

  private static void applyMatrixBinaryStep(int[][][] regs, int dest, int src1, int src2,
                                            LaneBinaryOp op) {
    int[][] out = regs[dest];
    int lane = 0;
    for (int row = 0; row < out.length; row++) {
      for (int col = 0; col < out[row].length; col++) {
        out[row][col] =
            op.apply(regs[dest][row][col], regs[src1][row][col], regs[src2][row][col], lane++);
      }
    }
  }

  private static void applyMatrixRowStep(int[][][] regs, int[][] rows, int dest, int srcMatrix,
                                         int rowReg) {
    int[][] out = regs[dest];
    for (int row = 0; row < out.length; row++) {
      for (int col = 0; col < out[row].length; col++) {
        out[row][col] = regs[srcMatrix][row][col] + rows[rowReg][col];
      }
    }
  }

  private static int[][] copyVectors(int[][] source) {
    int[][] out = new int[source.length][];
    for (int i = 0; i < source.length; i++) {
      out[i] = source[i].clone();
    }
    return out;
  }

  private static byte[][] copyRawVectors(byte[][] source) {
    byte[][] out = new byte[source.length][];
    for (int i = 0; i < source.length; i++) {
      out[i] = source[i].clone();
    }
    return out;
  }

  private static int[][][] copyMatrices(int[][][] source) {
    int[][][] out = new int[source.length][][];
    for (int i = 0; i < source.length; i++) {
      out[i] = new int[source[i].length][];
      for (int row = 0; row < source[i].length; row++) {
        out[i][row] = source[i][row].clone();
      }
    }
    return out;
  }

  private static void writeElfAndAsm(ProgramAssembler asm, Path elfPath) throws IOException {
    writeElf(asm, elfPath);
    var disassembler = DISASSEMBLER.get();
    if (disassembler == null) {
      return;
    }
    var asmPath = elfPath.resolveSibling(
        elfPath.getFileName().toString().replace(".elf", ".s"));
    writeAsmFile(asm, asmPath, disassembler);
  }

  private static void writeAsmFile(ProgramAssembler asm, Path asmPath,
                                   Disassembler disassembler) throws IOException {
    var textBytes = getTextBytes(asm);
    var textLabels = getTextLabels(asm);

    var labelsByOffset = new TreeMap<Integer, List<String>>();
    for (var entry : textLabels.entrySet()) {
      labelsByOffset.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
    }

    var sb = new StringBuilder();
    sb.append(".text\n");
    for (int offset = 0; offset + 3 < textBytes.length; offset += 4) {
      var labels = labelsByOffset.get(offset);
      if (labels != null) {
        for (var name : labels) {
          sb.append(name).append(":\n");
        }
      }
      int word = ByteBuffer.wrap(textBytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
      var encoding = Constant.Value.of(Integer.toUnsignedLong(Integer.reverseBytes(word)),
          Type.bits(32));
      sb.append("  ").append(disassembler.disassemble(encoding)).append('\n');
    }
    Files.writeString(asmPath, sb.toString(), StandardCharsets.UTF_8);
  }

  /**
   * Writes the manifest consumed by the Python benchmark runner.
   *
   * <p>The manifest is the stable contract between the Java generator and the runtime benchmark
   * executor. It records both the ELF filenames and the normalization metadata needed to compute
   * per-instruction and per-element timing metrics.
   */
  private static void writeManifest(Path manifest, List<BenchmarkArtifact> artifacts)
      throws IOException {
    var out = new StringBuilder();
    out.append("id,category,iterations,body_repeats,active_elements,result_bytes,file\n");
    for (var artifact : artifacts) {
      out.append(artifact.id()).append(',')
          .append(artifact.category()).append(',')
          .append(artifact.iterations()).append(',')
          .append(artifact.bodyRepeats()).append(',')
          .append(artifact.activeElements()).append(',')
          .append(artifact.resultBytes()).append(',')
          .append(artifact.elfPath().getFileName())
          .append('\n');
    }
    Files.writeString(manifest, out.toString(), StandardCharsets.UTF_8);
  }

  private static long maskCount(int mask) {
    return Integer.bitCount(mask & 0xffff_ffff);
  }

  private static int[] maskedVector(int[] input, int mask, boolean zeroElse) {
    int[] out = new int[input.length];
    for (int lane = 0; lane < input.length; lane++) {
      boolean active = ((mask >>> lane) & 1) != 0;
      out[lane] = active ? input[lane] : 0;
      if (!zeroElse && !active) {
        out[lane] = 0;
      }
    }
    return out;
  }

  private static int[] applyPredicateBinaryChain(int[] masks, int rounds) {
    int[] out = masks.clone();
    for (int round = 0; round < rounds; round++) {
      out[0] = out[1] & out[2];
      out[1] = out[0] & out[3];
      out[2] = out[1] & out[0];
      out[3] = out[2] & out[1];
    }
    return out;
  }

  private static int[] applyPredicateFirstChain(int[] masks, int rounds) {
    int[] out = masks.clone();
    for (int round = 0; round < rounds; round++) {
      out[0] = out[0] | firstSetMask(out[1]);
      out[1] = out[1] | firstSetMask(out[2]);
      out[2] = out[2] | firstSetMask(out[3]);
      out[3] = out[3] | firstSetMask(out[0]);
    }
    return out;
  }

  private static int[] applyPredicateNextChain(int[] masks, int rounds) {
    int[] out = masks.clone();
    for (int round = 0; round < rounds; round++) {
      out[0] = nextPredicateMask(out[0], out[1]);
      out[1] = nextPredicateMask(out[1], out[2]);
      out[2] = nextPredicateMask(out[2], out[3]);
      out[3] = nextPredicateMask(out[3], out[0]);
    }
    return out;
  }

  private static int firstSetMask(int mask) {
    if (mask == 0) {
      return 0;
    }
    return Integer.lowestOneBit(mask);
  }

  private static int nextPredicateMask(int src, int mask) {
    int activeMask = mask;
    int clearMask;
    if (src == 0) {
      clearMask = activeMask;
    } else {
      int clr = Integer.SIZE - Integer.numberOfLeadingZeros(src);
      if (clr >= 32) {
        clearMask = 0;
      } else {
        clearMask = activeMask >>> clr << clr;
      }
    }
    if (clearMask == 0) {
      return 0;
    }
    return Integer.lowestOneBit(clearMask);
  }

  private static int compareMaskLt(int[] left, int[] right) {
    int mask = 0;
    for (int lane = 0; lane < left.length; lane++) {
      if (left[lane] < right[lane]) {
        mask |= 1 << lane;
      }
    }
    return mask;
  }

  private static int[][] initialEightVectorRegs(int[][] extraRegs) {
    int[][] regs = new int[8][];
    var base = initialVectorRegs();
    System.arraycopy(base, 0, regs, 0, 4);
    System.arraycopy(extraRegs, 0, regs, 4, 4);
    return regs;
  }

  private static int[][] extraVectorRegs() {
    return new int[][] {
        offsetVectorData(5),
        offsetVectorData(9),
        offsetVectorData(13),
        offsetVectorData(17)
    };
  }

  private static byte[][] initialByteVectorRegs() {
    return new byte[][] {
        byteVectorDataD(),
        byteVectorDataA(),
        byteVectorDataB(),
        byteVectorDataC()
    };
  }

  private static int[][] offsetVectorRegs() {
    return new int[][] {
        offsetVectorData(3),
        offsetVectorData(11),
        offsetVectorData(19),
        offsetVectorData(27)
    };
  }

  private static int[] gatherVector(int[] data, int[] offsets, int mask, boolean zeroElse) {
    int[] out = new int[data.length];
    for (int lane = 0; lane < out.length; lane++) {
      boolean active = ((mask >>> lane) & 1) != 0;
      if (!active) {
        out[lane] = zeroElse ? 0 : out[lane];
        continue;
      }
      out[lane] = data[(offsets[lane] >>> 2) & 31];
    }
    return out;
  }

  private static byte[] scatterVector(int[] data, int[] offsets, int mask) {
    int[] out = new int[data.length];
    for (int lane = 0; lane < data.length; lane++) {
      if (((mask >>> lane) & 1) != 0) {
        out[(offsets[lane] >>> 2) & 31] = data[lane];
      }
    }
    return intsToBytes(out);
  }

  private static byte[] interleavePairs(int[] first, int[] second) {
    int[] out = new int[first.length * 2];
    for (int lane = 0; lane < first.length; lane++) {
      out[lane * 2] = first[lane];
      out[lane * 2 + 1] = second[lane];
    }
    return intsToBytes(out);
  }

  private static byte[] interleavePairsMasked(int[] first, int[] second, int mask) {
    int[] out = new int[first.length * 2];
    for (int lane = 0; lane < first.length; lane++) {
      if (((mask >>> lane) & 1) != 0) {
        out[lane * 2] = first[lane];
        out[lane * 2 + 1] = second[lane];
      }
    }
    return intsToBytes(out);
  }

  private static byte[] interleaveTriples(int[] first, int[] second, int[] third) {
    int[] out = new int[first.length * 3];
    for (int lane = 0; lane < first.length; lane++) {
      out[lane * 3] = first[lane];
      out[lane * 3 + 1] = second[lane];
      out[lane * 3 + 2] = third[lane];
    }
    return intsToBytes(out);
  }

  private static byte[] interleaveTriplesMasked(int[] first, int[] second, int[] third, int mask) {
    int[] out = new int[first.length * 3];
    for (int lane = 0; lane < first.length; lane++) {
      if (((mask >>> lane) & 1) != 0) {
        out[lane * 3] = first[lane];
        out[lane * 3 + 1] = second[lane];
        out[lane * 3 + 2] = third[lane];
      }
    }
    return intsToBytes(out);
  }

  private static byte[] interleaveQuads(int[] first, int[] second, int[] third, int[] fourth) {
    int[] out = new int[first.length * 4];
    for (int lane = 0; lane < first.length; lane++) {
      out[lane * 4] = first[lane];
      out[lane * 4 + 1] = second[lane];
      out[lane * 4 + 2] = third[lane];
      out[lane * 4 + 3] = fourth[lane];
    }
    return intsToBytes(out);
  }

  private static byte[] interleaveQuadsMasked(int[] first, int[] second, int[] third, int[] fourth,
                                              int mask) {
    int[] out = new int[first.length * 4];
    for (int lane = 0; lane < first.length; lane++) {
      if (((mask >>> lane) & 1) != 0) {
        out[lane * 4] = first[lane];
        out[lane * 4 + 1] = second[lane];
        out[lane * 4 + 2] = third[lane];
        out[lane * 4 + 3] = fourth[lane];
      }
    }
    return intsToBytes(out);
  }

  private static byte[] widenU8h(byte[] raw) {
    byte[] out = new byte[VEC_BYTES];
    for (int lane = 0; lane < 64; lane++) {
      out[lane * 2] = raw[lane];
      out[lane * 2 + 1] = 0;
    }
    return out;
  }

  private static byte[] narrowH8(byte[] raw) {
    byte[] out = new byte[VEC_BYTES];
    for (int lane = 0; lane < 64; lane++) {
      out[lane] = raw[lane * 2];
    }
    return out;
  }

  private static int[] vectorDataA() {
    int[] data = new int[32];
    for (int i = 0; i < data.length; i++) {
      data[i] = 0x1100 + i * 0x21;
    }
    return data;
  }

  private static int[] vectorDataB() {
    int[] data = new int[32];
    for (int i = 0; i < data.length; i++) {
      data[i] = 0x2200 - i * 0x13;
    }
    return data;
  }

  private static int[] vectorDataC() {
    int[] data = new int[32];
    for (int i = 0; i < data.length; i++) {
      data[i] = 0x3300 ^ (i * 0x55);
    }
    return data;
  }

  private static int[] vectorDataD() {
    int[] data = new int[32];
    for (int i = 0; i < data.length; i++) {
      data[i] = 0x4400 + i * 0x17 - (i % 5) * 3;
    }
    return data;
  }

  private static byte[] byteVectorDataA() {
    byte[] data = new byte[128];
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte) (0x10 + i * 3);
    }
    return data;
  }

  private static byte[] byteVectorDataB() {
    byte[] data = new byte[128];
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte) (0x80 - i * 5);
    }
    return data;
  }

  private static byte[] byteVectorDataC() {
    byte[] data = new byte[128];
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte) (0x33 ^ (i * 7));
    }
    return data;
  }

  private static byte[] byteVectorDataD() {
    byte[] data = new byte[128];
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte) (0x55 + i * 9 - (i % 3) * 11);
    }
    return data;
  }

  private static int[] offsetVectorData(int seed) {
    int[] data = new int[32];
    for (int i = 0; i < data.length; i++) {
      int idx = (i * 7 + seed) & 31;
      data[i] = idx * 4;
    }
    return data;
  }

  private static int[] rowData() {
    int[] data = new int[8];
    for (int i = 0; i < data.length; i++) {
      data[i] = 0x40 + i * 9;
    }
    return data;
  }

  private static int[] rowDataB() {
    int[] data = new int[8];
    for (int i = 0; i < data.length; i++) {
      data[i] = 0x80 - i * 7;
    }
    return data;
  }

  private static int[] rowDataC() {
    int[] data = new int[8];
    for (int i = 0; i < data.length; i++) {
      data[i] = 0x120 ^ (i * 11);
    }
    return data;
  }

  private static int[] rowDataD() {
    int[] data = new int[8];
    for (int i = 0; i < data.length; i++) {
      data[i] = 0x1C0 + i * 13;
    }
    return data;
  }

  private static int[][] matrixDataA() {
    int[][] data = new int[4][8];
    for (int r = 0; r < data.length; r++) {
      for (int c = 0; c < data[r].length; c++) {
        data[r][c] = 0x100 + r * 0x30 + c * 5;
      }
    }
    return data;
  }

  private static int[][] matrixDataB() {
    int[][] data = new int[4][8];
    for (int r = 0; r < data.length; r++) {
      for (int c = 0; c < data[r].length; c++) {
        data[r][c] = 0x200 - r * 7 + c * 3;
      }
    }
    return data;
  }

  private static int[][] matrixDataC() {
    int[][] data = new int[4][8];
    for (int r = 0; r < data.length; r++) {
      for (int c = 0; c < data[r].length; c++) {
        data[r][c] = 0x300 + r * 9 - c * 4;
      }
    }
    return data;
  }

  private static int[][] matrixDataD() {
    int[][] data = new int[4][8];
    for (int r = 0; r < data.length; r++) {
      for (int c = 0; c < data[r].length; c++) {
        data[r][c] = 0x180 ^ (r * 0x22 + c * 0x1b);
      }
    }
    return data;
  }

  private static int[] hugeDataA() {
    int[] data = new int[1024];
    for (int i = 0; i < data.length; i++) {
      data[i] = 0x100000 + i * 3;
    }
    return data;
  }

  private static int[] hugeDataB() {
    int[] data = new int[1024];
    for (int i = 0; i < data.length; i++) {
      data[i] = 0x200000 - i * 5;
    }
    return data;
  }

  private static int[] hugeDataC() {
    int[] data = new int[1024];
    for (int i = 0; i < data.length; i++) {
      data[i] = 0x180000 + i * 7;
    }
    return data;
  }

  private static int[] hugeDataD() {
    int[] data = new int[1024];
    for (int i = 0; i < data.length; i++) {
      data[i] = 0x280000 - i * 9;
    }
    return data;
  }

  private static int[] mapVector(int[] left, int[] right, Int2Int2Int function) {
    int[] out = new int[left.length];
    for (int i = 0; i < out.length; i++) {
      out[i] = function.apply(left[i], right[i]);
    }
    return out;
  }

  private static int[] mapVector(int[] src, int scalar, Int2Int2Int function) {
    int[] out = new int[src.length];
    for (int i = 0; i < out.length; i++) {
      out[i] = function.apply(src[i], scalar);
    }
    return out;
  }

  private static int[][] mapMatrix(int[][] left, int[][] right, Int2Int2Int function) {
    int[][] out = new int[left.length][left[0].length];
    for (int r = 0; r < out.length; r++) {
      for (int c = 0; c < out[r].length; c++) {
        out[r][c] = function.apply(left[r][c], right[r][c]);
      }
    }
    return out;
  }

  private static byte[] intsToBytes(int[] values) {
    ByteBuffer buf = ByteBuffer.allocate(values.length * 4).order(ByteOrder.LITTLE_ENDIAN);
    for (int value : values) {
      buf.putInt(value);
    }
    return buf.array();
  }

  private static byte[] matrixToBytes(int[][] matrix) {
    ByteBuffer buf = ByteBuffer.allocate(matrix.length * matrix[0].length * 4)
        .order(ByteOrder.LITTLE_ENDIAN);
    for (int[] row : matrix) {
      for (int value : row) {
        buf.putInt(value);
      }
    }
    return buf.array();
  }

  private static byte[] longToBytes(long value) {
    ByteBuffer buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
    buf.putLong(value);
    return buf.array();
  }

  private static byte[] concat(byte[]... arrays) {
    int length = 0;
    for (byte[] array : arrays) {
      length += array.length;
    }

    byte[] out = new byte[length];
    int offset = 0;
    for (byte[] array : arrays) {
      System.arraycopy(array, 0, out, offset, array.length);
      offset += array.length;
    }
    return out;
  }

  private static long checksum(byte[] bytes) {
    ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    long acc = CHECKSUM_SEED;
    while (buf.hasRemaining()) {
      long word = buf.getLong();
      acc = acc * Integer.toUnsignedLong(CHECKSUM_MUL) + word;
    }
    return acc;
  }

  @FunctionalInterface
  private interface Int2Int2Int {
    int apply(int left, int right);
  }
}
