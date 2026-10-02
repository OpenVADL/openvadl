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

package vadl.iss.codegen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.utility.MountableFile;
import vadl.TestUtils;
import vadl.asm.ElfProgramBuilder;
import vadl.asm.InstructionEncoder;
import vadl.asm.ProgramAssembler;
import vadl.iss.QemuIssTest;
import vadl.types.Type;
import vadl.utils.Disassembler;
import vadl.utils.VadlFileUtils;
import vadl.vdt.impl.regular.RegularDecodeTreeGenerator;
import vadl.viam.Constant;
import vadl.viam.InstructionSetArchitecture;

/**
 * Tests the chaining/exiting behavior of TCG translation blocks. Checks the emitted TCG
 * instructions and the runtime behavior of the translation blocks.
 */
public class IssTbChainExitTest extends QemuIssTest {

  private static final long TEXT_BASE = 0x0L;
  private static final long DATA_BASE = 0x10000L;
  private static final long TOHOST_ADDR = 0x20000L;
  private static final long FROMHOST_ADDR = 0x20008L;
  private static final int TOHOST_OFFSET = (int) (TOHOST_ADDR - TEXT_BASE);

  private static final String VADL_SPEC = "iss/tb-chain-exit/spec.vadl";
  private static final InstructionSetArchitecture ISA =
      TestUtils.compileToViam(readSpecSource()).isa().orElseThrow();
  private static final Disassembler DISASSEMBLER =
      new Disassembler(ISA, new RegularDecodeTreeGenerator(), ByteOrder.LITTLE_ENDIAN);

  // the test code is always the 3rd instruction
  private static final Pattern TCG_OPS_PATTERN = Pattern.compile(
      "---- 0000000000000008(\n[^\n]+)*\n\n", Pattern.MULTILINE);
  private static final Pattern NO_TB_END_PATTERN = Pattern.compile(
      "---- 0000000000000008(\n[^\n]+)*\n\n *---- 000000000000000c", Pattern.MULTILINE);
  private static final Pattern TB_END_PATTERN = Pattern.compile(
      "---- 0000000000000008(\n[^\n]+)*\n\n *[^-]", Pattern.MULTILINE);

  private static final Pattern GOTO_TB_0_PATTERN = Pattern.compile("goto_tb \\$0x0");
  private static final Pattern GOTO_TB_1_PATTERN = Pattern.compile("goto_tb \\$0x1");
  private static final Pattern LOOKUP_PTR_PATTERN = Pattern.compile("call lookup_tb_ptr");

  // used to retrieve the pointer of the first TB (based on the PC = 0),
  // which is later used to check whether it has been linked
  private static final Pattern TB_PTR_PATTERN = Pattern.compile(
      "Trace 0: (0x[0-9a-f]+) \\[[0-9a-f]*/0{16}/[0-9a-f]*/[0-9a-f]*]"
  );

