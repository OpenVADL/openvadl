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

package vadl.iss.riscv;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.jqwik.api.Arbitrary;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.TestMethodOrder;
import vadl.TestUtils;
import vadl.iss.IssTestUtils;

/**
 * Tests the RV64D instructions set.
 *
 * <p>Tests each instruction (single and double) with combinations of float variants
 * (e.g. {@code fadd.d ..., <sNaN value>, <normal value>}). All tests are performed using
 * rounding mode {@code rne}. Also compares the {@code fcsr} register, in addition to general
 * purpose float registers.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IssRV64DInstrTest extends AbstractIssRiscv64InstrTest {

  static final Map<String, String> GDB_REF_MAP_FLOAT = new HashMap<>();
  private static final String VADL_SPEC = "sys/risc-v/rv64d.vadl";
  // Note: due to the grid testing we have (4^argc+3^argc) tests per instr
  //       e.g. fmadd (takes 3 float args) gets (4^3+3^3)=91 tests
  //       because of that, this number is kept low
  private static final int TESTS_PER_INSTRUCTION = 1;
  private static final ArgType X_REG_TYPE = ArgType.I64;
  // The test linker places .text.init at 0x80000000 and aligns .tohost to the next 4 KiB page.
  // Keep randomized load/store scratch addresses above that reserved startup/HTIF region.
  private static final BigInteger TEST_DATA_MIN_ADDR = BigInteger.valueOf(0x80002000L);
  private static final BigInteger TEST_DATA_MAX_ADDR = BigInteger.valueOf(0x800F0000L);

  @Override
  public Map<String, String> gdbRegMap() {
    return GDB_REF_MAP_FLOAT;
  }

  @Override
  public int getTestPerInstruction() {
    return TESTS_PER_INSTRUCTION;
  }

  @Override
  public String getVadlSpec() {
    return VADL_SPEC;
  }

  @Override
  public Tool simulator() {
    return new Tool("/qemu/build/qemu-system-rv64id", "-bios");
  }

  @Override
  public Tool reference() {
    return new Tool("/qemu/build/qemu-system-riscv64", "-M spike -bios");
  }

  @Override
  public Tool compiler() {
    return new Tool("/scripts/compilers/riscv_compiler.py", "-march=rv64imd -mabi=lp64");
  }

  @Override
  public RV64IMVDTestBuilder getBuilder(String testNamePrefix, int id) {
    return new RV64IMVDTestBuilder(testNamePrefix + "_" + id);
  }

  static {
    GDB_REF_MAP_FLOAT.put("f0", "ft0");
    GDB_REF_MAP_FLOAT.put("f1", "ft1");
    GDB_REF_MAP_FLOAT.put("f2", "ft2");
    GDB_REF_MAP_FLOAT.put("f3", "ft3");
    GDB_REF_MAP_FLOAT.put("f4", "ft4");
    GDB_REF_MAP_FLOAT.put("f5", "ft5");
    GDB_REF_MAP_FLOAT.put("f6", "ft6");
    GDB_REF_MAP_FLOAT.put("f7", "ft7");
    GDB_REF_MAP_FLOAT.put("f8", "fs0");
    GDB_REF_MAP_FLOAT.put("f9", "fs1");
    GDB_REF_MAP_FLOAT.put("f10", "fa0");
    GDB_REF_MAP_FLOAT.put("f11", "fa1");
    GDB_REF_MAP_FLOAT.put("f12", "fa2");
    GDB_REF_MAP_FLOAT.put("f13", "fa3");
    GDB_REF_MAP_FLOAT.put("f14", "fa4");
    GDB_REF_MAP_FLOAT.put("f15", "fa5");
    GDB_REF_MAP_FLOAT.put("f16", "fa6");
    GDB_REF_MAP_FLOAT.put("f17", "fa7");
    GDB_REF_MAP_FLOAT.put("f18", "fs2");
    GDB_REF_MAP_FLOAT.put("f19", "fs3");
    GDB_REF_MAP_FLOAT.put("f20", "fs4");
    GDB_REF_MAP_FLOAT.put("f21", "fs5");
    GDB_REF_MAP_FLOAT.put("f22", "fs6");
    GDB_REF_MAP_FLOAT.put("f23", "fs7");
    GDB_REF_MAP_FLOAT.put("f24", "fs8");
    GDB_REF_MAP_FLOAT.put("f25", "fs9");
    GDB_REF_MAP_FLOAT.put("f26", "fs10");
    GDB_REF_MAP_FLOAT.put("f27", "fs11");
    GDB_REF_MAP_FLOAT.put("f28", "ft8");
    GDB_REF_MAP_FLOAT.put("f29", "ft9");
    GDB_REF_MAP_FLOAT.put("f30", "ft10");
    GDB_REF_MAP_FLOAT.put("f31", "ft11");

    GDB_REF_MAP_FLOAT.put("fcsr", "fcsr");
  }

  private enum ArgType {
    I32(true, 32),
    I64(true, 64),
    F32(false, 32),
    F64(false, 64);

    final boolean integer;
    final int size;

    ArgType(boolean integer, int size) {
      this.integer = integer;
      this.size = size;
    }
  }

  private enum FloatVariant {
    NORM(TestUtils::arbitraryNormalFloat),
    SUBN(TestUtils::arbitrarySubnormalFloat),
    ZERO(TestUtils::arbitraryZeroFloat),
    INF(TestUtils::arbitraryInfFloat),
    SNAN(fmt -> TestUtils.arbitrarySQNaNFloat(fmt, false)),
    QNAN(fmt -> TestUtils.arbitrarySQNaNFloat(fmt, true));

    final Function<TestUtils.FloatFormat, Arbitrary<BigInteger>> supplier;

    FloatVariant(Function<TestUtils.FloatFormat, Arbitrary<BigInteger>> supplier) {
      this.supplier = supplier;
    }

    // values for tests between non-NaN values
    static FloatVariantList dataValues() {
      return new FloatVariantList(List.of(NORM, SUBN, ZERO, INF), "DATA");
    }

    // values for tests with NaN values
    static FloatVariantList nanValues() {
      return new FloatVariantList(List.of(NORM, SNAN, QNAN), "NAN");
    }
  }

  private static class FloatVariantList {
    public final List<FloatVariant> variants;
    public final String name;

    private FloatVariantList(List<FloatVariant> variants, String name) {
      this.variants = variants;
      this.name = name;
    }

    public String suffix() {
      return "_%s_%s".formatted(
          name,
          variants.stream().map(FloatVariant::name).collect(Collectors.joining("_"))
      );
    }

    public void forEachProduct(int dim, Consumer<FloatVariantList> consumer) {
      forEachProduct(variants, dim, v -> consumer.accept(new FloatVariantList(List.copyOf(v), name)));
    }

    // with set={A,B} and dim=2, consumer gets called with (A,A), (A,B), (B,A) and (B,B)
    private <T> void forEachProduct(List<T> set, int dim, Consumer<List<T>> consumer) {
      if (dim == 0) {
        consumer.accept(new ArrayList<>());
        return;
      }
      forEachProduct(set, dim - 1, l -> {
        for (var e : set) {
          l.add(e);
          consumer.accept(l);
          l.removeLast();
        }
      });
    }
  }

  @Test
  @Order(1)
  void setupInstructionTests() throws IOException {
    initializeInstructionBatchFromTestCases(this::buildInstructionTestCases, this::getInstructionName);
  }

  @TestFactory
  @Order(2)
  Stream<DynamicNode> buildInstructionTests() throws IOException {
    initializeInstructionBatchFromTestCases(this::buildInstructionTestCases, this::getInstructionName);
    return buildInstructionTestContainers();
  }

  private String getInstructionName(IssTestUtils.TestCase testCase) {
    // return name before any "_" or "." (e.g. "FMV.D.X_12" -> "fmv")
    var separator = testCase.id().indexOf('_');
    var instructionId = separator <= 0 ? testCase.id() : testCase.id().substring(0, separator);
    var dot = instructionId.indexOf('.');
    return (dot >= 0 ? instructionId.substring(0, dot) : instructionId).toLowerCase();
  }

  private List<IssTestUtils.TestCase> buildInstructionTestCases() {
    var tests = new java.util.ArrayList<IssTestUtils.TestCase>();
    tests.addAll(gridTestArithInstr("fsqrt", 1));

    tests.addAll(gridTestArithInstr("fadd", 2));
    tests.addAll(gridTestArithInstr("fsub", 2));
    tests.addAll(gridTestArithInstr("fmul", 2));
    tests.addAll(gridTestArithInstr("fdiv", 2));

    tests.addAll(gridTestArithInstr("fmadd", 3));
    tests.addAll(gridTestArithInstr("fmsub", 3));
    tests.addAll(gridTestArithInstr("fnmadd", 3));
    tests.addAll(gridTestArithInstr("fnmsub", 3));

    tests.addAll(gridTestArithInstr("fmin", 2));
    tests.addAll(gridTestArithInstr("fmax", 2));

    tests.addAll(testCvtInstr());
    tests.addAll(testMvInstr());

    tests.addAll(testCmpSgnjInstr("fle", true));
    tests.addAll(testCmpSgnjInstr("flt", true));
    tests.addAll(testCmpSgnjInstr("feq", true));

    tests.addAll(testCmpSgnjInstr("fsgnj", false));
    tests.addAll(testCmpSgnjInstr("fsgnjn", false));
    tests.addAll(testCmpSgnjInstr("fsgnjx", false));

    tests.addAll(testClassInstr());
    tests.addAll(testLdStInstr());

    return tests;
  }

  private List<IssTestUtils.TestCase> gridTestArithInstr(String instruction, int instrArgc) {
    List<Function<Integer, IssTestUtils.TestCase>> generators = new ArrayList<>();
    Consumer<FloatVariantList> variantTester = variants -> {
      generators.add(testArithInstr(instruction, instrArgc, true, variants));
      generators.add(testArithInstr(instruction, instrArgc, false, variants));
    };
    FloatVariant.dataValues().forEachProduct(instrArgc, variantTester);
    FloatVariant.nanValues().forEachProduct(instrArgc, variantTester);
    return buildTestsWith(generators);
  }

  private List<IssTestUtils.TestCase> testCvtInstr() {
    List<Function<Integer, IssTestUtils.TestCase>> generators = new ArrayList<>();

    // int to float
    generators.add(cvtIF(false, false, false));
    generators.add(cvtIF(true,  false, false));
    generators.add(cvtIF(false, true,  false));
    generators.add(cvtIF(true,  true,  false));
    generators.add(cvtIF(false, false, true));
    generators.add(cvtIF(true,  false, true));
    generators.add(cvtIF(false, true,  true));
    generators.add(cvtIF(true,  true,  true));

    for (FloatVariant variant : FloatVariant.values()) {
      // float to int
      generators.add(cvtFI(variant, false, false, false));
      generators.add(cvtFI(variant, true,  false, false));
      generators.add(cvtFI(variant, false, true,  false));
      generators.add(cvtFI(variant, true,  true,  false));
      generators.add(cvtFI(variant, false, false, true));
      generators.add(cvtFI(variant, true,  false, true));
      generators.add(cvtFI(variant, false, true,  true));
      generators.add(cvtFI(variant, true,  true,  true));

      // float to float
      generators.add(cvtFF(variant, false));
      generators.add(cvtFF(variant, true));
    }
    return buildTestsWith(generators);
  }

  private List<IssTestUtils.TestCase> testMvInstr() {
    List<Function<Integer, IssTestUtils.TestCase>> generators = new ArrayList<>();

    // int to float
    generators.add(mvIF(false));
    generators.add(mvIF(true));

    for (FloatVariant variant : FloatVariant.values()) {
      // float to int
      generators.add(mvFI(false, variant));
      generators.add(mvFI(true, variant));
    }
    return buildTestsWith(generators);
  }

  private List<IssTestUtils.TestCase> testClassInstr() {
    List<Function<Integer, IssTestUtils.TestCase>> generators = new ArrayList<>();
    for (FloatVariant variant : FloatVariant.values()) {
      generators.add(fclass(false, variant));
      generators.add(fclass(true, variant));
    }
    return buildTestsWith(generators);
  }

  private List<IssTestUtils.TestCase> testLdStInstr() {
    List<Function<Integer, IssTestUtils.TestCase>> generators = new ArrayList<>();
    for (FloatVariant variant : FloatVariant.values()) {
      generators.add(ldSt(false, false, variant));
      generators.add(ldSt(true,  false, variant));
      generators.add(ldSt(false, true,  variant));
      generators.add(ldSt(true,  true,  variant));
    }
    return buildTestsWith(generators);
  }

  private List<IssTestUtils.TestCase> testCmpSgnjInstr(String instruction, boolean isCmp) {
    List<Function<Integer, IssTestUtils.TestCase>> generators = new ArrayList<>();
    Consumer<FloatVariantList> variantTester = variants -> {
      generators.add(cmpSgnj(instruction, false, isCmp, variants));
      generators.add(cmpSgnj(instruction, true, isCmp, variants));
    };
    FloatVariant.dataValues().forEachProduct(2, variantTester);
    FloatVariant.nanValues().forEachProduct(2, variantTester);
    return buildTestsWith(generators);
  }

  Function<Integer, IssTestUtils.TestCase> mvIF(boolean single) {
    var instrName = "fmv.%s.x".formatted(single ? "w" : "d");
    return id -> testInstr(getBuilder(instrName.toUpperCase(), id),
        instrName, 1, X_REG_TYPE, single ? ArgType.F32 : ArgType.F64, null);
  }

  Function<Integer, IssTestUtils.TestCase> mvFI(boolean single, FloatVariant variant) {
    var instrName = "fmv.x.%s".formatted(single ? "w" : "d");
    return id -> testInstr(getBuilder(instrName.toUpperCase() + "_" + variant.name(), id),
        instrName, 1, single ? ArgType.F32 : ArgType.F64, X_REG_TYPE, List.of(variant));
  }

  Function<Integer, IssTestUtils.TestCase> cvtIF(boolean inSingle, boolean outSingle,
                                                 boolean unsigned) {
    var instrName = "fcvt.%s.%s%s"
        .formatted(outSingle ? "s" : "d", inSingle ? "w" : "l", unsigned ? "u" : "");
    var inType = inSingle ? ArgType.I32 : ArgType.I64;
    var outType = outSingle ? ArgType.F32 : ArgType.F64;
    return id -> testInstr(getBuilder(instrName.toUpperCase(), id),
        instrName, 1, inType, outType, null);
  }

  Function<Integer, IssTestUtils.TestCase> cvtFI(FloatVariant variant, boolean inSingle,
                                                 boolean outSingle, boolean unsigned) {
    var instrName = "fcvt.%s%s.%s"
        .formatted(outSingle ? "w" : "l", unsigned ? "u" : "", inSingle ? "s" : "d");
    var inType = inSingle ? ArgType.F32 : ArgType.F64;
    var outType = outSingle ? ArgType.I32 : ArgType.I64;
    return id -> testInstr(getBuilder(instrName.toUpperCase() + "_" + variant.name(), id),
        instrName, 1, inType, outType, List.of(variant));
  }

  Function<Integer, IssTestUtils.TestCase> cvtFF(FloatVariant variant, boolean inSingle) {
    var instrName = "fcvt.%s.%s".formatted(inSingle ? "d" : "s", inSingle ? "s" : "d");
    var inType = inSingle ? ArgType.F32 : ArgType.F64;
    var outType = inSingle ? ArgType.F64 : ArgType.F32;
    return id -> testInstr(getBuilder(instrName.toUpperCase() + "_" + variant.name(), id),
        instrName, 1, inType, outType, List.of(variant));
  }

  Function<Integer, IssTestUtils.TestCase> cmpSgnj(String instr, boolean single, boolean isCmp,
                                                   FloatVariantList variants) {
    var instrName = "%s.%s".formatted(instr, single ? "s" : "d");
    var inType = single ? ArgType.F32 : ArgType.F64;
    return id -> testInstr(getBuilder(instrName.toUpperCase() + variants.suffix(), id),
        instrName, 2, inType, isCmp ? X_REG_TYPE : inType, variants.variants);
  }

  Function<Integer, IssTestUtils.TestCase> fclass(boolean single, FloatVariant variant) {
    var instrName = "fclass.%s".formatted(single ? "s" : "d");
    return id -> testInstr(getBuilder(instrName.toUpperCase() + "_" + variant.name(), id),
        instrName, 1, single ? ArgType.F32 : ArgType.F64, X_REG_TYPE, List.of(variant));
  }

  Function<Integer, IssTestUtils.TestCase> testArithInstr(String instr, int argc, boolean single,
                                                          FloatVariantList variants) {
    var instrName = "%s.%s".formatted(instr, single ? "s" : "d");
    var argType = single ? ArgType.F32 : ArgType.F64;
    return id -> testInstr(getBuilder(instrName.toUpperCase() + variants.suffix(), id),
        instrName, argc, argType, argType, variants.variants);
  }

  private IssTestUtils.TestCase testInstr(RV64IMVDTestBuilder b, String instr, int argc,
                                          ArgType inType, ArgType outType,
                                          List<FloatVariant> variants) {
    var regsSrc = (inType.integer ? b.anyTempReg() : b.anyTempFloatReg())
        .list().ofSize(argc).uniqueElements().sample();
    var regDest = (outType.integer ? b.anyTempReg() : b.anyTempFloatReg()).sample();

    b.configureCpuForFloatOps("x1");

    if (inType.integer) {
      for (int i = 0; i < argc; i++) {
        b.fillRegSigned(regsSrc.get(i), inType.size);
      }
    } else {
      var single = inType == ArgType.F32;
      var floatFormat = single ? TestUtils.IEEE_BINARY_32 : TestUtils.IEEE_BINARY_64;
      for (int i = 0; i < argc; i++) {
        b.fillFloatReg(regsSrc.get(i), variants.get(i).supplier.apply(floatFormat).sample(), single);
      }
    }

    b.add("%s %s, %s", instr, regDest, String.join(", ", regsSrc));
    return b.toTestCase(Stream.concat(regsSrc.stream(), Stream.of(regDest)).toArray(String[]::new));
  }

  private int calculateAlignment(int dataSizeInBits) {
    if (dataSizeInBits <= 0) {
      throw new IllegalArgumentException("Data size must be a positive integer.");
    }

    int dataSizeInBytes = (dataSizeInBits + 7) / 8; // Convert bits to bytes, rounding up
    return Integer.highestOneBit(dataSizeInBytes);
  }

  Function<Integer, IssTestUtils.TestCase> ldSt(boolean single, boolean testLoad,
                                                FloatVariant variant) {
    String suffix = single ? "w" : "d";
    // the thing we test is a float st/ld. The other one is a regular ld/st
    String loadInstr = "%sl%s".formatted(testLoad ? "f" : "", suffix);
    String storeInstr = "%ss%s".formatted(testLoad ? "" : "f", suffix);
    return id -> testLoadStoreInstr(
        getBuilder((testLoad ? loadInstr : storeInstr).toUpperCase() + "_" + variant.name(), id),
        single, testLoad, storeInstr, loadInstr, variant);
  }

  private IssTestUtils.TestCase testLoadStoreInstr(RV64IMVDTestBuilder b, boolean single,
                                                   boolean testLoad,
                                                   String storeInstr, String loadInstr,
                                                   FloatVariant variant) {
    b.configureCpuForFloatOps("x1");
    // if we test the ld, the store is regular and thus needs an int reg
    var storeReg = (testLoad ? b.anyTempReg() : b.anyTempFloatReg()).sample();
    var floatFormat = single ? TestUtils.IEEE_BINARY_32 : TestUtils.IEEE_BINARY_64;
    if (testLoad) {
      b.fillReg(storeReg, variant.supplier.apply(floatFormat).sample());
    } else {
      b.fillFloatReg(storeReg, variant.supplier.apply(floatFormat).sample(), single);
    }
    // the address is always in an int reg
    var addrReg = b.anyTempReg().sample();
    b.fillReg(addrReg, TEST_DATA_MIN_ADDR, TEST_DATA_MAX_ADDR,
        calculateAlignment(single ? 32 : 64));
    b.add("%s %s, 0(%s)", storeInstr, storeReg, addrReg);
    // if we test the ld, it is a float load and thus needs a float reg
    var loadReg = (testLoad ? b.anyTempFloatReg() : b.anyTempReg()).sample();
    b.add("%s %s, 0(%s)", loadInstr, loadReg, addrReg);
    return b.toTestCase(storeReg, loadReg, addrReg);
  }
}