  @TestFactory
  Stream<DynamicTest> tests() {
    return Stream.of(
        // Condition is dynamic: generate both branches and only one can use the jump slot
        // Expect: one goto_tb and one lookup_tb_ptr
        testWith(cfg_jmpslt_2dj_d(0)),
        testWith(cfg_jmpslt_2dj_d(1)),

        // Condition is static: generate only one branch
        // Expect: one goto_tb
        testWith(cfg_jmpslt_2dj_s(0)),
        testWith(cfg_jmpslt_2dj_s(1)),

        // Test that tcg instructions and runtime chain/exit behavior
        // change based on static condition

        /* nothing */
        testWith(cfg_all(0,  false, false, 0, 0, Behavior.NO_END)),
        /* direct jump */
        testWith(cfg_all(1,  true,  true,  0, 0, Behavior.CHAIN_1)),
        /* indirect jump */
        testWith(cfg_all(2,  true,  false, 1, 0, Behavior.END_NO_CHAIN)),
        /* direct + indirect jump */
        testWith(cfg_all(3,  true,  true,  1, 0, Behavior.CHAIN_1)),
        testWith(cfg_all(3,  true,  true,  1, 1, Behavior.END_NO_CHAIN)),

        /* raise */
        testWith(cfg_all(4,  true,  false, 0, 0, Behavior.END_NO_CHAIN)),
        /* direct jump + raise */
        testWith(cfg_all(5,  true,  true,  0, 0, Behavior.CHAIN_1)),
        testWith(cfg_all(5,  true,  true,  0, 1, Behavior.END_NO_CHAIN)),
        /* indirect jump + raise */
        testWith(cfg_all(6,  true,  false, 1, 0, Behavior.END_NO_CHAIN)),
        testWith(cfg_all(6,  true,  false, 1, 1, Behavior.END_NO_CHAIN)),
        /* direct + indirect jump + raise */
        testWith(cfg_all(7,  true,  true,  1, 0, Behavior.CHAIN_1)),
        testWith(cfg_all(7,  true,  true,  1, 1, Behavior.END_NO_CHAIN)),
        testWith(cfg_all(7,  true,  true,  1, 2, Behavior.END_NO_CHAIN)),

        /* static write */
        testWith(cfg_all(8,  true,  false, 0, 0, Behavior.CHAIN_0)),
        /* direct jump + static write */
        testWith(cfg_all(9,  true,  true,  0, 0, Behavior.CHAIN_1)),
        testWith(cfg_all(9,  true,  true,  0, 1, Behavior.CHAIN_0)),
        /* indirect jump + static write */
        testWith(cfg_all(10, true,  false, 1, 0, Behavior.END_NO_CHAIN)),
        testWith(cfg_all(10, true,  false, 1, 1, Behavior.CHAIN_0)),
        /* direct + indirect jump + static write */
        testWith(cfg_all(11, true,  true,  1, 0, Behavior.CHAIN_1)),
        testWith(cfg_all(11, true,  true,  1, 1, Behavior.END_NO_CHAIN)),
        testWith(cfg_all(11, true,  true,  1, 2, Behavior.CHAIN_0)),

        /* dynamic write */
        testWith(cfg_all(12, false, false, 0, 0, Behavior.END_NO_CHAIN)),
        /* direct jump + dynamic write */
        testWith(cfg_all(13, false, true,  0, 0, Behavior.CHAIN_1)),
        testWith(cfg_all(13, false, true,  0, 1, Behavior.END_NO_CHAIN)),
        /* indirect jump + dynamic write */
        testWith(cfg_all(14, false, false, 1, 0, Behavior.END_NO_CHAIN)),
        testWith(cfg_all(14, false, false, 1, 1, Behavior.END_NO_CHAIN)),
        /* direct + indirect jump + dynamic write */
        testWith(cfg_all(15, false, true,  1, 0, Behavior.CHAIN_1)),
        testWith(cfg_all(15, false, true,  1, 1, Behavior.END_NO_CHAIN)),
        testWith(cfg_all(15, false, true,  1, 2, Behavior.END_NO_CHAIN))
    );
  }

  private Config cfg_jmpslt_2dj_d(long r1) {
    return new Config(
        "jmpslt_2dj_d_" + r1,
        r1, 0,
        this::jmpslt_2dj_d,
        "op",
        trace -> {
          System.out.println(trace);
          var tcgOps = getInstrTcgOps(trace);
          assertTcgOpCount(tcgOps, GOTO_TB_1_PATTERN, 1);
          assertTcgOpCount(tcgOps, LOOKUP_PTR_PATTERN, 1);
        }
    );
  }

  private Config cfg_jmpslt_2dj_s(long a) {
    return new Config(
        "jmpslt_2dj_s_" + a,
        0, 0,
        asm -> jmpslt_2dj_s(asm, a),
        "op",
        trace -> {
          System.out.println(trace);
          var tcgOps = getInstrTcgOps(trace);
          assertTcgOpCount(tcgOps, GOTO_TB_1_PATTERN, 1);
          assertTcgOpCount(tcgOps, LOOKUP_PTR_PATTERN, 0);
        }
    );
  }

  private Config cfg_all(long a, boolean goto0, boolean goto1, int numLookupPtr, long r1,
                         Behavior expectedBehavior) {
    return new Config(
        "all_" + a + "_" + r1,
        r1, // this is the dynamic condition for the ALL instruction
        20, // this is added to PC (jump over 4 nops = +5 instr lengths = 20)
        asm -> all(asm, a),
        "op,exec",
        trace -> {
          System.out.println(trace);
          var tcgOps = getInstrTcgOps(trace);
          assertTcgOpCount(tcgOps, GOTO_TB_0_PATTERN, goto0 ? 1 : 0);
          assertTcgOpCount(tcgOps, GOTO_TB_1_PATTERN, goto1 ? 1 : 0);
          assertTcgOpCount(tcgOps, LOOKUP_PTR_PATTERN, numLookupPtr);
          assertTbBehavior(trace, expectedBehavior);
        }
    );
  }

  private String getInstrTcgOps(String trace) {
    var matcher = TCG_OPS_PATTERN.matcher(trace);
    assertTrue(matcher.find(), "Trace does not contain tcg ops for instruction");
    return matcher.group(0);
  }

  private int getPatternCount(String str, Pattern pattern) {
    var matcher = pattern.matcher(str);
    int count = 0;
    while (matcher.find()) {
      count++;
    }
    return count;
  }

  private void assertTcgOpCount(String ops, Pattern opPattern, int expectedCount) {
    assertEquals(expectedCount, getPatternCount(ops, opPattern),
        String.format("Unexpected amount of tcg ops with pattern: \"%s\"\n\nIn:\n\n%s",
            opPattern.pattern(), ops
        )
    );
  }

  private void assertTbBehavior(String trace, Behavior expected) {
    switch (expected) {
      case NO_END -> {
        assertEquals(1, getPatternCount(trace, NO_TB_END_PATTERN), "Expected tb not to end");
      }
      case END_NO_CHAIN -> {
        assertEquals(1, getPatternCount(trace, TB_END_PATTERN), "Expected tb to end");
        assertEquals(0, getPatternCount(trace, getChainPattern(getTbPtr(trace))),
            "Expected tb not to be chained");
      }
      case CHAIN_0 -> {
        assertEquals(1, getPatternCount(trace, TB_END_PATTERN), "Expected tb to end");
        assertEquals(1, getPatternCount(trace, getChain0Pattern(getTbPtr(trace))),
            "Expected tb to be chained using jump slot 0");
      }
      case CHAIN_1 -> {
        assertEquals(1, getPatternCount(trace, TB_END_PATTERN), "Expected tb to end");
        assertEquals(1, getPatternCount(trace, getChain1Pattern(getTbPtr(trace))),
            "Expected tb to be chained using jump slot 1");
      }
    }
  }

  private String getTbPtr(String trace) {
    var matcher = TB_PTR_PATTERN.matcher(trace);
    assertTrue(matcher.find(), "Cannot find execution trace of first translation block");
    return matcher.group(1);
  }

  private Pattern getChainPattern(String tbPtr) {
    return Pattern.compile("Linking TBs " + tbPtr);
  }

  private Pattern getChain0Pattern(String tbPtr) {
    return Pattern.compile("Linking TBs " + tbPtr + " index 0");
  }

  private Pattern getChain1Pattern(String tbPtr) {
    return Pattern.compile("Linking TBs " + tbPtr + " index 1");
  }

  private DynamicTest testWith(Config config) {
    return DynamicTest.dynamicTest(config.name, () -> {
      var dir = VadlFileUtils.createTempDirectory(config.name);
      createProgram(config, dir);
      runContainerTest(config, dir);
    });
  }

  private static InstructionEncoder.Operand operand(String name, long value) {
    return InstructionEncoder.Operand.of(name, value);
  }

  private void end(ProgramAssembler asm) {
    asm.text().emit("END", operand("addr", TOHOST_ADDR));
  }

  private void set_r1(ProgramAssembler asm, long val) {
    asm.text().emit("SET", operand("a", 0), operand("b", val), operand("c", 0));
  }

  private void set_r2(ProgramAssembler asm, long val) {
    asm.text().emit("SET", operand("a", 1), operand("b", val), operand("c", 0));
  }

  private void jmp(ProgramAssembler asm, String label) {
    asm.text().emitLabelRelative("SET", label, "b", operand("a", 2), operand("c", 0));
  }

  private void nop(ProgramAssembler asm) {
    asm.text().emit("NOP");
  }

  private void jmpslt_2dj_d(ProgramAssembler asm) {
    var patchOffset = asm.text().size();
    var name = asm.text().name();
    asm.text().emit32LittleEndian(0);
    asm.session().addLabelFixup(
        name, patchOffset, name, "exit",
        (context, sectionBytes) -> sectionBytes.putInt(
            context.sourceOffset(),
            asm.encoder().encode32("JMPSLT_2DJ_D",
                operand("a", context.relativeOffset()),
                operand("b", context.relativeOffset())
            )
        )
    );
  }

  private void jmpslt_2dj_s(ProgramAssembler asm, long cond) {
    var patchOffset = asm.text().size();
    var name = asm.text().name();
    asm.text().emit32LittleEndian(0);
    asm.session().addLabelFixup(
        name, patchOffset, name, "exit",
        (context, sectionBytes) -> sectionBytes.putInt(
            context.sourceOffset(),
            asm.encoder().encode32("JMPSLT_2DJ_S",
                operand("a", cond),
                operand("b", context.relativeOffset()),
                operand("c", context.relativeOffset())
            )
        )
    );
  }

  private void all(ProgramAssembler asm, long cond) {
    var patchOffset = asm.text().size();
    var name = asm.text().name();
    asm.text().emit32LittleEndian(0);
    asm.session().addLabelFixup(
        name, patchOffset, name, "exit",
        (context, sectionBytes) -> sectionBytes.putInt(
            context.sourceOffset(),
            asm.encoder().encode32("ALL",
                operand("a", cond),
                operand("b", context.relativeOffset()),
                operand("c", context.relativeOffset())
            )
        )
    );
  }

  private void createProgram(Config config, Path dir) throws IOException {
    var asm = new ProgramAssembler(ISA, ByteOrder.LITTLE_ENDIAN);

    set_r1(asm, config.r1);
    set_r2(asm, config.r2);

    config.codeGen.accept(asm);

    // insert nop gap so that jump from test code to end is not just +4
    // (that jump could be removed at some point since its redundant)
    nop(asm);
    nop(asm);
    nop(asm);
    nop(asm);

    asm.text().label("exit");
    end(asm);
    asm.text().label("loop");
    jmp(asm, "loop");

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
    Files.write(dir.resolve("elf"), builder.build());
    printAsmFile(asm);
  }

  private static void printAsmFile(ProgramAssembler asm) {
    var textBytes = asm.session().resolvedSectionBytes(".text");
    var textLabels = asm.session().labels(".text");

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
      sb.append("  ").append(DISASSEMBLER.disassemble(encoding)).append('\n');
    }
    System.out.println("Generated assembly:");
    System.out.println(sb);
  }

  private void runContainerTest(Config config, Path dir) throws IOException {
    var image = generateIssSimulator(VADL_SPEC);

    var guestElfPath = "/work/test/elf";
    var guestOutputDir = "/work/test/out";
    var traceFile = "trace.log";

    var runCommand = String.format(
        "mkdir %s && /qemu/build/qemu-system-test -d %s -nographic -D %s -bios %s",
        guestOutputDir,
        config.debugArgs,
        guestOutputDir + "/" + traceFile,
        guestElfPath
    );

    runContainer(image, container -> container
            .withCopyFileToContainer(MountableFile.forHostPath(dir.resolve("elf")), guestElfPath)
            .withCommand("/bin/bash", "-c", runCommand),
        container -> copyPathFromContainer(container, guestOutputDir, dir)
    );

    config.checker.accept(Files.readString(dir.resolve(traceFile)));
  }

  private static String readSpecSource() {
    try {
      return Files.readString(Path.of("test/resources/testSource/" + VADL_SPEC));
    } catch (IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private record Config(
      String name,
      long r1,
      long r2,
      Consumer<ProgramAssembler> codeGen,
      String debugArgs,
      Consumer<String> checker
  ) { }

  private enum Behavior {
    /**
     * The TB does not end.
     */
    NO_END,

    /**
     * The TB ends but is not chained.
     */
    END_NO_CHAIN,

    /**
     * The TB ends and jump slot 0 is used to chain.
     */
    CHAIN_0,

    /**
     * The TB ends and jump slot 1 is used to chain.
     */
    CHAIN_1
  }
}
